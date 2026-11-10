
package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class AnamneseRepository(
    private val anamneseDao: AnamneseDao
) {

    // LISTAR ANAMNESES
    fun listarAnamneses(
        alunoId: Long
    ): Flow<List<AnamneseEntity>> {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                anamneseDao.listarAnamnesesDoProfessor(
                    alunoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                val idAluno = obterAlunoDaSessao()

                require(idAluno == alunoId) {
                    "Você não tem permissão para consultar as anamneses deste aluno."
                }

                anamneseDao.listarAnamnesesDoAluno(alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar anamneses."
            )
        }
    }

    // BUSCAR ANAMNESE POR ID
    suspend fun buscarPorId(
        id: Long
    ): AnamneseEntity? {

        require(id > 0) {
            "Anamnese inválida."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                anamneseDao.buscarPorIdDoProfessor(
                    id,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                anamneseDao.buscarPorIdDoAluno(
                    id,
                    obterAlunoDaSessao()
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar esta anamnese."
            )
        }
    }

    // LISTAR VERSÕES
    fun listarVersoes(
        alunoId: Long,
        grupoId: String
    ): Flow<List<AnamneseEntity>> {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        require(grupoId.isNotBlank()) {
            "Identificador da anamnese inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                anamneseDao.listarVersoesDoProfessor(
                    alunoId,
                    grupoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                require(obterAlunoDaSessao() == alunoId) {
                    "Você não tem permissão para consultar estas versões."
                }

                anamneseDao.listarVersoesDoAluno(
                    alunoId,
                    grupoId
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar versões."
            )
        }
    }

    // SALVAR NOVA VERSÃO
    suspend fun salvarNovaVersao(
        anamnese: AnamneseEntity
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(anamnese.alunoId > 0) {
            "Selecione um aluno válido."
        }

        require(anamnese.grupoId.isNotBlank()) {
            "O identificador da anamnese é obrigatório."
        }

        require(
            anamneseDao.alunoPertenceAoProfessor(
                anamnese.alunoId,
                professorId
            )
        ) {
            "Este aluno não pertence ao seu cadastro."
        }

        val ultimaVersao = anamneseDao.obterUltimaVersaoDoProfessor(
            alunoId = anamnese.alunoId,
            grupoId = anamnese.grupoId,
            professorId = professorId
        ) ?: 0

        val novaVersao = anamnese.copy(
            id = 0,
            versao = ultimaVersao + 1
        )

        return anamneseDao.inserirAnamnese(novaVersao)
    }

    // IMPORTAR DOCUMENTO
    suspend fun importarDocumento(
        documento: DocumentoAnamneseEntity
    ): Long {

        val professorId = SessaoUsuario.exigirProfessorId()

        require(documento.alunoId > 0) {
            "Aluno inválido."
        }

        require(documento.nomeArquivo.isNotBlank()) {
            "O nome do arquivo é obrigatório."
        }

        require(documento.caminhoArquivo.isNotBlank()) {
            "O caminho do arquivo é obrigatório."
        }

        require(
            anamneseDao.alunoPertenceAoProfessor(
                documento.alunoId,
                professorId
            )
        ) {
            "Este aluno não pertence ao seu cadastro."
        }

        val tipoArquivo = documento.tipoArquivo
            .lowercase()
            .removePrefix("comparacao_")

        val formatosPermitidos = listOf(
            "pdf",
            "doc",
            "docx",
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif",
            "bmp",
            "heic",
            "heif",
            "avif"
        )

        require(tipoArquivo in formatosPermitidos) {
            "Formato de arquivo não permitido: ${documento.tipoArquivo}"
        }

        return anamneseDao.inserirDocumento(documento)
    }

    // LISTAR DOCUMENTOS
    fun listarDocumentos(
        alunoId: Long
    ): Flow<List<DocumentoAnamneseEntity>> {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        return when {
            SessaoUsuario.ehProfessor() -> {
                anamneseDao.listarDocumentosDoProfessor(
                    alunoId,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                require(obterAlunoDaSessao() == alunoId) {
                    "Você não tem permissão para consultar estes documentos."
                }

                anamneseDao.listarDocumentosDoAluno(alunoId)
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para consultar documentos."
            )
        }
    }

    // EXCLUIR DOCUMENTO
    suspend fun excluirDocumento(
        id: Long
    ) {

        require(id > 0) {
            "Documento inválido."
        }

        when {
            SessaoUsuario.ehProfessor() -> {
                anamneseDao.excluirDocumentoDoProfessor(
                    id,
                    SessaoUsuario.exigirProfessorId()
                )
            }

            SessaoUsuario.ehAluno() -> {
                anamneseDao.excluirDocumentoDoAluno(
                    id,
                    obterAlunoDaSessao()
                )
            }

            else -> throw IllegalStateException(
                "Usuário sem permissão para excluir documentos."
            )
        }
    }

    // VALIDAR ALUNO DA SESSÃO
    private fun obterAlunoDaSessao(): Long {
        return SessaoUsuario.usuarioAtual?.alunoId
            ?: throw IllegalStateException(
                "Conta de aluno sem cadastro vinculado."
            )
    }
}
