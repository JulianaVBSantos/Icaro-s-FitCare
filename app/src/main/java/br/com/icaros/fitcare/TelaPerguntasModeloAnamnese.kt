
package br.com.icaros.fitcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseEntity
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPerguntasModeloAnamnese(
    modeloId: Long,
    repository: PerguntaModeloAnamneseRepository,
    onVoltar: () -> Unit
) {
    // Observa automaticamente as perguntas cadastradas no Room.
    val perguntas by repository.listarPorModelo(modeloId)
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    // Controle dos diálogos e das mensagens.
    var mostrarDialogo by remember { mutableStateOf(false) }
    var perguntaEditando by remember {
        mutableStateOf<PerguntaModeloAnamneseEntity?>(null)
    }
    var perguntaParaExcluir by remember {
        mutableStateOf<PerguntaModeloAnamneseEntity?>(null)
    }

    // Campos do formulário de pergunta.
    var texto by remember { mutableStateOf("") }
    var tipoResposta by remember { mutableStateOf("TEXTO") }
    var opcoesTexto by remember { mutableStateOf("") }
    var obrigatoria by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }

    val tiposResposta = listOf(
        "TEXTO",
        "NUMERO",
        "DATA",
        "SIM_NAO",
        "MULTIPLA_ESCOLHA",
        "CHECKBOX"
    )

    // Descrições apresentadas ao profissional.
    fun descricaoTipo(tipo: String): String {
        return when (tipo) {
            "TEXTO" -> "Resposta em texto livre"
            "NUMERO" -> "Resposta numérica"
            "DATA" -> "Seleção de uma data"
            "SIM_NAO" -> "Resposta Sim ou Não"
            "MULTIPLA_ESCOLHA" -> "Escolher apenas uma alternativa"
            "CHECKBOX" -> "Marcar uma ou mais alternativas"
            else -> tipo
        }
    }

    // Abre o formulário para cadastrar uma pergunta.
    fun novaPergunta() {
        perguntaEditando = null
        texto = ""
        tipoResposta = "TEXTO"
        opcoesTexto = ""
        obrigatoria = false
        erro = null
        mostrarDialogo = true
    }

    // Abre o formulário preenchido para editar uma pergunta.
    fun editarPergunta(pergunta: PerguntaModeloAnamneseEntity) {
        perguntaEditando = pergunta
        texto = pergunta.texto
        tipoResposta = pergunta.tipoResposta
        obrigatoria = pergunta.obrigatoria

        opcoesTexto = try {
            val json = JSONArray(pergunta.opcoes)
            (0 until json.length()).joinToString("\n") {
                json.optString(it)
            }
        } catch (_: Exception) {
            ""
        }

        erro = null
        mostrarDialogo = true
    }

    // Converte as alternativas para JSON antes de salvar.
    fun converterOpcoes(): String {
        val json = JSONArray()

        opcoesTexto.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { json.put(it) }

        return json.toString()
    }

    // Salva uma pergunta nova ou atualiza uma já existente.
    fun salvarPergunta() {
        scope.launch {
            if (texto.isBlank()) {
                erro = "Informe o texto da pergunta."
                return@launch
            }

            val precisaOpcoes =
                tipoResposta == "MULTIPLA_ESCOLHA" ||
                        tipoResposta == "CHECKBOX"

            val alternativas = opcoesTexto.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

            if (precisaOpcoes && alternativas.size < 2) {
                erro = "Informe pelo menos duas alternativas."
                return@launch
            }

            salvando = true
            erro = null

            try {
                val pergunta = PerguntaModeloAnamneseEntity(
                    id = perguntaEditando?.id ?: 0L,
                    modeloId = modeloId,
                    texto = texto.trim(),
                    tipoResposta = tipoResposta,
                    opcoes = if (precisaOpcoes) {
                        converterOpcoes()
                    } else {
                        ""
                    },
                    obrigatoria = obrigatoria,
                    ordem = perguntaEditando?.ordem ?: perguntas.size
                )

                repository.salvar(pergunta)

                mostrarDialogo = false
                perguntaEditando = null

            } catch (e: Exception) {
                erro = e.message ?: "Não foi possível salvar a pergunta."
            } finally {
                salvando = false
            }
        }
    }

    // Altera a posição de uma pergunta e persiste a nova ordem.
    fun moverPergunta(indice: Int, direcao: Int) {
        val novoIndice = indice + direcao

        if (novoIndice !in perguntas.indices) return

        val novaLista = perguntas.toMutableList()
        val item = novaLista.removeAt(indice)
        novaLista.add(novoIndice, item)

        scope.launch {
            try {
                repository.salvarOrdenacao(novaLista)
            } catch (_: Exception) {
                erro = "Não foi possível alterar a ordem das perguntas."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        // CABEÇALHO
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF002B5C)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = "Perguntas do modelo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Text(
                    text = "Configure o formulário de anamnese",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // EXPLICAÇÃO
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                Text(
                    text = "Personalize sua avaliação",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF002B5C)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Adicione as perguntas que o aluno deverá responder. " +
                            "Você poderá definir o tipo de resposta e quais perguntas " +
                            "serão obrigatórias.",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTÃO PARA ADICIONAR PERGUNTAS
        Button(
            onClick = { novaPergunta() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF002B5C),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Adicionar pergunta",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Perguntas cadastradas (${perguntas.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // LISTA DE PERGUNTAS
        if (perguntas.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nenhuma pergunta cadastrada",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Utilize o botão acima para adicionar a primeira pergunta.",
                        fontSize = 13.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                perguntas.forEachIndexed { indice, pergunta ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${indice + 1}.",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF002B5C)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = pergunta.texto,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF212121)
                                )
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text = descricaoTipo(pergunta.tipoResposta),
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )

                            Text(
                                text = if (pergunta.obrigatoria) {
                                    "Obrigatória"
                                } else {
                                    "Opcional"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pergunta.obrigatoria) {
                                    Color(0xFF002B5C)
                                } else {
                                    Color(0xFF757575)
                                }
                            )

                            // Exibe as alternativas cadastradas.
                            if (
                                pergunta.tipoResposta == "MULTIPLA_ESCOLHA" ||
                                pergunta.tipoResposta == "CHECKBOX"
                            ) {
                                val alternativas = try {
                                    val json = JSONArray(pergunta.opcoes)
                                    (0 until json.length()).map {
                                        json.optString(it)
                                    }
                                } catch (_: Exception) {
                                    emptyList()
                                }

                                alternativas.forEach { alternativa ->
                                    Text(
                                        text = "• $alternativa",
                                        fontSize = 12.sp,
                                        color = Color(0xFF757575)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            HorizontalDivider(
                                color = Color(0xFFF0F0F0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    TextButton(
                                        enabled = indice > 0,
                                        onClick = {
                                            moverPergunta(indice, -1)
                                        }
                                    ) {
                                        Text("↑")
                                    }

                                    TextButton(
                                        enabled = indice < perguntas.lastIndex,
                                        onClick = {
                                            moverPergunta(indice, 1)
                                        }
                                    ) {
                                        Text("↓")
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editarPergunta(pergunta)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar",
                                            tint = Color(0xFF002B5C)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            perguntaParaExcluir = pergunta
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }

    // DIÁLOGO DE CADASTRO E EDIÇÃO
    if (mostrarDialogo) {
        AlertDialog(
            onDismissRequest = {
                if (!salvando) mostrarDialogo = false
            },
            title = {
                Text(
                    if (perguntaEditando == null) {
                        "Nova pergunta"
                    } else {
                        "Editar pergunta"
                    }
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Texto da pergunta *",
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = texto,
                        onValueChange = {
                            texto = it
                            erro = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Ex.: Você possui alguma lesão?")
                        },
                        minLines = 2,
                        maxLines = 4
                    )

                    Text(
                        text = "Tipo de resposta",
                        fontWeight = FontWeight.Bold
                    )

                    var menuAberto by remember {
                        mutableStateOf(false)
                    }

                    Box {
                        OutlinedButton(
                            onClick = { menuAberto = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                descricaoTipo(tipoResposta),
                                modifier = Modifier.weight(1f)
                            )
                            Text("⌄")
                        }

                        DropdownMenu(
                            expanded = menuAberto,
                            onDismissRequest = {
                                menuAberto = false
                            }
                        ) {
                            tiposResposta.forEach { tipo ->
                                DropdownMenuItem(
                                    text = {
                                        Text(descricaoTipo(tipo))
                                    },
                                    onClick = {
                                        tipoResposta = tipo
                                        menuAberto = false
                                        erro = null
                                    }
                                )
                            }
                        }
                    }

                    // Alternativas para perguntas de seleção.
                    if (
                        tipoResposta == "MULTIPLA_ESCOLHA" ||
                        tipoResposta == "CHECKBOX"
                    ) {
                        Text(
                            text = "Alternativas *",
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = opcoesTexto,
                            onValueChange = {
                                opcoesTexto = it
                                erro = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Digite uma alternativa por linha")
                            },
                            minLines = 3,
                            maxLines = 6
                        )

                        Text(
                            text = "Cadastre pelo menos duas alternativas, uma por linha.",
                            fontSize = 11.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    // Define se o aluno será obrigado a responder.
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F7FA)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Pergunta obrigatória",
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (obrigatoria) {
                                        "O aluno deverá responder esta pergunta."
                                    } else {
                                        "O aluno poderá deixar esta pergunta em branco."
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF757575)
                                )
                            }

                            Switch(
                                checked = obrigatoria,
                                onCheckedChange = {
                                    obrigatoria = it
                                }
                            )
                        }
                    }

                    if (erro != null) {
                        Text(
                            text = erro!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "* Campo obrigatório",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !salvando,
                    onClick = { salvarPergunta() }
                ) {
                    if (salvando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Salvar",
                            color = Color(0xFF002B5C)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !salvando,
                    onClick = {
                        mostrarDialogo = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // CONFIRMAÇÃO ANTES DE EXCLUIR UMA PERGUNTA.
    perguntaParaExcluir?.let { pergunta ->
        AlertDialog(
            onDismissRequest = {
                perguntaParaExcluir = null
            },
            title = {
                Text("Excluir pergunta?")
            },
            text = {
                Text(
                    "Deseja realmente excluir esta pergunta do modelo?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.excluir(pergunta)
                            } catch (_: Exception) {
                                erro = "Não foi possível excluir a pergunta."
                            } finally {
                                perguntaParaExcluir = null
                            }
                        }
                    }
                ) {
                    Text(
                        "Excluir",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        perguntaParaExcluir = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}