
package br.com.icaros.fitcare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import androidx.core.content.ContextCompat
import br.com.icaros.fitcare.data.AlunoEntity
import br.com.icaros.fitcare.data.AnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.PerguntaAnamneseAtribuidaEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AnamnesePdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    fun gerar(
        context: Context,
        aluno: AlunoEntity,
        anamnese: AnamneseAtribuidaEntity,
        perguntas: List<PerguntaAnamneseAtribuidaEntity>
    ): File {

        val pasta = File(context.filesDir, "anamneses")

        if (!pasta.exists() && !pasta.mkdirs()) {
            throw IllegalStateException(
                "Não foi possível criar a pasta de anamneses."
            )
        }

        val nomeSeguro = aluno.nome
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(40)

        val identificadorData = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val arquivo = File(
            pasta,
            "anamnese_${nomeSeguro}_${anamnese.id}_$identificadorData.pdf"
        )

        val documento = PdfDocument()

        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
        }

        val textoPequeno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.NORMAL
            )
        }

        val textoNegrito = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(25, 75, 125)
            textSize = 19f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        val subtitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(25, 75, 125)
            textSize = 12f
            typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )
        }

        val linha = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 0.8f
        }

        var numeroPagina = 0
        var pagina: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN

        fun iniciarPagina() {
            pagina?.let {
                documento.finishPage(it)
            }

            numeroPagina++

            val info = PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                numeroPagina
            ).create()

            pagina = documento.startPage(info)
            canvas = pagina!!.canvas
            y = MARGIN
        }

        fun garantirEspaco(altura: Float) {
            if (y + altura > PAGE_HEIGHT - MARGIN) {
                iniciarPagina()
            }
        }

        fun desenharTexto(
            textoLinha: String,
            paint: Paint = texto
        ) {
            garantirEspaco(16f)

            canvas?.drawText(
                textoLinha,
                MARGIN,
                y,
                paint
            )

            y += 15f
        }

        fun desenharTextoCentralizado(
            conteudo: String,
            paint: Paint
        ) {
            garantirEspaco(20f)

            val larguraTexto = paint.measureText(conteudo)
            val x = (PAGE_WIDTH - larguraTexto) / 2f

            canvas?.drawText(
                conteudo,
                x,
                y,
                paint
            )

            y += 15f
        }

        fun desenharTextoQuebrado(
            conteudo: String,
            paint: Paint = texto,
            largura: Float = CONTENT_WIDTH
        ) {
            val palavras = conteudo
                .replace("\n", " \n ")
                .split(Regex("\\s+"))

            var linhaAtual = ""

            for (palavra in palavras) {

                if (palavra == "\n") {
                    if (linhaAtual.isNotBlank()) {
                        garantirEspaco(15f)

                        canvas?.drawText(
                            linhaAtual,
                            MARGIN,
                            y,
                            paint
                        )

                        y += 14f
                        linhaAtual = ""
                    }

                    continue
                }

                val tentativa = if (linhaAtual.isEmpty()) {
                    palavra
                } else {
                    "$linhaAtual $palavra"
                }

                if (paint.measureText(tentativa) <= largura) {
                    linhaAtual = tentativa
                } else {
                    if (linhaAtual.isNotBlank()) {
                        garantirEspaco(15f)

                        canvas?.drawText(
                            linhaAtual,
                            MARGIN,
                            y,
                            paint
                        )

                        y += 14f
                    }

                    linhaAtual = palavra
                }
            }

            if (linhaAtual.isNotBlank()) {
                garantirEspaco(15f)

                canvas?.drawText(
                    linhaAtual,
                    MARGIN,
                    y,
                    paint
                )

                y += 14f
            }
        }

        fun desenharSecao(nome: String) {
            garantirEspaco(34f)

            y += 8f

            canvas?.drawText(
                nome,
                MARGIN,
                y,
                subtitulo
            )

            y += 8f

            canvas?.drawLine(
                MARGIN,
                y,
                PAGE_WIDTH - MARGIN,
                y,
                linha
            )

            y += 16f
        }

        fun desenharCampo(
            rotulo: String,
            valor: String
        ) {
            val conteudo = valor.ifBlank {
                "Não informado"
            }

            desenharTextoQuebrado(
                "$rotulo: $conteudo",
                texto
            )
        }

        try {
            iniciarPagina()

            // LOGOTIPO CENTRALIZADO

            val drawable: Drawable? = ContextCompat.getDrawable(
                context,
                R.drawable.logo_fitcare
            )

            if (drawable != null) {

                val largura = drawable.intrinsicWidth.coerceAtLeast(1)
                val altura = drawable.intrinsicHeight.coerceAtLeast(1)

                val bitmap = Bitmap.createBitmap(
                    largura,
                    altura,
                    Bitmap.Config.ARGB_8888
                )

                val canvasLogo = Canvas(bitmap)

                drawable.setBounds(
                    0,
                    0,
                    largura,
                    altura
                )

                drawable.draw(canvasLogo)

                val larguraFinal = 105f
                val alturaFinal = larguraFinal * altura / largura

                val esquerda = (PAGE_WIDTH - larguraFinal) / 2f

                canvas?.drawBitmap(
                    bitmap,
                    null,
                    RectF(
                        esquerda,
                        y,
                        esquerda + larguraFinal,
                        y + alturaFinal
                    ),
                    Paint(Paint.ANTI_ALIAS_FLAG)
                )

                y += alturaFinal + 14f

                bitmap.recycle()
            }

            // TÍTULO CENTRALIZADO

            desenharTextoCentralizado(
                "FICHA DE ANAMNESE",
                titulo
            )

            // SUBTÍTULO CENTRALIZADO

            desenharTextoCentralizado(
                "ÍCAROS FITCARE • Documento de avaliação do aluno",
                textoPequeno
            )

            y += 8f

            // DADOS DO ALUNO

            desenharSecao("DADOS DO ALUNO")

            desenharCampo("Nome", aluno.nome)
            desenharCampo("E-mail", aluno.email)
            desenharCampo("Data de cadastro", aluno.dataCadastro)
            desenharCampo("Objetivo", aluno.objetivo)
            desenharCampo("Status", aluno.status)

            desenharCampo(
                "Observações profissionais",
                aluno.observacoesProfissionais
            )

            desenharCampo("Condições", aluno.condicoes)
            desenharCampo("Restrições", aluno.restricoes)

            // IDENTIFICAÇÃO DA AVALIAÇÃO

            desenharSecao("IDENTIFICAÇÃO DA AVALIAÇÃO")

            desenharCampo("Modelo", anamnese.nomeModelo)
            desenharCampo("Data da avaliação", anamnese.dataCriacao)

            desenharCampo(
                "Profissional responsável",
                anamnese.profissionalResponsavel
            )

            desenharCampo(
                "Identificação",
                "Avaliação nº ${anamnese.id}"
            )

            // PERGUNTAS E RESPOSTAS

            desenharSecao("PERGUNTAS E RESPOSTAS")

            val perguntasOrdenadas = perguntas.sortedBy {
                it.ordem
            }

            if (perguntasOrdenadas.isEmpty()) {

                desenharTextoQuebrado(
                    "Este modelo não possui perguntas cadastradas.",
                    texto
                )

            } else {

                perguntasOrdenadas.forEachIndexed { indice, pergunta ->

                    garantirEspaco(85f)

                    y += 5f

                    desenharTextoQuebrado(
                        "${indice + 1}. ${pergunta.texto}" +
                                if (pergunta.obrigatoria) " *" else "",
                        textoNegrito
                    )

                    desenharTextoQuebrado(
                        "Tipo de resposta: ${pergunta.tipoResposta}",
                        textoPequeno
                    )

                    if (pergunta.opcoes.isNotBlank()) {
                        desenharTextoQuebrado(
                            "Opções: ${pergunta.opcoes}",
                            textoPequeno
                        )
                    }

                    val quantidadeLinhas = when (
                        pergunta.tipoResposta.uppercase(Locale.ROOT)
                    ) {
                        "TEXTO",
                        "TEXTO_LONGO",
                        "DESCRITIVA" -> 4

                        else -> 2
                    }

                    repeat(quantidadeLinhas) {

                        garantirEspaco(22f)

                        canvas?.drawLine(
                            MARGIN,
                            y + 8f,
                            PAGE_WIDTH - MARGIN,
                            y + 8f,
                            linha
                        )

                        y += 22f
                    }

                    y += 10f
                }
            }

            y += 12f

            desenharTextoQuebrado(
                "* Pergunta obrigatória no modelo.",
                textoPequeno
            )

            desenharTextoQuebrado(
                "Documento gerado pelo Ícaros FitCare.",
                textoPequeno
            )

            // FINALIZAÇÃO DO PDF

            pagina?.let {
                documento.finishPage(it)
                pagina = null
            }

            FileOutputStream(arquivo).use { saida ->
                documento.writeTo(saida)
            }

            return arquivo

        } catch (erro: Exception) {

            arquivo.delete()
            throw erro

        } finally {

            pagina?.let {
                documento.finishPage(it)
            }

            documento.close()
        }
    }
}