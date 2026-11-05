
package br.com.icaros.fitcare

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private suspend fun reconhecerTexto(
    bitmap: Bitmap
): String = suspendCancellableCoroutine { continuation ->

    val recognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    val imagem = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(imagem)
        .addOnSuccessListener { resultado ->
            if (continuation.isActive) {
                continuation.resume(resultado.text)
            }
            recognizer.close()
        }
        .addOnFailureListener { erro ->
            if (continuation.isActive) {
                continuation.resumeWithException(erro)
            }
            recognizer.close()
        }
}


suspend fun lerDocumentoAnamnese(
    context: Context,
    arquivo: File,
    extensao: String
): String = withContext(Dispatchers.IO) {

    require(arquivo.exists()) {
        "O documento não foi encontrado."
    }

    when (extensao.lowercase()) {

        "pdf" -> {

            // Inicializa o leitor de PDF.
            PDFBoxResourceLoader.init(context.applicationContext)

            // Primeiro tenta extrair o texto original do PDF.
            val textoExtraido = try {
                PDDocument.load(arquivo).use { documento ->
                    PDFTextStripper().getText(documento)
                }
            } catch (e: Exception) {
                ""
            }

            // Se o PDF possui texto digital, utiliza esse conteúdo.
            if (textoExtraido.isNotBlank() &&
                textoExtraido.count { it.isLetterOrDigit() } > 30
            ) {
                textoExtraido
            } else {

                // Se não houver texto digital, utiliza OCR nas páginas.
                val descritor = ParcelFileDescriptor.open(
                    arquivo,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )

                descritor.use { fd ->
                    PdfRenderer(fd).use { renderer ->

                        if (renderer.pageCount == 0) {
                            throw IllegalArgumentException(
                                "O PDF não possui páginas."
                            )
                        }

                        val quantidadePaginas =
                            minOf(renderer.pageCount, 10)

                        val textos = mutableListOf<String>()

                        for (indice in 0 until quantidadePaginas) {

                            renderer.openPage(indice).use { pagina ->

                                val bitmap = Bitmap.createBitmap(
                                    pagina.width * 2,
                                    pagina.height * 2,
                                    Bitmap.Config.ARGB_8888
                                )

                                try {
                                    val matriz = Matrix().apply {
                                        postScale(2f, 2f)
                                    }

                                    pagina.render(
                                        bitmap,
                                        null,
                                        matriz,
                                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                    )

                                    val texto = reconhecerTexto(bitmap)

                                    textos.add(
                                        "Página ${indice + 1}\n$texto"
                                    )

                                } finally {
                                    bitmap.recycle()
                                }
                            }
                        }

                        textos.joinToString("\n\n")
                    }
                }
            }
        }

        "jpg", "jpeg", "png" -> {

            val bitmap = BitmapFactory.decodeFile(
                arquivo.absolutePath
            ) ?: throw IllegalArgumentException(
                "Não foi possível ler a imagem."
            )

            try {
                reconhecerTexto(bitmap)
            } finally {
                bitmap.recycle()
            }
        }

        else -> throw IllegalArgumentException(
            "Formato não suportado."
        )
    }
}

/**
 * Extrai sugestões textuais de campos da anamnese.
 * Não realiza diagnóstico nem classificação clínica.
 */
data class DadosAnamneseExtraidos(
    val condicoes: String,
    val restricoes: String,
    val observacoes: String,
    val textoCompleto: String
)


fun interpretarTextoAnamnese(texto: String): DadosAnamneseExtraidos {

    val linhas = texto
        .replace("\r", "\n")
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    fun extrairSecao(
        inicio: String,
        proximosTitulos: List<String>
    ): String {
        val indiceInicio = linhas.indexOfFirst {
            it.contains(inicio, ignoreCase = true)
        }

        if (indiceInicio == -1) return ""

        val conteudo = mutableListOf<String>()

        // Captura eventual conteúdo que esteja na mesma linha do título.
        val linhaTitulo = linhas[indiceInicio]
        val posicaoDoisPontos = linhaTitulo.indexOf(':')

        if (posicaoDoisPontos >= 0 &&
            posicaoDoisPontos < linhaTitulo.lastIndex
        ) {
            conteudo.add(linhaTitulo.substring(posicaoDoisPontos + 1).trim())
        }

        for (i in indiceInicio + 1 until linhas.size) {
            val linha = linhas[i]

            val encontrouProximaSecao = proximosTitulos.any {
                linha.contains(it, ignoreCase = true)
            }

            if (encontrouProximaSecao) break

            // Ignora informações de rodapé e encerramento.
            if (linha.contains("FIM DO DOCUMENTO", ignoreCase = true) ||
                linha.contains("Página", ignoreCase = true) ||
                linha.contains("Ícaros FitCare", ignoreCase = true)
            ) {
                continue
            }

            conteudo.add(linha)
        }

        return conteudo
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    val secoes = listOf(
        "1. Histórico de saúde",
        "2. Medicamentos",
        "3. Cirurgias e lesões anteriores",
        "4. Acompanhamento médico",
        "5. Condições selecionadas",
        "6. Restrições e cuidados",
        "7. Riscos e observações",
        "8. Recomendações profissionais",
        "9. Objetivos do acompanhamento"
    )

    val historico = extrairSecao(
        "1. Histórico de saúde",
        secoes.drop(1)
    )

    val medicamentos = extrairSecao(
        "2. Medicamentos",
        secoes.drop(2)
    )

    val cirurgias = extrairSecao(
        "3. Cirurgias e lesões anteriores",
        secoes.drop(3)
    )

    val acompanhamento = extrairSecao(
        "4. Acompanhamento médico",
        secoes.drop(4)
    )

    val condicoesSelecionadas = extrairSecao(
        "5. Condições selecionadas",
        secoes.drop(5)
    )

    val restricoes = extrairSecao(
        "6. Restrições e cuidados",
        secoes.drop(6)
    )

    val riscos = extrairSecao(
        "7. Riscos e observações",
        secoes.drop(7)
    )

    val recomendacoes = extrairSecao(
        "8. Recomendações profissionais",
        secoes.drop(8)
    )

    val objetivos = extrairSecao(
        "9. Objetivos do acompanhamento",
        emptyList()
    )

    val condicoes = listOf(
        historico,
        condicoesSelecionadas
    )
        .filter { it.isNotBlank() }
        .joinToString("\n\n")

    val observacoes = listOf(
        "Medicamentos: $medicamentos",
        "Cirurgias e lesões: $cirurgias",
        "Acompanhamento médico: $acompanhamento",
        "Riscos e observações: $riscos",
        "Recomendações profissionais: $recomendacoes",
        "Objetivos do acompanhamento: $objetivos"
    )
        .filterNot {
            it.endsWith(": ")
        }
        .joinToString("\n\n")

    return DadosAnamneseExtraidos(
        condicoes = condicoes,
        restricoes = restricoes,
        observacoes = observacoes,
        textoCompleto = texto
    )
}