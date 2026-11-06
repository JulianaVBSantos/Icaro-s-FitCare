
package br.com.icaros.fitcare.data

import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnamneseAtribuidaRepository(
    private val database: FitCareDatabase,
    private val modeloDao: ModeloAnamneseDao,
    private val perguntaModeloDao: PerguntaModeloAnamneseDao,
    private val anamneseDao: AnamneseAtribuidaDao,
    private val perguntaAtribuidaDao: PerguntaAnamneseAtribuidaDao
) {

    private fun agora(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    fun listarPorAluno(alunoId: Long) =
        anamneseDao.listarPorAluno(alunoId)

    fun listarPorStatus(status: String) =
        anamneseDao.listarPorStatus(status)

    suspend fun buscarPorId(id: Long) =
        anamneseDao.buscarPorId(id)

    suspend fun atribuir(
        alunoId: Long,
        modeloId: Long,
        profissionalResponsavel: String = ""
    ): Long {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        return database.withTransaction {

            val modelo = modeloDao.buscarPorId(modeloId)
                ?: throw IllegalArgumentException(
                    "Modelo de anamnese não encontrado."
                )

            require(modelo.ativo) {
                "Este modelo está desativado."
            }

            val perguntas =
                perguntaModeloDao.buscarPorModelo(modeloId)

            require(perguntas.isNotEmpty()) {
                "Adicione perguntas ao modelo antes de atribuí-lo."
            }

            val data = agora()

            val anamnese = AnamneseAtribuidaEntity(
                alunoId = alunoId,
                modeloId = modeloId,
                nomeModelo = modelo.nome,
                status = "NAO_ENVIADA",
                dataCriacao = data,
                profissionalResponsavel =
                    profissionalResponsavel
            )

            val anamneseId =
                anamneseDao.inserir(anamnese)

            val perguntasCopiadas = perguntas.map { pergunta ->
                PerguntaAnamneseAtribuidaEntity(
                    anamneseAtribuidaId = anamneseId,
                    perguntaModeloId = pergunta.id,
                    texto = pergunta.texto,
                    tipoResposta = pergunta.tipoResposta,
                    opcoes = pergunta.opcoes,
                    obrigatoria = pergunta.obrigatoria,
                    ordem = pergunta.ordem
                )
            }

            perguntaAtribuidaDao.inserirTodas(
                perguntasCopiadas
            )

            anamneseId
        }
    }

    suspend fun enviar(id: Long) {
        val anamnese = buscarPorId(id)
            ?: throw IllegalArgumentException(
                "Anamnese não encontrada."
            )

        require(anamnese.status == "NAO_ENVIADA") {
            "Somente anamneses ainda não enviadas podem ser enviadas."
        }

        anamneseDao.atualizarEnvio(
            id = id,
            status = "PENDENTE",
            dataEnvio = agora()
        )
    }

    suspend fun excluir(
        id: Long
    ) {
        val anamnese = buscarPorId(id)
            ?: return

        anamneseDao.excluir(anamnese)
    }
}
