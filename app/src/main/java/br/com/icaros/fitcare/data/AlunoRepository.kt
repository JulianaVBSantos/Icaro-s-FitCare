package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class AlunoRepository(
    private val alunoDao: AlunoDao
) {

    fun listarAlunos(): Flow<List<AlunoEntity>> {
        return alunoDao.listarAlunos()
    }

    fun buscarPorId(id: Long): Flow<AlunoEntity?> {
        return alunoDao.buscarPorId(id)
    }

    fun buscarPorNome(termo: String): Flow<List<AlunoEntity>> {
        return alunoDao.buscarPorNome(termo)
    }

    suspend fun inserir(aluno: AlunoEntity): Long {
        validarAluno(aluno)

        val classificacao = classificarAluno(
            condicoes = aluno.condicoes,
            restricoes = aluno.restricoes,
            observacoes = aluno.observacoesProfissionais
        )

        val alunoClassificado = aluno.copy(
            status = classificacao
        )

        return alunoDao.inserir(alunoClassificado)
    }

    suspend fun atualizar(aluno: AlunoEntity) {
        validarAluno(aluno)

        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser atualizado."
        }

        val classificacao = classificarAluno(
            condicoes = aluno.condicoes,
            restricoes = aluno.restricoes,
            observacoes = aluno.observacoesProfissionais
        )

        val alunoClassificado = aluno.copy(
            status = classificacao
        )

        alunoDao.atualizar(alunoClassificado)
    }

    suspend fun excluir(aluno: AlunoEntity) {
        require(aluno.id > 0) {
            "O aluno precisa possuir um ID válido para ser excluído."
        }

        alunoDao.excluir(aluno)
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