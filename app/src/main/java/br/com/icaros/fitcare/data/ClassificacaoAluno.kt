
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

    return when {
        motivos.isNotEmpty() -> "Prioridade"

        condicoes.isNotBlank() ||
                restricoes.isNotBlank() ||
                observacoes.isNotBlank() -> "Atenção"

        else -> "Normal"
    }
}