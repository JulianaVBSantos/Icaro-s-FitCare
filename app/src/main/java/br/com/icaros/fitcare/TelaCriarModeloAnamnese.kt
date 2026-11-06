
package br.com.icaros.fitcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import br.com.icaros.fitcare.data.ModeloAnamneseEntity
import br.com.icaros.fitcare.data.ModeloAnamneseRepository
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseEntity
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseRepository
import kotlinx.coroutines.launch
import org.json.JSONArray

private val AzulAnamnese = Color(0xFF002B5C)
private val FundoAnamnese = Color(0xFFF5F7FA)

// Converte as opções digitadas para JSON antes de salvar.
private fun opcoesParaJson(texto: String): String {
    val array = JSONArray()

    texto.lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { array.put(it) }

    return array.toString()
}

// Converte o JSON armazenado para texto ao editar uma pergunta.
private fun jsonParaOpcoes(json: String): String {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).joinToString("\n") {
            array.optString(it)
        }
    } catch (_: Exception) {
        ""
    }
}

// Perguntas que já vêm disponíveis no modelo padrão.
private fun perguntasPadrao(): List<PerguntaModeloAnamneseEntity> {
    return listOf(
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Qual é o seu objetivo principal?",
            tipoResposta = "MULTIPLA_ESCOLHA",
            opcoes = opcoesParaJson(
                "Emagrecimento\nHipertrofia\nCondicionamento físico\nSaúde e qualidade de vida\nOutro"
            ),
            obrigatoria = true,
            ordem = 0
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Você pratica atividade física atualmente?",
            tipoResposta = "SIM_NAO",
            obrigatoria = true,
            ordem = 1
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Com que frequência pratica atividade física?",
            tipoResposta = "MULTIPLA_ESCOLHA",
            opcoes = opcoesParaJson(
                "Não pratico\n1 a 2 vezes por semana\n3 a 4 vezes por semana\n5 ou mais vezes por semana"
            ),
            obrigatoria = false,
            ordem = 2
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Possui alguma lesão ou limitação física?",
            tipoResposta = "SIM_NAO",
            obrigatoria = true,
            ordem = 3
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Descreva suas lesões ou limitações, se houver.",
            tipoResposta = "TEXTO",
            obrigatoria = false,
            ordem = 4
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Possui alguma restrição médica para atividade física?",
            tipoResposta = "SIM_NAO",
            obrigatoria = true,
            ordem = 5
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Utiliza algum medicamento?",
            tipoResposta = "SIM_NAO",
            obrigatoria = false,
            ordem = 6
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Qual é a sua disponibilidade semanal para treinar?",
            tipoResposta = "MULTIPLA_ESCOLHA",
            opcoes = opcoesParaJson(
                "1 a 2 dias\n3 a 4 dias\n5 a 6 dias\nTodos os dias"
            ),
            obrigatoria = true,
            ordem = 7
        ),
        PerguntaModeloAnamneseEntity(
            modeloId = 0,
            texto = "Existe alguma informação adicional que gostaria de compartilhar?",
            tipoResposta = "TEXTO",
            obrigatoria = false,
            ordem = 8
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCriarModeloAnamnese(
    modeloId: Long? = null,
    repository: ModeloAnamneseRepository,
    perguntaRepository: PerguntaModeloAnamneseRepository,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("PERSONALIZADO") }
    var ativo by remember { mutableStateOf(true) }

    var perguntas by remember {
        mutableStateOf<List<PerguntaModeloAnamneseEntity>>(emptyList())
    }

    // Mantém uma cópia das perguntas originalmente salvas.
    // Ela será utilizada para identificar perguntas removidas.
    var perguntasOriginais by remember {
        mutableStateOf<List<PerguntaModeloAnamneseEntity>>(emptyList())
    }

    var carregando by remember {
        mutableStateOf(modeloId != null)
    }

    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    var mostrarDialogPergunta by remember { mutableStateOf(false) }
    var perguntaEmEdicao by remember {
        mutableStateOf<PerguntaModeloAnamneseEntity?>(null)
    }

    var textoPergunta by remember { mutableStateOf("") }
    var tipoPergunta by remember { mutableStateOf("TEXTO") }
    var opcoesPergunta by remember { mutableStateOf("") }
    var obrigatoriaPergunta by remember { mutableStateOf(false) }

    var expandirTipos by remember { mutableStateOf(false) }

    // Identifica se a categoria anterior era Padrão.
    var categoriaAnteriorPadrao by remember {
        mutableStateOf(false)
    }

    val editando = modeloId != null

    val tiposResposta = listOf(
        "TEXTO",
        "NUMERO",
        "DATA",
        "SIM_NAO",
        "MULTIPLA_ESCOLHA",
        "CHECKBOX"
    )

    // Carrega os dados do modelo e suas perguntas na edição.
    LaunchedEffect(modeloId) {
        if (modeloId != null) {
            try {
                val modelo = repository.buscarPorId(modeloId)

                if (modelo != null) {
                    nome = modelo.nome
                    descricao = modelo.descricao
                    categoria = modelo.categoria
                    ativo = modelo.ativo

                    val existentes =
                        perguntaRepository.buscarPorModelo(modeloId)

                    perguntasOriginais = existentes
                    perguntas = existentes.sortedBy { it.ordem }
                } else {
                    erro = "Modelo não encontrado."
                }
            } catch (e: Exception) {
                erro = e.message ?: "Não foi possível carregar o modelo."
            } finally {
                carregando = false
            }
        }
    }

    // Abre o formulário para adicionar ou editar uma pergunta.
    fun abrirPergunta(pergunta: PerguntaModeloAnamneseEntity? = null) {
        perguntaEmEdicao = pergunta
        textoPergunta = pergunta?.texto ?: ""
        tipoPergunta = pergunta?.tipoResposta ?: "TEXTO"
        opcoesPergunta = pergunta?.let {
            jsonParaOpcoes(it.opcoes)
        } ?: ""
        obrigatoriaPergunta = pergunta?.obrigatoria ?: false
        mostrarDialogPergunta = true
    }

    // Altera a categoria e prepara as perguntas correspondentes.
    fun selecionarCategoria(novaCategoria: String) {
        if (categoria == novaCategoria) return

        categoria = novaCategoria

        if (novaCategoria == "PADRAO" && perguntas.isEmpty()) {
            perguntas = perguntasPadrao()
        } else if (
            novaCategoria == "PERSONALIZADO" &&
            categoriaAnteriorPadrao
        ) {
            perguntas = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoAnamnese)
            .statusBarsPadding()
    ) {
        // CABEÇALHO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = AzulAnamnese
                )
            }

            Column {
                Text(
                    text = if (editando) "Editar modelo" else "Novo modelo",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Text(
                    text = "Configuração da anamnese",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        if (carregando) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AzulAnamnese)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NOME DO MODELO
                Text(
                    "Nome do modelo *",
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        erro = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Ex.: Anamnese inicial")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // DESCRIÇÃO
                Text(
                    "Descrição",
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Descreva a finalidade deste modelo")
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                // CATEGORIA
                Text(
                    "Categoria do modelo",
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoria == "PADRAO",
                                onClick = {
                                    categoriaAnteriorPadrao = false
                                    selecionarCategoria("PADRAO")
                                }
                            )
                            Text(
                                "Padrão",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "Modelo com perguntas iniciais disponibilizadas pelo aplicativo.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 48.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = categoria == "PERSONALIZADO",
                                onClick = {
                                    categoriaAnteriorPadrao =
                                        categoria == "PADRAO"
                                    selecionarCategoria("PERSONALIZADO")
                                }
                            )
                            Text(
                                "Personalizado",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            "Crie suas próprias perguntas e configure os tipos de resposta.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }
                }

                // ÁREA DE PERGUNTAS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Perguntas da anamnese",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Text(
                            if (categoria == "PADRAO") {
                                "Perguntas sugeridas pelo aplicativo. Você pode editar, excluir ou adicionar outras."
                            } else {
                                "Adicione as perguntas que deseja utilizar neste formulário."
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        if (perguntas.isEmpty()) {
                            Text(
                                "Nenhuma pergunta adicionada ainda.",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        perguntas.forEachIndexed { index, pergunta ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = FundoAnamnese
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        "${index + 1}. ${pergunta.texto}",
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        "Tipo: ${pergunta.tipoResposta.replace("_", " ")}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Text(
                                        if (pergunta.obrigatoria) {
                                            "Resposta obrigatória"
                                        } else {
                                            "Resposta opcional"
                                        },
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Row(
                                        horizontalArrangement =
                                            Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                abrirPergunta(pergunta)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null
                                            )
                                            Text("Editar")
                                        }

                                        TextButton(
                                            onClick = {
                                                perguntas = perguntas
                                                    .filterIndexed { i, _ ->
                                                        i != index
                                                    }
                                                    .mapIndexed { i, item ->
                                                        item.copy(ordem = i)
                                                    }
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                "Excluir",
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { abrirPergunta() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Adicionar pergunta")
                        }
                    }
                }

                // STATUS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Modelo ativo",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (ativo) {
                                    "Disponível para utilização"
                                } else {
                                    "Indisponível para novas utilizações"
                                },
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Switch(
                            checked = ativo,
                            onCheckedChange = { ativo = it }
                        )
                    }
                }

                if (erro != null) {
                    Text(
                        text = erro!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Text(
                    "* Campos obrigatórios",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // SALVAR MODELO E PERGUNTAS
            Button(
                onClick = {
                    if (nome.isBlank()) {
                        erro = "Informe o nome do modelo."
                        return@Button
                    }

                    if (perguntas.isEmpty()) {
                        erro = "Adicione pelo menos uma pergunta."
                        return@Button
                    }

                    scope.launch {
                        salvando = true
                        erro = null

                        try {
                            val modelo = ModeloAnamneseEntity(
                                id = modeloId ?: 0L,
                                nome = nome.trim(),
                                descricao = descricao.trim(),
                                categoria = categoria,
                                ativo = ativo
                            )

                            // Primeiro salva o modelo e recupera seu ID.
                            val idSalvo = repository.salvar(modelo)

                            // Remove somente perguntas antigas que foram
                            // excluídas pelo profissional.
                            val idsMantidos = perguntas
                                .filter { it.id != 0L }
                                .map { it.id }
                                .toSet()

                            perguntasOriginais
                                .filter { it.id !in idsMantidos }
                                .forEach {
                                    perguntaRepository.excluir(it)
                                }

                            // Salva as perguntas novas e atualiza as antigas.
                            perguntas.forEachIndexed { index, pergunta ->
                                perguntaRepository.salvar(
                                    pergunta.copy(
                                        modeloId = idSalvo,
                                        ordem = index
                                    )
                                )
                            }

                            onSalvo()
                        } catch (e: Exception) {
                            erro = e.message
                                ?: "Não foi possível salvar o modelo e as perguntas."
                        } finally {
                            salvando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .height(52.dp),
                enabled = !salvando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulAnamnese
                )
            ) {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (editando) "Salvar alterações" else "Criar modelo",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // FORMULÁRIO PARA ADICIONAR OU EDITAR PERGUNTAS
    if (mostrarDialogPergunta) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogPergunta = false
            },
            title = {
                Text(
                    if (perguntaEmEdicao == null) {
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
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = textoPergunta,
                        onValueChange = { textoPergunta = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Texto da pergunta") },
                        placeholder = {
                            Text("Digite sua pergunta")
                        }
                    )

                    Text(
                        "Tipo de resposta",
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandirTipos,
                        onExpandedChange = {
                            expandirTipos = !expandirTipos
                        }
                    ) {
                        OutlinedTextField(
                            value = tipoPergunta.replace("_", " "),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("Tipo") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expandirTipos
                                )
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = expandirTipos,
                            onDismissRequest = {
                                expandirTipos = false
                            }
                        ) {
                            tiposResposta.forEach { tipo ->
                                DropdownMenuItem(
                                    text = {
                                        Text(tipo.replace("_", " "))
                                    },
                                    onClick = {
                                        tipoPergunta = tipo
                                        expandirTipos = false
                                        if (
                                            tipo != "MULTIPLA_ESCOLHA" &&
                                            tipo != "CHECKBOX"
                                        ) {
                                            opcoesPergunta = ""
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (
                        tipoPergunta == "MULTIPLA_ESCOLHA" ||
                        tipoPergunta == "CHECKBOX"
                    ) {
                        OutlinedTextField(
                            value = opcoesPergunta,
                            onValueChange = { opcoesPergunta = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Opções de resposta") },
                            placeholder = {
                                Text("Uma opção por linha")
                            },
                            minLines = 3
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = obrigatoriaPergunta,
                            onCheckedChange = {
                                obrigatoriaPergunta = it
                            }
                        )
                        Text("Resposta obrigatória")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (textoPergunta.isBlank()) {
                            erro = "Informe o texto da pergunta."
                            return@TextButton
                        }

                        if (
                            (tipoPergunta == "MULTIPLA_ESCOLHA" ||
                                    tipoPergunta == "CHECKBOX") &&
                            opcoesPergunta.lines().none {
                                it.isNotBlank()
                            }
                        ) {
                            erro = "Informe pelo menos uma opção."
                            return@TextButton
                        }

                        val pergunta = PerguntaModeloAnamneseEntity(
                            id = perguntaEmEdicao?.id ?: 0L,
                            modeloId = modeloId ?: 0L,
                            texto = textoPergunta.trim(),
                            tipoResposta = tipoPergunta,
                            opcoes = if (
                                tipoPergunta == "MULTIPLA_ESCOLHA" ||
                                tipoPergunta == "CHECKBOX"
                            ) {
                                opcoesParaJson(opcoesPergunta)
                            } else {
                                ""
                            },
                            obrigatoria = obrigatoriaPergunta,
                            ordem = perguntaEmEdicao?.ordem
                                ?: perguntas.size
                        )

                        perguntas = if (perguntaEmEdicao == null) {
                            (perguntas + pergunta).mapIndexed { index, item ->
                                item.copy(ordem = index)
                            }
                        } else {
                            perguntas.map {
                                if (it.id == perguntaEmEdicao?.id &&
                                    perguntaEmEdicao?.id != 0L
                                ) {
                                    pergunta.copy(ordem = it.ordem)
                                } else if (
                                    perguntaEmEdicao?.id == 0L &&
                                    it == perguntaEmEdicao
                                ) {
                                    pergunta.copy(ordem = it.ordem)
                                } else {
                                    it
                                }
                            }
                        }

                        erro = null
                        mostrarDialogPergunta = false
                    }
                ) {
                    Text("Salvar pergunta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogPergunta = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
