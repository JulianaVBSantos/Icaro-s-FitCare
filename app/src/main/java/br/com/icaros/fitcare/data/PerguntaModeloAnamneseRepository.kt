
package br.com.icaros.fitcare.data

class PerguntaModeloAnamneseRepository(
    private val dao: PerguntaModeloAnamneseDao
) {

    fun listarPorModelo(modeloId: Long) =
        dao.listarPorModelo(modeloId)

    suspend fun buscarPorModelo(modeloId: Long) =
        dao.buscarPorModelo(modeloId)

    suspend fun buscarPorId(id: Long) =
        dao.buscarPorId(id)

    suspend fun salvar(
        pergunta: PerguntaModeloAnamneseEntity
    ): Long {
        require(pergunta.texto.isNotBlank()) {
            "Informe o texto da pergunta."
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
        dao.excluir(pergunta)
    }

    suspend fun salvarOrdenacao(
        perguntas: List<PerguntaModeloAnamneseEntity>
    ) {
        val ordenadas = perguntas.mapIndexed { index, pergunta ->
            pergunta.copy(ordem = index)
        }

        dao.atualizarTodas(ordenadas)
    }

    suspend fun excluirTodas(modeloId: Long) {
        dao.excluirTodasDoModelo(modeloId)
    }
}