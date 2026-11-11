package br.com.icaros.fitcare.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow

class ModeloAnamneseRepository(
    private val dao: ModeloAnamneseDao
) {

    private fun agora(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    // LISTAR MODELOS

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

    // BUSCAR MODELO

    suspend fun buscarPorId(
        id: Long
    ): ModeloAnamneseEntity? {

        require(id > 0) {
            "Modelo inválido."
        }

        return dao.buscarPorIdDoProfessor(
            id,
            SessaoUsuario.exigirProfessorId()
        )
    }

    // SALVAR MODELO

    suspend fun salvar(
        modelo: ModeloAnamneseEntity
    ): Long {

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

        return if (modelo.id == 0L) {

            // Permite criar vários modelos padrão ou personalizados.
            val novoModelo = modelo.copy(
                id = 0L,
                nome = modelo.nome.trim(),
                categoria = modelo.categoria,
                professorId = professorId,
                dataCriacao = data,
                dataAtualizacao = data
            )

            dao.inserir(novoModelo)

        } else {

            // Somente modelos personalizados podem ser editados.
            val modeloExistente =
                dao.buscarPersonalizadoDoProfessor(
                    modelo.id,
                    professorId
                ) ?: throw IllegalArgumentException(
                    "Modelo não encontrado ou você não tem permissão para editá-lo."
                )

            val modeloAtualizado = modeloExistente.copy(
                nome = modelo.nome.trim(),
                descricao = modelo.descricao,
                ativo = modelo.ativo,
                dataAtualizacao = data
            )

            dao.atualizar(modeloAtualizado)

            modeloExistente.id
        }
    }

    // EXCLUIR MODELO PADRÃO OU PERSONALIZADO

    suspend fun excluir(
        modelo: ModeloAnamneseEntity
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(modelo.id > 0) {
            "Modelo inválido."
        }

        // Confere se o modelo está disponível para este professor.
        val modeloExistente =
            dao.buscarPorIdDoProfessor(
                modelo.id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Modelo não encontrado ou indisponível."
            )

        // Exclui conforme a categoria.
        val linhasAfetadas = when (modeloExistente.categoria) {

            "PADRAO" -> {
                dao.excluirModeloPadrao(
                    modeloExistente.id
                )
            }

            "PERSONALIZADO" -> {
                dao.excluirPersonalizado(
                    modeloExistente.id,
                    professorId
                )
            }

            else -> {
                throw IllegalArgumentException(
                    "Categoria de modelo inválida."
                )
            }
        }

        check(linhasAfetadas > 0) {
            "Não foi possível excluir o modelo."
        }
    }

    // ATIVAR OU DESATIVAR MODELO PERSONALIZADO

    suspend fun alterarAtivo(
        id: Long,
        ativo: Boolean
    ) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(id > 0) {
            "Modelo inválido."
        }

        val modelo =
            dao.buscarPersonalizadoDoProfessor(
                id,
                professorId
            ) ?: throw IllegalArgumentException(
                "Modelo não encontrado ou você não tem permissão para alterá-lo."
            )

        dao.atualizarStatusPersonalizado(
            id = modelo.id,
            ativo = ativo,
            dataAtualizacao = agora(),
            professorId = professorId
        )
    }
}