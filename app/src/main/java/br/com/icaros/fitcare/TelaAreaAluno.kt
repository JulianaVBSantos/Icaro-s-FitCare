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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun TelaAreaAluno(
    alunoId: Long?,
    anamneseRepository: AnamneseAtribuidaRepository,
    respostaRepository: RespostaAnamneseRepository,
    onSair: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val anamnesesFlow = remember(alunoId) {
        if (alunoId != null) {
            anamneseRepository.listarPorAluno(alunoId)
        } else {
            flowOf(emptyList<AnamneseAtribuidaEntity>())
        }
    }

    val anamneses by anamnesesFlow.collectAsState(
        initial = emptyList()
    )

    var anamneseSelecionadaId by remember {
        mutableStateOf<Long?>(null)
    }

    var perguntas by remember {
        mutableStateOf<List<PerguntaAnamneseAtribuidaEntity>>(emptyList())
    }

    var respostas by remember {
        mutableStateOf<Map<Long, String>>(emptyMap())
    }

    var carregando by remember {
        mutableStateOf(false)
    }

    var mensagemErro by remember {
        mutableStateOf("")
    }

    val salvamentosPendentes = remember {
        mutableMapOf<Long, Job>()
    }

    val anamneseSelecionada = anamneses.firstOrNull {
        it.id == anamneseSelecionadaId
    }

    suspend fun aguardarSalvamentos() {
        salvamentosPendentes.values.toList().forEach {
            it.join()
        }
        salvamentosPendentes.clear()
    }

    LaunchedEffect(anamneseSelecionadaId, alunoId) {
        val id = anamneseSelecionadaId
        val idAluno = alunoId

        if (id != null && idAluno != null) {
            carregando = true
            mensagemErro = ""

            try {
                val novasPerguntas =
                    anamneseRepository.buscarPerguntas(id)

                val respostasSalvas =
                    respostaRepository.buscarRespostasDaAnamnese(id)

                perguntas = novasPerguntas

                respostas = respostasSalvas.associate {
                    it.perguntaAtribuidaId to it.resposta
                }
            } catch (e: Exception) {
                mensagemErro =
                    e.message ?: "Não foi possível carregar a anamnese."
            } finally {
                carregando = false
            }
        } else {
            perguntas = emptyList()
            respostas = emptyMap()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Área do aluno",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Aqui você poderá preencher e acompanhar suas anamneses."
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (alunoId == null) {
            Text(
                text = "Não foi possível identificar o cadastro do aluno. Entre em contato com o profissional."
            )
        } else if (anamneseSelecionadaId != null) {

            Text(
                text = anamneseSelecionada?.nomeModelo
                    ?: "Preenchimento da anamnese",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        aguardarSalvamentos()
                        anamneseSelecionadaId = null
                        mensagemErro = ""
                    }
                }
            ) {
                Text("VOLTAR ÀS ANAMNESES")
            }

            if (carregando) {
                CircularProgressIndicator()
            } else {
                perguntas.forEach { pergunta ->

                    val respostaAtual =
                        respostas[pergunta.id] ?: ""

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = respostaAtual,
                        onValueChange = { novaResposta ->

                            respostas = respostas + (
                                    pergunta.id to novaResposta
                                    )

                            mensagemErro = ""

                            val idAluno = alunoId

                            if (idAluno != null) {
                                salvamentosPendentes[pergunta.id]?.cancel()

                                salvamentosPendentes[pergunta.id] =
                                    scope.launch {
                                        delay(300)

                                        try {
                                            respostaRepository.salvarRespostaDoAluno(
                                                alunoId = idAluno,
                                                perguntaId = pergunta.id,
                                                resposta = novaResposta
                                            )
                                        } catch (e: Exception) {
                                            mensagemErro =
                                                e.message
                                                    ?: "Erro ao salvar resposta."
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                pergunta.texto +
                                        if (pergunta.obrigatoria) " *" else ""
                            )
                        },
                        supportingText = {
                            if (pergunta.opcoes.isNotBlank()) {
                                Text("Opções: ${pergunta.opcoes}")
                            }

                            if (pergunta.tipoResposta.isNotBlank()) {
                                Text("Tipo: ${pergunta.tipoResposta}")
                            }
                        },
                        minLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val perguntasObrigatoriasPendentes =
                    perguntas.count { pergunta ->
                        pergunta.obrigatoria &&
                                respostas[pergunta.id].isNullOrBlank()
                    }

                if (perguntasObrigatoriasPendentes > 0) {
                    Text(
                        text = "Ainda faltam $perguntasObrigatoriasPendentes pergunta(s) obrigatória(s).",
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = {
                        scope.launch {
                            try {
                                carregando = true
                                mensagemErro = ""

                                aguardarSalvamentos()

                                respostaRepository.concluirDoAluno(
                                    alunoId = alunoId,
                                    anamneseId = anamneseSelecionadaId!!
                                )

                                anamneseSelecionadaId = null
                                perguntas = emptyList()
                                respostas = emptyMap()
                            } catch (e: Exception) {
                                mensagemErro =
                                    e.message
                                        ?: "Não foi possível concluir a anamnese."
                            } finally {
                                carregando = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !carregando &&
                            perguntas.isNotEmpty() &&
                            perguntasObrigatoriasPendentes == 0
                ) {
                    Text("CONCLUIR ANAMNESE")
                }
            }

        } else {

            val pendentes = anamneses.filter {
                it.status == "PENDENTE" ||
                        it.status == "EM_ANDAMENTO"
            }

            val naoEnviadas = anamneses.filter {
                it.status == "NAO_ENVIADA"
            }

            val respondidas = anamneses.filter {
                it.status == "RESPONDIDA"
            }

            if (anamneses.isEmpty()) {
                Text(
                    text = "Você ainda não possui uma anamnese atribuída. Aguarde o envio pelo profissional."
                )
            }

            if (pendentes.isNotEmpty()) {
                Text(
                    text = "Anamneses disponíveis",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                pendentes.forEach { anamnese ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = anamnese.nomeModelo,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = if (
                                    anamnese.status == "EM_ANDAMENTO"
                                ) {
                                    "Preenchimento iniciado"
                                } else {
                                    "Aguardando preenchimento"
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    anamneseSelecionadaId = anamnese.id
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (
                                        anamnese.status == "EM_ANDAMENTO"
                                    ) {
                                        "CONTINUAR PREENCHIMENTO"
                                    } else {
                                        "PREENCHER ANAMNESE"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (naoEnviadas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Aguardando liberação do profissional",
                    style = MaterialTheme.typography.titleMedium
                )

                naoEnviadas.forEach { anamnese ->
                    Text(
                        text = "• ${anamnese.nomeModelo} — ainda não enviada."
                    )
                }
            }

            if (respondidas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Anamneses concluídas",
                    style = MaterialTheme.typography.titleLarge
                )

                respondidas.forEach { anamnese ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(anamnese.nomeModelo)

                            Text(
                                text = "Concluída",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (
                anamneses.isNotEmpty() &&
                pendentes.isEmpty() &&
                naoEnviadas.isEmpty() &&
                respondidas.size == anamneses.size
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Todas as suas anamneses foram concluídas. Seu acesso está liberado.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (mensagemErro.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mensagemErro,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    aguardarSalvamentos()
                    onSair()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SAIR")
        }
    }
}