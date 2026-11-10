
package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class TreinoRepository(
    private val treinoDao: TreinoDao
) {

    // LISTAGEM DE TREINOS

    fun listarTodosTreinos(): Flow<List<TreinoEntity>> {
        return when {
            SessaoUsuario.ehProfessor() -> {
                treinoDao.listarTreinosDoProfessor(
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                treinoDao.listarTreinosDoAluno(alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar treinos."
            )
        }
    }

    fun listarTreinosDoAluno(
        alunoId: Long
    ): Flow<List<TreinoEntity>> {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                treinoDao.listarTreinosDoAlunoPeloProfessor(
                    alunoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val idDaSessao = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                require(idDaSessao == alunoId) {
                    "Você não tem permissão para consultar os treinos deste aluno."
                }

                treinoDao.listarTreinosDoAluno(alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar treinos."
            )
        }
    }

    // EXERCÍCIOS

    fun listarExercicios(
        treinoId: Long
    ): Flow<List<ExercicioEntity>> {

        require(treinoId > 0) {
            "Treino inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                treinoDao.listarExerciciosDoProfessor(
                    treinoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                treinoDao.listarExerciciosDoAluno(
                    treinoId,
                    alunoId
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar exercícios."
            )
        }
    }

    // BUSCAR TREINO

    suspend fun buscarTreinoPorId(
        treinoId: Long
    ): TreinoEntity? {

        require(treinoId > 0) {
            "Treino inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                treinoDao.buscarTreinoDoProfessor(
                    treinoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val alunoId = SessaoUsuario.usuarioAtual?.alunoId
                    ?: throw IllegalStateException(
                        "Conta de aluno sem cadastro vinculado."
                    )

                treinoDao.buscarTreinoDoAluno(
                    treinoId,
                    alunoId
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar este treino."
            )
        }
    }

    // INSERIR TREINO

    suspend fun inserirTreino(
        treino: TreinoEntity
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(treino.alunoId > 0) {
            "Selecione um aluno válido."
        }

        require(treino.nome.isNotBlank()) {
            "Informe o nome do treino."
        }

        require(treino.dataInicio.isNotBlank()) {
            "Informe a data de início."
        }

        require(
            treinoDao.alunoPertenceAoProfessor(
                treino.alunoId,
                professorId
            )
        ) {
            "Este aluno não pertence ao seu cadastro."
        }

        return treinoDao.inserirTreino(
            treino.copy(id = 0)
        )
    }

    // ATUALIZAR TREINO

    suspend fun atualizarTreino(
        treino: TreinoEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(treino.id > 0) {
            "Treino inválido."
        }

        require(treino.nome.isNotBlank()) {
            "Informe o nome do treino."
        }

        require(treino.dataInicio.isNotBlank()) {
            "Informe a data de início."
        }

        val treinoExistente =
            treinoDao.buscarTreinoDoProfessor(
                treino.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Treino não encontrado ou sem permissão para alterá-lo."
            )

        require(treino.alunoId == treinoExistente.alunoId) {
            "Não é permitido transferir o treino para outro aluno."
        }

        treinoDao.atualizarTreino(treino)
    }

    // EXCLUIR TREINO

    suspend fun excluirTreino(
        treino: TreinoEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(treino.id > 0) {
            "Treino inválido."
        }

        val treinoExistente =
            treinoDao.buscarTreinoDoProfessor(
                treino.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Treino não encontrado ou sem permissão para excluí-lo."
            )

        treinoDao.excluirTreino(treinoExistente)
    }

    // INSERIR EXERCÍCIO

    suspend fun inserirExercicio(
        exercicio: ExercicioEntity
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        validarExercicio(exercicio)

        require(
            treinoDao.treinoPertenceAoProfessor(
                exercicio.treinoId,
                professorId
            )
        ) {
            "Este treino não pertence a um aluno seu."
        }

        return treinoDao.inserirExercicio(exercicio)
    }

    // ATUALIZAR EXERCÍCIO

    suspend fun atualizarExercicio(
        exercicio: ExercicioEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        validarExercicio(exercicio)

        val treinoIdOriginal =
            treinoDao.buscarTreinoIdDoExercicio(exercicio.id)
                ?: throw IllegalArgumentException(
                    "Exercício não encontrado."
                )

        require(treinoIdOriginal == exercicio.treinoId) {
            "Não é permitido transferir o exercício para outro treino."
        }

        require(
            treinoDao.exercicioPertenceAoProfessor(
                exercicio.id,
                professorId
            )
        ) {
            "Você não tem permissão para alterar este exercício."
        }

        treinoDao.atualizarExercicio(exercicio)
    }

    // EXCLUIR EXERCÍCIO

    suspend fun excluirExercicio(
        exercicio: ExercicioEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(exercicio.id > 0) {
            "Exercício inválido."
        }

        require(
            treinoDao.exercicioPertenceAoProfessor(
                exercicio.id,
                professorId
            )
        ) {
            "Você não tem permissão para excluir este exercício."
        }

        treinoDao.excluirExercicio(exercicio)
    }

    // EXCLUIR TODOS OS EXERCÍCIOS DE UM TREINO

    suspend fun excluirExerciciosDoTreino(
        treinoId: Long
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(treinoId > 0) {
            "Treino inválido."
        }

        require(
            treinoDao.treinoPertenceAoProfessor(
                treinoId,
                professorId
            )
        ) {
            "Você não tem permissão para alterar este treino."
        }

        treinoDao.excluirExerciciosDoTreino(
            treinoId,
            professorId
        )
    }

    // VALIDAÇÃO

    private fun validarExercicio(
        exercicio: ExercicioEntity
    ) {

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