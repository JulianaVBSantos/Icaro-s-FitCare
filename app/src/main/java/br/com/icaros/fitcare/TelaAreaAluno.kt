package br.com.icaros.fitcare

import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.tasks.await
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    if (resposta.equals("Sim:", ignoreCase = true)) return false
    val itens = extrairItensResposta(tipoResposta, resposta)
    if (itens.isEmpty()) return false
    if (itens.any { ehOpcaoOutro(it) }) return false
    if (itens.any { it.equals("Outro:", ignoreCase = true) }) return false
    return true
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
                when {
                    item.startsWith("Outro:", ignoreCase = true) -> item.substringAfter(":").trim()
                    item.startsWith("Detalhes:", ignoreCase = true) -> item.substringAfter(":")
                        .trim()

                    else -> item
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
                if (!respostaBruta.equals("Não", ignoreCase = true)) {
                    condicoesExtraidas.addAll(itens)
                }
            }

            tituloLower.contains("restriç") ||
                    tituloLower.contains("restric") ||
                    tituloLower.contains("limitaç") ||
                    tituloLower.contains("limitac") -> {
                atualizouRestricoes = true
                if (!respostaBruta.equals("Não", ignoreCase = true)) {
                    restricoesExtraidas.addAll(itens)
                }
            }
        }
    }

    val novasCondicoes =
        if (atualizouCondicoes) condicoesExtraidas.joinToString(", ") else aluno.condicoes
    val novasRestricoes =
        if (atualizouRestricoes) restricoesExtraidas.joinToString(", ") else aluno.restricoes
    val totalItens = novasCondicoes.split(",").count { it.trim().isNotBlank() } +
            novasRestricoes.split(",").count { it.trim().isNotBlank() }

    val novoStatus = when {
        totalItens == 0 -> "Normal"
        totalItens == 1 -> "Atenção"
        else -> "Prioridade"
    }
    var novoObjetivo = aluno.objetivo
    for (pergunta in perguntas) {
        val tituloLower = pergunta.texto.lowercase(Locale.ROOT)
        val respostaBruta = respostas[pergunta.id].orEmpty().trim()
        if (respostaBruta.isBlank()) continue
        if (tituloLower.contains("objetivo")) {
            val objExtraido = formatarRespostaAluno(pergunta.tipoResposta, respostaBruta)
                .removePrefix("Outro:")
                .trim()
            if (objExtraido.isNotBlank()) {
                novoObjetivo = objExtraido
            }
        }
    }

    val alunoAtualizado = aluno.copy(
        objetivo = novoObjetivo,
        condicoes = novasCondicoes,
        restricoes = novasRestricoes,
        status = novoStatus
    )
    alunoRepository.atualizarDireto(alunoAtualizado)

    if (!aluno.firebaseId.isNullOrBlank()) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("alunos")
                .document(aluno.firebaseId)
                .set(
                    mapOf(
                        "objetivo" to novoObjetivo,
                        "condicoes" to novasCondicoes,
                        "restricoes" to novasRestricoes
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                .await()
        } catch (_: Exception) {
        }
    }

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
    val temaEscuro =
        temaEscuroAtivoState || prefsSessao.getBoolean("tema_escuro_ativo", isSystemInDarkTheme())

    val corFundo = if (temaEscuro) Color(0xFF0F1722) else Color(0xFFEAF3F9)
    val corCabecalhoTopo = if (temaEscuro) Color(0xFF141F2E) else Color(0xFFDCEEFF)
    val corCabecalhoBase = if (temaEscuro) Color(0xFF0F1722) else Color(0xFFEAF3F9)
    val corCartao = if (temaEscuro) Color(0xFF172331) else Color.White
    val corCardSecundario = if (temaEscuro) Color(0xFF1E2D3D) else Color(0xFFF0F6FC)
    val corTextoPrincipal = if (temaEscuro) Color(0xFFEAF2FA) else Color(0xFF002B5C)
    val corTextoSecundario = if (temaEscuro) Color(0xFFA2B5C8) else Color(0xFF526579)
    val corDestaque = if (temaEscuro) Color(0xFF7AB8F5) else Color(0xFF002B5C)

    val idAluno = alunoId ?: SessaoUsuario.usuarioAtual?.alunoId
    val db = remember { FitCareDatabase.getDatabase(context) }
    val treinoDao = remember { db.treinoDao() }
    val periodizacaoDao = remember { db.periodizacaoDao() }

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

    // Fazer novas anamneses atribuídas aparecerem instantaneamente para o aluno
    DisposableEffect(idAluno) {
        var listener: com.google.firebase.firestore.ListenerRegistration? = null
        if (idAluno != null && idAluno > 0L) {
            scope.launch(Dispatchers.IO) {
                val alunoEnt = db.alunoDao().buscarPorIdDireto(idAluno)
                val fbId = alunoEnt?.firebaseId
                if (!fbId.isNullOrBlank()) {
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    withContext(Dispatchers.Main) {
                        listener = syncService.iniciarMonitoramentoTempoRealAluno(scope, fbId)
                    }
                }
            }
        }
        onDispose {
            listener?.remove()
        }
    }

    val anamnesesFlow = remember(idAluno) {
        if (idAluno != null && idAluno > 0L) anamneseRepository.listarPorAluno(idAluno)
        else flowOf(emptyList())
    }
    val anamneses by anamnesesFlow.collectAsState(initial = emptyList())

    val treinosFlow = remember(idAluno) {
        if (idAluno != null && idAluno > 0L) treinoDao.listarTreinosDoAluno(idAluno)
        else flowOf(emptyList())
    }
    val treinosAluno by treinosFlow.collectAsState(initial = emptyList())

    val macrociclosFlow = remember(idAluno) {
        flowOf(emptyList<MacrocicloEntity>())
    }

    val macrociclosAluno by macrociclosFlow.collectAsState(initial = emptyList())

    val documentosFlow = remember(idAluno, documentoRepository) {
        if (idAluno != null && idAluno > 0L) documentoRepository.listarDocumentos(idAluno)
        else flowOf(emptyList())
    }
    val documentos by documentosFlow.collectAsState(initial = emptyList())

    var abaAtualAluno by remember(idAluno) { mutableStateOf("DASHBOARD") }
    var anamneseSelecionadaId by remember(idAluno) { mutableStateOf<Long?>(null) }
    var treinoSelecionadoAluno by remember(idAluno) { mutableStateOf<TreinoEntity?>(null) }
    var treinoEmExecucao by remember(idAluno) { mutableStateOf(false) }
    val exerciciosConcluidosSessao = remember { mutableStateMapOf<Long, Boolean>() }
    var exercicioEditandoCarga by remember(idAluno) { mutableStateOf<ExercicioEntity?>(null) }
    var perguntas by remember { mutableStateOf<List<PerguntaAnamneseAtribuidaEntity>>(emptyList()) }
    var respostas by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf("") }
    var mensagemSucesso by remember { mutableStateOf("") }
    var mensagemDocumento by remember { mutableStateOf("") }
    val salvamentosPendentes = remember { mutableMapOf<Long, Job>() }
    val anamneseSelecionada = anamneses.firstOrNull { it.id == anamneseSelecionadaId }
    val pendentes = anamneses
        .filter { it.status == "PENDENTE" || it.status == "EM_ANDAMENTO" }
        .distinctBy { anam ->
            if (anam.identificadorExterno == "PADRAO_OBRIGATORIA") "PADRAO_OBRIGATORIA"
            else anam.id.toString()
        }
    val bloqueadoPorAnamneseObrigatoria = pendentes.isNotEmpty()
    val prefsFreqAluno = remember(idAluno) {
        context.getSharedPreferences(
            "fitcare_freq_aluno_${idAluno ?: 0L}",
            android.content.Context.MODE_PRIVATE
        )
    }
    var datasTreinadas by remember(idAluno) {
        mutableStateOf(
            prefsFreqAluno.getStringSet("datas_treinadas", emptySet<String>())?.toSet()
                ?: emptySet<String>()
        )
    }

    fun registrarTreinoConcluidoHoje(nomeTreino: String = "Treino") {
        val hojeStr = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
        val agoraMs = System.currentTimeMillis()
        val atualizado = datasTreinadas + hojeStr
        datasTreinadas = atualizado
        prefsFreqAluno.edit().putStringSet("datas_treinadas", atualizado).apply()

        scope.launch(Dispatchers.IO) {
            try {
                val alunoEnt =
                    db.alunoDao().buscarPorIdDireto(idAluno ?: return@launch) ?: return@launch
                val fbId = alunoEnt.firebaseId ?: return@launch
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("alunos")
                    .document(fbId)
                    .collection("eventos_aluno")
                    .document(agoraMs.toString())
                    .set(
                        mapOf(
                            "id" to agoraMs,
                            "tipo" to "Treino",
                            "data" to hojeStr,
                            "titulo" to "Treino concluído: $nomeTreino",
                            "descricao" to "Treino finalizado pelo aluno na Área do Aluno."
                        )
                    )
                    .await()
            } catch (e: Exception) {
            }
        }
    }

    suspend fun aguardarSalvamentos() {
        salvamentosPendentes.values.toList().forEach { it.join() }
        salvamentosPendentes.clear()
    }

    var mostrarNotificacoesAluno by remember { mutableStateOf(false) }
    val prefsNotifAluno = remember {
        context.getSharedPreferences(
            "fitcare_notificacoes_aluno_ui",
            android.content.Context.MODE_PRIVATE
        )
    }
    var idsLidasAluno by remember {
        mutableStateOf(
            prefsNotifAluno.getStringSet("lidas_ids", emptySet<String>())?.toSet()
                ?: emptySet<String>()
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
            val resumoEx =
                exs.joinToString("|") { "${it.nome}:${it.series}x${it.repeticoes}:${it.carga}:${it.observacoes}" }
            val hash = "${tr.id}${tr.nome}${tr.dataInicio}${tr.dataFim}${resumoEx.hashCode()}"
            val desc = if (exs.isEmpty()) "Ficha disponível (Início: ${tr.dataInicio})"
            else "${exs.size} exercício(s) • Início: ${tr.dataInicio}"
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
        Triple("treino$hash", "Treino / exercício atualizado: ${tr.nome}", desc)
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
                    .distinctBy { it.texto.trim().lowercase() }
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
        // CABEÇALHO: Sino à esquerda | Logo ao centro | Tema (Sol/Lua) + Sair à direita
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
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                IconButton(onClick = { mostrarNotificacoesAluno = true }) {
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
                        imageVector = if (temaEscuroAtivoState) Icons.Filled.LightMode else Icons.Filled.DarkMode,
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
                text = if (primeiroNomeAluno.isNotBlank()) "ALUNO • ${primeiroNomeAluno.uppercase()}" else "ALUNO",
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
                text = "Acompanhe seus treinos, evolução de cargas e avaliações.",
                style = MaterialTheme.typography.bodySmall,
                color = corTextoSecundario
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    CircularProgressIndicator(color = corDestaque)
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
                                Spacer(modifier = Modifier.height(4.dp))
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
                                    val todasDoRoom = anamneseRepository.buscarPerguntas(anamneseSelecionadaId!!)
                                    val respostasPorTexto = perguntas.associate { p ->
                                        p.texto.trim().lowercase() to (respostas[p.id] ?: "")
                                    }
                                    for (pRoom in todasDoRoom) {
                                        val respVal = respostas[pRoom.id]
                                            ?: respostasPorTexto[pRoom.texto.trim().lowercase()]
                                            ?: ""
                                        respostaRepository.salvarRespostaDoAluno(
                                            alunoId = idAluno,
                                            perguntaId = pRoom.id,
                                            resposta = respVal
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
                                            val syncService = FirestoreSyncService(db.usuarioDao(), db)
                                            syncService.sincronizarAnamnesesAtribuidasDoAluno()
                                            syncService.sincronizarTreinosDoAluno()
                                        } catch (_: Exception) {
                                        }
                                    }

                                    AlunoSyncWorker.agendar(context)
                                    anamneseSelecionadaId = null
                                    perguntas = emptyList()
                                    respostas = emptyMap()
                                    abaAtualAluno = "DASHBOARD"
                                    mensagemSucesso =
                                        "Anamnese concluída! Seu acesso está liberado."
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
            } else if (bloqueadoPorAnamneseObrigatoria) {
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
                            text = "Preenchimento obrigatório: conclua sua anamnese pendente abaixo para liberar o acesso ao seu dashboard de treinos, evolução e documentos.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6D4C00)
                        )
                    }
                }
                pendentes.forEach { anamnese ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = corCartao)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = anamnese.nomeModelo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (anamnese.status == "EM_ANDAMENTO") "Preenchimento iniciado" else "Aguardando preenchimento inicial",
                                style = MaterialTheme.typography.bodySmall,
                                color = corTextoSecundario
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { anamneseSelecionadaId = anamnese.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (anamnese.status == "EM_ANDAMENTO") "CONTINUAR PREENCHIMENTO" else "PREENCHER ANAMNESE")
                            }
                        }
                    }
                }
            } else {
                when (abaAtualAluno) {
                    "DASHBOARD" -> {
                        CartaoFrequenciaSemanalAluno(
                            datasTreinadas = datasTreinadas,
                            corCartao = corCartao,
                            corTextoPrincipal = corTextoPrincipal,
                            corTextoSecundario = corTextoSecundario,
                            corDestaque = corDestaque
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Acesso rápido",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = corTextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CartaoMenuAluno(
                                modifier = Modifier.weight(1f),
                                titulo = "Meus Treinos",
                                subtitulo = "${treinosAluno.size} ficha(s) ativa(s)",
                                icone = Icons.Filled.FitnessCenter,
                                corFundo = corCartao,
                                corDestaque = corDestaque,
                                corTextoPrincipal = corTextoPrincipal,
                                corTextoSecundario = corTextoSecundario,
                                onClick = {
                                    treinoSelecionadoAluno = null
                                    treinoEmExecucao = false
                                    abaAtualAluno = "TREINOS"
                                }
                            )
                            CartaoMenuAluno(
                                modifier = Modifier.weight(1f),
                                titulo = "Anamneses",
                                subtitulo = "${anamneses.size} avaliação(ões)",
                                icone = Icons.Filled.Assignment,
                                corFundo = corCartao,
                                corDestaque = Color(0xFF36844A),
                                corTextoPrincipal = corTextoPrincipal,
                                corTextoSecundario = corTextoSecundario,
                                onClick = { abaAtualAluno = "ANAMNESES" }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CartaoMenuAluno(
                                modifier = Modifier.weight(1f),
                                titulo = "Progresso de Cargas",
                                subtitulo = "Evolução por exercício",
                                icone = Icons.Filled.ShowChart,
                                corFundo = corCartao,
                                corDestaque = Color(0xFFE5A50A),
                                corTextoPrincipal = corTextoPrincipal,
                                corTextoSecundario = corTextoSecundario,
                                onClick = { abaAtualAluno = "PROGRESSO" }
                            )
                            CartaoMenuAluno(
                                modifier = Modifier.weight(1f),
                                titulo = "Documentos",
                                subtitulo = "${documentos.size} arquivo(s) enviado(s)",
                                icone = Icons.Filled.FolderOpen,
                                corFundo = corCartao,
                                corDestaque = Color(0xFF5B9BD5),
                                corTextoPrincipal = corTextoPrincipal,
                                corTextoSecundario = corTextoSecundario,
                                onClick = { abaAtualAluno = "DOCUMENTOS" }
                            )
                        }
                        if (macrociclosAluno.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            CartaoMenuAluno(
                                modifier = Modifier.fillMaxWidth(),
                                titulo = "Planejamento e Periodização",
                                subtitulo = "Acompanhe suas fases e semanas de treino",
                                icone = Icons.Filled.CalendarMonth,
                                corFundo = corCartao,
                                corDestaque = corDestaque,
                                corTextoPrincipal = corTextoPrincipal,
                                corTextoSecundario = corTextoSecundario,
                                onClick = { abaAtualAluno = "PERIODIZACAO" }
                            )
                        }
                    }

                    "TREINOS" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(
                                onVoltar = {
                                    if (treinoSelecionadoAluno != null) {
                                        treinoSelecionadoAluno = null
                                        treinoEmExecucao = false
                                    } else {
                                        abaAtualAluno = "DASHBOARD"
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = treinoSelecionadoAluno?.nome ?: "Meus Treinos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        if (treinoSelecionadoAluno != null) {
                            val tr = treinoSelecionadoAluno!!
                            val exerciciosDoTreino by treinoDao
                                .listarExerciciosDoAluno(tr.id, idAluno)
                                .collectAsState(initial = emptyList())

                            var mostrarDialogoDorAluno by remember { mutableStateOf(false) }

                            Button(
                                onClick = {
                                    if (!treinoEmExecucao) {
                                        treinoEmExecucao = true
                                        exerciciosConcluidosSessao.clear()
                                    } else {
                                        registrarTreinoConcluidoHoje(tr.nome)
                                        treinoEmExecucao = false
                                        Toast.makeText(
                                            context,
                                            "Treino concluído e enviado ao professor!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (treinoEmExecucao) Color(0xFF36844A) else Color(
                                        0xFF002B5C
                                    ),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = if (treinoEmExecucao) "✓ FINALIZAR TREINO DE HOJE" else "▶ INICIAR TREINO",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (treinoEmExecucao) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = { mostrarDialogoDorAluno = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("+ Novo registro", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Opcional: utilize este botão caso sinta alguma dor ou desconforto durante a execução deste treino.",
                                    fontSize = 11.sp,
                                    color = corTextoSecundario
                                )
                            }

                            if (mostrarDialogoDorAluno) {
                                var dataDor by remember {
                                    mutableStateOf(
                                        SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale("pt", "BR")
                                        ).format(Date())
                                    )
                                }
                                var tituloDor by remember { mutableStateOf("") }
                                var descDor by remember { mutableStateOf("") }
                                var intensidadeDor by remember { mutableStateOf("") }
                                var localDor by remember { mutableStateOf("") }
                                var momentoDor by remember { mutableStateOf("") }
                                var relacaoDor by remember { mutableStateOf("Durante o treino: ${tr.nome}") }
                                var limitacoesDor by remember { mutableStateOf("") }
                                var erroDor by remember { mutableStateOf("") }

                                AlertDialog(
                                    onDismissRequest = { mostrarDialogoDorAluno = false },
                                    title = { Text("Novo registro • Dor / desconforto") },
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 420.dp)
                                                .verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = dataDor,
                                                onValueChange = { dataDor = it },
                                                label = { Text("Data") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = tituloDor,
                                                onValueChange = { tituloDor = it },
                                                label = { Text("Título *") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = descDor,
                                                onValueChange = { descDor = it },
                                                label = { Text("Descrição / observação") },
                                                minLines = 2,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = intensidadeDor,
                                                onValueChange = {
                                                    intensidadeDor =
                                                        it.filter(Char::isDigit).take(2)
                                                },
                                                label = { Text("Intensidade (0 a 10) *") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = localDor,
                                                onValueChange = { localDor = it },
                                                label = { Text("Localização da dor") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = momentoDor,
                                                onValueChange = { momentoDor = it },
                                                label = { Text("Momento da ocorrência") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = relacaoDor,
                                                onValueChange = { relacaoDor = it },
                                                label = { Text("Relação com o treinamento") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = limitacoesDor,
                                                onValueChange = { limitacoesDor = it },
                                                label = { Text("Limitações") },
                                                minLines = 2,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (erroDor.isNotBlank()) {
                                                Text(
                                                    erroDor,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            val intVal = intensidadeDor.toIntOrNull()
                                            if (tituloDor.isBlank()) {
                                                erroDor = "Informe um título."
                                                return@TextButton
                                            }
                                            if (intVal == null || intVal !in 0..10) {
                                                erroDor = "Intensidade deve ser de 0 a 10."
                                                return@TextButton
                                            }
                                            val evId = System.currentTimeMillis()
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val alunoEnt =
                                                        db.alunoDao().buscarPorIdDireto(idAluno)
                                                            ?: return@launch
                                                    val fbId = alunoEnt.firebaseId ?: return@launch
                                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                        .collection("alunos")
                                                        .document(fbId)
                                                        .collection("eventos_aluno")
                                                        .document(evId.toString())
                                                        .set(
                                                            mapOf(
                                                                "id" to evId,
                                                                "tipo" to "Dor / desconforto",
                                                                "data" to dataDor.trim(),
                                                                "titulo" to tituloDor.trim(),
                                                                "descricao" to descDor.trim(),
                                                                "intensidade" to intVal,
                                                                "localizacao" to localDor.trim(),
                                                                "momentoOcorrencia" to momentoDor.trim(),
                                                                "relacaoTreinamento" to relacaoDor.trim(),
                                                                "limitacoes" to limitacoesDor.trim()
                                                            )
                                                        )
                                                        .await()
                                                    withContext(Dispatchers.Main) {
                                                        mostrarDialogoDorAluno = false
                                                        Toast.makeText(
                                                            context,
                                                            "Registro de dor enviado ao professor!",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } catch (e: Exception) {
                                                }
                                            }
                                        }) { Text("Salvar registro") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            mostrarDialogoDorAluno = false
                                        }) { Text("Cancelar") }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            var videoEmReproducao by remember { mutableStateOf<Triple<String, String, String>?>(null) }
                            exerciciosDoTreino.sortedBy { it.ordem }.forEachIndexed { index, ex ->
                                val concluido = exerciciosConcluidosSessao[ex.id] == true
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (concluido) Color(0xFF36844A).copy(alpha = 0.14f) else corCartao
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (treinoEmExecucao) {
                                                    Checkbox(
                                                        checked = concluido,
                                                        onCheckedChange = {
                                                            exerciciosConcluidosSessao[ex.id] = it
                                                        }
                                                    )
                                                }
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
                                                text = "Séries: ${ex.series} × ${ex.repeticoes}",
                                                fontSize = 13.sp,
                                                color = corTextoPrincipal
                                            )
                                            Text(
                                                text = "Carga: ${ex.carga.ifBlank { "Não informada" }}",
                                                fontSize = 12.sp,
                                                color = corTextoSecundario
                                            )
                                            if (ex.observacoes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Instruções: ${ex.observacoes}",
                                                    fontSize = 12.sp,
                                                    color = corTextoSecundario
                                                )
                                            }
                                        }
                                        if (ex.videoUrl.isNotBlank()) {
                                            val tipoVid = ex.videoTipo.ifBlank { if (ex.videoUrl.startsWith("/")) "LOCAL" else "LINK" }
                                            CardPreviewVideoExercicio(
                                                tituloExercicio = ex.nome,
                                                videoUrl = ex.videoUrl,
                                                videoTipo = tipoVid,
                                                modifier = Modifier
                                                    .width(102.dp)
                                                    .height(138.dp),
                                                onAbrirPlayer = {
                                                    videoEmReproducao = Triple(ex.nome, ex.videoUrl, tipoVid)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            videoEmReproducao?.let { (titulo, url, tipo) ->
                                DialogoPlayerVideoExercicio(
                                    tituloExercicio = titulo,
                                    videoUrl = url,
                                    videoTipo = tipo,
                                    onFechar = { videoEmReproducao = null }
                                )
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
                                                val prefsHist = context.getSharedPreferences(
                                                    "fitcare_hist_cargas_$idAluno",
                                                    android.content.Context.MODE_PRIVATE
                                                )
                                                val hojeHist = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
                                                val chaveEx = atualizado.nome.trim()
                                                val atualHist = prefsHist.getString(chaveEx, "").orEmpty()
                                                val entradas = atualHist.split("|").filter { it.isNotBlank() }.toMutableList()
                                                val novaEntrada = "$hojeHist:${atualizado.carga.trim()}"
                                                if (entradas.isEmpty() && exAlvo.carga.isNotBlank() && exAlvo.carga.trim() != atualizado.carga.trim()) { entradas.add("$hojeHist:${exAlvo.carga.trim()}") }
                                                if (entradas.lastOrNull() != novaEntrada) {
                                                    entradas.add(novaEntrada)
                                                }
                                                prefsHist.edit().putString(chaveEx, entradas.joinToString("|")).apply()

                                                withContext(Dispatchers.Main) {
                                                    exercicioEditandoCarga = null
                                                    Toast.makeText(
                                                        context,
                                                        "Carga e observação sincronizadas!",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }) { Text("Salvar") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            exercicioEditandoCarga = null
                                        }) { Text("Cancelar") }
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
                                    modifier = Modifier.padding(16.dp),
                                    color = corTextoSecundario,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            treinosAluno.forEach { treino ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp)
                                        .clickable { treinoSelecionadoAluno = treino },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = corCartao)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = treino.nome,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = corTextoPrincipal
                                        )
                                        Text(
                                            text = "Início: ${treino.dataInicio}" + if (treino.dataFim.isNotBlank()) " • Término: ${treino.dataFim}" else "",
                                            fontSize = 12.sp,
                                            color = corTextoSecundario
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Toque para iniciar treino, ver vídeos e registrar cargas ›",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = corDestaque
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "ANAMNESES" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(onVoltar = { abaAtualAluno = "DASHBOARD" })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Minhas Anamneses (${anamneses.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (anamneses.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Text(
                                    text = "Nenhuma anamnese registrada no momento.",
                                    modifier = Modifier.padding(16.dp),
                                    color = corTextoSecundario
                                )
                            }
                        } else {
                            anamneses.forEach { anamnese ->
                                val respondida = anamnese.status == "RESPONDIDA"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = corCartao)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = anamnese.nomeModelo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = corTextoPrincipal
                                        )
                                        Text(
                                            text = if (respondida) {
                                                "Concluída" + if (anamnese.dataResposta.isNotBlank()) " em ${anamnese.dataResposta}" else ""
                                            } else {
                                                "Pendente de preenchimento"
                                            },
                                            color = if (respondida) Color(0xFF2E7D32) else Color(
                                                0xFFB17B00
                                            ),
                                            fontSize = 12.sp
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
                                            Text(if (respondida) "VISUALIZAR RESPOSTAS" else "PREENCHER AGORA")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "PROGRESSO" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(onVoltar = { abaAtualAluno = "DASHBOARD" })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Progresso de Cargas",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SecaoProgressoCargasAluno(
                            treinosAluno = treinosAluno,
                            treinoDao = treinoDao,
                            idAluno = idAluno,
                            corCartao = corCartao,
                            corTextoPrincipal = corTextoPrincipal,
                            corTextoSecundario = corTextoSecundario,
                            corDestaque = corDestaque
                        )
                    }

                    "DOCUMENTOS" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(onVoltar = { abaAtualAluno = "DASHBOARD" })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Meus Documentos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Importe exames, laudos médicos e imagens de evolução para o seu professor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = corTextoSecundario
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                                text = "Nenhum documento importado ainda.",
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
                                                text = if (ehComparacao) "Imagem para comparação"
                                                else "Documento médico — ${
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
                                            ) { Text("ABRIR DOCUMENTO") }
                                        }
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        documentoRepository.excluirDocumento(
                                                            documento.id
                                                        )
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

                    "PERIODIZACAO" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotaoVoltarPadrao(onVoltar = { abaAtualAluno = "DASHBOARD" })
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Periodização do Treino",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = corTextoPrincipal
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        var dadosPeriodizacaoRemota by remember {
                            mutableStateOf<Map<String, Any>?>(
                                null
                            )
                        }
                        LaunchedEffect(idAluno) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val alunoEnt = db.alunoDao().buscarPorIdDireto(idAluno)
                                    val fbId = alunoEnt?.firebaseId
                                    if (!fbId.isNullOrBlank()) {
                                        val doc =
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("alunos")
                                                .document(fbId)
                                                .collection("periodizacao")
                                                .document("atual")
                                                .get()
                                                .await()
                                        if (doc.exists()) {
                                            dadosPeriodizacaoRemota = doc.data
                                        }
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }

                        val remoto = dadosPeriodizacaoRemota
                        if (remoto != null) {
                            val macroNome = remoto["macroNome"] as? String ?: "Planejamento"
                            val macroObj = remoto["macroObjetivo"] as? String ?: ""
                            val ini = remoto["dataInicio"] as? String ?: ""
                            val fim = remoto["dataFim"] as? String ?: ""
                            val fases = remoto["fases"] as? List<*> ?: emptyList<Any>()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = macroNome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = corDestaque
                                    )
                                    if (macroObj.isNotBlank()) {
                                        Text(
                                            text = "Objetivo: $macroObj",
                                            fontSize = 12.sp,
                                            color = corTextoPrincipal
                                        )
                                    }
                                    Text(
                                        text = "Período: $ini até $fim",
                                        fontSize = 12.sp,
                                        color = corTextoSecundario
                                    )

                                    fases.forEach { fItem ->
                                        val fMap = fItem as? Map<*, *> ?: return@forEach
                                        val fNome = fMap["nome"] as? String ?: ""
                                        val fFoco = fMap["foco"] as? String ?: ""
                                        val micros =
                                            fMap["microciclos"] as? List<*> ?: emptyList<Any>()

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    corCardSecundario,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Fase: $fNome" + if (fFoco.isNotBlank()) " ($fFoco)" else "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = corTextoPrincipal
                                            )
                                            micros.forEach { mItem ->
                                                val mMap = mItem as? Map<*, *> ?: return@forEach
                                                val semNum =
                                                    (mMap["semanaNumero"] as? Number)?.toInt() ?: 1
                                                val mNome = mMap["nome"] as? String ?: ""
                                                val mObj = mMap["objetivoSemanal"] as? String ?: ""
                                                val mIni = mMap["dataInicio"] as? String ?: ""
                                                val mFim = mMap["dataFim"] as? String ?: ""
                                                Text(
                                                    text = "• Semana $semNum ($mNome): $mIni a $mFim" +
                                                            if (mObj.isNotBlank()) " — $mObj" else "",
                                                    fontSize = 12.sp,
                                                    color = corTextoSecundario
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (macrociclosAluno.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = corCartao)
                            ) {
                                Text(
                                    text = "Nenhuma periodização publicada pelo professor no momento.",
                                    modifier = Modifier.padding(16.dp),
                                    color = corTextoSecundario,
                                    fontSize = 13.sp
                                )
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
                                            val idAnam =
                                                chave.removePrefix("anam_").substringBefore("")
                                                    .toLongOrNull()
                                            if (idAnam != null) anamneseSelecionadaId = idAnam
                                        } else if (chave.startsWith("treino") && !bloqueadoPorAnamneseObrigatoria) {
                                            val idTr =
                                                chave.removePrefix("treino_").substringBefore("_")
                                                    .toLongOrNull()
                                            val trAlvo = treinosAluno.firstOrNull { it.id == idTr }
                                            if (trAlvo != null) {
                                                treinoSelecionadoAluno = trAlvo
                                                abaAtualAluno = "TREINOS"
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (jaLida) corCartao else corDestaque.copy(
                                        alpha = 0.14f
                                    )
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
private fun CartaoFrequenciaSemanalAluno(
    datasTreinadas: Set<String>,
    corCartao: Color,
    corTextoPrincipal: Color,
    corTextoSecundario: Color,
    corDestaque: Color
) {
    val fmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val diasSemana = remember(datasTreinadas) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val siglas = listOf("S", "T", "Q", "Q", "S", "S", "D")
        siglas.map { sigla ->
            val dataStr = fmt.format(cal.time)
            val diaMes = cal.get(Calendar.DAY_OF_MONTH).toString()
            val treinou = dataStr in datasTreinadas
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Triple(sigla, diaMes, treinou)
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corCartao),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Frequência de Treinos",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )
                Text(
                    text = "${diasSemana.count { it.third }} treino(s) na semana",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF36844A)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                diasSemana.forEach { (sigla, diaMes, treinou) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = sigla,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = corTextoSecundario
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (treinou) Color(0xFF36844A)
                                    else corDestaque.copy(alpha = 0.12f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (treinou) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Treinado",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = diaMes,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = corTextoPrincipal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartaoMenuAluno(
    modifier: Modifier = Modifier,
    titulo: String,
    subtitulo: String,
    icone: ImageVector,
    corFundo: Color,
    corDestaque: Color,
    corTextoPrincipal: Color,
    corTextoSecundario: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(corDestaque.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icone,
                    contentDescription = titulo,
                    tint = corDestaque,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal,
                    maxLines = 1
                )
                Text(
                    text = subtitulo,
                    fontSize = 11.sp,
                    color = corTextoSecundario,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SecaoProgressoCargasAluno(
    treinosAluno: List<TreinoEntity>,
    treinoDao: TreinoDao,
    idAluno: Long,
    corCartao: Color,
    corTextoPrincipal: Color,
    corTextoSecundario: Color,
    corDestaque: Color
) {
    val context = LocalContext.current
    var abaProgresso by remember { mutableStateOf("GRAFICOS") }
    val azulGrafico = Color(0xFF2196F3)

    val prefsHist = remember(idAluno) {
        context.getSharedPreferences(
            "fitcare_hist_cargas_$idAluno",
            android.content.Context.MODE_PRIVATE
        )
    }

    val todosExercicios by produceState(
        initialValue = emptyList<Pair<String, ExercicioEntity>>(),
        key1 = treinosAluno
    ) {
        val lista = mutableListOf<Pair<String, ExercicioEntity>>()
        for (tr in treinosAluno) {
            val exs = try {
                treinoDao.listarExerciciosDoAluno(tr.id, idAluno).first()
            } catch (e: Exception) {
                emptyList()
            }
            exs.forEach { lista.add(tr.nome to it) }
        }
        value = lista
    }

    val seriesPorExercicio = remember(todosExercicios) {
        val hojeStr = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())
        todosExercicios.distinctBy { it.second.nome.trim().lowercase() }.map { (_, ex) ->
            val bruto = prefsHist.getString(ex.nome.trim(), "").orEmpty()
            val pontos = bruto.split("|")
                .mapNotNull { item ->
                    val partes = item.split(":")
                    if (partes.size >= 2) {
                        val data = partes[0].trim()
                        val cargaTxt = partes[1].trim()
                        val valorNum = cargaTxt
                            .replace(",", ".")
                            .replace(Regex("[^0-9.]"), "")
                            .toFloatOrNull()
                        if (data.isNotBlank() && valorNum != null) {
                            Triple(data, cargaTxt, valorNum)
                        } else null
                    } else null
                }
                .toMutableList()

            if (pontos.isEmpty() && ex.carga.isNotBlank()) {
                val valAtual = ex.carga
                    .replace(",", ".")
                    .replace(Regex("[^0-9.]"), "")
                    .toFloatOrNull()
                if (valAtual != null) {
                    pontos.add(Triple(hojeStr, ex.carga, valAtual))
                }
            }
            ex.nome to pontos
        }.filter { it.second.isNotEmpty() }
    }

    // Seletor de Abas: Gráficos | Planilha
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(corCartao)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (abaProgresso == "GRAFICOS") azulGrafico else Color.Transparent)
                .clickable { abaProgresso = "GRAFICOS" }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Gráficos",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (abaProgresso == "GRAFICOS") Color.White else corTextoPrincipal
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (abaProgresso == "PLANILHA") azulGrafico else Color.Transparent)
                .clickable { abaProgresso = "PLANILHA" }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Planilha",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (abaProgresso == "PLANILHA") Color.White else corTextoPrincipal
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (seriesPorExercicio.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = corCartao),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Atualize as cargas numéricas nos seus exercícios (ex.: 25 kg) para visualizar seus gráficos e planilha de evolução.",
                modifier = Modifier.padding(16.dp),
                color = corTextoSecundario,
                fontSize = 13.sp
            )
        }
        return
    }

    if (abaProgresso == "GRAFICOS") {
        seriesPorExercicio.forEach { (nomeExercicio, pontos) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = corCartao),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = nomeExercicio,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(10.dp)
                                .background(azulGrafico.copy(alpha = 0.25f))
                                .padding(1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kg", fontSize = 12.sp, color = corTextoSecundario)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    val valores = pontos.map { it.third }
                    val minVal = ((valores.minOrNull() ?: 0f) - 5f).coerceAtLeast(0f)
                    val maxVal = (valores.maxOrNull() ?: 10f) + 5f
                    val range = (maxVal - minVal).coerceAtLeast(1f)

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        val stepX = if (pontos.size > 1) size.width / (pontos.size - 1) else size.width / 2f
                        val path = androidx.compose.ui.graphics.Path()

                        // Linhas de grade horizontais
                        for (i in 0..3) {
                            val yGrid = size.height * (i / 3f)
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.25f),
                                start = androidx.compose.ui.geometry.Offset(0f, yGrid),
                                end = androidx.compose.ui.geometry.Offset(size.width, yGrid),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        pontos.forEachIndexed { idx, (_, _, valor) ->
                            val x = if (pontos.size > 1) idx * stepX else size.width / 2f
                            val y = size.height - ((valor - minVal) / range) * size.height
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = azulGrafico,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                        )

                        pontos.forEachIndexed { idx, (_, _, valor) ->
                            val x = if (pontos.size > 1) idx * stepX else size.width / 2f
                            val y = size.height - ((valor - minVal) / range) * size.height
                            drawCircle(
                                color = azulGrafico,
                                radius = 4.dp.toPx(),
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        pontos.forEach { (data, cargaTxt, _) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cargaTxt,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = azulGrafico
                                )
                                Text(
                                    text = data,
                                    fontSize = 10.sp,
                                    color = corTextoSecundario
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
// ABA PLANILHA (Arraste horizontal com datas e badges azuis de carga)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = corCartao)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                seriesPorExercicio.forEach { (nomeExercicio, pontos) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nomeExercicio,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = corTextoPrincipal,
                            modifier = Modifier.width(120.dp)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pontos.forEach { (data, cargaTxt, _) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "→ $data",
                                        fontSize = 11.sp,
                                        color = corTextoSecundario
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(azulGrafico, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (cargaTxt.contains("kg", ignoreCase = true)) cargaTxt else "${cargaTxt}kg",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = corTextoSecundario.copy(alpha = 0.15f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(azulGrafico.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Arraste a planilha para o lado e veja todos os valores.",
                        fontSize = 12.sp,
                        color = azulGrafico,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoRespostaAnamnese(
    pergunta: PerguntaAnamneseAtribuidaEntity,
    valor: String,
    corTextoPrincipal: Color = Color(0xFF002B5C),
    onChange: (String) -> Unit
) {
    val tituloLowerCampo = pergunta.texto.lowercase(Locale.ROOT)
    val tipoEfetivo = when {
        pergunta.tipoResposta == "TEXTO" && (tituloLowerCampo.contains("frequência") || tituloLowerCampo.contains("frequencia")) -> "CHECKBOX"
        pergunta.tipoResposta == "TEXTO" && tituloLowerCampo.contains("disponibilidade") -> "CHECKBOX"
        else -> pergunta.tipoResposta
    }

    val alternativas = remember(pergunta.opcoes, pergunta.texto) {
        val doBanco = try {
            val json = JSONArray(pergunta.opcoes)
            (0 until json.length()).map { json.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        when {
            doBanco.isNotEmpty() -> doBanco
            tituloLowerCampo.contains("frequência") || tituloLowerCampo.contains("frequencia") -> listOf(
                "Não pratico atualmente",
                "1 a 2 vezes por semana",
                "3 a 4 vezes por semana",
                "5 ou mais vezes por semana"
            )
            tituloLowerCampo.contains("disponibilidade") -> listOf(
                "Segunda-feira",
                "Terça-feira",
                "Quarta-feira",
                "Quinta-feira",
                "Sexta-feira",
                "Sábado",
                "Domingo"
            )
            else -> emptyList()
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
        when (tipoEfetivo) {
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
                        if (novo.matches(Regex("0-9?"))) onChange(novo)
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
                val itemOutroAtual = selecionadas.firstOrNull {
                    ehOpcaoOutro(it) || it.startsWith("Outro:", ignoreCase = true)
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
                val itemDetalhesAtual =
                    selecionadas.firstOrNull { it.startsWith("Detalhes:", ignoreCase = true) }
                val textoDetalhesAtual = itemDetalhesAtual?.substringAfter(":")?.trimStart() ?: ""
                fun salvarListaCondicional(novas: Set<String>) {
                    if (novas.isEmpty()) {
                        onChange("SIM_ABERTO")
                    } else {
                        val arr = JSONArray()
                        novas.forEach { arr.put(it) }
                        onChange(arr.toString())
                    }
                }
                Row(modifier = Modifier.selectableGroup()) {
                    listOf("Sim", "Não").forEach { opcao ->
                        val sel = if (opcao == "Sim") respondeuSim else respondeuNao
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = sel,
                                    onClick = {
                                        if (opcao == "Não") onChange("Não")
                                        else if (!respondeuSim) onChange("SIM_ABERTO")
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
                        text = "Selecione as opções que se aplicam:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = corTextoPrincipal
                    )
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
                                    salvarListaCondicional(novas)
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
                                    salvarListaCondicional(novas)
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
                                salvarListaCondicional(novas)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Descreva qual outro") },
                            minLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = textoDetalhesAtual,
                        onValueChange = { digitado ->
                            val novas = selecionadas.filterNot {
                                it.startsWith("Detalhes:", ignoreCase = true)
                            }.toMutableSet()
                            if (digitado.isNotBlank()) {
                                novas.add("Detalhes: $digitado")
                            }
                            salvarListaCondicional(novas)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descreva detalhes adicionais (opcional)") },
                        minLines = 2
                    )
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
                        (0 until json.length()).map { json.getString(it) }.toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                }
                val itemOutroAtual = selecionadas.firstOrNull {
                    ehOpcaoOutro(it) || it.startsWith("Outro:", ignoreCase = true)
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
                        novas.forEach { item -> json.put(item) }
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
            "SIM_NAO_TEXTO" -> {
                val respondeuNao = valor == "Não"
                val respondeuSim = valor == "Sim" || valor.startsWith("Sim:", ignoreCase = true)
                val detalheSim = if (valor.startsWith("Sim:", ignoreCase = true)) {
                    valor.substringAfter(":").trimStart()
                } else ""

                Row(modifier = Modifier.selectableGroup()) {
                    listOf("Sim", "Não").forEach { opcao ->
                        val sel = if (opcao == "Sim") respondeuSim else respondeuNao
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .selectable(
                                    selected = sel,
                                    onClick = { onChange(if (opcao == "Não") "Não" else "Sim:") },
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
                    OutlinedTextField(
                        value = detalheSim,
                        onValueChange = { digitado -> onChange("Sim: $digitado") },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Descreva qual(is) medicamento(s) *") },
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