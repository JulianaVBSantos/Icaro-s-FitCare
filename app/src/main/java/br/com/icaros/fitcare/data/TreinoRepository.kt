
package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class TreinoRepository(
    private val treinoDao: TreinoDao
) {

    fun listarTodosTreinos(): Flow<List<TreinoEntity>> =
        treinoDao.listarTodosTreinos()

    fun listarTreinosDoAluno(alunoId: Long): Flow<List<TreinoEntity>> =
        treinoDao.listarTreinosDoAluno(alunoId)

    fun listarExercicios(treinoId: Long): Flow<List<ExercicioEntity>> =
        treinoDao.listarExercicios(treinoId)

    suspend fun buscarTreinoPorId(treinoId: Long): TreinoEntity? =
        treinoDao.buscarTreinoPorId(treinoId)

    suspend fun inserirTreino(treino: TreinoEntity): Long {
        require(treino.alunoId > 0) {
            "Selecione um aluno válido."
        }
        require(treino.nome.isNotBlank()) {
            "Informe o nome do treino."
        }
        require(treino.dataInicio.isNotBlank()) {
            "Informe a data de início."
        }

        return treinoDao.inserirTreino(treino)
    }

    suspend fun atualizarTreino(treino: TreinoEntity) {
        require(treino.nome.isNotBlank()) {
            "Informe o nome do treino."
        }
        require(treino.dataInicio.isNotBlank()) {
            "Informe a data de início."
        }

        treinoDao.atualizarTreino(treino)
    }

    suspend fun excluirTreino(treino: TreinoEntity) {
        treinoDao.excluirTreino(treino)
    }

    suspend fun inserirExercicio(exercicio: ExercicioEntity): Long {
        validarExercicio(exercicio)
        return treinoDao.inserirExercicio(exercicio)
    }

    suspend fun atualizarExercicio(exercicio: ExercicioEntity) {
        validarExercicio(exercicio)
        treinoDao.atualizarExercicio(exercicio)
    }

    suspend fun excluirExerciciosDoTreino(treinoId: Long) {
        treinoDao.excluirExerciciosDoTreino(treinoId)
    }

    private fun validarExercicio(exercicio: ExercicioEntity) {
        require(exercicio.treinoId > 0) {
            "Treino inválido."
        }
        require(exercicio.nome.isNotBlank()) {
            "Informe o nome do exercício."
        }
        require(exercicio.series > 0) {
            "O número de séries deve ser maior que zero."
        }
        require(exercicio.repeticoes.isNotBlank()) {
            "Informe as repetições."
        }
    }
}