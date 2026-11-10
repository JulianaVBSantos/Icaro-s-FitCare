
package br.com.icaros.fitcare.data

class PerguntaModeloAnamneseRepository(
    private val dao: PerguntaModeloAnamneseDao
) {

    fun listarPorModelo(modeloId: Long) =
        dao.listarPorModeloDoProfessor(
            modeloId,
            SessaoUsuario.exigirProfessorId()
        )

    suspend fun buscarPorModelo(modeloId: Long) =
        dao.buscarPorModeloDoProfessor(
            modeloId,
            SessaoUsuario.exigirProfessorId()
        )

    suspend fun buscarPorId(id: Long) =
        dao.buscarPorIdDoProfessor(
            id,
            SessaoUsuario.exigirProfessorId()
        )

    suspend fun salvar(
        pergunta: PerguntaModeloAnamneseEntity
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(pergunta.texto.isNotBlank()) {
            "Informe o texto da pergunta."
        }

        val modeloRepository = dao

        val perguntasDoModelo =
            modeloRepository.buscarPorModeloDoProfessor(
                pergunta.modeloId,
                professorId
            )

        require(
            pergunta.id == 0L ||
                    perguntasDoModelo.any { it.id == pergunta.id }
        ) {
            "Pergunta não encontrada ou você não tem permissão para editá-la."
        }

        return if (pergunta.id == 0L) {

            dao.inserir(
                pergunta.copy(
                    texto = pergunta.texto.trim()
                )
            )

        } else {

            dao.atualizar(
                pergunta.copy(
                    texto = pergunta.texto.trim()
                )
            )

            pergunta.id
        }
    }

    suspend fun excluir(
        pergunta: PerguntaModeloAnamneseEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        val perguntaExistente =
            dao.buscarPorIdDoProfessor(
                pergunta.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Pergunta não encontrada ou você não tem permissão para excluí-la."
            )

        require(
            perguntaExistente.modeloId == pergunta.modeloId
        ) {
            "A pergunta informada não corresponde ao modelo."
        }

        val perguntasDoModelo =
            dao.buscarPorModeloDoProfessor(
                pergunta.modeloId,
                professorId
            )

        require(
            perguntasDoModelo.any { it.id == pergunta.id }
        ) {
            "Você não tem permissão para excluir esta pergunta."
        }

        dao.excluir(perguntaExistente)
    }

    suspend fun salvarOrdenacao(
        perguntas: List<PerguntaModeloAnamneseEntity>
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        if (perguntas.isEmpty()) return

        val modeloIds = perguntas.map { it.modeloId }.distinct()

        require(modeloIds.size == 1) {
            "As perguntas devem pertencer ao mesmo modelo."
        }

        val modeloId = modeloIds.first()

        val perguntasPermitidas =
            dao.buscarPorModeloDoProfessor(
                modeloId,
                professorId
            )

        val idsPermitidos = perguntasPermitidas.map { it.id }.toSet()

        require(
            perguntas.all { it.id in idsPermitidos }
        ) {
            "Existem perguntas que não pertencem a este modelo."
        }

        val ordenadas = perguntas.mapIndexed { index, pergunta ->
            pergunta.copy(ordem = index)
        }

        dao.atualizarTodas(ordenadas)
    }

    suspend fun excluirTodas(modeloId: Long) {

        val professorId = SessaoUsuario.exigirProfessorId()

        val perguntas =
            dao.buscarPorModeloDoProfessor(
                modeloId,
                professorId
            )

        require(perguntas.isNotEmpty()) {
            "Modelo não encontrado ou sem perguntas."
        }

        dao.excluirTodasDoModeloDoProfessor(
            modeloId = modeloId,
            professorId = professorId
        )
    }
}