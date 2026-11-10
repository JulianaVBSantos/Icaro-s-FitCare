
package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AlunoRepository(
    private val alunoDao: AlunoDao
) {

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
            status = classificacao
        )

        return alunoDao.inserir(alunoClassificado)
    }

    suspend fun atualizar(aluno: AlunoEntity) {

        val professorId = SessaoUsuario.exigirProfessorId()

        validarAluno(aluno)

        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser atualizado."
        }

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

        val alunoClassificado = aluno.copy(
            professorId = alunoExistente.professorId,
            status = classificacao
        )

        alunoDao.atualizar(alunoClassificado)
    }

    suspend fun excluir(aluno: AlunoEntity) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser excluído."
        }

        val alunoExistente =
            alunoDao.buscarPorIdDoProfessorDireto(
                aluno.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Aluno não encontrado ou sem permissão para excluí-lo."
            )

        alunoDao.excluir(alunoExistente)
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

        alunoDao.atualizar(
            alunoAtualizado.copy(status = classificacao)
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