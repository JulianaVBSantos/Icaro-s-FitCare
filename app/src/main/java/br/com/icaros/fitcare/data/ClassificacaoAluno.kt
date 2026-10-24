
package br.com.icaros.fitcare.data

import java.text.Normalizer
import java.util.Locale

private fun normalizarTexto(texto: String): String {
    return Normalizer
        .normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun identificarMotivosAcompanhamento(
    condicoes: String,
    restricoes: String,
    observacoes: String
): List<String> {

    val texto = normalizarTexto(
        "$condicoes $restricoes $observacoes"
    )

    val regras = listOf(
        "Prioridade" to listOf("prioridade"),
        "Urgência identificada" to listOf("urgente", "urgencia"),
        "Necessidade de liberação médica" to listOf(
            "liberacao medica"
        ),
        "Dor intensa" to listOf("dor intensa"),
        "Lesão recente" to listOf("lesao recente")
    )

    return regras
        .filter { (_, termos) ->
            termos.any { termo -> texto.contains(termo) }
        }
        .map { (motivo, _) -> motivo }
        .distinct()
}


fun classificarAluno(
    condicoes: String,
    restricoes: String,
    observacoes: String
): String {

    val motivos = identificarMotivosAcompanhamento(
        condicoes = condicoes,
        restricoes = restricoes,
        observacoes = observacoes
    )

    if (motivos.isNotEmpty()) {
        return "Prioridade"
    }

    fun contarItens(texto: String): Int {
        return texto
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { normalizarTexto(it) }
            .size
    }

    val totalItens =
        contarItens(condicoes) +
                contarItens(restricoes)

    return when {
        totalItens >= 2 -> "Prioridade"
        totalItens == 1 -> "Atenção"
        else -> "Normal"
    }
}