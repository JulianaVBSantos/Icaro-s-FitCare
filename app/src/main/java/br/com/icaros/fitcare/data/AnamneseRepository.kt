package br.com.icaros.fitcare.data

import kotlinx.coroutines.flow.Flow

class AnamneseRepository(
    private val anamneseDao: AnamneseDao
) {

    fun listarAnamneses(
        alunoId: Long
    ): Flow<List<AnamneseEntity>> {
        return anamneseDao.listarAnamneses(alunoId)
    }

    suspend fun buscarPorId(
        id: Long
    ): AnamneseEntity? {
        return anamneseDao.buscarPorId(id)
    }

    fun listarVersoes(
        alunoId: Long,
        grupoId: String
    ): Flow<List<AnamneseEntity>> {
        return anamneseDao.listarVersoes(alunoId, grupoId)
    }

    suspend fun salvarNovaVersao(
        anamnese: AnamneseEntity
    ): Long {

        require(anamnese.alunoId > 0) {
            "Selecione um aluno válido."
        }

        require(anamnese.grupoId.isNotBlank()) {
            "O identificador da anamnese é obrigatório."
        }

        val ultimaVersao = anamneseDao.obterUltimaVersao(
            alunoId = anamnese.alunoId,
            grupoId = anamnese.grupoId
        ) ?: 0

        val novaVersao = anamnese.copy(
            id = 0,
            versao = ultimaVersao + 1
        )

        return anamneseDao.inserirAnamnese(novaVersao)
    }

    suspend fun importarDocumento(
        documento: DocumentoAnamneseEntity
    ): Long {

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
            documento.tipoArquivo.lowercase() in
                    listOf("pdf", "jpg", "jpeg", "png")
        ) {
            "Formato de arquivo não permitido."
        }

        return anamneseDao.inserirDocumento(documento)
    }

    fun listarDocumentos(
        alunoId: Long
    ): Flow<List<DocumentoAnamneseEntity>> {

        return anamneseDao.listarDocumentos(alunoId)
    }

    suspend fun excluirDocumento(
        id: Long
    ) {
        anamneseDao.excluirDocumento(id)
    }
}