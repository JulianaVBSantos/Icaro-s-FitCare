
package br.com.icaros.fitcare.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

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
    }

    /**
     * Valida a sessão Firebase e o vínculo com o usuário local.
     */
    suspend fun validarSessao(): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser

            if (firebaseUser == null) {
                Log.e(TAG, "Não existe usuário autenticado no Firebase.")
                return false
            }

            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(firebaseUser.uid)

            if (usuarioLocal == null) {
                Log.e(
                    TAG,
                    "Usuário Firebase ${firebaseUser.uid} não possui vínculo no Room."
                )
                return false
            }

            if (!usuarioLocal.ativo) {
                Log.e(TAG, "Usuário local está inativo.")
                return false
            }

            if (usuarioLocal.perfil != PERFIL_PROFESSOR) {
                Log.e(
                    TAG,
                    "Perfil inválido para sincronização: ${usuarioLocal.perfil}"
                )
                return false
            }

            Log.i(TAG, "Sessão validada. UID=${firebaseUser.uid}")

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

    /**
     * Envia as operações pendentes do Room para o Firestore.
     */
    suspend fun sincronizarAlunos(): Boolean {
        val firebaseUser = firebaseAuth.currentUser

        if (firebaseUser == null) {
            Log.e(TAG, "Escrita cancelada: usuário Firebase não autenticado.")
            return false
        }

        val uid = firebaseUser.uid

        return try {
            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuarioLocal == null ||
                !usuarioLocal.ativo ||
                usuarioLocal.perfil != PERFIL_PROFESSOR
            ) {
                Log.e(
                    TAG,
                    "Escrita bloqueada: vínculo local inválido. UID=$uid"
                )
                return false
            }

            val syncDao = database.syncOperationDao()
            val alunoDao = database.alunoDao()

            val operacoes = syncDao.listarPendentes()
                .filter { it.entityType == TIPO_ALUNO }

            Log.i(
                TAG,
                "Encontradas ${operacoes.size} operações pendentes para o professor $uid."
            )

            var todasConcluidas = true

            for (operacao in operacoes) {
                try {
                    val remoteDocumentId = operacao.remoteDocumentId

                    if (remoteDocumentId.isNullOrBlank()) {
                        throw IllegalStateException(
                            "Operação ${operacao.id} não possui remoteDocumentId."
                        )
                    }

                    val documento = firestore
                        .collection(COLECAO_ALUNOS)
                        .document(remoteDocumentId)

                    Log.i(
                        TAG,
                        "Processando operação ${operacao.id}: " +
                                "${operacao.operationType}, documento=$remoteDocumentId, UID=$uid"
                    )

                    when (operacao.operationType.uppercase()) {

                        INSERT, UPDATE -> {
                            val aluno = alunoDao.buscarPorIdDireto(
                                operacao.localEntityId
                            )

                            if (aluno == null) {
                                throw IllegalStateException(
                                    "Aluno local ${operacao.localEntityId} não encontrado."
                                )
                            }

                            if (aluno.professorId != usuarioLocal.id) {
                                throw SecurityException(
                                    "Aluno ${aluno.id} não pertence ao professor autenticado."
                                )
                            }

                            if (aluno.firebaseId != remoteDocumentId) {
                                throw IllegalStateException(
                                    "firebaseId do aluno não corresponde ao documento remoto."
                                )
                            }

                            val dadosRemotos = hashMapOf(
                                "firebaseId" to remoteDocumentId,
                                "professorUid" to uid,
                                "nome" to aluno.nome,
                                "email" to aluno.email,
                                "dataCadastro" to aluno.dataCadastro,
                                "objetivo" to aluno.objetivo
                            )

                            when (operacao.operationType.uppercase()) {

                                INSERT -> {
                                    Log.i(
                                        TAG,
                                        "ENVIANDO INSERT: documento=$remoteDocumentId, nome=${aluno.nome}"
                                    )

                                    documento.set(dadosRemotos).await()

                                    Log.i(
                                        TAG,
                                        "INSERT aceito pelo Firestore: documento=$remoteDocumentId"
                                    )
                                }

                                UPDATE -> {
                                    Log.i(
                                        TAG,
                                        "ENVIANDO UPDATE: UID=$uid, " +
                                                "documento=$remoteDocumentId, " +
                                                "nome=${aluno.nome}, email=${aluno.email}"
                                    )

                                    documento.set(
                                        dadosRemotos,
                                        SetOptions.merge()
                                    ).await()

                                    Log.i(
                                        TAG,
                                        "ESCRITA ACEITA PELO FIRESTORE: documento=$remoteDocumentId"
                                    )

                                    // Consulta diretamente o servidor,
                                    // sem utilizar dados do cache local.
                                    val confirmado = documento.get(
                                        com.google.firebase.firestore.Source.SERVER
                                    ).await()

                                    val nomeServidor =
                                        confirmado.getString("nome")

                                    val uidServidor =
                                        confirmado.getString("professorUid")

                                    Log.i(
                                        TAG,
                                        "CONFIRMAÇÃO NO SERVIDOR: " +
                                                "documento=${confirmado.id}, " +
                                                "existe=${confirmado.exists()}, " +
                                                "nome=$nomeServidor, " +
                                                "UID=$uidServidor"
                                    )

                                    if (!confirmado.exists()) {
                                        throw IllegalStateException(
                                            "Documento não encontrado no servidor após UPDATE."
                                        )
                                    }

                                    if (nomeServidor != aluno.nome) {
                                        throw IllegalStateException(
                                            "Divergência após UPDATE. " +
                                                    "Nome local=${aluno.nome}; " +
                                                    "nome no servidor=$nomeServidor"
                                        )
                                    }

                                    if (uidServidor != uid) {
                                        throw IllegalStateException(
                                            "UID do documento remoto não corresponde ao professor autenticado."
                                        )
                                    }
                                }
                            }

                            Log.i(
                                TAG,
                                "Escrita concluída: ${operacao.operationType}, documento=$remoteDocumentId"
                            )
                        }

                        DELETE -> {
                            documento.delete().await()

                            Log.i(
                                TAG,
                                "Exclusão remota concluída: $remoteDocumentId"
                            )
                        }

                        else -> {
                            throw IllegalArgumentException(
                                "Tipo de operação desconhecido: ${operacao.operationType}"
                            )
                        }
                    }

                    // Remove somente depois da confirmação da operação.
                    syncDao.remover(operacao.id)

                    Log.i(
                        TAG,
                        "Operação ${operacao.id} removida da fila após sucesso."
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    todasConcluidas = false

                    Log.e(
                        TAG,
                        "Falha na operação ${operacao.id}. " +
                                "Tipo=${operacao.operationType}, " +
                                "Documento=${operacao.remoteDocumentId}, " +
                                "UID=$uid",
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
                            "Não foi possível registrar a falha da operação ${operacao.id}.",
                            erroRegistro
                        )
                    }
                }
            }

            Log.i(
                TAG,
                "Processamento de escrita finalizado. Sucesso total=$todasConcluidas"
            )

            todasConcluidas

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro geral durante a sincronização de escrita.", e)
            false
        }
    }

    /**
     * Recebe alterações do Firestore para o Room.
     */
    suspend fun sincronizarLeituraAlunos(): Boolean {
        val firebaseUser = firebaseAuth.currentUser

        if (firebaseUser == null) {
            Log.e(TAG, "Leitura cancelada: usuário Firebase não autenticado.")
            return false
        }

        val uid = firebaseUser.uid

        return try {
            val usuarioLocal =
                usuarioDao.buscarPorFirebaseUid(uid)

            if (
                usuarioLocal == null ||
                !usuarioLocal.ativo ||
                usuarioLocal.perfil != PERFIL_PROFESSOR
            ) {
                Log.e(
                    TAG,
                    "Leitura bloqueada: vínculo local inválido. UID=$uid"
                )
                return false
            }

            val resultado = firestore
                .collection(COLECAO_ALUNOS)
                .whereEqualTo("professorUid", uid)
                .get()
                .await()

            val alunoDao = database.alunoDao()
            val syncDao = database.syncOperationDao()

            Log.i(
                TAG,
                "Firestore retornou ${resultado.documents.size} alunos para UID=$uid."
            )

            for (documento in resultado.documents) {
                try {
                    val firebaseId = documento.getString("firebaseId")
                        ?: documento.id

                    val nome = documento.getString("nome") ?: ""
                    val email = documento.getString("email") ?: ""

                    val dataCadastro =
                        documento.getString("dataCadastro") ?: ""

                    val objetivo = documento.getString("objetivo") ?: ""
                    val status = documento.getString("status") ?: ""

                    val observacoesProfissionais =
                        documento.getString("observacoesProfissionais") ?: ""

                    val condicoes =
                        documento.getString("condicoes") ?: ""

                    val restricoes =
                        documento.getString("restricoes") ?: ""

                    val alunoExistente =
                        alunoDao.buscarPorFirebaseId(firebaseId)

                    if (alunoExistente != null) {
                        val operacoesPendentes =
                            syncDao.buscarPorRegistro(
                                TIPO_ALUNO,
                                alunoExistente.id
                            )

                        if (operacoesPendentes.isNotEmpty()) {
                            Log.w(
                                TAG,
                                "Aluno ${alunoExistente.id} possui alterações locais pendentes. Leitura remota ignorada."
                            )
                            continue
                        }

                        alunoDao.atualizar(
                            alunoExistente.copy(
                                nome = nome,
                                email = email,
                                dataCadastro = dataCadastro,
                                objetivo = objetivo,
                                status = status,
                                observacoesProfissionais = observacoesProfissionais,
                                condicoes = condicoes,
                                restricoes = restricoes
                            )
                        )

                        Log.i(
                            TAG,
                            "Aluno atualizado no Room: $firebaseId, nome=$nome"
                        )

                    } else {
                        val novoAluno = AlunoEntity(
                            nome = nome,
                            email = email,
                            dataCadastro = dataCadastro,
                            objetivo = objetivo,
                            status = status,
                            observacoesProfissionais = observacoesProfissionais,
                            condicoes = condicoes,
                            restricoes = restricoes,
                            professorId = usuarioLocal.id,
                            firebaseId = firebaseId
                        )

                        alunoDao.inserir(novoAluno)

                        Log.i(
                            TAG,
                            "Novo aluno recebido do Firestore: $firebaseId"
                        )
                    }

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Erro ao processar documento remoto ${documento.id}.",
                        e
                    )
                    throw e
                }
            }

            Log.i(TAG, "Leitura do Firestore concluída com sucesso.")
            true

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro ao sincronizar leitura de alunos do Firestore. UID=$uid",
                e
            )
            false
        }
    }
}
