package br.com.icaros.fitcare.data

fun classificarAluno(
    condicoes: String,
    restricoes: String,
    observacoes: String
): String {

    val textoCompleto =
        "$condicoes $restricoes $observacoes".lowercase()

    val termosPrioridade = listOf(
        "prioridade",
        "urgente",
        "liberação médica",
        "liberacao medica",
        "dor intensa",
        "lesão recente",
        "lesao recente"
    )

    val possuiPrioridade = termosPrioridade.any {
        textoCompleto.contains(it)
    }

    return when {
        possuiPrioridade -> "Prioridade"

        condicoes.isNotBlank() ||
                restricoes.isNotBlank() ||
                observacoes.isNotBlank() -> "Atenção"

        else -> "Normal"
    }
}