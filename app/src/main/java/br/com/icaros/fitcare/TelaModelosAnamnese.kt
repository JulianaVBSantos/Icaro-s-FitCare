package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
    val escuro = temaEscuroAtivoState

    val corFundo = if (escuro) Color(0xFF0F1722) else Color(0xFFF5F7FA)
    val corCabecalhoTopo = if (escuro) Color(0xFF141F2E) else Color(0xFFDCEEFF)
    val corCabecalhoBase = if (escuro) Color(0xFF0F1722) else Color(0xFFF5F7FA)
    val corCartao = if (escuro) Color(0xFF172331) else Color.White
    val corCartaoSecundario = if (escuro) Color(0xFF1E2D3D) else Color(0xFFEAF3F9)
    val corTextoPrincipal = if (escuro) Color(0xFFEAF2FA) else Color(0xFF212121)
    val corTextoSecundario = if (escuro) Color(0xFFA2B5C8) else Color(0xFF757575)
    val corDestaque = if (escuro) Color(0xFF7AB8F5) else Color(0xFF002B5C)

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

                withContext(Dispatchers.IO) {
                    val db = FitCareDatabase.getDatabase(context)
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarAnamnesesAtribuidas()
                    Unit
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
            .background(corFundo)
            .navigationBarsPadding()
    ) {
        // CABEÇALHO COM DEGRADÊ VERTICAL PARA CONTRASTE DA LOGO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(corCabecalhoTopo, corCabecalhoBase)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onVoltar,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = corDestaque
                )
            }

            LogoFitCarePadrao(
                modifier = Modifier.align(Alignment.Center),
                largura = 135.dp,
                altura = 60.dp
            )

        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Modelos de Anamnese",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = corTextoPrincipal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Gerencie, visualize e envie modelos de avaliação aos alunos.",
                fontSize = 13.sp,
                color = corTextoSecundario
            )

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
                color = corTextoPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (modelos.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = corCartao
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
                            tint = corDestaque,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Nenhum modelo cadastrado",
                            fontWeight = FontWeight.Bold,
                            color = corTextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Crie um modelo para começar a montar suas anamneses.",
                            color = corTextoSecundario,
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
                                containerColor = corCartao
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
                                    color = corTextoPrincipal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (modelo.categoria == "PADRAO") {
                                        "Modelo padrão"
                                    } else {
                                        "Modelo personalizado"
                                    },
                                    fontSize = 12.sp,
                                    color = corDestaque,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (modelo.descricao.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = modelo.descricao,
                                        fontSize = 13.sp,
                                        color = corTextoSecundario
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (modelo.ativo) "Ativo" else "Inativo",
                                    fontSize = 12.sp,
                                    color = if (modelo.ativo) corDestaque else corTextoSecundario
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = corCartaoSecundario)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { onPerguntas(modelo) }
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
                                                onClick = { onEditarModelo(modelo) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar modelo",
                                                    tint = corDestaque
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { modeloParaExcluir = modelo }
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
                                OutlinedButton(
                                    onClick = { modeloParaVisualizar = modelo },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = corDestaque
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
    }
    modeloParaVisualizar?.let { modelo ->
        AlertDialog(
            onDismissRequest = { modeloParaVisualizar = null },
            title = {
                Column {
                    Text(
                        text = "Pré-visualização da anamnese",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = corDestaque
                    )
                    Text(
                        text = modelo.nome,
                        fontSize = 13.sp,
                        color = corTextoSecundario
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
                        CircularProgressIndicator(color = corDestaque)
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
                                color = corTextoSecundario
                            )
                            HorizontalDivider()
                        }
                        if (perguntasPreVisualizacao.isEmpty()) {
                            Text(
                                text = "Este modelo ainda não possui perguntas cadastradas.",
                                fontSize = 13.sp,
                                color = corTextoSecundario
                            )
                        } else {
                            perguntasPreVisualizacao.forEachIndexed { index, pergunta ->
                                val opcoes = remember(pergunta.opcoes) {
                                    try {
                                        val arr = JSONArray(pergunta.opcoes)
                                        (0 until arr.length()).map { idx ->
                                            arr.optString(idx)
                                        }
                                    } catch (e: Exception) {
                                        emptyList<String>()
                                    }
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = corCartaoSecundario
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
                                            color = corTextoPrincipal
                                        )
                                        Text(
                                            text = "Tipo: ${pergunta.tipoResposta.replace("", " ")}",
                                            fontSize = 11.sp,
                                            color = corTextoSecundario
                                        )
                                        if (opcoes.isNotEmpty()) {
                                            Text(
                                                text = "Opções: ${opcoes.joinToString(" • ")}",
                                                fontSize = 11.sp,
                                                color = corTextoSecundario
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
                    onClick = { modeloParaVisualizar = null }
                ) {
                    Text(
                        text = "Fechar",
                        color = corDestaque,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
    modeloParaSelecionarAluno?.let { modelo ->
        AlertDialog(
            onDismissRequest = { modeloParaSelecionarAluno = null },
            title = {
                Text(
                    text = "Selecionar aluno",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = corDestaque
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
                            color = corTextoSecundario
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
                                    containerColor = corCartaoSecundario
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = aluno.nome,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = corTextoPrincipal
                                    )
                                    if (aluno.objetivo.isNotBlank()) {
                                        Text(
                                            text = aluno.objetivo,
                                            fontSize = 12.sp,
                                            color = corTextoSecundario
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
                    onClick = { modeloParaSelecionarAluno = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    modeloParaExcluir?.let { modelo ->
        AlertDialog(
            onDismissRequest = { modeloParaExcluir = null },
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
                                mensagemEnvio = "Modelo excluído com sucesso."
                                envioSucesso = true
                            } catch (e: Exception) {
                                mensagemEnvio = e.message ?: "Não foi possível excluir o modelo."
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
                    onClick = { modeloParaExcluir = null }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}