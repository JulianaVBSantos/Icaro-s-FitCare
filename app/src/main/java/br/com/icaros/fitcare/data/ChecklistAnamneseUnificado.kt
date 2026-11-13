package br.com.icaros.fitcare.data

import br.com.icaros.fitcare.OPCOES_CONDICOES_PADRAO
import br.com.icaros.fitcare.OPCOES_RESTRICOES_PADRAO
import org.json.JSONArray

object ChecklistAnamneseUnificado {

    fun opcoesCondicoesJson(): String {
        val json = JSONArray()
        OPCOES_CONDICOES_PADRAO.forEach { json.put(it) }
        json.put("Nenhuma")
        json.put("Outro")
        return json.toString()
    }

    fun opcoesRestricoesJson(): String {
        val json = JSONArray()
        OPCOES_RESTRICOES_PADRAO.forEach { json.put(it) }
        json.put("Nenhuma")
        json.put("Outro")
        return json.toString()
    }

    fun criarPerguntasPadraoAcompanhamento(modeloId: Long): List<PerguntaModeloAnamneseEntity> {
        return listOf(
            PerguntaModeloAnamneseEntity(
                modeloId = modeloId,
                texto = "Condições de saúde ou dores articulares",
                tipoResposta = "CHECKBOX",
                opcoes = opcoesCondicoesJson(),
                obrigatoria = true,
                ordem = 0
            ),
            PerguntaModeloAnamneseEntity(
                modeloId = modeloId,
                texto = "Restrições físicas ou limitações de exercício",
                tipoResposta = "CHECKBOX",
                opcoes = opcoesRestricoesJson(),
                obrigatoria = true,
                ordem = 1
            )
        )
    }
}