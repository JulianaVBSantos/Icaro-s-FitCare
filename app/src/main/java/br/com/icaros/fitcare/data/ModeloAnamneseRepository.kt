
package br.com.icaros.fitcare.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ModeloAnamneseRepository(
    private val dao: ModeloAnamneseDao
) {
    private fun agora(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    fun listarTodos() = dao.listarTodos()

    fun listarAtivos() = dao.listarAtivos()

    suspend fun buscarPorId(id: Long) =
        dao.buscarPorId(id)

    suspend fun salvar(
        modelo: ModeloAnamneseEntity
    ): Long {
        val data = agora()

        return if (modelo.id == 0L) {
            require(modelo.nome.isNotBlank()) {
                "Informe o nome do modelo."
            }

            dao.inserir(
                modelo.copy(
                    nome = modelo.nome.trim(),
                    dataCriacao = data,
                    dataAtualizacao = data
                )
            )
        } else {
            require(modelo.nome.isNotBlank()) {
                "Informe o nome do modelo."
            }

            dao.atualizar(
                modelo.copy(
                    nome = modelo.nome.trim(),
                    dataAtualizacao = data
                )
            )

            modelo.id
        }
    }

    suspend fun excluir(
        modelo: ModeloAnamneseEntity
    ) {
        dao.excluir(modelo)
    }

    suspend fun alterarAtivo(
        id: Long,
        ativo: Boolean
    ) {
        dao.atualizarStatus(
            id = id,
            ativo = ativo,
            dataAtualizacao = agora()
        )
    }
}
