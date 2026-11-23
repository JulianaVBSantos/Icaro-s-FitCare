package br.com.icaros.fitcare

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.util.Locale

private fun ehOpcaoOutro(opcao: String): Boolean {
    val norm = opcao.trim().lowercase(Locale.ROOT)
    return norm == "outro" || norm == "outros" || norm == "outra" || norm == "outras"
}

private fun extrairItensResposta(
    tipoResposta: String,
    respostaBruta: String
): List<String> {
    if (respostaBruta.isBlank()) return emptyList()
    if (respostaBruta.trim().startsWith("[")) {
        return try {
            val json = JSONArray(respostaBruta)
            (0 until json.length()).mapNotNull { i ->
                json.optString(i).trim().takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList<String>()
        }
    }
    return listOf(respostaBruta.trim())
}

private fun respostaEstaPreenchida(
    tipoResposta: String,
    resposta: String?
): Boolean {
    if (resposta.isNullOrBlank()) return false
    if (resposta == "SIM_ABERTO") return false
    val itens = extrairItensResposta(tipoResposta, resposta)
    if (itens.isEmpty()) return false
    return itens.none { ehOpcaoOutro(it) }
}

private fun formatarRespostaAluno(
    tipoResposta: String,
    respostaBruta: String
): String {
    if (respostaBruta == "SIM_ABERTO") return "Sim"
    val itens = extrairItensResposta(tipoResposta, respostaBruta)
    return if (itens.isEmpty()) "" else itens.joinToString(", ")
}

private suspend fun sincronizarAcompanhamentoAlunoComRespostas(
    alunoId: Long,
    alunoRepository: AlunoRepository,
    perguntas: List<PerguntaAnamneseAtribuidaEntity>,
    respostas: Map<Long, String>
) {
    val aluno = alunoRepository.buscarPorIdDireto(alunoId) ?: return

    val condicoesExtraidas = linkedSetOf<String>()
    val restricoesExtraidas = linkedSetOf<String>()

    var atualizouCondicoes = false
    var atualizouRestricoes = false

    for (pergunta in perguntas) {
        if (pergunta.tipoResposta == "SIM_NAO") continue

        val tituloLower = pergunta.texto.lowercase(Locale.ROOT)
        val respostaBruta = respostas[pergunta.id].orEmpty().trim()
        if (respostaBruta.isBlank()) continue

        val itens = extrairItensResposta(pergunta.tipoResposta, respostaBruta)
            .map { item ->
                if (item.startsWith("Outro:", ignoreCase = true)) {
                    item.substringAfter(":").trim()
                } else {
                    item
                }
            }
            .filter {
                it.isNotBlank() &&
                        !it.equals("Nenhuma", ignoreCase = true) &&
                        !it.equals("Não", ignoreCase = true) &&
                        !it.equals("Nao", ignoreCase = true) &&
                        !it.equals("Sim", ignoreCase = true) &&
                        !it.equals("SIM_ABERTO", ignoreCase = true) &&
                        !ehOpcaoOutro(it)
            }

        when {
            tituloLower.contains("condiç") ||
                    tituloLower.contains("condic") ||
                    tituloLower.contains("saúde") ||
                    tituloLower.contains("saude") ||
                    tituloLower.contains("dor") -> {
                atualizouCondicoes = true
                condicoesExtraidas.addAll(itens)
            }

            tituloLower.contains("restriç") ||
                    tituloLower.contains("restric") ||
                    tituloLower.contains("limitaç") ||
                    tituloLower.contains("limitac") -> {
                atualizouRestricoes = true
                restricoesExtraidas.addAll(itens)
            }
        }
    }

    val novasCondicoes = if (atualizouCondicoes) {
        condicoesExtraidas.joinToString(", ")
    } else {
        aluno.condicoes
    }

    val novasRestricoes = if (atualizouRestricoes) {
        restricoesExtraidas.joinToString(", ")
    } else {
        aluno.restricoes
    }

    val totalItens =
        novasCondicoes.split(",").count { it.trim().isNotBlank() } +
                novasRestricoes.split(",").count { it.trim().isNotBlank() }

    val novoStatus = when {
        totalItens == 0 -> "Normal"
        totalItens == 1 -> "Atenção"
        else -> "Prioridade"
    }

    alunoRepository.atualizarDireto(
        aluno.copy(
            condicoes = novasCondicoes,
            restricoes = novasRestricoes,
            status = novoStatus
        )
    )
}

@Composable
fun TelaAreaAluno(
    alunoId: Long?,
    anamneseRepository: AnamneseAtribuidaRepository,
    respostaRepository: RespostaAnamneseRepository,
    documentoRepository: AnamneseRepository,
    alunoRepository: AlunoRepository,
    onSair: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val prefsSessao = remember {
        context.getSharedPreferences("fitcare_sessao", android.content.Context.MODE_PRIVATE)
    }
    val temaEscuro = temaEscuroAtivoState || prefsSessao.getBoolean("tema_escuro_ativo", isSystemInDarkTheme())

    val corFundo = if (temaEscuro) Color(0xFF0F1722) else Color(0xFFEAF3F9)
    val corCabecalhoTopo = if (temaEscuro) Color(0xFF141F2E) else Color(0xFFDCEEFF)
    val corCabecalhoBase = if (temaEscuro) Color(0xFF0F1722) else Color(0xFFEAF3F9)
    val corCartao = if (temaEscuro) Color(0xFF172331) else Color.White
    val corTextoPrincipal = if (temaEscuro) Color(0xFFEAF2FA) else Color(0xFF002B5C)
    val corTextoSecundario = if (temaEscuro) Color(0xFFA2B5C8) else Color(0xFF526579)
    val corDestaque = if (temaEscuro) Color(0xFF7AB8F5) else Color(0xFF002B5C)

    val idAluno = alunoId ?: SessaoUsuario.usuarioAtual?.alunoId

    val db = remember { FitCareDatabase.getDatabase(context) }
    val treinoDao = remember { db.treinoDao() }

    // Sincroniza uma única vez ao entrar na área do aluno (sob demanda)
    LaunchedEffect(idAluno) {
        if (idAluno != null && idAluno > 0L) {
            withContext(Dispatchers.IO) {
                try {
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarAnamnesesAtribuidasDoAluno()
                    syncService.sincronizarTreinosDoAluno()
                } catch (e: Exception) {
                }
            }
        }
    }

    val anamnesesFlow = remember(idAluno) {
        if (idAluno != null && idAluno > 0L) {
            anamneseRepository.listarPorAluno(idAluno)
        } else {
            flowOf(emptyList())
        }
    }
    val anamneses by anamnesesFlow.collectAsState(initial = emptyList())

    val treinosFlow = remember(idAluno) {
        if (idAluno != null && idAluno > 0L) {
            treinoDao.listarTreinosDoAluno(idAluno)
        } else {
            flowOf(emptyList())
        }
    }
    val treinosAluno by treinosFlow.collectAsState(initial = emptyList())

    val documentosFlow = remember(idAluno, documentoRepository) {
        if (idAluno != null && idAluno > 0L) {
            documentoRepository.listarDocumentos(idAluno)
        } else {
            flowOf(emptyList())
        }
    }
    val documentos by documentosFlow.collectAsState(initial = emptyList())

    var anamneseSelecionadaId by remember(idAluno) { mutableStateOf<Long?>(null) }
    var treinoSelecionadoAluno by remember(idAluno) { mutableStateOf<TreinoEntity?>(null) }
    var exercicioEditandoCarga by remember { mutableStateOf<ExercicioEntity?>(null) }

    var perguntas by remember { mutableStateOf<List<PerguntaAnamneseAtribuidaEntity>>(emptyList()) }
    var respostas by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf("") }
    var mensagemSucesso by remember { mutableStateOf("") }
    var mensagemDocumento by remember { mutableStateOf("") }

    val salvamentosPendentes = remember { mutableMapOf<Long, Job>() }
    val anamneseSelecionada = anamneses.firstOrNull { it.id == anamneseSelecionadaId }

    val pendentes = anamneses.filter { it.status == "PENDENTE" || it.status == "EM_ANDAMENTO" }
    val bloqueadoPorAnamneseObrigatoria = pendentes.isNotEmpty()

    suspend fun aguardarSalvamentos() {
        salvamentosPendentes.values.toList().forEach { it.join() }
        salvamentosPendentes.clear()
    }
// Estados do sininho de notificações do aluno
    var mostrarNotificacoesAluno by remember { mutableStateOf(false) }
    val prefsNotifAluno = remember {
        context.getSharedPreferences("fitcare_notificacoes_aluno_ui", android.content.Context.MODE_PRIVATE)
    }
    var idsLidasAluno by remember {
        mutableStateOf(
            prefsNotifAluno.getStringSet("lidas_ids", emptySet())?.toSet() ?: emptySet()
        )
    }
    fun marcarNotifAlunoComoLida(chave: String) {
        val novo = idsLidasAluno + chave
        idsLidasAluno = novo
        prefsNotifAluno.edit().putStringSet("lidas_ids", novo).apply()
    }
    val assinaturaTreinos by produceState(
        initialValue = emptyList<Triple<TreinoEntity, String, String>>(),
        key1 = treinosAluno
    ) {
        val lista = mutableListOf<Triple<TreinoEntity, String, String>>()
        for (tr in treinosAluno) {
            val exs = try {
                treinoDao.listarExerciciosDoAluno(tr.id, idAluno ?: 0L).first()
            } catch (e: Exception) {
                emptyList()
            }
            val resumoEx = exs.joinToString("|") { "${it.nome}:${it.series}x${it.repeticoes}:${it.carga}:${it.observacoes}" }
            val hash = "${tr.id}${tr.nome}${tr.dataInicio}${tr.dataFim}${resumoEx.hashCode()}"
            val desc = if (exs.isEmpty()) {
                "Ficha disponível (Início: ${tr.dataInicio})"
            } else {
                "${exs.size} exercício(s) • Início: ${tr.dataInicio}"
            }
            lista.add(Triple(tr, hash, desc))
        }
        value = lista
    }
    val notifAnamnesesAluno = pendentes.map { anam ->
        Triple(
            "anam_${anam.id}${anam.status}",
            "Nova anamnese: ${anam.nomeModelo}",
            "Toque para preencher agora."
        )
    }
    val notifTreinosAluno = assinaturaTreinos.map { (tr, hash, desc) ->
        Triple(
            "treino$hash",
            "Treino / exercício atualizado: ${tr.nome}",
            desc
        )
    }
    val todasNotificacoesAluno = notifAnamnesesAluno + notifTreinosAluno
    val naoLidasAluno = todasNotificacoesAluno.filter { (chave, _, _) -> chave !in idsLidasAluno }
    LaunchedEffect(anamneseSelecionadaId, idAluno) {
        val id = anamneseSelecionadaId
        val idDoAluno = idAluno
        if (id != null && idDoAluno != null && idDoAluno > 0L) {
            carregando = true
            mensagemErro = ""
            mensagemSucesso = ""
            try {
                perguntas = anamneseRepository.buscarPerguntas(id)
                val respostasSalvas = respostaRepository.buscarRespostasDaAnamnese(id)
                respostas = respostasSalvas.associate { it.perguntaAtribuidaId to it.resposta }
            } catch (e: Exception) {
                mensagemErro = e.message ?: "Não foi possível carregar a anamnese."
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
            .background(corFundo)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
// CABEÇALHO LIMPO: Sino à esquerda | Logo ao centro | Tema (Sol/Lua) + Sair à direita
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(corCabecalhoTopo, corCabecalhoBase)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                IconButton(
                    onClick = { mostrarNotificacoesAluno = true }
                ) {
                    BadgedBox(
                        badge = {
                            if (naoLidasAluno.isNotEmpty()) {
                                Badge(
                                    containerColor = Color(0xFFD32F2F),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = naoLidasAluno.size.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notificações do aluno",
                            tint = corDestaque,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            LogoFitCarePadrao(
                modifier = Modifier.align(Alignment.Center),
                largura = 130.dp,
                altura = 58.dp
            )
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val novoTema = !temaEscuroAtivoState
                        temaEscuroAtivoState = novoTema
                        prefsSessao.edit().putBoolean("tema_escuro_ativo", novoTema).apply()
                    }
                ) {
                    Icon(
                        imageVector = if (temaEscuroAtivoState) {
                            Icons.Filled.LightMode
                        } else {
                            Icons.Filled.DarkMode
                        },
                        contentDescription = "Alternar tema",
                        tint = corDestaque,
                        modifier = Modifier.size(22.dp)
                    )
                }
                BotaoSairPadrao(
                    onSair = {
                        scope.launch {
                            aguardarSalvamentos()
                            onSair()
                        }
                    }
                )
            }
        }
        val primeiroNomeAluno = SessaoUsuario.usuarioAtual
            ?.nome
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.contains("@") }
            ?.substringBefore(" ")
            .orEmpty()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (primeiroNomeAluno.isNotBlank()) {
                    "ALUNO • ${primeiroNomeAluno.uppercase()}"
                } else {
                    "ALUNO"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = corTextoSecundario
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (primeiroNomeAluno.isNotBlank()) "Seja bem-vindo, $primeiroNomeAluno!" else "Seja bem-vindo!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = corTextoPrincipal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Acompanhe suas anamneses, treinos e documentos.",
                style = MaterialTheme.typography.bodySmall,
                color = corTextoSecundario
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 16.dp)
        ) {
            if (mensagemSucesso.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Text(
                        text = mensagemSucesso,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFF2E7D32),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (bloqueadoPorAnamneseObrigatoria) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Acesso bloqueado",
                            tint = Color(0xFFB17B00),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Preenchimento obrigatório: conclua sua anamnese pendente abaixo para liberar o acesso aos seus treinos e documentos.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6D4C00)
                        )
                    }
                }
            }
            if (idAluno == null || idAluno <= 0L) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = "Não foi possível identificar o cadastro do aluno. Entre em contato com o profissional.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFB71C1C)
                    )
                }
            } else if (anamneseSelecionadaId != null) {
                val modoSomenteLeitura = anamneseSelecionada?.status == "RESPONDIDA"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = {
                            scope.launch {
                                aguardarSalvamentos()
                                anamneseSelecionadaId = null
                                mensagemErro = ""
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = anamneseSelecionada?.nomeModelo ?: "Anamnese",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (carregando) {
                    CircularProgressIndicator()
                } else if (modoSomenteLeitura) {
                    perguntas.forEachIndexed { index, pergunta ->
                        val respostaExibida = formatarRespostaAluno(
                            tipoResposta = pergunta.tipoResposta,
                            respostaBruta = respostas[pergunta.id].orEmpty()
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = corCartao)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "${index + 1}. ${pergunta.texto}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = corTextoPrincipal
                                )
                                Text(
                                    text = respostaExibida.ifBlank { "Não respondida" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = corTextoSecundario
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            anamneseSelecionadaId = null
                            mensagemErro = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("VOLTAR")
                    }
                } else {
                    perguntas.forEach { pergunta ->
                        val respostaAtual = respostas[pergunta.id] ?: ""
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = corCartao)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                CampoRespostaAnamnese(
                                    pergunta = pergunta,
                                    valor = respostaAtual,
                                    corTextoPrincipal = corTextoPrincipal,
                                    onChange = { novaResposta ->
                                        respostas = respostas + (pergunta.id to novaResposta)
                                        mensagemErro = ""
                                        salvamentosPendentes[pergunta.id]?.cancel()
                                        salvamentosPendentes[pergunta.id] = scope.launch {
                                            delay(250)
                                            try {
                                                respostaRepository.salvarRespostaDoAluno(
                                                    alunoId = idAluno,
                                                    perguntaId = pergunta.id,
                                                    resposta = novaResposta
                                                )
                                            } catch (e: Exception) {
                                                mensagemErro =
                                                    e.message ?: "Erro ao salvar resposta."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val perguntasObrigatoriasPendentes = perguntas.count { pergunta ->
                        pergunta.obrigatoria && !respostaEstaPreenchida(
                            pergunta.tipoResposta,
                            respostas[pergunta.id]
                        )
                    }
                    if (perguntasObrigatoriasPendentes > 0) {
                        Text(
                            text = "Ainda faltam $perguntasObrigatoriasPendentes pergunta(s) obrigatória(s).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
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
                                    for (pergunta in perguntas) {
                                        respostaRepository.salvarRespostaDoAluno(
                                            alunoId = idAluno,
                                            perguntaId = pergunta.id,
                                            resposta = respostas[pergunta.id] ?: ""
                                        )
                                    }
                                    respostaRepository.concluirDoAluno(
                                        alunoId = idAluno,
                                        anamneseId = anamneseSelecionadaId!!
                                    )
                                    sincronizarAcompanhamentoAlunoComRespostas(
                                        alunoId = idAluno,
                                        alunoRepository = alunoRepository,
                                        perguntas = perguntas,
                                        respostas = respostas
                                    )
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val syncService =
                                                FirestoreSyncService(db.usuarioDao(), db)
                                            syncService.sincronizarAnamnesesAtribuidasDoAluno()
                                            syncService.sincronizarTreinosDoAluno()
                                        } catch (e: Exception) {
                                        }
                                    }
                                    AlunoSyncWorker.agendar(context)
                                    anamneseSelecionadaId = null
                                    perguntas = emptyList()
                                    respostas = emptyMap()
                                    mensagemSucesso =
                                        "Anamnese concluída! Seu acesso aos treinos e documentos está liberado."
                                } catch (e: Exception) {
                                    mensagemErro =
                                        e.message ?: "Não foi possível concluir a anamnese."
                                } finally {
                                    carregando = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !carregando && perguntas.isNotEmpty() && perguntasObrigatoriasPendentes == 0
                    ) {
                        Text("CONCLUIR ANAMNESE")
                    }
                }
            } else {
// LISTAGEM DE ANAMNESES
                LaunchedEffect(pendentes) {
                    if (pendentes.isNotEmpty()) {
                        val prefsAluno = context.getSharedPreferences(
                            "fitcare_notificacoes_aluno",
                            android.content.Context.MODE_PRIVATE
                        )
                        val jaAlertadas = prefsAluno
                            .getStringSet("anamneses_alertadas_ids", emptySet())
                            ?.toMutableSet()
                            ?: mutableSetOf()
                        var houveNova = false
                        for (anamnese in pendentes) {
                            val idStr = anamnese.id.toString()
                            if (idStr !in jaAlertadas) {
                                FitCareMessagingService.exibirNotificacao(
                                    context = context,
                                    titulo = "Nova anamnese disponível",
                                    mensagem = "Você recebeu a anamnese ${anamnese.nomeModelo} para preenchimento.",
                                    notificacaoId = (anamnese.id % Int.MAX_VALUE).toInt() + 10000
                                )
                                jaAlertadas.add(idStr)
                                houveNova = true
                            }
                        }
                        if (houveNova) {
                            prefsAluno.edit()
                                .putStringSet("anamneses_alertadas_ids", jaAlertadas)
                                .apply()
                        }
                    }
                }
                if (pendentes.isNotEmpty()) {
                    Text(
                        text = "Anamneses pendentes (Obrigatório)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pendentes.forEach { anamnese ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = corCartao)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = anamnese.nomeModelo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = corTextoPrincipal
                                )
                                Text(
                                    text = if (anamnese.status == "EM_ANDAMENTO") "Preenchimento iniciado" else "Aguardando preenchimento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = corTextoSecundario
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { anamneseSelecionadaId = anamnese.id },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (anamnese.status == "EM_ANDAMENTO") "CONTINUAR PREENCHIMENTO" else "PREENCHER ANAMNESE")
                                }
                            }
                        }
                    }
                }
// BLOQUEIA AS DEMAIS SEÇÕES ENQUANTO HOUVER ANAMNESE OBRIGATÓRIA PENDENTE
                if (!bloqueadoPorAnamneseObrigatoria) {
                    val respondidas = anamneses.filter { it.status == "RESPONDIDA" }
                    if (respondidas.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Anamneses concluídas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = corTextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        respondidas.forEach { anamnese ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = anamnese.nomeModelo,
                                        fontWeight = FontWeight.Bold,
                                        color = corTextoPrincipal
                                    )
                                    Text(
                                        text = "Concluída" + if (anamnese.dataResposta.isNotBlank()) " em ${anamnese.dataResposta}" else "",
                                        color = Color(0xFF2E7D32),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { anamneseSelecionadaId = anamnese.id },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("VISUALIZAR ANAMNESE")
                                    }
                                }
                            }
                        }
                    }
// SEÇÃO: MEUS TREINOS SINCRONIZADOS
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "MEUS TREINOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (treinoSelecionadoAluno != null) {
                        val tr = treinoSelecionadoAluno!!
                        val exerciciosDoTreino by treinoDao
                            .listarExerciciosDoAluno(tr.id, idAluno)
                            .collectAsState(initial = emptyList())
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(onVoltar = { treinoSelecionadoAluno = null })
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = corTextoPrincipal
                                )
                                Text(
                                    text = "Início: ${tr.dataInicio}" + if (tr.dataFim.isNotBlank()) " • Término: ${tr.dataFim}" else "",
                                    fontSize = 11.sp,
                                    color = corTextoSecundario
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        exerciciosDoTreino.sortedBy { it.ordem }.forEachIndexed { index, ex ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${index + 1}. ${ex.nome}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = corTextoPrincipal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { exercicioEditandoCarga = ex }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Atualizar carga e observações",
                                                tint = corDestaque
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${ex.series} séries × ${ex.repeticoes}",
                                        fontSize = 13.sp,
                                        color = corTextoPrincipal
                                    )
                                    Text(
                                        text = "Carga utilizada: ${ex.carga.ifBlank { "Não informada" }}",
                                        fontSize = 12.sp,
                                        color = corTextoSecundario
                                    )
                                    if (ex.observacoes.isNotBlank()) {
                                        Text(
                                            text = "Observações: ${ex.observacoes}",
                                            fontSize = 12.sp,
                                            color = corTextoSecundario
                                        )
                                    }
                                    if (ex.videoUrl.isNotBlank() && ex.videoTipo == "LINK") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(ex.videoUrl)
                                                    )
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        context,
                                                        "Não foi possível abrir o vídeo.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("▶ Assistir vídeo demonstrativo")
                                        }
                                    }
                                }
                            }
                        }
                        exercicioEditandoCarga?.let { exAlvo ->
                            var cargaNova by remember(exAlvo) { mutableStateOf(exAlvo.carga) }
                            var obsNova by remember(exAlvo) { mutableStateOf(exAlvo.observacoes) }
                            AlertDialog(
                                onDismissRequest = { exercicioEditandoCarga = null },
                                title = { Text("Registrar carga e execução") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(exAlvo.nome, fontWeight = FontWeight.Bold)
                                        OutlinedTextField(
                                            value = cargaNova,
                                            onValueChange = { cargaNova = it },
                                            label = { Text("Carga utilizada (ex.: 20 kg)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = obsNova,
                                            onValueChange = { obsNova = it },
                                            label = { Text("Observações de execução") },
                                            minLines = 2,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            val atualizado = exAlvo.copy(
                                                carga = cargaNova.trim(),
                                                observacoes = obsNova.trim()
                                            )
                                            treinoDao.atualizarExercicio(atualizado)
                                            val todosAtualizados = exerciciosDoTreino.map {
                                                if (it.id == atualizado.id) atualizado else it
                                            }
                                            FirestoreSyncService(db.usuarioDao(), db)
                                                .atualizarExercicioDoAlunoNoFirestore(
                                                    alunoId = idAluno,
                                                    treino = tr,
                                                    exerciciosAtualizados = todosAtualizados
                                                )
                                            withContext(Dispatchers.Main) {
                                                exercicioEditandoCarga = null
                                                Toast.makeText(
                                                    context,
                                                    "Carga e observação sincronizadas!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Text("Salvar")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { exercicioEditandoCarga = null }) {
                                        Text("Cancelar")
                                    }
                                }
                            )
                        }
                    } else if (treinosAluno.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = corCartao)
                        ) {
                            Text(
                                text = "Nenhum treino prescrito disponível no momento.",
                                modifier = Modifier.padding(14.dp),
                                color = corTextoSecundario,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        treinosAluno.forEach { treino ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable { treinoSelecionadoAluno = treino },
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = treino.nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = corTextoPrincipal
                                    )
                                    Text(
                                        text = "Início: ${treino.dataInicio}" + if (treino.dataFim.isNotBlank()) " • Término: ${treino.dataFim}" else "",
                                        fontSize = 12.sp,
                                        color = corTextoSecundario
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Toque para ver exercícios, vídeos e registrar cargas ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = corDestaque
                                    )
                                }
                            }
                        }
                    }
// DOCUMENTOS E IMAGENS DO ALUNO
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "MEUS DOCUMENTOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Importe exames, laudos e imagens para comparação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = corTextoSecundario
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    BotaoImportarDocumentoAnamnese(
                        alunoId = idAluno,
                        anamneseRepository = documentoRepository,
                        alunoRepository = alunoRepository,
                        onDocumentoImportado = { mensagemDocumento = "" }
                    )
                    if (mensagemDocumento.isNotBlank()) {
                        Text(
                            text = mensagemDocumento,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (documentos.isEmpty()) {
                        Text(
                            text = "Nenhum documento importado.",
                            color = corTextoSecundario,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        documentos.forEach { documento ->
                            val ehComparacao = documento.tipoArquivo.startsWith(
                                "comparacao_",
                                ignoreCase = true
                            ) ||
                                    documento.nomeArquivo.startsWith(
                                        "comparacao_",
                                        ignoreCase = true
                                    )
                            val nomeExibido = documento.nomeArquivo.removePrefix("comparacao_")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 12.dp,
                                            end = 6.dp,
                                            top = 10.dp,
                                            bottom = 10.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = nomeExibido,
                                            fontWeight = FontWeight.SemiBold,
                                            color = corTextoPrincipal
                                        )
                                        Text(
                                            text = if (ehComparacao) "Imagem para comparação" else "Documento médico — ${
                                                documento.tipoArquivo.uppercase(
                                                    Locale.ROOT
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = corTextoSecundario
                                        )
                                        TextButton(
                                            onClick = {
                                                try {
                                                    abrirDocumentoAnamnese(context, documento)
                                                    mensagemErro = ""
                                                } catch (erro: Exception) {
                                                    mensagemErro = erro.message
                                                        ?: "Não foi possível abrir o documento."
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("ABRIR DOCUMENTO")
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                try {
                                                    documentoRepository.excluirDocumento(documento.id)
                                                    File(documento.caminhoArquivo).delete()
                                                    mensagemErro = ""
                                                } catch (erro: Exception) {
                                                    mensagemErro = erro.message
                                                        ?: "Não foi possível excluir o documento."
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir documento",
                                            tint = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (mensagemErro.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensagemErro,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
// Modal do Sininho de Notificações do Aluno
    if (mostrarNotificacoesAluno) {
        AlertDialog(
            onDismissRequest = { mostrarNotificacoesAluno = false },
            title = {
                Text(
                    text = "Notificações",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )
            },
            text = {
                if (todasNotificacoesAluno.isEmpty()) {
                    Text(
                        text = "Nenhuma notificação no momento.",
                        fontSize = 13.sp,
                        color = corTextoSecundario
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        todasNotificacoesAluno.forEach { (chave, titulo, subtitulo) ->
                            val jaLida = chave in idsLidasAluno
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        marcarNotifAlunoComoLida(chave)
                                        mostrarNotificacoesAluno = false
                                        if (chave.startsWith("anam_")) {
                                            val idAnam = chave.removePrefix("anam_").substringBefore("").toLongOrNull()
                                            if (idAnam != null) anamneseSelecionadaId = idAnam
                                        } else if (chave.startsWith("treino") && !bloqueadoPorAnamneseObrigatoria) {
                                            val idTr = chave.removePrefix("treino_").substringBefore("_").toLongOrNull()
                                            val trAlvo = treinosAluno.firstOrNull { it.id == idTr }
                                            if (trAlvo != null) treinoSelecionadoAluno = trAlvo
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (jaLida) corCartao else corDestaque.copy(alpha = 0.14f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = titulo + if (!jaLida) " (Nova)" else "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = corTextoPrincipal
                                    )
                                    Text(
                                        text = subtitulo,
                                        fontSize = 12.sp,
                                        color = corTextoSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarNotificacoesAluno = false }) {
                    Text("Fechar", color = corDestaque)
                }
            }
        )
    }
}
@Composable
private fun CampoRespostaAnamnese(
    pergunta: PerguntaAnamneseAtribuidaEntity,
    valor: String,
    corTextoPrincipal: Color = Color(0xFF002B5C),
    onChange: (String) -> Unit
) {
    val alternativas = remember(pergunta.opcoes) {
        try {
            val json = JSONArray(pergunta.opcoes)
            (0 until json.length()).map { json.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = pergunta.texto,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = corTextoPrincipal
        )
        if (pergunta.obrigatoria) {
            Text(
                text = "Obrigatória",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        when (pergunta.tipoResposta) {
            "TEXTO" -> {
                OutlinedTextField(
                    value = valor,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sua resposta") },
                    minLines = 2
                )
            }
            "NUMERO" -> {
                OutlinedTextField(
                    value = valor,
                    onValueChange = { novo ->
                        if (novo.matches(Regex("0-9?"))) {
                            onChange(novo)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Informe um número") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            "DATA" -> {
                OutlinedTextField(
                    value = valor,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Data") },
                    placeholder = { Text("DD/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
            "SIM_NAO" -> {
                Row(modifier = Modifier.selectableGroup()) {
                    listOf("Sim", "Não").forEach { opcao ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = valor == opcao,
                                    onClick = { onChange(opcao) },
                                    role = Role.RadioButton
                                )
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = valor == opcao, onClick = null)
                            Text(text = opcao, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            "SIM_NAO_CHECKBOX" -> {
                val respondeuNao = valor == "Não"
                val selecionadas = remember(valor) {
                    if (valor.startsWith("[")) {
                        try {
                            val json = JSONArray(valor)
                            (0 until json.length()).map { json.getString(it) }.toSet()
                        } catch (e: Exception) {
                            emptySet()
                        }
                    } else {
                        emptySet()
                    }
                }
                val respondeuSim = valor == "SIM_ABERTO" || selecionadas.isNotEmpty()
                Row(modifier = Modifier.selectableGroup()) {
                    listOf("Sim", "Não").forEach { opcao ->
                        val sel = if (opcao == "Sim") respondeuSim else respondeuNao
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = sel,
                                    onClick = {
                                        if (opcao == "Não") {
                                            onChange("Não")
                                        } else if (!respondeuSim) {
                                            onChange("SIM_ABERTO")
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = sel, onClick = null)
                            Text(text = opcao, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (respondeuSim) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Selecione todas as opções que se aplicam:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = corTextoPrincipal
                    )
                    alternativas.forEach { opcao ->
                        val marcada = opcao in selecionadas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val novas = selecionadas.toMutableSet()
                                    if (opcao in novas) novas.remove(opcao) else novas.add(opcao)
                                    if (novas.isEmpty()) {
                                        onChange("SIM_ABERTO")
                                    } else {
                                        val arr = JSONArray()
                                        novas.forEach { arr.put(it) }
                                        onChange(arr.toString())
                                    }
                                }
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = marcada,
                                onCheckedChange = { checked ->
                                    val novas = selecionadas.toMutableSet()
                                    if (checked) novas.add(opcao) else novas.remove(opcao)
                                    if (novas.isEmpty()) {
                                        onChange("SIM_ABERTO")
                                    } else {
                                        val arr = JSONArray()
                                        novas.forEach { arr.put(it) }
                                        onChange(arr.toString())
                                    }
                                }
                            )
                            Text(text = opcao, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            "MULTIPLA_ESCOLHA" -> {
                val outroSelecionado =
                    ehOpcaoOutro(valor) || valor.startsWith("Outro:", ignoreCase = true)
                val textoOutroAtual = if (valor.startsWith("Outro:", ignoreCase = true)) {
                    valor.substringAfter(":").trimStart()
                } else {
                    ""
                }
                Column(modifier = Modifier.selectableGroup()) {
                    alternativas.forEach { opcao ->
                        val ehEstaOutro = ehOpcaoOutro(opcao)
                        val selecionada = if (ehEstaOutro) outroSelecionado else valor == opcao
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selecionada,
                                    onClick = {
                                        if (ehEstaOutro) {
                                            if (!outroSelecionado) onChange("Outro:")
                                        } else {
                                            onChange(opcao)
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selecionada, onClick = null)
                            Text(text = opcao, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (outroSelecionado) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textoOutroAtual,
                            onValueChange = { digitado -> onChange("Outro: $digitado") },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Descreva qual") },
                            minLines = 2
                        )
                    }
                }
            }
            "CHECKBOX" -> {
                val selecionadas = remember(valor) {
                    try {
                        val json = JSONArray(valor)
                        (0 until json.length()).map { idx -> json.getString(idx) }.toSet()
                    } catch (e: Exception) {
                        emptySet<String>()
                    }
                }
                val itemOutroAtual = selecionadas.firstOrNull { item ->
                    ehOpcaoOutro(item) || item.startsWith("Outro:", ignoreCase = true)
                }
                val outroMarcado = itemOutroAtual != null
                val textoOutroAtual = if (itemOutroAtual != null && itemOutroAtual.startsWith(
                        "Outro:",
                        ignoreCase = true
                    )
                ) {
                    itemOutroAtual.substringAfter(":").trimStart()
                } else {
                    ""
                }

                fun salvarConjunto(novas: Set<String>) {
                    if (novas.isEmpty()) {
                        onChange("")
                    } else {
                        val json = JSONArray()
                        novas.forEach { json.put(it) }
                        onChange(json.toString())
                    }
                }
                alternativas.forEach { opcao ->
                    val ehEstaOutro = ehOpcaoOutro(opcao)
                    val marcada = if (ehEstaOutro) outroMarcado else opcao in selecionadas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val novas = selecionadas.toMutableSet()
                                if (ehEstaOutro) {
                                    if (outroMarcado) {
                                        novas.removeAll {
                                            ehOpcaoOutro(it) || it.startsWith(
                                                "Outro:",
                                                ignoreCase = true
                                            )
                                        }
                                    } else {
                                        novas.add("Outro:")
                                    }
                                } else {
                                    if (opcao in novas) novas.remove(opcao) else novas.add(opcao)
                                }
                                salvarConjunto(novas)
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = marcada,
                            onCheckedChange = { checked ->
                                val novas = selecionadas.toMutableSet()
                                if (ehEstaOutro) {
                                    if (checked) {
                                        if (!outroMarcado) novas.add("Outro:")
                                    } else {
                                        novas.removeAll {
                                            ehOpcaoOutro(it) || it.startsWith(
                                                "Outro:",
                                                ignoreCase = true
                                            )
                                        }
                                    }
                                } else {
                                    if (checked) novas.add(opcao) else novas.remove(opcao)
                                }
                                salvarConjunto(novas)
                            }
                        )
                        Text(text = opcao, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (outroMarcado) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = textoOutroAtual,
                        onValueChange = { digitado ->
                            val novas = selecionadas.filterNot {
                                ehOpcaoOutro(it) || it.startsWith("Outro:", ignoreCase = true)
                            }.toMutableSet()
                            novas.add("Outro: $digitado")
                            salvarConjunto(novas)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descreva qual") },
                        minLines = 2
                    )
                }
            }
            else -> {
                OutlinedTextField(
                    value = valor,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sua resposta") }
                )
            }
        }
    }
}