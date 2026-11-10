
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

    private fun exigirAlunoId(): Long {
        return SessaoUsuario.exigirAlunoId()
    }

    fun listarPorAnamnese(anamneseId: Long) =
        when {
            SessaoUsuario.ehProfessor() ->
                respostaDao.listarPorAnamneseDoProfessor(
                    anamneseId,
                    SessaoUsuario.exigirProfessorId()
                )

            SessaoUsuario.ehAluno() ->
                respostaDao.listarPorAnamneseDoAluno(
                    anamneseId,
                    exigirAlunoId()
                )

            else -> throw IllegalStateException(
                "Perfil sem permissão para consultar respostas."
            )
        }

    suspend fun buscarPorPergunta(perguntaId: Long) =
        when {
            SessaoUsuario.ehProfessor() ->
                respostaDao.buscarPorPerguntaDoProfessor(
                    perguntaId,
                    SessaoUsuario.exigirProfessorId()
                )

            SessaoUsuario.ehAluno() ->
                respostaDao.buscarPorPerguntaDoAluno(
                    perguntaId,
                    exigirAlunoId()
                )

            else -> throw IllegalStateException(
                "Perfil sem permissão para consultar respostas."
            )
        }

    suspend fun salvarResposta(
        perguntaId: Long,
        resposta: String
    ) {
        salvarRespostaDoAluno(
            alunoId = exigirAlunoId(),
            perguntaId = perguntaId,
            resposta = resposta
        )
    }

    suspend fun salvarRespostaDoAluno(
        alunoId: Long,
        perguntaId: Long,
        resposta: String
    ) {
        val alunoAutenticado = exigirAlunoId()

        require(alunoId == alunoAutenticado) {
            "Não é permitido responder por outro aluno."
        }

        val pergunta = perguntaDao.buscarPorIdDoAluno(
            perguntaId,
            alunoAutenticado
        ) ?: throw IllegalArgumentException(
            "Pergunta não encontrada ou sem permissão de acesso."
        )

        val anamnese = anamneseDao.buscarPorIdDoAluno(
            pergunta.anamneseAtribuidaId,
            alunoAutenticado
        ) ?: throw IllegalArgumentException(
            "Anamnese não encontrada ou sem permissão de acesso."
        )

        require(
            anamnese.status == "PENDENTE" ||
                    anamnese.status == "EM_ANDAMENTO"
        ) {
            "Esta anamnese não está disponível para respostas."
        }

        val resultado = respostaDao.salvarRespostaDoAluno(
            perguntaId = perguntaId,
            alunoId = alunoAutenticado,
            resposta = resposta,
            dataAtualizacao = agora()
        )

        require(resultado > 0) {
            "Não foi possível salvar a resposta. Verifique se a anamnese ainda está disponível."
        }

        if (anamnese.status == "PENDENTE") {
            anamneseDao.atualizarInicioRespostaDoAluno(
                id = anamnese.id,
                status = "EM_ANDAMENTO",
                dataInicioResposta = agora(),
                alunoId = alunoAutenticado
            )
        }
    }

    suspend fun concluir(anamneseId: Long) {
        concluirDoAluno(
            alunoId = exigirAlunoId(),
            anamneseId = anamneseId
        )
    }

    suspend fun concluirDoAluno(
        alunoId: Long,
        anamneseId: Long
    ) {
        val alunoAutenticado = exigirAlunoId()

        require(alunoId == alunoAutenticado) {
            "Não é permitido concluir a anamnese de outro aluno."
        }

        val anamnese = anamneseDao.buscarPorIdDoAluno(
            anamneseId,
            alunoAutenticado
        ) ?: throw IllegalArgumentException(
            "Anamnese não encontrada ou sem permissão de acesso."
        )

        require(
            anamnese.status == "PENDENTE" ||
                    anamnese.status == "EM_ANDAMENTO"
        ) {
            "Esta anamnese não pode ser concluída."
        }

        val perguntas = perguntaDao.buscarPorAnamneseDoAluno(
            anamneseId,
            alunoAutenticado
        )

        require(perguntas.isNotEmpty()) {
            "Esta anamnese não possui perguntas."
        }

        for (pergunta in perguntas) {
            if (pergunta.obrigatoria) {
                val resposta =
                    respostaDao.buscarPorPerguntaDoAluno(
                        pergunta.id,
                        alunoAutenticado
                    )

                require(
                    resposta != null &&
                            resposta.resposta.isNotBlank()
                ) {
                    "Preencha todas as perguntas obrigatórias."
                }
            }
        }

        anamneseDao.atualizarConclusaoDoAluno(
            id = anamneseId,
            status = "RESPONDIDA",
            dataResposta = agora(),
            alunoId = alunoAutenticado
        )
    }

    suspend fun contarRespostasPreenchidas(
        anamneseId: Long
    ): Int {
        return when {
            SessaoUsuario.ehProfessor() ->
                respostaDao.contarRespostasPreenchidasDoProfessor(
                    anamneseId,
                    SessaoUsuario.exigirProfessorId()
                )

            SessaoUsuario.ehAluno() ->
                respostaDao.contarRespostasPreenchidasDoAluno(
                    anamneseId,
                    exigirAlunoId()
                )

            else -> throw IllegalStateException(
                "Perfil sem permissão para consultar respostas."
            )
        }
    }

    suspend fun buscarRespostasDaAnamnese(
        anamneseId: Long
    ): List<RespostaAnamneseEntity> {
        return when {
            SessaoUsuario.ehProfessor() ->
                respostaDao.buscarTodasPorAnamneseDoProfessor(
                    anamneseId,
                    SessaoUsuario.exigirProfessorId()
                )

            SessaoUsuario.ehAluno() ->
                respostaDao.buscarTodasPorAnamneseDoAluno(
                    anamneseId,
                    exigirAlunoId()
                )

            else -> throw IllegalStateException(
                "Perfil sem permissão para consultar respostas."
            )
        }
    }
}
