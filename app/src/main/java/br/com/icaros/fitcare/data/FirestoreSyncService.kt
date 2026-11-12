
package br.com.icaros.fitcare.data

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
        private const val COLLECTION_ALUNOS = "alunos"

        private const val ENTITY_TYPE_ALUNO = "ALUNO"

        private const val OPERATION_INSERT = "INSERT"
        private const val OPERATION_UPDATE = "UPDATE"
        private const val OPERATION_DELETE = "DELETE"
    }

    /**
     * Valida a sessão comparando o UID Firebase
     * com o usuário cadastrado no Room.
     */
    suspend fun validarSessao(): Boolean {
        return try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return false

            val usuarioLocal = usuarioDao.buscarPorFirebaseUid(
                firebaseUser.uid
            ) ?: return false

            usuarioLocal.ativo

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retorna o UID somente se a sessão estiver válida.
     */
    suspend fun obterUidAutenticado(): String? {
        return if (validarSessao()) {
            firebaseAuth.currentUser?.uid
        } else {
            null
        }
    }

    /**
     * Retorna o Firestore somente se existir
     * uma sessão correspondente a uma conta ativa.
     */
    suspend fun obterFirestoreAutenticado(): FirebaseFirestore? {
        return if (validarSessao()) {
            firestore
        } else {
            null
        }
    }

    /**
     * Processa as operações pendentes de alunos.
     *
     * Retorna true quando todas as operações de alunos
     * foram processadas com sucesso.
     *
     * Operações de outros tipos permanecem na fila.
     */
    suspend fun sincronizarAlunos(): Boolean {

        val firebaseUser = firebaseAuth.currentUser
            ?: return false

        val usuarioLocal = usuarioDao.buscarPorFirebaseUid(
            firebaseUser.uid
        ) ?: return false

        if (
            !usuarioLocal.ativo ||
            usuarioLocal.perfil != "PROFESSOR"
        ) {
            return false
        }

        val alunoDao = database.alunoDao()
        val syncDao = database.syncOperationDao()

        val operacoes = syncDao.listarPendentes()
            .filter { it.entityType == ENTITY_TYPE_ALUNO }

        var todasConcluidas = true

        for (operacao in operacoes) {

            try {
                val remoteDocumentId = operacao.remoteDocumentId
                    ?: throw IllegalStateException(
                        "Operação sem identificador remoto."
                    )

                val documento = firestore
                    .collection(COLLECTION_ALUNOS)
                    .document(remoteDocumentId)

                when (operacao.operationType) {

                    OPERATION_INSERT,
                    OPERATION_UPDATE -> {

                        val aluno = alunoDao.buscarPorIdDireto(
                            operacao.localEntityId
                        ) ?: throw IllegalStateException(
                            "Aluno não encontrado no Room."
                        )

                        if (aluno.professorId != usuarioLocal.id) {
                            throw IllegalStateException(
                                "Aluno não pertence ao professor autenticado."
                            )
                        }

                        if (aluno.firebaseId != remoteDocumentId) {
                            throw IllegalStateException(
                                "Identificador remoto não corresponde ao aluno."
                            )
                        }
                        
                        val dadosRemotos = mapOf(
                            "firebaseId" to remoteDocumentId,
                            "professorUid" to firebaseUser.uid,
                            "nome" to aluno.nome,
                            "email" to aluno.email,
                            "dataCadastro" to aluno.dataCadastro
                        )

                        when (operacao.operationType) {

                            OPERATION_INSERT -> {
                                documento.set(dadosRemotos).await()
                            }

                            OPERATION_UPDATE -> {
                                // Atualiza ou cria o documento
                                // preservando outros campos existentes.
                                documento.set(
                                    dadosRemotos,
                                    SetOptions.merge()
                                ).await()
                            }
                        }
                    }

                    OPERATION_DELETE -> {
                        // A exclusão remota também deverá ser
                        // protegida pelas regras do Firestore,
                        // verificando o professorUid do documento.
                        documento.delete().await()
                    }

                    else -> {
                        throw IllegalArgumentException(
                            "Tipo de operação desconhecido: " +
                                    operacao.operationType
                        )
                    }
                }

                // Só remove depois da confirmação do Firestore.
                syncDao.remover(operacao.id)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {

                syncDao.registrarFalha(
                    operacaoId = operacao.id,
                    erro = e.message
                        ?: "Erro desconhecido na sincronização."
                )

                todasConcluidas = false
            }
        }

        return todasConcluidas
    }
}