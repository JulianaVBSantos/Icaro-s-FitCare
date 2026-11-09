
package br.com.icaros.fitcare.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RespostaAnamneseRepository(
    private val respostaDao: RespostaAnamneseDao,
    private val perguntaDao: PerguntaAnamneseAtribuidaDao,
    private val anamneseDao: AnamneseAtribuidaDao
) {

    private fun agora(): String =
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    fun listarPorAnamnese(anamneseId: Long) =
        respostaDao.listarPorAnamnese(anamneseId)

    suspend fun buscarPorPergunta(perguntaId: Long) =
        respostaDao.buscarPorPergunta(perguntaId)

    suspend fun salvarResposta(
        perguntaId: Long,
        resposta: String
    ) {
        val pergunta = perguntaDao.buscarPorId(perguntaId)
            ?: throw IllegalArgumentException(
                "Pergunta não encontrada."
            )

        val anamnese = anamneseDao.buscarPorId(
            pergunta.anamneseAtribuidaId
        ) ?: throw IllegalArgumentException(
            "Anamnese não encontrada."
        )

        require(
            anamnese.status == "PENDENTE" ||
                    anamnese.status == "EM_ANDAMENTO"
        ) {
            "Esta anamnese não está disponível para respostas."
        }

        val data = agora()

        val respostaExistente =
            respostaDao.buscarPorPergunta(perguntaId)

        val registro = if (respostaExistente == null) {
            RespostaAnamneseEntity(
                perguntaAtribuidaId = perguntaId,
                resposta = resposta,
                dataAtualizacao = data
            )
        } else {
            respostaExistente.copy(
                resposta = resposta,
                dataAtualizacao = data
            )
        }

        respostaDao.inserir(registro)

        if (anamnese.status == "PENDENTE") {
            anamneseDao.atualizarInicioResposta(
                id = anamnese.id,
                status = "EM_ANDAMENTO",
                dataInicioResposta = data
            )
        }
    }

    suspend fun concluir(anamneseId: Long) {
        val anamnese = anamneseDao.buscarPorId(anamneseId)
            ?: throw IllegalArgumentException(
                "Anamnese não encontrada."
            )

        require(
            anamnese.status == "PENDENTE" ||
                    anamnese.status == "EM_ANDAMENTO"
        ) {
            "Esta anamnese não pode ser concluída."
        }

        val perguntas =
            perguntaDao.buscarPorAnamnese(anamneseId)

        require(perguntas.isNotEmpty()) {
            "Esta anamnese não possui perguntas."
        }

        for (pergunta in perguntas) {
            if (pergunta.obrigatoria) {
                val resposta =
                    respostaDao.buscarPorPergunta(pergunta.id)

                require(
                    resposta != null &&
                            resposta.resposta.isNotBlank()
                ) {
                    "Preencha todas as perguntas obrigatórias."
                }
            }
        }

        anamneseDao.atualizarConclusao(
            id = anamneseId,
            status = "RESPONDIDA",
            dataResposta = agora()
        )
    }

    suspend fun contarRespostasPreenchidas(
        anamneseId: Long
    ): Int {
        return respostaDao.contarRespostasPreenchidas(
            anamneseId
        )
    }

    suspend fun buscarRespostasDaAnamnese(
        anamneseId: Long
    ): List<RespostaAnamneseEntity> {
        return respostaDao.buscarTodasPorAnamnese(anamneseId)
    }

    suspend fun salvarRespostaDoAluno(
        alunoId: Long,
        perguntaId: Long,
        resposta: String
    ) {
        require(alunoId > 0) {
            "Aluno inválido."
        }

        require(perguntaDao.pertenceAoAluno(perguntaId, alunoId)) {
            "Esta pergunta não pertence ao aluno autenticado."
        }

        salvarResposta(perguntaId, resposta)
    }

    suspend fun concluirDoAluno(
        alunoId: Long,
        anamneseId: Long
    ) {
        require(alunoId > 0) {
            "Aluno inválido."
        }

        require(
            anamneseDao.buscarPorIdDoAluno(anamneseId, alunoId) != null
        ) {
            "Esta anamnese não pertence ao aluno autenticado."
        }

        concluir(anamneseId)
    }
}