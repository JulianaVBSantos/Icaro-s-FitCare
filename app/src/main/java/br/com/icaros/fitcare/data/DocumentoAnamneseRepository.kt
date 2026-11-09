package br.com.icaros.fitcare.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentoAnamneseRepository(
    private val documentoDao: DocumentoAnamneseDao
) {

    fun listarPorAluno(alunoId: Long) =
        documentoDao.listarPorAluno(alunoId)

    suspend fun buscarPorId(id: Long): DocumentoAnamneseEntity? {
        return documentoDao.buscarPorId(id)
    }

    suspend fun salvarDocumento(
        alunoId: Long,
        nomeArquivo: String,
        caminhoArquivo: String,
        tipoArquivo: String,
        tamanhoArquivo: Long
    ): Long {

        require(alunoId > 0) {
            "Aluno inválido."
        }

        require(nomeArquivo.isNotBlank()) {
            "Nome do arquivo não informado."
        }

        require(caminhoArquivo.isNotBlank()) {
            "Caminho do arquivo não informado."
        }

        val documento = DocumentoAnamneseEntity(
            alunoId = alunoId,
            nomeArquivo = nomeArquivo,
            caminhoArquivo = caminhoArquivo,
            tipoArquivo = tipoArquivo,
            tamanhoArquivo = tamanhoArquivo,
            dataImportacao = SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss",
                Locale.getDefault()
            ).format(Date())
        )

        return documentoDao.inserir(documento)
    }

    suspend fun excluirDocumento(id: Long) {
        require(id > 0) {
            "Documento inválido."
        }

        documentoDao.excluirPorId(id)
    }
}