package br.com.icaros.fitcare.data

import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlinx.coroutines.flow.first

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
            val firebaseUser = firebaseAuth.currentUser ?: return false
            val usuarioLocal = usuarioDao.buscarPorFirebaseUid(firebaseUser.uid)
            usuarioLocal != null && usuarioLocal.ativo && usuarioLocal.perfil == PERFIL_PROFESSOR
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar sessão Firebase.", e)
            false
        }
    }

    fun obterUidAutenticado(): String? = firebaseAuth.currentUser?.uid

    fun obterFirestoreAutenticado(): FirebaseFirestore = firestore

    fun iniciarMonitoramentoTempoRealProfessor(
        scope: CoroutineScope,
        onContaBloqueada: () -> Unit
    ): List<ListenerRegistration> {
        val firebaseUser = firebaseAuth.currentUser ?: return emptyList()
        val uid = firebaseUser.uid
        val registros = mutableListOf<ListenerRegistration>()
        val listenersPorAluno = mutableMapOf<String, ListenerRegistration>()

        // 1. Monitora bloqueio ou exclusão da conta do professor pelo Admin
        val regUsuario = firestore.collection("usuarios")
            .document(uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val ativo = snapshot.getBoolean("ativo") ?: false
                    val perfil = snapshot.getString("perfil")
                    if (!ativo || perfil != PERFIL_PROFESSOR) {
                        scope.launch {
                            val local = usuarioDao.buscarPorFirebaseUid(uid)
                            if (local != null) {
                                usuarioDao.atualizarStatus(local.id, false)
                            }
                            firebaseAuth.signOut()
                            SessaoUsuario.encerrar()
                            onContaBloqueada()
                        }
                    }
                }
            }
        registros.add(regUsuario)

        // 2. Monitora alterações de cadastro nos alunos e anexa listener direto na subcoleção de cada aluno
        val regAlunos = firestore.collection(COLECAO_ALUNOS)
            .whereEqualTo("professorUid", uid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    val idsAtuais = snapshots.documents.map { it.id }.toSet()

                    val removidos = listenersPorAluno.keys.filter { it !in idsAtuais }
                    removidos.forEach { idRemovido ->
                        listenersPorAluno.remove(idRemovido)?.remove()
                    }

                    for (docAluno in snapshots.documents) {
                        val alunoDocId = docAluno.id
                        if (!listenersPorAluno.containsKey(alunoDocId)) {
                            val regSub = firestore.collection(COLECAO_ALUNOS)
                                .document(alunoDocId)
                                .collection("anamneses_atribuidas")
                                .addSnapshotListener { subSnap, _ ->
                                    if (subSnap != null) {
                                        scope.launch(Dispatchers.IO) {
                                            sincronizarAnamnesesAtribuidas()
                                        }
                                    }
                                }
                            listenersPorAluno[alunoDocId] = regSub
                        }
                    }

                    scope.launch(Dispatchers.IO) {
                        sincronizarLeituraAlunos()
                    }
                }
            }
        registros.add(regAlunos)

        // 3. Monitora modelos de anamnese criados ou alterados em outro dispositivo
        val regModelos = firestore.collection("usuarios")
            .document(uid)
            .collection("modelos_anamnese")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    scope.launch(Dispatchers.IO) {
                        sincronizarModelosAnamnese()
                    }
                }
            }
        registros.add(regModelos)

        val cleanupWrapper = ListenerRegistration {
            listenersPorAluno.values.forEach { it.remove() }
            listenersPorAluno.clear()
        }
        registros.add(cleanupWrapper)

        return registros
    }

    fun iniciarMonitoramentoTempoRealAluno(
        scope: CoroutineScope,
        alunoFirebaseId: String
    ): ListenerRegistration? {
        if (alunoFirebaseId.isBlank()) return null
        return firestore.collection(COLECAO_ALUNOS)
            .document(alunoFirebaseId)
            .collection("anamneses_atribuidas")
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    scope.launch(Dispatchers.IO) {
                        sincronizarAnamnesesAtribuidasDoAluno()
                    }
                }
            }
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

            if (operacoes.isEmpty()) {
                return true
            }

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
                            )

                            if (aluno == null || aluno.professorId != usuarioLocal.id || aluno.firebaseId != remoteDocumentId) {
                                syncDao.remover(operacao.id)
                                continue
                            }

                            val usuarioAluno = usuarioDao.buscarPorAlunoId(aluno.id)

                            val alunoAuthUid = usuarioAluno
                                ?.takeIf {
                                    it.perfil == PERFIL_ALUNO &&
                                            it.ativo &&
                                            it.email.equals(aluno.email, ignoreCase = true)
                                }
                                ?.firebaseUid
                                ?.takeIf { it.isNotBlank() }

                            val emailNormalizado = aluno.email.trim().lowercase()

                            val dadosRemotos = hashMapOf<String, Any>(
                                "firebaseId" to remoteDocumentId,
                                "professorUid" to uid,
                                "nome" to aluno.nome,
                                "email" to emailNormalizado,
                                "dataCadastro" to aluno.dataCadastro,
                                "objetivo" to aluno.objetivo,
                                "status" to aluno.status,
                                "observacoesProfissionais" to aluno.observacoesProfissionais,
                                "condicoes" to aluno.condicoes,
                                "restricoes" to aluno.restricoes
                            )

                            if (alunoAuthUid != null) {
                                dadosRemotos["alunoAuthUid"] = alunoAuthUid
                            }

                            documento.set(dadosRemotos, SetOptions.merge()).await()
                        }

                        DELETE -> {
                            documento.delete().await()
                        }

                        else -> {
                            syncDao.remover(operacao.id)
                            continue
                        }
                    }
                    syncDao.remover(operacao.id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    todasConcluidas = false
                    Log.e(TAG, "Falha na operação ${operacao.id}.", e)
                    try {
                        syncDao.registrarFalha(operacao.id, e.message ?: e.javaClass.simpleName)
                    } catch (_: Exception) {
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
        val emailAuth = firebaseUser.email?.trim()?.lowercase().orEmpty()
        return try {
            var usuarioLocal = usuarioDao.buscarPorFirebaseUid(uid)
                ?: emailAuth.takeIf { it.isNotBlank() }?.let { usuarioDao.buscarPorEmail(it) }
            if (usuarioLocal == null && emailAuth.isNotBlank()) {
                val docProf = firestore.collection("usuarios").document(uid).get().await()
                if (docProf.exists() && docProf.getString("perfil") == PERFIL_PROFESSOR) {
                    val novoId = usuarioDao.inserir(
                        UsuarioEntity(
                            nome = docProf.getString("nome") ?: emailAuth.substringBefore("@"),
                            email = emailAuth,
                            senhaHash = "",
                            perfil = PERFIL_PROFESSOR,
                            ativo = docProf.getBoolean("ativo") ?: true,
                            firebaseUid = uid
                        )
                    )
                    usuarioLocal = usuarioDao.buscarPorId(novoId)
                }
            } else if (usuarioLocal != null && usuarioLocal.firebaseUid.isNullOrBlank()) {
                usuarioDao.atualizarFirebaseUid(usuarioLocal.id, uid)
                usuarioLocal = usuarioLocal.copy(firebaseUid = uid)
            }
            val professorLocalId = SessaoUsuario.usuarioAtual?.id
                ?: usuarioLocal?.id
                ?: return false
            val resultado = firestore
                .collection(COLECAO_ALUNOS)
                .whereEqualTo("professorUid", uid)
                .get()
                .await()
            val alunoDao = database.alunoDao()
            val syncDao = database.syncOperationDao()
            val firebaseIdsRemotos = HashSet<String>()
            for (documento in resultado.documents) {
                try {
                    val firebaseId = documento.getString("firebaseId") ?: documento.id
                    firebaseIdsRemotos.add(firebaseId)
                    val emailRemoto = documento.getString("email")?.trim()?.lowercase().orEmpty()
                    val nomeRemoto = documento.getString("nome").orEmpty()
                    val alunoExistente = alunoDao.buscarPorFirebaseId(firebaseId)
                        ?: emailRemoto.takeIf { it.isNotBlank() }?.let { alunoDao.buscarPorEmail(it) }
                    if (alunoExistente != null) {
                        val operacoesPendentes = syncDao.buscarPorRegistro(
                            TIPO_ALUNO,
                            alunoExistente.id
                        )
                        if (operacoesPendentes.isNotEmpty()) {
                            continue
                        }
                        val condicoesRemotas = documento.getString("condicoes") ?: alunoExistente.condicoes
                        val restricoesRemotas = documento.getString("restricoes") ?: alunoExistente.restricoes
                        val statusRemoto = documento.getString("status")?.takeIf { it.isNotBlank() } ?: alunoExistente.status
                        val obsRemotas = documento.getString("observacoesProfissionais") ?: alunoExistente.observacoesProfissionais
                        val objRemoto = documento.getString("objetivo") ?: alunoExistente.objetivo
                        val dataRemota = documento.getString("dataCadastro") ?: alunoExistente.dataCadastro
                        val atualizado = alunoExistente.copy(
                            professorId = professorLocalId,
                            firebaseId = firebaseId,
                            nome = nomeRemoto.ifBlank { alunoExistente.nome },
                            email = emailRemoto.ifBlank { alunoExistente.email },
                            dataCadastro = dataRemota,
                            objetivo = objRemoto,
                            status = statusRemoto,
                            observacoesProfissionais = obsRemotas,
                            condicoes = condicoesRemotas,
                            restricoes = restricoesRemotas
                        )
// Só atualiza o Room se realmente houver mudança (evita disparos desnecessários de UI)
                        if (atualizado != alunoExistente) {
                            alunoDao.atualizar(atualizado)
                        }
                    } else {
                        val statusRemoto = documento.getString("status")?.takeIf { it.isNotBlank() } ?: "Normal"
                        val novoAluno = AlunoEntity(
                            nome = nomeRemoto,
                            email = emailRemoto,
                            dataCadastro = documento.getString("dataCadastro") ?: "",
                            objetivo = documento.getString("objetivo") ?: "",
                            status = statusRemoto,
                            observacoesProfissionais = documento.getString("observacoesProfissionais") ?: "",
                            condicoes = documento.getString("condicoes") ?: "",
                            restricoes = documento.getString("restricoes") ?: "",
                            professorId = professorLocalId,
                            firebaseId = firebaseId
                        )
                        try {
                            alunoDao.inserir(novoAluno)
                        } catch (_: Exception) {
                            val orfao = emailRemoto.takeIf { it.isNotBlank() }?.let { alunoDao.buscarPorEmail(it) }
                            if (orfao != null) {
                                alunoDao.atualizar(novoAluno.copy(id = orfao.id))
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao processar aluno remoto ${documento.id}", e)
                }
            }
// Remove localmente alunos excluídos no Firestore
            val alunosLocais = alunoDao.listarAlunosDoProfessorDireto(professorLocalId)
            for (local in alunosLocais) {
                val fbId = local.firebaseId
                if (!fbId.isNullOrBlank() && fbId !in firebaseIdsRemotos) {
                    val pendentes = syncDao.buscarPorRegistro(TIPO_ALUNO, local.id)
                    if (pendentes.isEmpty()) {
                        alunoDao.excluir(local)
                    }
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
            sincronizarAlunos()
            sincronizarLeituraAlunos()
            val anamneseDao = database.anamneseAtribuidaDao()
            val perguntaDao = database.perguntaAnamneseAtribuidaDao()
            val alunoDao = database.alunoDao()
            var sucesso = true
// 1. Baixa do servidor todas as atribuições, exclusões e respostas dos alunos deste professor
            val alunosRemotos = firestore
                .collection(COLECAO_ALUNOS)
                .whereEqualTo("professorUid", uid)
                .get()
                .await()
            for (documentoAluno in alunosRemotos.documents) {
                try {
                    val alunoFirebaseId = documentoAluno.getString("firebaseId") ?: documentoAluno.id
                    val alunoLocal = alunoDao.buscarPorFirebaseId(alunoFirebaseId) ?: continue
                    val atribuicoesRemotas = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(alunoFirebaseId)
                        .collection("anamneses_atribuidas")
                        .get()
                        .await()
                    for (documento in atribuicoesRemotas.documents) {
                        try {
                            importarAnamneseRemota(
                                documento = documento,
                                alunoId = alunoLocal.id,
                                uidAutenticado = uid,
                                importarRespostas = true
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            sucesso = false
                            Log.e(TAG, "Erro ao importar atribuição ${documento.id}.", e)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(TAG, "Erro ao consultar atribuições do aluno ${documentoAluno.id}.", e)
                }
            }
// 2. Publica atribuições locais novas ou atualizadas no servidor (somente se houve mudança)
            val anamneses = anamneseDao.listarTodasParaSincronizacaoDoProfessor(usuario.id)
            for (anamnese in anamneses) {
                try {
                    val aluno = alunoDao.buscarPorIdDireto(anamnese.alunoId) ?: continue
                    if (aluno.professorId != usuario.id) continue
                    val alunoFirebaseId = aluno.firebaseId?.takeIf { it.isNotBlank() } ?: continue
                    if (anamnese.excluida) {
                        val fbId = anamnese.firestoreId?.takeIf { it.isNotBlank() }
                        if (fbId != null) {
                            try {
                                firestore
                                    .collection(COLECAO_ALUNOS)
                                    .document(alunoFirebaseId)
                                    .collection("anamneses_atribuidas")
                                    .document(fbId)
                                    .delete()
                                    .await()
                            } catch (_: Exception) {
                            }
                        }
                        continue
                    }
                    val firestoreId = anamnese.firestoreId?.takeIf { it.isNotBlank() }
                        ?: UUID.randomUUID().toString().also {
                            anamneseDao.atualizarFirestoreId(anamnese.id, it, System.currentTimeMillis())
                        }
                    val documentoAnamnese = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(alunoFirebaseId)
                        .collection("anamneses_atribuidas")
                        .document(firestoreId)
                    val remotoExistente = documentoAnamnese.get().await()
                    if (!remotoExistente.exists()) {
                        val documentoAluno = firestore
                            .collection(COLECAO_ALUNOS)
                            .document(alunoFirebaseId)
                            .get()
                            .await()
                        if (!documentoAluno.exists() || documentoAluno.getString("professorUid") != uid) {
                            continue
                        }
                        val alunoAuthUid = documentoAluno.getString("alunoAuthUid")?.takeIf { it.isNotBlank() }
                        val dados = hashMapOf<String, Any>(
                            "firestoreId" to firestoreId,
                            "professorUid" to uid,
                            "alunoFirebaseId" to alunoFirebaseId,
                            "nomeModelo" to anamnese.nomeModelo,
                            "profissionalResponsavel" to anamnese.profissionalResponsavel,
                            "identificadorExterno" to anamnese.identificadorExterno,
                            "excluida" to false,
                            "status" to anamnese.status,
                            "dataCriacao" to anamnese.dataCriacao,
                            "dataEnvio" to anamnese.dataEnvio,
                            "dataInicioResposta" to anamnese.dataInicioResposta,
                            "dataResposta" to anamnese.dataResposta,
                            "updatedAtMillis" to maxOf(anamnese.updatedAtMillis, System.currentTimeMillis())
                        )
                        if (alunoAuthUid != null) {
                            dados["alunoAuthUid"] = alunoAuthUid
                        }
                        documentoAnamnese.set(dados, SetOptions.merge()).await()
                        val perguntas = perguntaDao.listarParaSincronizacao(anamnese.id)
                        for (pergunta in perguntas) {
                            val perguntaFirestoreId = pergunta.firestoreId?.takeIf { it.isNotBlank() }
                                ?: UUID.randomUUID().toString().also {
                                    perguntaDao.atribuirFirestoreId(pergunta.id, it)
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
                    } else {
                        val statusRemoto = remotoExistente.getString("status") ?: anamnese.status
                        val statusCombinado = combinarStatus(anamnese.status, statusRemoto)
                        val remotoMillis = remotoExistente.getLong("updatedAtMillis") ?: 0L
// Trava anti-loop: só escreve no Firestore se o local for mais recente ou o status mudou
                        if (anamnese.updatedAtMillis <= remotoMillis && statusCombinado == statusRemoto) {
                            continue
                        }
                        val dados = hashMapOf<String, Any>(
                            "nomeModelo" to anamnese.nomeModelo,
                            "profissionalResponsavel" to anamnese.profissionalResponsavel,
                            "identificadorExterno" to anamnese.identificadorExterno,
                            "status" to statusCombinado,
                            "dataEnvio" to escolherDataMaisCompleta(
                                anamnese.dataEnvio,
                                remotoExistente.getString("dataEnvio") ?: ""
                            ),
                            "dataInicioResposta" to escolherDataMaisCompleta(
                                anamnese.dataInicioResposta,
                                remotoExistente.getString("dataInicioResposta") ?: ""
                            ),
                            "dataResposta" to escolherDataMaisCompleta(
                                anamnese.dataResposta,
                                remotoExistente.getString("dataResposta") ?: ""
                            ),
                            "excluida" to false,
                            "updatedAtMillis" to maxOf(anamnese.updatedAtMillis, remotoMillis)
                        )
                        documentoAnamnese.set(dados, SetOptions.merge()).await()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(TAG, "Erro ao publicar anamnese ${anamnese.id}.", e)
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
    suspend fun sincronizarModelosAnamnese(): Boolean {
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
            val modeloDao = database.modeloAnamneseDao()
            val perguntaDao = database.perguntaModeloAnamneseDao()
            val colecao = firestore
                .collection("usuarios")
                .document(uid)
                .collection("modelos_anamnese")
            val modelosLocais = modeloDao.listarModelosParaSincronizacao(usuarioLocal.id)
            var sucesso = true
            for (modelo in modelosLocais) {
                try {
                    val firestoreId = modelo.firestoreId
                        ?: throw IllegalStateException("Modelo ${modelo.id} sem firestoreId.")
                    val documento = colecao.document(firestoreId)
                    val remoto = documento.get().await()
                    val remotoMillis = remoto.getLong("updatedAtMillis") ?: 0L
                    if (remoto.exists() && remotoMillis > modelo.updatedAtMillis) {
                        importarModeloRemoto(remoto, usuarioLocal.id, modeloDao, perguntaDao)
                        continue
                    }
// Trava anti-loop: não regrava no Firestore se já está sincronizado na mesma versão
                    if (remoto.exists() && remotoMillis == modelo.updatedAtMillis) {
                        continue
                    }
                    val perguntas = perguntaDao.buscarPorModeloDoProfessor(modelo.id, usuarioLocal.id)
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
                        categoria !in setOf("PADRAO", "PERSONALIZADO")
                    ) {
                        continue
                    }
                    val firestoreId = documento.getString("firestoreId") ?: documento.id
                    val local = modeloDao.buscarPorFirestoreId(firestoreId)
                    val remotoMillis = documento.getLong("updatedAtMillis") ?: 0L
                    if (local == null || remotoMillis > local.updatedAtMillis) {
                        importarModeloRemoto(documento, usuarioLocal.id, modeloDao, perguntaDao)
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
            val alunoId = usuario.alunoId ?: throw IllegalStateException("Usuário sem alunoId.")
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
                throw IllegalStateException("Cadastro remoto do aluno não encontrado.")
            }
            val emailAutenticado = firebaseUser.email
                ?: throw IllegalStateException("A conta Firebase do aluno não possui e-mail.")
            val professorUid = vincularContaAlunoNoFirestore(
                alunoFirebaseId = alunoFirebaseId,
                uid = uid,
                emailAutenticado = emailAutenticado
            )
// Só atualiza condições/restrições no Firestore se houver divergência real com o servidor
            val condicoesRemotas = documentoAluno.getString("condicoes") ?: ""
            val restricoesRemotas = documentoAluno.getString("restricoes") ?: ""
            val obsRemotas = documentoAluno.getString("observacoesProfissionais") ?: ""
            val statusRemotoAluno = documentoAluno.getString("status") ?: "Normal"
            if (
                aluno.condicoes != condicoesRemotas ||
                aluno.restricoes != restricoesRemotas ||
                aluno.observacoesProfissionais != obsRemotas ||
                aluno.status != statusRemotoAluno
            ) {
                try {
                    firestore.collection(COLECAO_ALUNOS)
                        .document(alunoFirebaseId)
                        .set(
                            mapOf(
                                "condicoes" to aluno.condicoes,
                                "restricoes" to aluno.restricoes,
                                "observacoesProfissionais" to aluno.observacoesProfissionais,
                                "status" to aluno.status
                            ),
                            SetOptions.merge()
                        )
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao sincronizar acompanhamento do aluno ${aluno.id}", e)
                }
            }
            val colecaoAnamneses = firestore
                .collection(COLECAO_ALUNOS)
                .document(alunoFirebaseId)
                .collection("anamneses_atribuidas")
            val resultado = colecaoAnamneses.get().await()
            var sucesso = true
            for (documento in resultado.documents) {
                try {
                    if (documento.getString("professorUid") != professorUid) {
                        continue
                    }
                    val uidAtribuicao = documento.getString("alunoAuthUid")
                    if (!uidAtribuicao.isNullOrBlank() && uidAtribuicao != uid) {
                        continue
                    }
                    val anamneseLocalId = importarAnamneseRemota(
                        documento = documento,
                        alunoId = alunoId,
                        uidAutenticado = uid,
                        importarRespostas = true
                    )
                    val anamneseLocal = database
                        .anamneseAtribuidaDao()
                        .buscarPorFirestoreId(documento.getString("firestoreId") ?: documento.id)
                    if (anamneseLocal != null && !anamneseLocal.excluida) {
                        enviarRespostasLocaisDoAluno(
                            documento = documento,
                            anamneseFirestoreId = documento.id,
                            anamneseLocalId = anamneseLocalId,
                            anamneseLocal = anamneseLocal,
                            alunoId = alunoId,
                            uid = uid
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    sucesso = false
                    Log.e(TAG, "Erro ao sincronizar anamnese do aluno: ${documento.id}", e)
                }
            }
            sucesso
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro geral na sincronização das anamneses do aluno.", e)
            false
        }
    }

    suspend fun sincronizarTreinos(): Boolean {
        val firebaseUser = firebaseAuth.currentUser ?: return false
        val uid = firebaseUser.uid
        return try {
            val usuario = usuarioDao.buscarPorFirebaseUid(uid) ?: return false
            if (!usuario.ativo || usuario.perfil != PERFIL_PROFESSOR) return false

            val treinoDao = database.treinoDao()
            val alunoDao = database.alunoDao()

            // 1. Publica treinos e exercícios locais do professor no Firestore
            val alunosLocais = alunoDao.listarAlunosDoProfessorDireto(usuario.id)
            val todosTreinosLocais = treinoDao.listarTreinosDoProfessor(usuario.id).first()

            for (treino in todosTreinosLocais) {
                val aluno = alunosLocais.firstOrNull { it.id == treino.alunoId } ?: continue
                val alunoFbId = aluno.firebaseId?.takeIf { it.isNotBlank() } ?: continue
                val treinoDocId = "treino_${treino.id}"

                val docRef = firestore.collection(COLECAO_ALUNOS)
                    .document(alunoFbId)
                    .collection("treinos")
                    .document(treinoDocId)

                val exercicios = treinoDao.listarExerciciosDoProfessor(treino.id, usuario.id).first()
                val listaExerciciosMap = exercicios.map { ex ->
                    hashMapOf(
                        "nome" to ex.nome,
                        "series" to ex.series,
                        "repeticoes" to ex.repeticoes,
                        "carga" to ex.carga,
                        "observacoes" to ex.observacoes,
                        "ordem" to ex.ordem,
                        "videoTipo" to ex.videoTipo,
                        "videoUrl" to if (ex.videoTipo == "LINK") ex.videoUrl else "",
                        "videoNome" to ex.videoNome
                    )
                }

                val remoto = docRef.get().await()
                val nomeRemoto = remoto.getString("nome") ?: ""
                val inicioRemoto = remoto.getString("dataInicio") ?: ""
                val fimRemoto = remoto.getString("dataFim") ?: ""
                val descRemota = remoto.getString("descricao") ?: ""
                val obsRemota = remoto.getString("observacoes") ?: ""
                val exRemotos = remoto.get("exercicios") as? List<*>

                // Só grava no Firestore se houver diferença real (trava anti-loop)
                if (!remoto.exists() ||
                    nomeRemoto != treino.nome ||
                    inicioRemoto != treino.dataInicio ||
                    fimRemoto != treino.dataFim ||
                    descRemota != treino.descricao ||
                    obsRemota != treino.observacoes ||
                    (exRemotos?.size ?: -1) != exercicios.size
                ) {
                    val dadosTreino = hashMapOf<String, Any>(
                        "professorUid" to uid,
                        "alunoFirebaseId" to alunoFbId,
                        "localTreinoId" to treino.id,
                        "nome" to treino.nome,
                        "descricao" to treino.descricao,
                        "dataInicio" to treino.dataInicio,
                        "dataFim" to treino.dataFim,
                        "observacoes" to treino.observacoes,
                        "exercicios" to listaExerciciosMap
                    )
                    docRef.set(dadosTreino, SetOptions.merge()).await()
                }
            }

            // 2. Baixa treinos remotos que ainda não existam neste dispositivo
            val alunosRemotos = firestore.collection(COLECAO_ALUNOS)
                .whereEqualTo("professorUid", uid)
                .get()
                .await()

            for (docAluno in alunosRemotos.documents) {
                val alunoFbId = docAluno.getString("firebaseId") ?: docAluno.id
                val alunoLocal = alunoDao.buscarPorFirebaseId(alunoFbId) ?: continue
                val treinosAtuaisAluno = treinoDao.listarTreinosDoAluno(alunoLocal.id).first()

                val treinosRemotos = firestore.collection(COLECAO_ALUNOS)
                    .document(alunoFbId)
                    .collection("treinos")
                    .get()
                    .await()

                for (docTreino in treinosRemotos.documents) {
                    val nomeRemoto = docTreino.getString("nome") ?: continue
                    val inicioRemoto = docTreino.getString("dataInicio") ?: ""
                    val fimRemoto = docTreino.getString("dataFim") ?: ""
                    val descRemota = docTreino.getString("descricao") ?: ""
                    val obsRemota = docTreino.getString("observacoes") ?: ""

                    val jaExisteLocal = treinosAtuaisAluno.any {
                        it.nome == nomeRemoto && it.dataInicio == inicioRemoto
                    }

                    if (!jaExisteLocal) {
                        val novoTreinoId = treinoDao.inserirTreino(
                            TreinoEntity(
                                alunoId = alunoLocal.id,
                                nome = nomeRemoto,
                                descricao = descRemota,
                                dataInicio = inicioRemoto,
                                dataFim = fimRemoto,
                                observacoes = obsRemota
                            )
                        )
                        val exList = docTreino.get("exercicios") as? List<*> ?: emptyList<Any>()
                        exList.forEachIndexed { idx, item ->
                            val mapa = item as? Map<*, *> ?: return@forEachIndexed
                            treinoDao.inserirExercicio(
                                ExercicioEntity(
                                    treinoId = novoTreinoId,
                                    nome = mapa["nome"] as? String ?: "",
                                    series = mapa["series"] as? String ?: "3",
                                    repeticoes = mapa["repeticoes"] as? String ?: "12",
                                    carga = mapa["carga"] as? String ?: "",
                                    observacoes = mapa["observacoes"] as? String ?: "",
                                    ordem = (mapa["ordem"] as? Number)?.toInt() ?: idx,
                                    videoTipo = mapa["videoTipo"] as? String ?: "",
                                    videoUrl = mapa["videoUrl"] as? String ?: "",
                                    videoNome = mapa["videoNome"] as? String ?: ""
                                )
                            )
                        }
                    }
                }
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar treinos.", e)
            false
        }
    }

    private suspend fun importarAnamneseRemota(
        documento: DocumentSnapshot,
        alunoId: Long,
        uidAutenticado: String,
        importarRespostas: Boolean
    ): Long {
        val firestoreId = documento.getString("firestoreId") ?: documento.id
        val nomeModelo = documento.getString("nomeModelo") ?: ""
        val statusRemoto = documento.getString("status") ?: "PENDENTE"
        val dataCriacao = documento.getString("dataCriacao") ?: ""
        val dataEnvioRemota = documento.getString("dataEnvio") ?: ""
        val dataInicioRemota = documento.getString("dataInicioResposta") ?: ""
        val dataRespostaRemota = documento.getString("dataResposta") ?: ""
        val profissionalResponsavel = documento.getString("profissionalResponsavel") ?: ""
        val identificadorExterno = documento.getString("identificadorExterno") ?: ""
        val updatedAtMillis = documento.getLong("updatedAtMillis") ?: 0L
        val excluidaRemota = documento.getBoolean("excluida") ?: false
        val perguntasRemotas = documento.reference.collection("perguntas").get().await()
        val respostasRemotasDocs = if (importarRespostas) {
            val consultaRespostas = documento.reference.collection("respostas")
            val perfilUsuario = usuarioDao.buscarPorFirebaseUid(uidAutenticado)?.perfil
            if (perfilUsuario == PERFIL_ALUNO) {
                consultaRespostas.whereEqualTo("alunoAuthUid", uidAutenticado).get().await().documents
            } else {
                consultaRespostas.get().await().documents
            }
        } else {
            emptyList()
        }
        val anamneseDao = database.anamneseAtribuidaDao()
        val perguntaDao = database.perguntaAnamneseAtribuidaDao()
        val respostaDao = database.respostaAnamneseDao()
        return database.withTransaction {
            val existente = anamneseDao.buscarPorFirestoreId(firestoreId)
            val statusLocal = existente?.status
            val dataEnvioLocal = existente?.dataEnvio ?: ""
            val dataInicioLocal = existente?.dataInicioResposta ?: ""
            val dataRespostaLocal = existente?.dataResposta ?: ""
            val statusFinal = combinarStatus(statusLocal, statusRemoto)
            val dataEnvioFinal = escolherDataMaisCompleta(dataEnvioLocal, dataEnvioRemota)
            val dataInicioFinal = escolherDataMaisCompleta(dataInicioLocal, dataInicioRemota)
            val dataRespostaFinal = escolherDataMaisCompleta(dataRespostaLocal, dataRespostaRemota)
            val excluidaFinal = (existente?.excluida == true) || excluidaRemota
            val anamneseLocalId = if (existente == null) {
                anamneseDao.inserirRecebidaDoFirestore(
                    AnamneseAtribuidaEntity(
                        id = 0L,
                        alunoId = alunoId,
                        modeloId = 0L,
                        nomeModelo = nomeModelo,
                        status = statusFinal,
                        dataCriacao = dataCriacao,
                        dataEnvio = dataEnvioFinal,
                        dataInicioResposta = dataInicioFinal,
                        dataResposta = dataRespostaFinal,
                        profissionalResponsavel = profissionalResponsavel,
                        identificadorExterno = identificadorExterno,
                        firestoreId = firestoreId,
                        updatedAtMillis = updatedAtMillis,
                        excluida = excluidaFinal
                    )
                )
            } else {
                anamneseDao.atualizarRecebidaDoFirestore(
                    firestoreId = firestoreId,
                    nomeModelo = nomeModelo.ifBlank { existente.nomeModelo },
                    status = statusFinal,
                    dataCriacao = dataCriacao.ifBlank { existente.dataCriacao },
                    dataEnvio = dataEnvioFinal,
                    dataInicioResposta = dataInicioFinal,
                    dataResposta = dataRespostaFinal,
                    profissionalResponsavel = profissionalResponsavel.ifBlank {
                        existente.profissionalResponsavel
                    },
                    identificadorExterno = identificadorExterno.ifBlank {
                        existente.identificadorExterno
                    },
                    updatedAtMillis = maxOf(existente.updatedAtMillis, updatedAtMillis),
                    excluida = excluidaFinal
                )
                existente.id
            }
            if (anamneseLocalId <= 0L) {
                throw IllegalStateException("Não foi possível importar a anamnese $firestoreId.")
            }
            for (perguntaDocumento in perguntasRemotas.documents) {
                val perguntaFirestoreId = perguntaDocumento.getString("firestoreId") ?: perguntaDocumento.id
                val pergunta = PerguntaAnamneseAtribuidaEntity(
                    id = 0L,
                    anamneseAtribuidaId = anamneseLocalId,
                    perguntaModeloId = null,
                    texto = perguntaDocumento.getString("texto") ?: "",
                    tipoResposta = perguntaDocumento.getString("tipoResposta") ?: "TEXTO",
                    opcoes = perguntaDocumento.getString("opcoes") ?: "",
                    obrigatoria = perguntaDocumento.getBoolean("obrigatoria") ?: false,
                    ordem = (perguntaDocumento.getLong("ordem") ?: 0L).toInt(),
                    firestoreId = perguntaFirestoreId
                )
                perguntaDao.inserirOuAtualizarParaSincronizacao(pergunta)
            }
            if (importarRespostas) {
                val perfilUsuario = usuarioDao.buscarPorFirebaseUid(uidAutenticado)?.perfil
                for (respostaDocumento in respostasRemotasDocs) {
                    val respostaUid = respostaDocumento.getString("alunoAuthUid")
                    if (!respostaUid.isNullOrBlank() && respostaUid != uidAutenticado && perfilUsuario == PERFIL_ALUNO) {
                        continue
                    }
                    val perguntaFirestoreId = respostaDocumento.getString("perguntaFirestoreId") ?: respostaDocumento.id
                    val respostaRemota = respostaDocumento.getString("resposta") ?: ""
                    val dataAtualizacaoRemota = respostaDocumento.getString("dataAtualizacao") ?: ""
                    val respostaExistente = respostaDao.buscarPorPerguntaFirestoreId(perguntaFirestoreId)
                    if (respostaExistente != null && respostaExistente.resposta.isNotBlank() && respostaRemota.isBlank()) {
                        continue
                    }
                    respostaDao.salvarRespostaSincronizada(
                        perguntaFirestoreId = perguntaFirestoreId,
                        resposta = respostaRemota,
                        dataAtualizacao = dataAtualizacaoRemota
                    )
                }
            }
            anamneseLocalId
        }
    }
    private suspend fun enviarRespostasLocaisDoAluno(
        documento: DocumentSnapshot,
        anamneseFirestoreId: String,
        anamneseLocalId: Long,
        anamneseLocal: AnamneseAtribuidaEntity,
        alunoId: Long,
        uid: String
    ) {
        val respostaDao = database.respostaAnamneseDao()
        val perguntaDao = database.perguntaAnamneseAtribuidaDao()
        val respostasLocais = respostaDao.buscarTodasPorAnamneseDoAluno(anamneseLocalId, alunoId)
        val perguntasLocais = perguntaDao.listarParaSincronizacao(anamneseLocalId)
        val firestoreIdPorPerguntaLocal = perguntasLocais.mapNotNull { pergunta ->
            pergunta.firestoreId?.takeIf { it.isNotBlank() }?.let { pergunta.id to it }
        }.toMap()
        val alunoFirebaseId = documento.getString("alunoFirebaseId")
            ?: throw IllegalStateException("Atribuição sem alunoFirebaseId.")
        val documentoAnamnese = firestore
            .collection(COLECAO_ALUNOS)
            .document(alunoFirebaseId)
            .collection("anamneses_atribuidas")
            .document(anamneseFirestoreId)
        for (respostaLocal in respostasLocais) {
            if (respostaLocal.resposta.isBlank()) continue
            val perguntaFirestoreId = firestoreIdPorPerguntaLocal[respostaLocal.perguntaAtribuidaId] ?: continue
            val dadosResposta = hashMapOf(
                "perguntaFirestoreId" to perguntaFirestoreId,
                "alunoAuthUid" to uid,
                "resposta" to respostaLocal.resposta,
                "dataAtualizacao" to respostaLocal.dataAtualizacao
            )
            documentoAnamnese
                .collection("respostas")
                .document(perguntaFirestoreId)
                .set(dadosResposta, SetOptions.merge())
                .await()
        }
        val statusRemoto = documento.getString("status") ?: "PENDENTE"
        val statusFinal = combinarStatus(anamneseLocal.status, statusRemoto)
        val remotoMillis = documento.getLong("updatedAtMillis") ?: 0L
        val uidRemoto = documento.getString("alunoAuthUid")
// Só atualiza o documento da anamnese se o status, carimbo de tempo ou vínculo realmente mudaram
        if (anamneseLocal.updatedAtMillis > remotoMillis || statusFinal != statusRemoto || uidRemoto != uid) {
            val atualizacaoStatus = hashMapOf<String, Any>(
                "alunoAuthUid" to uid,
                "status" to statusFinal,
                "dataInicioResposta" to escolherDataMaisCompleta(
                    anamneseLocal.dataInicioResposta,
                    documento.getString("dataInicioResposta") ?: ""
                ),
                "dataResposta" to escolherDataMaisCompleta(
                    anamneseLocal.dataResposta,
                    documento.getString("dataResposta") ?: ""
                ),
                "updatedAtMillis" to maxOf(anamneseLocal.updatedAtMillis, remotoMillis)
            )
            documentoAnamnese.set(atualizacaoStatus, SetOptions.merge()).await()
        }
    }
    private fun combinarStatus(
        statusLocal: String?,
        statusRemoto: String
    ): String {
        if (statusLocal.isNullOrBlank()) return statusRemoto
        fun prioridade(status: String): Int {
            return when (status.uppercase()) {
                "CONCLUIDA", "CONCLUÍDA", "FINALIZADA", "RESPONDIDA" -> 3
                "EM_ANDAMENTO", "EM ANDAMENTO" -> 2
                "PENDENTE", "ENVIADA" -> 1
                "NAO_ENVIADA", "NÃO_ENVIADA" -> 0
                else -> 0
            }
        }
        return if (prioridade(statusLocal) > prioridade(statusRemoto)) statusLocal else statusRemoto
    }
    private fun escolherDataMaisCompleta(
        local: String,
        remota: String
    ): String {
        return when {
            local.isBlank() -> remota
            remota.isBlank() -> local
            else -> local
        }
    }
    private suspend fun importarModeloRemoto(
        documento: DocumentSnapshot,
        usuarioLocalId: Long,
        modeloDao: ModeloAnamneseDao,
        perguntaDao: PerguntaModeloAnamneseDao
    ) {
        val categoria = documento.getString("categoria")
            ?: throw IllegalStateException("Modelo remoto sem categoria: ${documento.id}")
        require(categoria == "PADRAO" || categoria == "PERSONALIZADO") {
            "Categoria inválida no modelo remoto: $categoria"
        }
        val firestoreId = documento.getString("firestoreId") ?: documento.id
        val nome = documento.getString("nome") ?: ""
        val descricao = documento.getString("descricao") ?: ""
        val ativo = documento.getBoolean("ativo") ?: true
        val dataCriacao = documento.getString("dataCriacao") ?: ""
        val dataAtualizacao = documento.getString("dataAtualizacao") ?: ""
        val updatedAtMillis = documento.getLong("updatedAtMillis") ?: 0L
        val excluido = documento.getBoolean("deleted") ?: false
        val deletedAtMillis = documento.getLong("deletedAtMillis")
        val perguntasRemotas = documento.get("perguntas") as? List<*> ?: emptyList<Any>()
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
            val existente = modeloDao.buscarPorFirestoreId(firestoreId)
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
                        it.copy(id = 0L, modeloId = modeloLocal.id)
                    }
                )
            }
        }
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
                throw IllegalStateException("Cadastro remoto do aluno não encontrado.")
            }
            val emailCadastro = snapshot.getString("email")
                ?.trim()
                ?.lowercase()
                ?: throw IllegalStateException("Cadastro remoto sem e-mail.")
            if (emailCadastro != emailAutenticado.trim().lowercase()) {
                throw SecurityException("O e-mail da conta autenticada não corresponde ao cadastro do aluno.")
            }
            val uidExistente = snapshot.getString("alunoAuthUid")
            if (!uidExistente.isNullOrBlank() && uidExistente != uid) {
                throw SecurityException("Este cadastro já está vinculado a outra conta Firebase.")
            }
            if (uidExistente.isNullOrBlank()) {
                transacao.update(documento, "alunoAuthUid", uid)
            }
            snapshot.getString("professorUid")
            snapshot.getString("professorUid")
                ?: throw IllegalStateException("Cadastro remoto sem professorUid.")
        }.await()
    }
}