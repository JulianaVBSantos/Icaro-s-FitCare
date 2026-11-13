package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.AlunoEntity
import br.com.icaros.fitcare.data.AlunoRepository
import br.com.icaros.fitcare.data.AlunoSyncWorker
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
import br.com.icaros.fitcare.data.FirestoreSyncService
import br.com.icaros.fitcare.data.FitCareDatabase
import br.com.icaros.fitcare.data.ModeloAnamneseEntity
import br.com.icaros.fitcare.data.ModeloAnamneseRepository
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseEntity
import br.com.icaros.fitcare.data.PerguntaModeloAnamneseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Composable
fun TelaModelosAnamnese(
    repository: ModeloAnamneseRepository,
    alunoRepository: AlunoRepository,
    anamneseAtribuidaRepository: AnamneseAtribuidaRepository,
    perguntaModeloRepository: PerguntaModeloAnamneseRepository,
    alunoId: Long?,
    onVoltar: () -> Unit,
    onNovoModelo: () -> Unit,
    onEditarModelo: (ModeloAnamneseEntity) -> Unit,
    onPerguntas: (ModeloAnamneseEntity) -> Unit,
    onAbrirAnamnese: (Long) -> Unit,
    onAnamneseEnviada: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val modelos by repository.listarTodos()
        .collectAsState(initial = emptyList())

    val alunos by alunoRepository.listarAlunos()
        .collectAsState(initial = emptyList())

    var modeloEmPreenchimentoId by remember {
        mutableStateOf<Long?>(null)
    }

    var mensagemEnvio by remember {
        mutableStateOf<String?>(null)
    }

    var envioSucesso by remember {
        mutableStateOf(false)
    }

    var modeloParaExcluir by remember {
        mutableStateOf<ModeloAnamneseEntity?>(null)
    }

    var modeloParaVisualizar by remember {
        mutableStateOf<ModeloAnamneseEntity?>(null)
    }

    var perguntasPreVisualizacao by remember {
        mutableStateOf<List<PerguntaModeloAnamneseEntity>>(emptyList())
    }

    var carregandoPreVisualizacao by remember {
        mutableStateOf(false)
    }

    var modeloParaSelecionarAluno by remember {
        mutableStateOf<ModeloAnamneseEntity?>(null)
    }

    val scope = rememberCoroutineScope()

    fun executarEnvioParaAluno(
        modelo: ModeloAnamneseEntity,
        destinoAlunoId: Long
    ) {
        if (modeloEmPreenchimentoId != null || !modelo.ativo) return

        modeloEmPreenchimentoId = modelo.id
        mensagemEnvio = null
        envioSucesso = false

        scope.launch {
            try {
                val novaAnamneseId =
                    anamneseAtribuidaRepository.atribuir(
                        alunoId = destinoAlunoId,
                        modeloId = modelo.id
                    )

                anamneseAtribuidaRepository.enviar(
                    novaAnamneseId
                )

                // Sincroniza imediatamente com o Firestore para refletir nos demais dispositivos
                withContext(Dispatchers.IO) {
                    val db = FitCareDatabase.getDatabase(context)
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarAnamnesesAtribuidas()
                }

                AlunoSyncWorker.agendar(context)

                val nomeDestinatario = alunos
                    .firstOrNull { it.id == destinoAlunoId }
                    ?.nome
                    .orEmpty()

                envioSucesso = true
                mensagemEnvio = if (nomeDestinatario.isNotBlank()) {
                    "Anamnese enviada com sucesso para $nomeDestinatario."
                } else {
                    "Anamnese enviada com sucesso ao aluno."
                }

                if (alunoId != null && onAnamneseEnviada != null) {
                    onAnamneseEnviada()
                }

            } catch (e: Exception) {
                mensagemEnvio =
                    e.message
                        ?: "Não foi possível enviar a anamnese ao aluno."

                envioSucesso = false

            } finally {
                modeloEmPreenchimentoId = null
            }
        }
    }

    LaunchedEffect(modeloParaVisualizar) {
        val modelo = modeloParaVisualizar
        if (modelo != null) {
            carregandoPreVisualizacao = true
            try {
                perguntasPreVisualizacao =
                    perguntaModeloRepository.buscarPorModelo(modelo.id)
                        .sortedBy { it.ordem }
            } catch (_: Exception) {
                perguntasPreVisualizacao = emptyList()
            } finally {
                carregandoPreVisualizacao = false
            }
        } else {
            perguntasPreVisualizacao = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // CABEÇALHO
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

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Ícaros FitCare",
                modifier = Modifier.height(48.dp)
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Modelos de Anamnese",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Gerencie, visualize e envie modelos de avaliação aos alunos.",
            fontSize = 13.sp,
            color = Color(0xFF757575)
        )

        // MENSAGEM DE RESULTADO
        mensagemEnvio?.let { mensagem ->
            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (envioSucesso) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFEBEE)
                    }
                )
            ) {
                Text(
                    text = mensagem,
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (envioSucesso) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CRIAR NOVO MODELO
        Button(
            onClick = onNovoModelo,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF002B5C),
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Criar novo modelo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Modelos cadastrados (${modelos.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (modelos.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhum modelo cadastrado",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Crie um modelo para começar a montar suas anamneses.",
                        color = Color(0xFF757575),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                modelos.forEach { modelo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = modelo.nome,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (modelo.categoria == "PADRAO") {
                                    "Modelo padrão"
                                } else {
                                    "Modelo personalizado"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF002B5C),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (modelo.descricao.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = modelo.descricao,
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (modelo.ativo) "Ativo" else "Inativo",
                                fontSize = 12.sp,
                                color = if (modelo.ativo) {
                                    Color(0xFF002B5C)
                                } else {
                                    Color(0xFF757575)
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(
                                color = Color(0xFFF5F7FA)
                            )
// AÇÕES DO MODELO
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onPerguntas(modelo)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Perguntas")
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (modelo.categoria == "PERSONALIZADO") {
                                        IconButton(
                                            onClick = {
                                                onEditarModelo(modelo)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar modelo",
                                                tint = Color(0xFF1565C0)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            modeloParaExcluir = modelo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir modelo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
// BOTÃO 1: VISUALIZAR ANAMNESE (APENAS PRÉ-VISUALIZA O MODELO)
                            OutlinedButton(
                                onClick = {
                                    modeloParaVisualizar = modelo
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF002B5C)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Visualizar anamnese",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
// BOTÃO 2: ENVIAR ANAMNESE AO ALUNO (APENAS ENVIA, SEM ABRIR VISUALIZAÇÃO)
                            Button(
                                onClick = {
                                    if (
                                        modeloEmPreenchimentoId == null &&
                                        modelo.ativo
                                    ) {
                                        if (alunoId != null) {
                                            executarEnvioParaAluno(
                                                modelo = modelo,
                                                destinoAlunoId = alunoId
                                            )
                                        } else {
                                            modeloParaSelecionarAluno = modelo
                                        }
                                    }
                                },
                                enabled = modeloEmPreenchimentoId == null &&
                                        modelo.ativo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF002B5C),
                                    contentColor = Color.White
                                )
                            ) {
                                if (modeloEmPreenchimentoId == modelo.id) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enviando anamnese...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (modelo.ativo) {
                                            "Enviar anamnese ao aluno"
                                        } else {
                                            "Modelo indisponível"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
// DIÁLOGO DE PRÉ-VISUALIZAÇÃO DO MODELO
    modeloParaVisualizar?.let { modelo ->
        AlertDialog(
            onDismissRequest = {
                modeloParaVisualizar = null
            },
            title = {
                Column {
                    Text(
                        text = "Pré-visualização da anamnese",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002B5C)
                    )
                    Text(
                        text = modelo.nome,
                        fontSize = 13.sp,
                        color = Color(0xFF526579)
                    )
                }
            },
            text = {
                if (carregandoPreVisualizacao) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF002B5C)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (modelo.descricao.isNotBlank()) {
                            Text(
                                text = modelo.descricao,
                                fontSize = 12.sp,
                                color = Color(0xFF526579)
                            )
                            HorizontalDivider()
                        }
                        if (perguntasPreVisualizacao.isEmpty()) {
                            Text(
                                text = "Este modelo ainda não possui perguntas cadastradas.",
                                fontSize = 13.sp,
                                color = Color(0xFF757575)
                            )
                        } else {
                            perguntasPreVisualizacao.forEachIndexed { index, pergunta ->
                                val opcoes = remember(pergunta.opcoes) {
                                    try {
                                        val arr = JSONArray(pergunta.opcoes)
                                        (0 until arr.length()).map {
                                            arr.optString(it)
                                        }
                                    } catch (_: Exception) {
                                        emptyList()
                                    }
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFEAF3F9)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}. ${pergunta.texto}" +
                                                    if (pergunta.obrigatoria) " *" else "",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF002B5C)
                                        )
                                        Text(
                                            text = "Tipo: ${pergunta.tipoResposta.replace("_", " ")}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF526579)
                                        )
                                        if (opcoes.isNotEmpty()) {
                                            Text(
                                                text = "Opções: ${opcoes.joinToString(" • ")}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF526579)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        modeloParaVisualizar = null
                    }
                ) {
                    Text(
                        text = "Fechar",
                        color = Color(0xFF002B5C),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
// DIÁLOGO DE SELEÇÃO DE ALUNO
    modeloParaSelecionarAluno?.let { modelo ->
        AlertDialog(
            onDismissRequest = {
                modeloParaSelecionarAluno = null
            },
            title = {
                Text(
                    text = "Selecionar aluno",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF002B5C)
                )
            },
            text = {
                if (alunos.isEmpty()) {
                    Text(
                        text = "Nenhum aluno cadastrado para receber a anamnese.",
                        fontSize = 13.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Escolha o aluno que receberá '${modelo.nome}':",
                            fontSize = 13.sp,
                            color = Color(0xFF526579)
                        )
                        alunos.forEach { aluno: AlunoEntity ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val destinoId = aluno.id
                                        val modeloEscolhido = modelo
                                        modeloParaSelecionarAluno = null
                                        executarEnvioParaAluno(
                                            modelo = modeloEscolhido,
                                            destinoAlunoId = destinoId
                                        )
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFEAF3F9)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = aluno.nome,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF002B5C)
                                    )
                                    if (aluno.objetivo.isNotBlank()) {
                                        Text(
                                            text = aluno.objetivo,
                                            fontSize = 12.sp,
                                            color = Color(0xFF526579)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        modeloParaSelecionarAluno = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
// CONFIRMAÇÃO DE EXCLUSÃO
    modeloParaExcluir?.let { modelo ->
        AlertDialog(
            onDismissRequest = {
                modeloParaExcluir = null
            },
            title = {
                Text("Excluir modelo?")
            },
            text = {
                Text(
                    if (modelo.categoria == "PADRAO") {
                        "Deseja realmente excluir o modelo padrão '${modelo.nome}'? " +
                                "Ele será removido da lista de modelos padrão de todos os professores. " +
                                "As anamneses já atribuídas aos alunos não devem ser afetadas."
                    } else {
                        "Deseja realmente excluir o modelo personalizado '${modelo.nome}'? " +
                                "As anamneses já atribuídas aos alunos não devem ser afetadas."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.excluir(modelo)
                                mensagemEnvio =
                                    "Modelo excluído com sucesso."
                                envioSucesso = true
                            } catch (e: Exception) {
                                mensagemEnvio =
                                    e.message
                                        ?: "Não foi possível excluir o modelo."
                                envioSucesso = false
                            } finally {
                                modeloParaExcluir = null
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
                        modeloParaExcluir = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}