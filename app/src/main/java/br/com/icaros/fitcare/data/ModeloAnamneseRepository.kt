
package br.com.icaros.fitcare.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ModeloAnamneseRepository(
    private val dao: ModeloAnamneseDao
) {

    private fun agora(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    private fun agoraMillis(): Long = System.currentTimeMillis()

    fun listarTodos(): Flow<List<ModeloAnamneseEntity>> {
        return dao.listarTodosDoProfessor(
            SessaoUsuario.exigirProfessorId()
        )
    }

    fun listarAtivos(): Flow<List<ModeloAnamneseEntity>> {
        return dao.listarAtivosDoProfessor(
            SessaoUsuario.exigirProfessorId()
        )
    }

    fun listarPersonalizados(): Flow<List<ModeloAnamneseEntity>> {
        return dao.listarPersonalizadosDoProfessor(
            SessaoUsuario.exigirProfessorId()
        )
    }

    suspend fun buscarPorId(id: Long): ModeloAnamneseEntity? {
        require(id > 0) { "Modelo inválido." }

        return dao.buscarPorIdDoProfessor(
            id,
            SessaoUsuario.exigirProfessorId()
        )
    }

    suspend fun salvar(modelo: ModeloAnamneseEntity): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(modelo.nome.isNotBlank()) {
            "Informe o nome do modelo."
        }

        require(
            modelo.categoria == "PADRAO" ||
                    modelo.categoria == "PERSONALIZADO"
        ) {
            "Categoria de modelo inválida."
        }

        val data = agora()
        val millis = agoraMillis()

        return if (modelo.id == 0L) {

            val novoModelo = modelo.copy(
                id = 0L,
                nome = modelo.nome.trim(),
                professorId = professorId,
                dataCriacao = data,
                dataAtualizacao = data,
                firestoreId = UUID.randomUUID().toString(),
                excluido = false,
                updatedAtMillis = millis,
                deletedAtMillis = null
            )

            dao.inserir(novoModelo)

        } else {

            val existente = dao.buscarModeloEditavelDoProfessor(
                modelo.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Modelo não encontrado ou você não tem permissão para editá-lo."
            )

            val atualizado = existente.copy(
                nome = modelo.nome.trim(),
                descricao = modelo.descricao,
                ativo = modelo.ativo,
                dataAtualizacao = data,
                updatedAtMillis = millis
            )

            dao.atualizar(atualizado)

            existente.id
        }
    }

    suspend fun excluir(modelo: ModeloAnamneseEntity) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(modelo.id > 0) {
            "Modelo inválido."
        }

        val existente = dao.buscarPorIdDoProfessor(
            modelo.id,
            professorId
        ) ?: throw IllegalArgumentException(
            "Modelo não encontrado ou indisponível."
        )

        when (existente.categoria) {

            "PADRAO" -> {
                val linhas = dao.excluirModeloPadrao(existente.id)

                check(linhas > 0) {
                    "Não foi possível excluir o modelo padrão."
                }
            }

            "PERSONALIZADO" -> {
                val data = agora()
                val millis = agoraMillis()

                val linhas = dao.marcarPersonalizadoComoExcluido(
                    id = existente.id,
                    professorId = professorId,
                    dataAtualizacao = data,
                    updatedAtMillis = millis
                )

                check(linhas > 0) {
                    "Não foi possível excluir o modelo."
                }
            }

            else -> {
                throw IllegalArgumentException(
                    "Categoria de modelo inválida."
                )
            }
        }
    }

    suspend fun alterarAtivo(
        id: Long,
        ativo: Boolean
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(id > 0) {
            "Modelo inválido."
        }

        val modelo = dao.buscarPersonalizadoDoProfessor(
            id,
            professorId
        ) ?: throw IllegalArgumentException(
            "Modelo não encontrado ou você não tem permissão para alterá-lo."
        )

        val data = agora()
        val millis = agoraMillis()

        dao.atualizarStatusPersonalizado(
            id = modelo.id,
            ativo = ativo,
            dataAtualizacao = data,
            professorId = professorId
        )

        dao.atualizarDataAlteracaoParaSincronizacao(
            modeloId = modelo.id,
            professorId = professorId,
            dataAtualizacao = data,
            updatedAtMillis = millis
        )
    }
}