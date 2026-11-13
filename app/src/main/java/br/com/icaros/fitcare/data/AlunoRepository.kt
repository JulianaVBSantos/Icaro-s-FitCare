
package br.com.icaros.fitcare.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AlunoRepository(
    private val database: FitCareDatabase,
    private val context: Context
) {

    private val alunoDao: AlunoDao
        get() = database.alunoDao()

    private val syncDao: SyncOperationDao
        get() = database.syncOperationDao()

    companion object {
        private const val ENTITY_TYPE_ALUNO = "ALUNO"

        private const val OPERATION_INSERT = "INSERT"
        private const val OPERATION_UPDATE = "UPDATE"
        private const val OPERATION_DELETE = "DELETE"
    }

    fun listarAlunos(): Flow<List<AlunoEntity>> {
        return when {
            SessaoUsuario.ehProfessor() -> {
                alunoDao.listarAlunosDoProfessor(
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                alunoDao.listarAlunoDoAluno(alunoId).map { aluno ->
                    listOfNotNull(aluno)
                }
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar alunos."
            )
        }
    }

    fun buscarPorId(id: Long): Flow<AlunoEntity?> {
        require(id > 0) { "Aluno inválido." }

        return when {
            SessaoUsuario.ehProfessor() -> {
                alunoDao.buscarPorIdDoProfessor(
                    id,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                alunoDao.buscarPorIdDoAluno(id, alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar este aluno."
            )
        }
    }

    fun buscarPorNome(termo: String): Flow<List<AlunoEntity>> {
        return when {
            SessaoUsuario.ehProfessor() -> {
                alunoDao.buscarPorNomeDoProfessor(
                    termo,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                alunoDao.buscarPorNomeDoAluno(termo, alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para pesquisar alunos."
            )
        }
    }

    suspend fun inserir(aluno: AlunoEntity): Long {
        val professorId = SessaoUsuario.exigirProfessorId()

        validarAluno(aluno)

        val classificacao = classificarAluno(
            condicoes = aluno.condicoes,
            restricoes = aluno.restricoes,
            observacoes = aluno.observacoesProfissionais
        )

        val alunoClassificado = aluno.copy(
            id = 0,
            professorId = professorId,
            status = classificacao,
            firebaseId = UUID.randomUUID().toString()
        )

        val novoId = database.withTransaction {
            val id = alunoDao.inserir(alunoClassificado)

            registrarOperacao(
                localEntityId = id,
                remoteDocumentId = alunoClassificado.firebaseId,
                operationType = OPERATION_INSERT
            )

            id
        }

        // Agenda somente depois de confirmar a transação Room.
        AlunoSyncWorker.agendar(context)

        return novoId
    }

    suspend fun atualizar(aluno: AlunoEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()

        validarAluno(aluno)

        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser atualizado."
        }

        database.withTransaction {
            val alunoExistente =
                alunoDao.buscarPorIdDoProfessorDireto(
                    aluno.id,
                    professorId
                ) ?: throw IllegalArgumentException(
                    "Aluno não encontrado ou sem permissão para alterá-lo."
                )

            val classificacao = classificarAluno(
                condicoes = aluno.condicoes,
                restricoes = aluno.restricoes,
                observacoes = aluno.observacoesProfissionais
            )

            // Alunos antigos ainda sem firebaseId recebem um UUID.
            // Nesse caso, a primeira operação remota será INSERT.
            val firebaseId = alunoExistente.firebaseId
                ?: UUID.randomUUID().toString()

            val alunoClassificado = aluno.copy(
                professorId = alunoExistente.professorId,
                status = classificacao,
                firebaseId = firebaseId
            )

            alunoDao.atualizar(alunoClassificado)

            registrarOperacao(
                localEntityId = alunoClassificado.id,
                remoteDocumentId = firebaseId,
                operationType = if (alunoExistente.firebaseId == null) {
                    OPERATION_INSERT
                } else {
                    OPERATION_UPDATE
                }
            )
        }

        // Agenda somente depois de confirmar a transação Room.
        AlunoSyncWorker.agendar(context)
    }

    suspend fun excluir(aluno: AlunoEntity) {
        val professorId = SessaoUsuario.exigirProfessorId()

        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser excluído."
        }

        database.withTransaction {
            val alunoExistente =
                alunoDao.buscarPorIdDoProfessorDireto(
                    aluno.id,
                    professorId
                ) ?: throw IllegalArgumentException(
                    "Aluno não encontrado ou sem permissão para excluí-lo."
                )

            val operacoesPendentes = syncDao.buscarPorRegistro(
                entityType = ENTITY_TYPE_ALUNO,
                localEntityId = alunoExistente.id
            )

            if (
                alunoExistente.firebaseId != null &&
                operacoesPendentes.none {
                    it.operationType == OPERATION_INSERT
                }
            ) {
                // O aluno já existe remotamente.
                // Registrar DELETE para futura sincronização.
                syncDao.removerPorRegistro(
                    ENTITY_TYPE_ALUNO,
                    alunoExistente.id
                )

                syncDao.inserir(
                    SyncOperationEntity(
                        entityType = ENTITY_TYPE_ALUNO,
                        localEntityId = alunoExistente.id,
                        remoteDocumentId = alunoExistente.firebaseId,
                        operationType = OPERATION_DELETE
                    )
                )
            } else {
                // Aluno ainda não enviado ao Firebase.
                // Não é necessário enviar DELETE.
                syncDao.removerPorRegistro(
                    ENTITY_TYPE_ALUNO,
                    alunoExistente.id
                )
            }

            alunoDao.excluir(alunoExistente)
        }

        // Agenda depois da exclusão e do registro da operação.
        AlunoSyncWorker.agendar(context)
    }

    /**
     * Consolida as operações pendentes de um aluno.
     *
     * Se já existe INSERT, mantém INSERT.
     * Se o aluno já existe remotamente, registra UPDATE.
     *
     * Esta função deve ser chamada dentro de uma transação Room.
     */
    private suspend fun registrarOperacao(
        localEntityId: Long,
        remoteDocumentId: String?,
        operationType: String
    ) {
        val operacoesExistentes = syncDao.buscarPorRegistro(
            entityType = ENTITY_TYPE_ALUNO,
            localEntityId = localEntityId
        )

        val possuiInsertPendente = operacoesExistentes.any {
            it.operationType == OPERATION_INSERT
        }

        val operacaoFinal = if (
            operationType == OPERATION_INSERT ||
            possuiInsertPendente
        ) {
            OPERATION_INSERT
        } else {
            operationType
        }

        syncDao.removerPorRegistro(
            entityType = ENTITY_TYPE_ALUNO,
            localEntityId = localEntityId
        )

        syncDao.inserir(
            SyncOperationEntity(
                entityType = ENTITY_TYPE_ALUNO,
                localEntityId = localEntityId,
                remoteDocumentId = remoteDocumentId,
                operationType = operacaoFinal
            )
        )
    }

    suspend fun atualizarAcompanhamentoPorAnamnese(
        alunoId: Long,
        condicoes: String,
        restricoes: String,
        observacoes: String
    ) {
        require(alunoId > 0) {
            "Selecione um aluno válido."
        }

        val alunoAtual = when {
            SessaoUsuario.ehProfessor() -> {
                alunoDao.buscarPorIdDoProfessorDireto(
                    alunoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val idDaSessao = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                if (idDaSessao != alunoId) {
                    throw IllegalArgumentException(
                        "Você não tem permissão para alterar este aluno."
                    )
                }

                alunoDao.buscarPorIdDireto(alunoId)
            }

            else -> null

        } ?: throw IllegalArgumentException(
            "Aluno não encontrado ou sem permissão."
        )

        val alunoAtualizado = alunoAtual.copy(
            condicoes = condicoes.ifBlank {
                alunoAtual.condicoes
            },
            restricoes = restricoes.ifBlank {
                alunoAtual.restricoes
            },
            observacoesProfissionais = observacoes.ifBlank {
                alunoAtual.observacoesProfissionais
            }
        )

        val classificacao = classificarAluno(
            condicoes = alunoAtualizado.condicoes,
            restricoes = alunoAtualizado.restricoes,
            observacoes = alunoAtualizado.observacoesProfissionais
        )

        // Dados de saúde permanecem somente no Room nesta etapa.
        alunoDao.atualizar(
            alunoAtualizado.copy(status = classificacao)
        )
    }
    suspend fun buscarPorIdDireto(id: Long): AlunoEntity? {
        if (id <= 0) return null
        return alunoDao.buscarPorIdDireto(id)
    }

    suspend fun atualizarDireto(aluno: AlunoEntity) {
        atualizarAcompanhamentoPorAnamnese(
            alunoId = aluno.id,
            condicoes = aluno.condicoes,
            restricoes = aluno.restricoes,
            observacoes = aluno.observacoesProfissionais
        )
    }
    private fun validarAluno(aluno: AlunoEntity) {
        require(aluno.nome.isNotBlank()) {
            "O nome do aluno é obrigatório."
        }

        require(aluno.email.isNotBlank()) {
            "O e-mail do aluno é obrigatório."
        }

        require(aluno.objetivo.isNotBlank()) {
            "O objetivo do aluno é obrigatório."
        }
    }
}
