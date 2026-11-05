
package br.com.icaros.fitcare

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import br.com.icaros.fitcare.data.AnamneseEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun gerarPdfAnamnese(
    context: Context,
    alunoId: Long,
    anamnese: AnamneseEntity
): File {

    require(alunoId > 0) { "Aluno inválido." }

    val pasta = File(
        context.filesDir,
        "anamneses/aluno_$alunoId"
    )

    if (!pasta.exists()) {
        pasta.mkdirs()
    }

    val dataArquivo = SimpleDateFormat(
        "yyyyMMdd_HHmmss",
        Locale.getDefault()
    ).format(Date())

    val arquivo = File(
        pasta,
        "anamnese_${alunoId}_v${anamnese.versao}_$dataArquivo.pdf"
    )

    val documento = PdfDocument()

    val largura = 595
    val altura = 842
    val margem = 45f
    val larguraTexto = largura - margem * 2

    val tinta = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.NORMAL
        )
    }

    val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(25, 65, 110)
        textSize = 17f
        typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD
        )
    }

    val subtitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(25, 65, 110)
        textSize = 12f
        typeface = Typeface.create(
            Typeface.DEFAULT,
            Typeface.BOLD
        )
    }

    val linhas = mutableListOf<Pair<String, Int>>()

    fun adicionarSecao(nome: String, conteudo: String) {
        if (conteudo.isNotBlank()) {
            linhas.add(nome to 1)

            conteudo.lines().forEach {
                linhas.add(it to 0)
            }

            linhas.add("" to 0)
        }
    }

    linhas.add("IDENTIFICAÇÃO DO ALUNO" to 1)
    linhas.add("Código do aluno: $alunoId" to 0)
    linhas.add("Data da avaliação: ${anamnese.dataAvaliacao}" to 0)
    linhas.add("Versão: ${anamnese.versao}" to 0)
    linhas.add("Profissional: ${anamnese.profissionalResponsavel}" to 0)
    linhas.add("" to 0)

    adicionarSecao(
        "HISTÓRICO DE SAÚDE",
        anamnese.historicoSaude
    )

    adicionarSecao(
        "MEDICAMENTOS",
        anamnese.medicamentos
    )

    adicionarSecao(
        "CIRURGIAS E PROCEDIMENTOS",
        anamnese.cirurgias
    )

    adicionarSecao(
        "ACOMPANHAMENTO MÉDICO",
        anamnese.acompanhamentoMedico
    )

    adicionarSecao(
        "CONDIÇÕES DE SAÚDE",
        anamnese.condicoesSelecionadas
            .split("|")
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    )

    adicionarSecao(
        "RESTRIÇÕES",
        anamnese.restricoesSelecionadas
            .split("|")
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    )

    adicionarSecao(
        "RISCOS E CUIDADOS",
        anamnese.riscosSelecionados
            .split("|")
            .filter { it.isNotBlank() }
            .joinToString("\n") { "• $it" }
    )

    adicionarSecao(
        "OBSERVAÇÕES PROFISSIONAIS",
        anamnese.observacoes
    )

    adicionarSecao(
        "RECOMENDAÇÕES",
        anamnese.recomendacoes
    )

    adicionarSecao(
        "CUIDADOS DURANTE OS EXERCÍCIOS",
        anamnese.cuidados
    )

    adicionarSecao(
        "OBJETIVOS E PLANEJAMENTO",
        anamnese.objetivos
    )

    linhas.add(
        "Documento gerado pelo aplicativo Ícaro's FitCare." to 0
    )

    var paginaNumero = 1
    var pagina = documento.startPage(
        PdfDocument.PageInfo.Builder(
            largura,
            altura,
            paginaNumero
        ).create()
    )

    var canvas = pagina.canvas
    var y = 55f

    canvas.drawText(
        "ÍCARO'S FITCARE",
        margem,
        y,
        titulo
    )

    y += 28f

    canvas.drawText(
        "FICHA DE ANAMNESE",
        margem,
        y,
        subtitulo
    )

    y += 30f

    fun iniciarNovaPagina() {
        documento.finishPage(pagina)

        paginaNumero++

        pagina = documento.startPage(
            PdfDocument.PageInfo.Builder(
                largura,
                altura,
                paginaNumero
            ).create()
        )

        canvas = pagina.canvas
        y = 55f

        canvas.drawText(
            "ÍCARO'S FITCARE - ANAMNESE",
            margem,
            y,
            subtitulo
        )

        y += 30f
    }

    linhas.forEach { (texto, tipo) ->

        if (texto.isBlank()) {
            y += 8f
            return@forEach
        }

        val paint = if (tipo == 1) subtitulo else tinta

        if (tipo == 1) {
            y += 5f
        }

        val palavras = texto.split(Regex("\\s+"))
        var linhaAtual = ""

        for (palavra in palavras) {
            val tentativa = if (linhaAtual.isEmpty()) {
                palavra
            } else {
                "$linhaAtual $palavra"
            }

            if (paint.measureText(tentativa) > larguraTexto) {
                if (y > altura - 55f) {
                    iniciarNovaPagina()
                }

                canvas.drawText(
                    linhaAtual,
                    margem,
                    y,
                    tinta
                )

                y += 15f
                linhaAtual = palavra
            } else {
                linhaAtual = tentativa
            }
        }

        if (y > altura - 55f) {
            iniciarNovaPagina()
        }

        canvas.drawText(
            linhaAtual,
            margem,
            y,
            paint
        )

        y += if (tipo == 1) 20f else 15f
    }

    documento.finishPage(pagina)

    try {
        FileOutputStream(arquivo).use {
            documento.writeTo(it)
        }
    } finally {
        documento.close()
    }

    return arquivo
}