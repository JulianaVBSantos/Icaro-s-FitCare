
package br.com.icaros.fitcare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.icaros.fitcare.data.AnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
import br.com.icaros.fitcare.data.PerguntaAnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.RespostaAnamneseRepository
import br.com.icaros.fitcare.data.RespostaAnamneseEntity

@Composable
fun TelaVisualizarAnamnese(
    anamneseId: Long,
    anamneseRepository: AnamneseAtribuidaRepository,
    respostaRepository: RespostaAnamneseRepository,
    onVoltar: () -> Unit
) {
    var anamnese by remember {
        mutableStateOf<AnamneseAtribuidaEntity?>(null)
    }

    var perguntas by remember {
        mutableStateOf<List<PerguntaAnamneseAtribuidaEntity>>(emptyList())
    }

    var respostas by remember {
        mutableStateOf<List<RespostaAnamneseEntity>>(emptyList())
    }

    var carregando by remember {
        mutableStateOf(true)
    }

    var erro by remember {
        mutableStateOf("")
    }

    LaunchedEffect(anamneseId) {
        carregando = true
        erro = ""

        try {
            anamnese = anamneseRepository.buscarPorId(anamneseId)

            perguntas = anamneseRepository.buscarPerguntas(anamneseId)

            respostas = respostaRepository.buscarRespostasDaAnamnese(
                anamneseId
            )
        } catch (e: Exception) {
            erro = e.message ?: "Não foi possível carregar a anamnese."
        } finally {
            carregando = false
        }
    }

    val respostasPorPergunta = respostas.associateBy {
        it.perguntaAtribuidaId
    }

    val statusDescricao = when (anamnese?.status) {
        "NAO_ENVIADA" -> "Ainda não enviada ao aluno"
        "PENDENTE" -> "Aguardando preenchimento"
        "EM_ANDAMENTO" -> "Em preenchimento"
        "RESPONDIDA" -> "Concluída pelo aluno"
        else -> "Status não identificado"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Visualizar anamnese",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onVoltar) {
            Text("VOLTAR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (carregando) {
            CircularProgressIndicator()
        } else if (erro.isNotBlank()) {
            Text(
                text = erro,
                color = MaterialTheme.colorScheme.error
            )
        } else if (anamnese == null) {
            Text("Anamnese não encontrada.")
        } else {
            Text(
                text = anamnese!!.nomeModelo,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Status: $statusDescricao")

                    if (anamnese!!.profissionalResponsavel.isNotBlank()) {
                        Text(
                            "Profissional: ${anamnese!!.profissionalResponsavel}"
                        )
                    }

                    if (anamnese!!.dataCriacao.isNotBlank()) {
                        Text(
                            "Data de criação: ${anamnese!!.dataCriacao}"
                        )
                    }

                    if (anamnese!!.dataEnvio.isNotBlank()) {
                        Text(
                            "Data de envio: ${anamnese!!.dataEnvio}"
                        )
                    }

                    if (anamnese!!.dataResposta.isNotBlank()) {
                        Text(
                            "Data de conclusão: ${anamnese!!.dataResposta}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Perguntas e respostas",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (perguntas.isEmpty()) {
                Text("Esta anamnese não possui perguntas.")
            }

            perguntas.forEachIndexed { index, pergunta ->
                val resposta = respostasPorPergunta[pergunta.id]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "${index + 1}. ${pergunta.texto}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (pergunta.obrigatoria) {
                            Text(
                                text = "Pergunta obrigatória",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = resposta?.resposta
                                ?.takeIf { it.isNotBlank() }
                                ?: "Ainda não respondida",
                            color = if (
                                resposta?.resposta.isNullOrBlank()
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )

                        if (resposta?.dataAtualizacao?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Última atualização: ${resposta.dataAtualizacao}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Total de perguntas: ${perguntas.size}"
            )

            Text(
                text = "Perguntas respondidas: ${
                    perguntas.count {
                        !respostasPorPergunta[it.id]?.resposta.isNullOrBlank()
                    }
                }"
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("VOLTAR")
        }
    }
}