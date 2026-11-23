package br.com.icaros.fitcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.AnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
import br.com.icaros.fitcare.data.PerguntaAnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.RespostaAnamneseEntity
import br.com.icaros.fitcare.data.RespostaAnamneseRepository
import kotlinx.coroutines.launch
import org.json.JSONArray

private fun formatarRespostaExibicao(
    tipoResposta: String,
    respostaBruta: String
): String {
    if (respostaBruta.isBlank()) return ""
    if (tipoResposta == "CHECKBOX" && respostaBruta.trim().startsWith("[")) {
        return try {
            val json = JSONArray(respostaBruta)
            val itens = (0 until json.length()).mapNotNull { i ->
                json.optString(i).takeIf { it.isNotBlank() }
            }
            if (itens.isEmpty()) "" else itens.joinToString(", ")
        } catch (_: Exception) {
            respostaBruta
        }
    }
    return respostaBruta
}

@Composable
fun TelaVisualizarAnamnese(
    anamneseId: Long,
    anamneseRepository: AnamneseAtribuidaRepository,
    respostaRepository: RespostaAnamneseRepository,
    onVoltar: () -> Unit,
    onAnamneseExcluida: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val escuro = temaEscuroAtivoState

    val corFundo = if (escuro) Color(0xFF0F1722) else Color(0xFFF5F9FC)
    val corCartao = if (escuro) Color(0xFF172331) else Color.White
    val corTextoPrincipal = if (escuro) Color(0xFFEAF2FA) else Color(0xFF002B5C)
    val corTextoSecundario = if (escuro) Color(0xFFA2B5C8) else Color(0xFF526579)

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

    var confirmarExclusao by remember {
        mutableStateOf(false)
    }

    var excluindo by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(anamneseId) {
        carregando = true
        erro = ""

        try {
            anamnese = anamneseRepository.buscarPorId(anamneseId)
            perguntas = anamneseRepository.buscarPerguntas(anamneseId)
            respostas = respostaRepository.buscarRespostasDaAnamnese(anamneseId)
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
            .background(corFundo)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CabecalhoPadrao(
            titulo = "Visualizar anamnese",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (carregando) {
                CircularProgressIndicator(color = corTextoPrincipal)
            } else if (erro.isNotBlank()) {
                Text(
                    text = erro,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (anamnese == null) {
                Text(
                    text = "Anamnese não encontrada.",
                    fontSize = 14.sp,
                    color = corTextoPrincipal
                )
            } else {
                Text(
                    text = anamnese!!.nomeModelo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = corCartao
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Status: $statusDescricao",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = corTextoPrincipal
                        )

                        if (anamnese!!.profissionalResponsavel.isNotBlank()) {
                            Text(
                                text = "Profissional: ${anamnese!!.profissionalResponsavel}",
                                fontSize = 12.sp,
                                color = corTextoSecundario
                            )
                        }

                        if (anamnese!!.dataCriacao.isNotBlank()) {
                            Text(
                                text = "Data de criação: ${anamnese!!.dataCriacao}",
                                fontSize = 12.sp,
                                color = corTextoSecundario
                            )
                        }

                        if (anamnese!!.dataEnvio.isNotBlank()) {
                            Text(
                                text = "Data de envio: ${anamnese!!.dataEnvio}",
                                fontSize = 12.sp,
                                color = corTextoSecundario
                            )
                        }

                        if (anamnese!!.dataResposta.isNotBlank()) {
                            Text(
                                text = "Data de conclusão: ${anamnese!!.dataResposta}",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Perguntas e respostas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (perguntas.isEmpty()) {
                    Text(
                        text = "Esta anamnese não possui perguntas.",
                        fontSize = 13.sp,
                        color = corTextoSecundario
                    )
                }

                perguntas.forEachIndexed { index, pergunta ->
                    val resposta = respostasPorPergunta[pergunta.id]
                    val respostaFormatada = formatarRespostaExibicao(
                        tipoResposta = pergunta.tipoResposta,
                        respostaBruta = resposta?.resposta.orEmpty()
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = corCartao
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "${index + 1}. ${pergunta.texto}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )

                            if (pergunta.obrigatoria) {
                                Text(
                                    text = "Pergunta obrigatória",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = respostaFormatada
                                    .takeIf { it.isNotBlank() }
                                    ?: "Ainda não respondida",
                                fontSize = 13.sp,
                                color = if (respostaFormatada.isBlank()) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    corTextoPrincipal
                                }
                            )
                            if (resposta?.dataAtualizacao?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Última atualização: ${resposta.dataAtualizacao}",
                                    fontSize = 11.sp,
                                    color = corTextoSecundario
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Total de perguntas: ${perguntas.size}",
                    fontSize = 12.sp,
                    color = corTextoSecundario
                )
                Text(
                    text = "Perguntas respondidas: ${
                        perguntas.count {
                            !respostasPorPergunta[it.id]?.resposta.isNullOrBlank()
                        }
                    }",
                    fontSize = 12.sp,
                    color = corTextoSecundario
                )
                if (onAnamneseExcluida != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { confirmarExclusao = true },
                        enabled = !excluindo,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFE34B63)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excluir anamnese atribuída",
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VOLTAR",
                    fontSize = 13.sp
                )
            }
        }
    }
    if (confirmarExclusao && onAnamneseExcluida != null) {
        AlertDialog(
            onDismissRequest = {
                if (!excluindo) confirmarExclusao = false
            },
            title = {
                Text("Excluir anamnese?")
            },
            text = {
                Text(
                    "Deseja realmente excluir esta anamnese atribuída? " +
                            "Ela será removida da visualização do professor e do aluno."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !excluindo,
                    onClick = {
                        excluindo = true
                        scope.launch {
                            try {
                                anamneseRepository.excluir(anamneseId)
                                confirmarExclusao = false
                                onAnamneseExcluida()
                            } catch (e: Exception) {
                                erro = e.message
                                    ?: "Não foi possível excluir a anamnese."
                                confirmarExclusao = false
                            } finally {
                                excluindo = false
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (excluindo) "Excluindo..." else "Excluir",
                        color = Color(0xFFE34B63)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !excluindo,
                    onClick = { confirmarExclusao = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}