package br.com.icaros.fitcare.data

import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

    private fun agoraMillis(): Long = System.currentTimeMillis()

    fun listarPorAluno(alunoId: Long) = when {
        SessaoUsuario.ehProfessor() -> {
            require(alunoId > 0) {
                "Aluno inválido."
            }

            anamneseDao.listarPorAlunoDoProfessor(
                alunoId,
                SessaoUsuario.exigirProfessorId()
            )
        }

        SessaoUsuario.ehAluno() -> {
            val idAluno = obterAlunoDaSessao()

            require(idAluno == alunoId) {
                "Você não tem permissão para consultar as anamneses deste aluno."
            }

            anamneseDao.listarPorAluno(idAluno)
        }

        else -> throw IllegalStateException(
            "Usuário sem permissão para consultar anamneses."
        )
    }

    fun listarPorStatus(status: String) = when {
        SessaoUsuario.ehProfessor() -> {
            require(status.isNotBlank()) {
                "Informe um status válido."
            }

            anamneseDao.listarPorStatusDoProfessor(
                status,
                SessaoUsuario.exigirProfessorId()
            )
        }

        SessaoUsuario.ehAluno() -> {
            require(status.isNotBlank()) {
                "Informe um status válido."
            }

            anamneseDao.listarPorStatusDoAluno(
                status,
                obterAlunoDaSessao()
            )
        }

        else -> throw IllegalStateException(
            "Usuário sem permissão para consultar anamneses."
        )
    }

    suspend fun buscarPorId(
        id: Long
    ) = when {
        SessaoUsuario.ehProfessor() -> {
            require(id > 0) {
                "Anamnese inválida."
            }

            anamneseDao.buscarPorIdDoProfessor(
                id,
                SessaoUsuario.exigirProfessorId()
            )
        }

        SessaoUsuario.ehAluno() -> {
            require(id > 0) {
                "Anamnese inválida."
            }

            anamneseDao.buscarPorIdDoAluno(
                id,
                obterAlunoDaSessao()
            )
        }

        else -> throw IllegalStateException(
            "Usuário sem permissão para consultar esta anamnese."
        )
    }

    suspend fun buscarPerguntas(
        anamneseId: Long
    ): List<PerguntaAnamneseAtribuidaEntity> {

        require(anamneseId > 0) {
            "Anamnese inválida."
        }

        val anamnese = buscarPorId(anamneseId)
            ?: throw IllegalArgumentException(
                "Anamnese não encontrada ou sem permissão de acesso."
            )

        return when {
            SessaoUsuario.ehProfessor() -> {
                perguntaAtribuidaDao.buscarPorAnamneseDoProfessor(
                    anamnese.id,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                perguntaAtribuidaDao.buscarPorAnamneseDoAluno(
                    anamnese.id,
                    obterAlunoDaSessao()
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar estas perguntas."
            )
        }
    }

    suspend fun atribuir(
        alunoId: Long,
        modeloId: Long,
        profissionalResponsavel: String = ""
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(alunoId > 0) {
            "Aluno inválido."
        }

        require(modeloId > 0) {
            "Modelo de anamnese inválido."
        }

        return database.withTransaction {
            require(
                anamneseDao.alunoPertenceAoProfessor(
                    alunoId,
                    professorId
                )
            ) {
                "Este aluno não pertence ao seu cadastro."
            }

            val modelo = modeloDao.buscarPorIdDoProfessor(
                modeloId,
                professorId
            ) ?: throw IllegalArgumentException(
                "Modelo não encontrado ou sem permissão de acesso."
            )

            require(modelo.ativo) {
                "Este modelo está desativado."
            }

            val perguntas = perguntaModeloDao.buscarPorModeloDoProfessor(
                modeloId,
                professorId
            )

            require(perguntas.isNotEmpty()) {
                "Adicione perguntas ao modelo antes de atribuí-lo."
            }

            val data = agora()
            val millis = agoraMillis()

            val anamnese = AnamneseAtribuidaEntity(
                alunoId = alunoId,
                modeloId = modeloId,
                nomeModelo = modelo.nome,
                status = "NAO_ENVIADA",
                dataCriacao = data,
                profissionalResponsavel = profissionalResponsavel,
                firestoreId = UUID.randomUUID().toString(),
                updatedAtMillis = millis,
                excluida = false
            )

            val anamneseId = anamneseDao.inserirDoProfessor(
                anamnese,
                professorId
            )

            val perguntasCopiadas = perguntas.map { pergunta ->
                PerguntaAnamneseAtribuidaEntity(
                    anamneseAtribuidaId = anamneseId,
                    perguntaModeloId = pergunta.id,
                    texto = pergunta.texto,
                    tipoResposta = pergunta.tipoResposta,
                    opcoes = pergunta.opcoes,
                    obrigatoria = pergunta.obrigatoria,
                    ordem = pergunta.ordem,
                    firestoreId = UUID.randomUUID().toString()
                )
            }

            perguntaAtribuidaDao.inserirTodasDoProfessor(
                perguntasCopiadas,
                professorId
            )

            anamneseId
        }
    }

    suspend fun enviar(id: Long) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(id > 0) {
            "Anamnese inválida."
        }

        val anamnese = anamneseDao.buscarPorIdDoProfessor(
            id,
            professorId
        ) ?: throw IllegalArgumentException(
            "Anamnese não encontrada ou sem permissão para enviá-la."
        )

        require(anamnese.status == "NAO_ENVIADA") {
            "Somente anamneses ainda não enviadas podem ser enviadas."
        }

        val linhasAfetadas = anamneseDao.atualizarEnvioDoProfessor(
            id = id,
            status = "PENDENTE",
            dataEnvio = agora(),
            professorId = professorId,
            updatedAtMillis = agoraMillis()
        )

        check(linhasAfetadas == 1) {
            "Não foi possível enviar a anamnese. Verifique se ela ainda não foi enviada."
        }
    }

    // Exclusão lógica permitida em qualquer status (incluindo RESPONDIDA),
    // preservando os registros de respostas no banco e sincronizando a exclusão.
    suspend fun excluir(id: Long) {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(id > 0) {
            "Anamnese inválida."
        }

        val anamnese = anamneseDao.buscarPorIdDoProfessor(
            id,
            professorId
        ) ?: throw IllegalArgumentException(
            "Anamnese não encontrada ou sem permissão para excluí-la."
        )

        val linhasAfetadas = anamneseDao.excluirLogicamenteDoProfessor(
            id = anamnese.id,
            professorId = professorId,
            updatedAtMillis = agoraMillis()
        )

        check(linhasAfetadas == 1) {
            "Não foi possível excluir a anamnese."
        }
    }

    private fun obterAlunoDaSessao(): Long {
        return SessaoUsuario.usuarioAtual?.alunoId
            ?: throw IllegalStateException(
                "Conta de aluno sem cadastro vinculado."
            )
    }
}