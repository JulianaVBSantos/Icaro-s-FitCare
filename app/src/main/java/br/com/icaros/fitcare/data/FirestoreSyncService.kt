package br.com.icaros.fitcare.data

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreSyncService(
    private val usuarioDao: UsuarioDao,
    private val database: FitCareDatabase,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "FirestoreSyncService"

        private const val COLECAO_ALUNOS = "alunos"
        private const val TIPO_ALUNO = "ALUNO"

        private const val INSERT = "INSERT"
        private const val UPDATE = "UPDATE"
        private const val DELETE = "DELETE"

        private const val PERFIL_PROFESSOR = "PROFESSOR"
        private const val PERFIL_ALUNO = "ALUNO"
    }

    suspend fun validarSessao(): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                Log.e(TAG, "Não existe usuário autenticado no Firebase.")
                return false
            }

            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(firebaseUser.uid)

            if (usuarioLocal == null || !usuarioLocal.ativo) {
                Log.e(TAG, "Usuário local inexistente ou inativo.")
                return false
            }

            if (usuarioLocal.perfil != PERFIL_PROFESSOR) {
                Log.e(TAG, "Perfil inválido: ${usuarioLocal.perfil}")
                return false
            }

            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar sessão Firebase.", e)
            false
        }
    }

    fun obterUidAutenticado(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun obterFirestoreAutenticado(): FirebaseFirestore {
        return firestore
    }


    suspend fun sincronizarAlunos(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid

        return try {
            val usuarioLocal = usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuarioLocal == null ||
                !usuarioLocal.ativo ||
                usuarioLocal.perfil != PERFIL_PROFESSOR
            ) {
                return false
            }

            val syncDao = database.syncOperationDao()
            val alunoDao = database.alunoDao()

            val operacoes = syncDao.listarPendentes()
                .filter { it.entityType == TIPO_ALUNO }

            var todasConcluidas = true

            for (operacao in operacoes) {
                try {
                    val remoteDocumentId = operacao.remoteDocumentId
                        ?: throw IllegalStateException(
                            "Operação ${operacao.id} sem remoteDocumentId."
                        )

                    val documento = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(remoteDocumentId)

                    when (operacao.operationType.uppercase()) {
                        INSERT, UPDATE -> {
                            val aluno = alunoDao.buscarPorIdDireto(
                                operacao.localEntityId
                            ) ?: throw IllegalStateException(
                                "Aluno local ${operacao.localEntityId} não encontrado."
                            )

                            if (aluno.professorId != usuarioLocal.id) {
                                throw SecurityException(
                                    "Aluno ${aluno.id} não pertence ao professor."
                                )
                            }

                            if (aluno.firebaseId != remoteDocumentId) {
                                throw IllegalStateException(
                                    "firebaseId não corresponde ao documento remoto."
                                )
                            }

                            // Recupera o vínculo Firebase da conta do aluno,
                            // caso ele também esteja cadastrado neste Room.
                            val usuarioAluno = usuarioDao.buscarPorAlunoId(aluno.id)

                            val alunoAuthUid = usuarioAluno
                                ?.takeIf {
                                    it.perfil == PERFIL_ALUNO &&
                                            it.ativo &&
                                            it.email.equals(aluno.email, ignoreCase = true)
                                }
                                ?.firebaseUid
                                ?.takeIf { it.isNotBlank() }

                            val dadosRemotos = hashMapOf<String, Any>(
                                "firebaseId" to remoteDocumentId,
                                "professorUid" to uid,
                                "nome" to aluno.nome,
                                "email" to aluno.email,
                                "dataCadastro" to aluno.dataCadastro,
                                "objetivo" to aluno.objetivo
                            )

                            // Só grava o UID quando existe um vínculo local
                            // identificado e validado.
                            if (alunoAuthUid != null) {
                                dadosRemotos["alunoAuthUid"] = alunoAuthUid
                            }

                            // Merge preserva campos que não fazem parte
                            // desta operação, inclusive vínculos já existentes.
                            documento.set(
                                dadosRemotos,
                                SetOptions.merge()
                            ).await()

                            val confirmado = documento.get(
                                com.google.firebase.firestore.Source.SERVER
                            ).await()

                            if (!confirmado.exists()) {
                                throw IllegalStateException(
                                    "Documento não encontrado após sincronização."
                                )
                            }

                            if (
                                confirmado.getString("nome") != aluno.nome ||
                                confirmado.getString("professorUid") != uid
                            ) {
                                throw IllegalStateException(
                                    "Os dados confirmados no servidor divergem dos dados locais."
                                )
                            }

                            if (
                                alunoAuthUid != null &&
                                confirmado.getString("alunoAuthUid") != alunoAuthUid
                            ) {
                                throw IllegalStateException(
                                    "O UID Firebase do aluno não foi confirmado no servidor."
                                )
                            }
                        }

                        DELETE -> {
                            documento.delete().await()
                        }

                        else -> {
                            throw IllegalArgumentException(
                                "Tipo de operação desconhecido: ${operacao.operationType}"
                            )
                        }
                    }

                    syncDao.remover(operacao.id)

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    todasConcluidas = false

                    Log.e(
                        TAG,
                        "Falha na operação ${operacao.id}.",
                        e
                    )

                    try {
                        syncDao.registrarFalha(
                            operacao.id,
                            e.message ?: e.javaClass.simpleName
                        )
                    } catch (erroRegistro: Exception) {
                        Log.e(
                            TAG,
                            "Não foi possível registrar a falha.",
                            erroRegistro
                        )
                    }
                }
            }

            todasConcluidas

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar alunos.", e)
            false
        }
    }

    suspend fun sincronizarLeituraAlunos(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid

        return try {
            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuarioLocal == null ||
                !usuarioLocal.ativo ||
                usuarioLocal.perfil != PERFIL_PROFESSOR
            ) {
                return false
            }

            val resultado = firestore
                .collection(COLECAO_ALUNOS)
                .whereEqualTo("professorUid", uid)
                .get()
                .await()

            val alunoDao = database.alunoDao()
            val syncDao = database.syncOperationDao()

            for (documento in resultado.documents) {
                val firebaseId = documento.getString("firebaseId")
                    ?: documento.id

                val alunoExistente =
                    alunoDao.buscarPorFirebaseId(firebaseId)

                if (alunoExistente != null) {
                    val operacoesPendentes =
                        syncDao.buscarPorRegistro(
                            TIPO_ALUNO,
                            alunoExistente.id
                        )

                    if (operacoesPendentes.isNotEmpty()) {
                        continue
                    }

                    alunoDao.atualizar(
                        alunoExistente.copy(
                            nome = documento.getString("nome") ?: "",
                            email = documento.getString("email") ?: "",
                            dataCadastro = documento.getString("dataCadastro") ?: "",
                            objetivo = documento.getString("objetivo") ?: "",
                            status = documento.getString("status") ?: "",
                            observacoesProfissionais =
                                documento.getString("observacoesProfissionais") ?: "",
                            condicoes = documento.getString("condicoes") ?: "",
                            restricoes = documento.getString("restricoes") ?: ""
                        )
                    )
                } else {
                    alunoDao.inserir(
                        AlunoEntity(
                            nome = documento.getString("nome") ?: "",
                            email = documento.getString("email") ?: "",
                            dataCadastro = documento.getString("dataCadastro") ?: "",
                            objetivo = documento.getString("objetivo") ?: "",
                            status = documento.getString("status") ?: "",
                            observacoesProfissionais =
                                documento.getString("observacoesProfissionais") ?: "",
                            condicoes = documento.getString("condicoes") ?: "",
                            restricoes = documento.getString("restricoes") ?: "",
                            professorId = usuarioLocal.id,
                            firebaseId = firebaseId
                        )
                    )
                }
            }

            true

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao receber alunos do Firestore. UID=$uid", e)
            false
        }
    }

    suspend fun sincronizarModelosAnamnese(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid

        return try {
            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuarioLocal == null ||
                !usuarioLocal.ativo ||
                usuarioLocal.perfil != PERFIL_PROFESSOR
            ) {
                return false
            }

            val modeloDao = database.modeloAnamneseDao()
            val perguntaDao = database.perguntaModeloAnamneseDao()

            val colecao = firestore
                .collection("usuarios")
                .document(uid)
                .collection("modelos_anamnese")

            val modelosLocais =
                modeloDao.listarModelosParaSincronizacao(usuarioLocal.id)

            var sucesso = true

            for (modelo in modelosLocais) {
                try {
                    val firestoreId = modelo.firestoreId
                        ?: throw IllegalStateException(
                            "Modelo ${modelo.id} sem firestoreId."
                        )

                    val documento = colecao.document(firestoreId)
                    val remoto = documento.get().await()

                    val remotoMillis =
                        remoto.getLong("updatedAtMillis") ?: 0L

                    if (
                        remoto.exists() &&
                        remotoMillis > modelo.updatedAtMillis
                    ) {
                        importarModeloRemoto(
                            remoto,
                            usuarioLocal.id,
                            modeloDao,
                            perguntaDao
                        )
                        continue
                    }

                    val perguntas =
                        perguntaDao.buscarPorModeloDoProfessor(
                            modelo.id,
                            usuarioLocal.id
                        )

                    val perguntasRemotas = perguntas.map {
                        hashMapOf(
                            "texto" to it.texto,
                            "tipoResposta" to it.tipoResposta,
                            "opcoes" to it.opcoes,
                            "obrigatoria" to it.obrigatoria,
                            "ordem" to it.ordem
                        )
                    }

                    val dados = hashMapOf(
                        "ownerUid" to uid,
                        "firestoreId" to firestoreId,
                        "nome" to modelo.nome,
                        "descricao" to modelo.descricao,
                        "categoria" to modelo.categoria,
                        "ativo" to modelo.ativo,
                        "dataCriacao" to modelo.dataCriacao,
                        "dataAtualizacao" to modelo.dataAtualizacao,
                        "updatedAtMillis" to modelo.updatedAtMillis,
                        "deleted" to modelo.excluido,
                        "deletedAtMillis" to modelo.deletedAtMillis,
                        "perguntas" to perguntasRemotas
                    )

                    documento.set(dados, SetOptions.merge()).await()

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(TAG, "Erro ao enviar modelo ${modelo.id}.", e)
                }
            }

            val resultado = colecao.get().await()

            for (documento in resultado.documents) {
                try {
                    val categoria = documento.getString("categoria")

                    if (
                        documento.getString("ownerUid") != uid ||
                        categoria !in listOf("PADRAO", "PERSONALIZADO")
                    ) {
                        continue
                    }

                    val firestoreId =
                        documento.getString("firestoreId") ?: documento.id

                    val local =
                        modeloDao.buscarPorFirestoreId(firestoreId)

                    val remotoMillis =
                        documento.getLong("updatedAtMillis") ?: 0L

                    if (local == null || remotoMillis > local.updatedAtMillis) {
                        importarModeloRemoto(
                            documento,
                            usuarioLocal.id,
                            modeloDao,
                            perguntaDao
                        )
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(TAG, "Erro ao importar modelo ${documento.id}.", e)
                }
            }

            sucesso

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar modelos.", e)
            false
        }
    }

    /**
     * Sincroniza as anamneses atribuídas pelo professor.
     * Envia as atribuições e perguntas e recebe as respostas dos alunos.
     */
    suspend fun sincronizarAnamnesesAtribuidas(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid

        return try {
            val usuario = usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuario == null ||
                !usuario.ativo ||
                usuario.perfil != PERFIL_PROFESSOR
            ) {
                return false
            }

            val anamneseDao = database.anamneseAtribuidaDao()
            val perguntaDao = database.perguntaAnamneseAtribuidaDao()
            val respostaDao = database.respostaAnamneseDao()
            val alunoDao = database.alunoDao()

            var sucesso = true

            val anamneses =
                anamneseDao.listarTodasParaSincronizacaoDoProfessor(usuario.id)

            for (anamnese in anamneses) {
                if (anamnese.status.equals("NAO_ENVIADA", ignoreCase = true)) {
                    continue
                }

                try {
                    val aluno = alunoDao.buscarPorIdDireto(anamnese.alunoId)
                        ?: throw IllegalStateException("Aluno não encontrado.")

                    if (aluno.professorId != usuario.id) {
                        throw SecurityException("Aluno não pertence ao professor.")
                    }

                    val alunoFirebaseId = aluno.firebaseId
                        ?: throw IllegalStateException("Aluno sem firebaseId.")

                    val documentoAluno = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(alunoFirebaseId)
                        .get()
                        .await()

                    if (!documentoAluno.exists()) {
                        throw IllegalStateException(
                            "Documento do aluno não encontrado."
                        )
                    }

                    if (documentoAluno.getString("professorUid") != uid) {
                        throw SecurityException(
                            "Documento remoto não pertence ao professor."
                        )
                    }

                    val alunoAuthUid =
                        documentoAluno.getString("alunoAuthUid")
                            ?: throw IllegalStateException(
                                "O aluno ainda não vinculou sua conta Firebase Authentication."
                            )

                    val firestoreId = anamnese.firestoreId
                        ?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString().also {
                            anamneseDao.atualizarFirestoreId(
                                anamnese.id,
                                it,
                                System.currentTimeMillis()
                            )
                        }

                    val documentoAnamnese = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(alunoFirebaseId)
                        .collection("anamneses_atribuidas")
                        .document(firestoreId)

                    val remotoExistente = documentoAnamnese.get().await()

                    val dadosAnamnese = hashMapOf<String, Any>(
                        "firestoreId" to firestoreId,
                        "professorUid" to uid,
                        "alunoAuthUid" to alunoAuthUid,
                        "alunoFirebaseId" to alunoFirebaseId,
                        "nomeModelo" to anamnese.nomeModelo,
                        "profissionalResponsavel" to anamnese.profissionalResponsavel,
                        "identificadorExterno" to anamnese.identificadorExterno,
                        "excluida" to anamnese.excluida
                    )

                    if (!remotoExistente.exists()) {
                        dadosAnamnese["status"] = anamnese.status
                        dadosAnamnese["dataCriacao"] = anamnese.dataCriacao
                        dadosAnamnese["dataEnvio"] = anamnese.dataEnvio
                        dadosAnamnese["dataInicioResposta"] =
                            anamnese.dataInicioResposta
                        dadosAnamnese["dataResposta"] = anamnese.dataResposta
                        dadosAnamnese["updatedAtMillis"] =
                            System.currentTimeMillis()
                    }

                    documentoAnamnese.set(
                        dadosAnamnese,
                        SetOptions.merge()
                    ).await()

                    val perguntas =
                        perguntaDao.listarParaSincronizacao(anamnese.id)

                    for (pergunta in perguntas) {
                        val perguntaFirestoreId = pergunta.firestoreId
                            ?.takeIf { it.isNotBlank() }
                            ?: UUID.randomUUID().toString().also {
                                perguntaDao.atribuirFirestoreId(
                                    pergunta.id,
                                    it
                                )
                            }

                        val dadosPergunta = hashMapOf(
                            "firestoreId" to perguntaFirestoreId,
                            "texto" to pergunta.texto,
                            "tipoResposta" to pergunta.tipoResposta,
                            "opcoes" to pergunta.opcoes,
                            "obrigatoria" to pergunta.obrigatoria,
                            "ordem" to pergunta.ordem
                        )

                        documentoAnamnese
                            .collection("perguntas")
                            .document(perguntaFirestoreId)
                            .set(dadosPergunta, SetOptions.merge())
                            .await()
                    }

                    // Recebe o status atualizado pelo aluno.
                    val anamneseRemota = documentoAnamnese.get().await()

                    val statusRemoto =
                        anamneseRemota.getString("status")
                            ?: anamnese.status

                    val dataInicioRemota =
                        anamneseRemota.getString("dataInicioResposta")
                            ?: anamnese.dataInicioResposta

                    val dataRespostaRemota =
                        anamneseRemota.getString("dataResposta")
                            ?: anamnese.dataResposta

                    val updatedAtRemoto =
                        anamneseRemota.getLong("updatedAtMillis") ?: 0L

                    anamneseDao.atualizarStatusSincronizado(
                        firestoreId = firestoreId,
                        status = statusRemoto,
                        dataInicioResposta = dataInicioRemota,
                        dataResposta = dataRespostaRemota,
                        updatedAtMillis = updatedAtRemoto
                    )

                    // O professor só consulta as respostas; não as altera.
                    val respostasRemotas = documentoAnamnese
                        .collection("respostas")
                        .get()
                        .await()

                    for (respostaDocumento in respostasRemotas.documents) {
                        val perguntaFirestoreId =
                            respostaDocumento.getString("perguntaFirestoreId")
                                ?: respostaDocumento.id

                        respostaDao.salvarRespostaSincronizada(
                            perguntaFirestoreId = perguntaFirestoreId,
                            resposta = respostaDocumento.getString("resposta") ?: "",
                            dataAtualizacao =
                                respostaDocumento.getString("dataAtualizacao") ?: ""
                        )
                    }

                    Log.i(
                        TAG,
                        "Anamnese do professor sincronizada: $firestoreId"
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(
                        TAG,
                        "Erro ao sincronizar anamnese ${anamnese.id}.",
                        e
                    )
                }
            }

            sucesso

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização das anamneses do professor.", e)
            false
        }
    }

    /**
     * Recebe as anamneses atribuídas ao aluno e envia suas respostas.
     *
     * O status local e o remoto são combinados para evitar que uma
     * sincronização sobrescreva um andamento mais recente.
     */
    suspend fun sincronizarAnamnesesAtribuidasDoAluno(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid

        return try {
            val usuario = usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuario == null ||
                !usuario.ativo ||
                usuario.perfil != PERFIL_ALUNO
            ) {
                Log.e(TAG, "Sincronização bloqueada: usuário não é aluno.")
                return false
            }

            val alunoId = usuario.alunoId
                ?: throw IllegalStateException("Usuário sem alunoId.")

            val aluno = database.alunoDao().buscarPorIdDireto(alunoId)
                ?: throw IllegalStateException("Aluno não encontrado no Room.")

            val alunoFirebaseId = aluno.firebaseId
                ?: throw IllegalStateException("Aluno sem firebaseId.")

            val documentoAluno = firestore
                .collection(COLECAO_ALUNOS)
                .document(alunoFirebaseId)
                .get()
                .await()

            if (!documentoAluno.exists()) {
                throw IllegalStateException(
                    "Cadastro remoto do aluno não encontrado."
                )
            }

            val emailAutenticado = firebaseUser.email
                ?: throw IllegalStateException(
                    "A conta Firebase do aluno não possui e-mail."
                )

            val professorUid = vincularContaAlunoNoFirestore(
                alunoFirebaseId = alunoFirebaseId,
                uid = uid,
                emailAutenticado = emailAutenticado
            )

            val anamneseDao = database.anamneseAtribuidaDao()
            val perguntaDao = database.perguntaAnamneseAtribuidaDao()
            val respostaDao = database.respostaAnamneseDao()

            val colecaoAnamneses = firestore
                .collection(COLECAO_ALUNOS)
                .document(alunoFirebaseId)
                .collection("anamneses_atribuidas")

            // A consulta é filtrada pelo UID para respeitar as regras
            // de leitura do Firestore para a conta do aluno.
            val resultado = colecaoAnamneses
                .whereEqualTo("alunoAuthUid", uid)
                .get()
                .await()

            var sucesso = true

            for (documento in resultado.documents) {
                try {
                    if (documento.getString("professorUid") != professorUid) {
                        continue
                    }

                    if (documento.getString("alunoAuthUid") != uid) {
                        continue
                    }

                    if (documento.getBoolean("excluida") == true) {
                        continue
                    }

                    val firestoreId =
                        documento.getString("firestoreId") ?: documento.id

                    val nomeModelo =
                        documento.getString("nomeModelo") ?: ""

                    val statusRemoto =
                        documento.getString("status") ?: "PENDENTE"

                    val dataCriacao =
                        documento.getString("dataCriacao") ?: ""

                    val dataEnvio =
                        documento.getString("dataEnvio") ?: ""

                    val dataInicioRemota =
                        documento.getString("dataInicioResposta") ?: ""

                    val dataRespostaRemota =
                        documento.getString("dataResposta") ?: ""

                    val profissionalResponsavel =
                        documento.getString("profissionalResponsavel") ?: ""

                    val identificadorExterno =
                        documento.getString("identificadorExterno") ?: ""

                    val updatedAtMillis =
                        documento.getLong("updatedAtMillis") ?: 0L

                    // Guarda o estado local ANTES de importar o remoto.
                    val existente =
                        anamneseDao.buscarPorFirestoreId(firestoreId)

                    val statusLocal = existente?.status
                    val dataInicioLocal = existente?.dataInicioResposta ?: ""
                    val dataRespostaLocal = existente?.dataResposta ?: ""

                    val statusFinal = combinarStatus(
                        statusLocal,
                        statusRemoto
                    )

                    val dataInicioFinal =
                        escolherDataMaisCompleta(
                            dataInicioLocal,
                            dataInicioRemota
                        )

                    val dataRespostaFinal =
                        escolherDataMaisCompleta(
                            dataRespostaLocal,
                            dataRespostaRemota
                        )

                    val anamneseLocalId = if (existente == null) {
                        anamneseDao.inserirRecebidaDoFirestore(
                            AnamneseAtribuidaEntity(
                                id = 0L,
                                alunoId = alunoId,
                                modeloId = 0L,
                                nomeModelo = nomeModelo,
                                status = statusFinal,
                                dataCriacao = dataCriacao,
                                dataEnvio = dataEnvio,
                                dataInicioResposta = dataInicioFinal,
                                dataResposta = dataRespostaFinal,
                                profissionalResponsavel =
                                    profissionalResponsavel,
                                identificadorExterno = identificadorExterno,
                                firestoreId = firestoreId,
                                updatedAtMillis = updatedAtMillis,
                                excluida = false
                            )
                        )
                    } else {
                        anamneseDao.atualizarRecebidaDoFirestore(
                            firestoreId = firestoreId,
                            nomeModelo = nomeModelo,
                            status = statusFinal,
                            dataCriacao = dataCriacao,
                            dataEnvio = dataEnvio,
                            dataInicioResposta = dataInicioFinal,
                            dataResposta = dataRespostaFinal,
                            profissionalResponsavel =
                                profissionalResponsavel,
                            identificadorExterno = identificadorExterno,
                            updatedAtMillis = maxOf(
                                existente.updatedAtMillis,
                                updatedAtMillis
                            ),
                            excluida = false
                        )

                        existente.id
                    }

                    if (anamneseLocalId <= 0L) {
                        throw IllegalStateException(
                            "Não foi possível importar a anamnese $firestoreId."
                        )
                    }

                    // Importa as perguntas.
                    val perguntasRemotas = documento.reference
                        .collection("perguntas")
                        .get()
                        .await()

                    for (perguntaDocumento in perguntasRemotas.documents) {
                        val perguntaFirestoreId =
                            perguntaDocumento.getString("firestoreId")
                                ?: perguntaDocumento.id

                        val pergunta = PerguntaAnamneseAtribuidaEntity(
                            id = 0L,
                            anamneseAtribuidaId = anamneseLocalId,
                            perguntaModeloId = null,
                            texto = perguntaDocumento.getString("texto") ?: "",
                            tipoResposta =
                                perguntaDocumento.getString("tipoResposta")
                                    ?: "TEXTO",
                            opcoes =
                                perguntaDocumento.getString("opcoes") ?: "",
                            obrigatoria =
                                perguntaDocumento.getBoolean("obrigatoria")
                                    ?: false,
                            ordem =
                                (perguntaDocumento.getLong("ordem") ?: 0L)
                                    .toInt(),
                            firestoreId = perguntaFirestoreId
                        )

                        perguntaDao.inserirOuAtualizarParaSincronizacao(
                            pergunta
                        )
                    }

                    // Recebe somente respostas pertencentes ao aluno.
                    val respostasRemotas = documento.reference
                        .collection("respostas")
                        .whereEqualTo("alunoAuthUid", uid)
                        .get()
                        .await()

                    for (respostaDocumento in respostasRemotas.documents) {
                        val perguntaFirestoreId =
                            respostaDocumento.getString("perguntaFirestoreId")
                                ?: respostaDocumento.id

                        respostaDao.salvarRespostaSincronizada(
                            perguntaFirestoreId = perguntaFirestoreId,
                            resposta =
                                respostaDocumento.getString("resposta") ?: "",
                            dataAtualizacao =
                                respostaDocumento.getString("dataAtualizacao")
                                    ?: ""
                        )
                    }

                    // Envia as respostas locais do aluno.
                    val respostasLocais =
                        respostaDao.buscarTodasPorAnamneseDoAluno(
                            anamneseLocalId,
                            alunoId
                        )

                    val perguntasLocais =
                        perguntaDao.listarParaSincronizacao(anamneseLocalId)

                    val firestoreIdPorPerguntaLocal =
                        perguntasLocais.mapNotNull { pergunta ->
                            pergunta.firestoreId
                                ?.takeIf { it.isNotBlank() }
                                ?.let { pergunta.id to it }
                        }.toMap()

                    for (respostaLocal in respostasLocais) {
                        val perguntaFirestoreId =
                            firestoreIdPorPerguntaLocal[
                                respostaLocal.perguntaAtribuidaId
                            ] ?: continue

                        val dadosResposta = hashMapOf(
                            "perguntaFirestoreId" to perguntaFirestoreId,
                            "alunoAuthUid" to uid,
                            "resposta" to respostaLocal.resposta,
                            "dataAtualizacao" to respostaLocal.dataAtualizacao
                        )

                        documento.reference
                            .collection("respostas")
                            .document(perguntaFirestoreId)
                            .set(dadosResposta, SetOptions.merge())
                            .await()
                    }

                    // Só atualiza o documento remoto se o estado local
                    // tiver um andamento mais recente ou datas preenchidas.
                    val precisaAtualizarStatus =
                        statusFinal != statusRemoto ||
                                dataInicioFinal != dataInicioRemota ||
                                dataRespostaFinal != dataRespostaRemota

                    if (precisaAtualizarStatus) {
                        val dadosStatus = hashMapOf<String, Any>(
                            "status" to statusFinal,
                            "dataInicioResposta" to dataInicioFinal,
                            "dataResposta" to dataRespostaFinal,
                            "updatedAtMillis" to System.currentTimeMillis()
                        )

                        documento.reference.set(
                            dadosStatus,
                            SetOptions.merge()
                        ).await()
                    }

                    Log.i(
                        TAG,
                        "Anamnese recebida/sincronizada pelo aluno: $firestoreId"
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(
                        TAG,
                        "Erro ao sincronizar anamnese do aluno: ${documento.id}",
                        e
                    )
                }
            }

            sucesso

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro geral na sincronização das anamneses do aluno.",
                e
            )
            false
        }
    }

    /**
     * Evita regredir o andamento de uma anamnese durante a sincronização.
     */
    private fun combinarStatus(
        statusLocal: String?,
        statusRemoto: String
    ): String {
        if (statusLocal.isNullOrBlank()) {
            return statusRemoto
        }

        fun prioridade(status: String): Int {
            return when (status.uppercase()) {
                "CONCLUIDA",
                "CONCLUÍDA",
                "FINALIZADA",
                "RESPONDIDA" -> 3

                "EM_ANDAMENTO",
                "EM ANDAMENTO" -> 2

                "PENDENTE",
                "ENVIADA" -> 1

                "NAO_ENVIADA",
                "NÃO_ENVIADA" -> 0

                else -> 0
            }
        }

        return if (prioridade(statusLocal) > prioridade(statusRemoto)) {
            statusLocal
        } else {
            statusRemoto
        }
    }

    private fun escolherDataMaisCompleta(
        local: String,
        remota: String
    ): String {
        return when {
            local.isBlank() -> remota
            remota.isBlank() -> local
            else -> {
                // As datas são mantidas como strings do formato já utilizado
                // pelo aplicativo. A data local não é descartada.
                local
            }
        }
    }

    private suspend fun importarModeloRemoto(
        documento: com.google.firebase.firestore.DocumentSnapshot,
        usuarioLocalId: Long,
        modeloDao: ModeloAnamneseDao,
        perguntaDao: PerguntaModeloAnamneseDao
    ) {
        val categoria = documento.getString("categoria")
            ?: throw IllegalStateException(
                "Modelo remoto sem categoria: ${documento.id}"
            )

        require(
            categoria == "PADRAO" || categoria == "PERSONALIZADO"
        ) {
            "Categoria inválida no modelo remoto: $categoria"
        }

        val firestoreId =
            documento.getString("firestoreId") ?: documento.id

        val nome = documento.getString("nome") ?: ""
        val descricao = documento.getString("descricao") ?: ""
        val ativo = documento.getBoolean("ativo") ?: true
        val dataCriacao = documento.getString("dataCriacao") ?: ""
        val dataAtualizacao = documento.getString("dataAtualizacao") ?: ""

        val updatedAtMillis =
            documento.getLong("updatedAtMillis") ?: 0L

        val excluido =
            documento.getBoolean("deleted") ?: false

        val deletedAtMillis =
            documento.getLong("deletedAtMillis")

        val perguntasRemotas =
            documento.get("perguntas") as? List<*> ?: emptyList<Any>()

        val perguntasConvertidas = perguntasRemotas.mapNotNull { item ->
            val mapa = item as? Map<*, *> ?: return@mapNotNull null

            PerguntaModeloAnamneseEntity(
                id = 0L,
                modeloId = 0L,
                texto = mapa["texto"] as? String ?: "",
                tipoResposta = mapa["tipoResposta"] as? String ?: "TEXTO",
                opcoes = mapa["opcoes"] as? String ?: "",
                obrigatoria = mapa["obrigatoria"] as? Boolean ?: false,
                ordem = (mapa["ordem"] as? Number)?.toInt() ?: 0
            )
        }

        database.withTransaction {
            val existente =
                modeloDao.buscarPorFirestoreId(firestoreId)

            val modeloLocal = if (existente == null) {
                val novo = ModeloAnamneseEntity(
                    id = 0L,
                    nome = nome,
                    descricao = descricao,
                    categoria = categoria,
                    ativo = ativo && !excluido,
                    dataCriacao = dataCriacao,
                    dataAtualizacao = dataAtualizacao,
                    professorId = usuarioLocalId,
                    firestoreId = firestoreId,
                    excluido = excluido,
                    updatedAtMillis = updatedAtMillis,
                    deletedAtMillis = deletedAtMillis
                )

                val novoId = modeloDao.inserir(novo)
                novo.copy(id = novoId)

            } else {
                val atualizado = existente.copy(
                    nome = nome,
                    descricao = descricao,
                    ativo = ativo && !excluido,
                    dataCriacao = dataCriacao,
                    dataAtualizacao = dataAtualizacao,
                    excluido = excluido,
                    updatedAtMillis = updatedAtMillis,
                    deletedAtMillis = deletedAtMillis
                )

                modeloDao.atualizar(atualizado)
                atualizado
            }

            perguntaDao.excluirTodasDoModelo(modeloLocal.id)

            if (!excluido && perguntasConvertidas.isNotEmpty()) {
                perguntaDao.inserirTodas(
                    perguntasConvertidas.map {
                        it.copy(
                            id = 0L,
                            modeloId = modeloLocal.id
                        )
                    }
                )
            }
        }

        Log.i(
            TAG,
            "Modelo importado: $firestoreId, excluido=$excluido"
        )
    }

    private suspend fun vincularContaAlunoNoFirestore(
        alunoFirebaseId: String,
        uid: String,
        emailAutenticado: String
    ): String {
        val documento = firestore
            .collection(COLECAO_ALUNOS)
            .document(alunoFirebaseId)

        return firestore.runTransaction { transacao ->
            val snapshot = transacao.get(documento)

            if (!snapshot.exists()) {
                throw IllegalStateException(
                    "Cadastro remoto do aluno não encontrado."
                )
            }

            val emailCadastro = snapshot.getString("email")
                ?.trim()
                ?.lowercase()
                ?: throw IllegalStateException(
                    "Cadastro remoto sem e-mail."
                )

            if (emailCadastro != emailAutenticado.trim().lowercase()) {
                throw SecurityException(
                    "O e-mail da conta autenticada não corresponde ao cadastro do aluno."
                )
            }

            val uidExistente = snapshot.getString("alunoAuthUid")

            if (!uidExistente.isNullOrBlank() && uidExistente != uid) {
                throw SecurityException(
                    "Este cadastro já está vinculado a outra conta Firebase."
                )
            }

            if (uidExistente.isNullOrBlank()) {
                transacao.update(documento, "alunoAuthUid", uid)
            }

            snapshot.getString("professorUid")
                ?: throw IllegalStateException(
                    "Cadastro remoto sem professorUid."
                )
        }.await()
    }

}
