package br.com.icaros.fitcare

import br.com.icaros.fitcare.data.SessaoUsuario
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.AlunoRepository
import br.com.icaros.fitcare.data.AlunoSyncWorker
import br.com.icaros.fitcare.data.AnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
import br.com.icaros.fitcare.data.AnamneseRepository
import br.com.icaros.fitcare.data.DocumentoAnamneseEntity
import br.com.icaros.fitcare.data.FirestoreSyncService
import br.com.icaros.fitcare.data.FitCareDatabase
import br.com.icaros.fitcare.data.PerguntaAnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.RespostaAnamneseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    if (tipoResposta == "CHECKBOX" && respostaBruta.trim().startsWith("[")) {
        return try {
            val json = JSONArray(respostaBruta)
            (0 until json.length()).mapNotNull { i ->
                json.optString(i).trim().takeIf { it.isNotBlank() }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    return listOf(respostaBruta.trim())
}

private fun respostaEstaPreenchida(
    tipoResposta: String,
    resposta: String?
): Boolean {
    if (resposta.isNullOrBlank()) return false
    val itens = extrairItensResposta(tipoResposta, resposta)
    if (itens.isEmpty()) return false
    return itens.none { ehOpcaoOutro(it) }
}

private fun formatarRespostaAluno(
    tipoResposta: String,
    respostaBruta: String
): String {
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

    val idAluno = alunoId ?: br.com.icaros.fitcare.data.SessaoUsuario.usuarioAtual?.alunoId

    // Sincroniza e monitora em tempo real novas anamneses enviadas pelo professor
    DisposableEffect(idAluno) {
        var registration: com.google.firebase.firestore.ListenerRegistration? = null
        if (idAluno != null && idAluno > 0L) {
            scope.launch(Dispatchers.IO) {
                try {
                    val db = FitCareDatabase.getDatabase(context)
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarAnamnesesAtribuidasDoAluno()
                    val alunoLocal = db.alunoDao().buscarPorIdDireto(idAluno)
                    val fbId = alunoLocal?.firebaseId
                    if (!fbId.isNullOrBlank()) {
                        registration = syncService.iniciarMonitoramentoTempoRealAluno(
                            scope = scope,
                            alunoFirebaseId = fbId
                        )
                    }
                } catch (_: Exception) {
                }
            }
        }
        onDispose {
            registration?.remove()
        }
    }

    val anamnesesFlow = remember(idAluno) {
        if (idAluno != null && idAluno > 0L) {
            anamneseRepository.listarPorAluno(idAluno)
        } else {
            flowOf(emptyList<AnamneseAtribuidaEntity>())
        }
    }

    val anamneses by anamnesesFlow.collectAsState(
        initial = emptyList()
    )

    val documentosFlow = remember(idAluno, documentoRepository) {
        if (idAluno != null && idAluno > 0L) {
            documentoRepository.listarDocumentos(idAluno)
        } else {
            flowOf(emptyList<DocumentoAnamneseEntity>())
        }
    }

    val documentos by documentosFlow.collectAsState(
        initial = emptyList()
    )

    var anamneseSelecionadaId by remember(idAluno) {
        mutableStateOf<Long?>(null)
    }

    var perguntas by remember {
        mutableStateOf<List<PerguntaAnamneseAtribuidaEntity>>(
            emptyList()
        )
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

    var mensagemSucesso by remember {
        mutableStateOf("")
    }

    var mensagemDocumento by remember {
        mutableStateOf("")
    }

    val salvamentosPendentes = remember {
        mutableMapOf<Long, Job>()
    }

    val anamneseSelecionada = anamneses.firstOrNull {
        it.id == anamneseSelecionadaId
    }

    LaunchedEffect(anamneseSelecionadaId, anamneses) {
        if (
            anamneseSelecionadaId != null &&
            anamneses.isNotEmpty() &&
            anamneses.none { it.id == anamneseSelecionadaId }
        ) {
            anamneseSelecionadaId = null
        }
    }

    suspend fun aguardarSalvamentos() {
        salvamentosPendentes.values.toList().forEach {
            it.join()
        }
        salvamentosPendentes.clear()
    }

    LaunchedEffect(anamneseSelecionadaId, idAluno) {
        val id = anamneseSelecionadaId
        val idDoAluno = idAluno
        if (id != null && idDoAluno != null && idDoAluno > 0L) {
            carregando = true
            mensagemErro = ""
            mensagemSucesso = ""
            try {
                perguntas = anamneseRepository.buscarPerguntas(id)
                val respostasSalvas =
                    respostaRepository.buscarRespostasDaAnamnese(id)
                respostas = respostasSalvas.associate {
                    it.perguntaAtribuidaId to it.resposta
                }
            } catch (e: Exception) {
                mensagemErro = e.message
                    ?: "Não foi possível carregar a anamnese."
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
            .background(Color(0xFFEAF3F9))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
// CABEÇALHO PADRÃO DO FITCARE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFEAF3F9))
                .padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_fitcare),
                    contentDescription = "Logo Ícaros FitCare",
                    modifier = Modifier
                        .width(125.dp)
                        .height(62.dp)
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            aguardarSalvamentos()
                            onSair()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .height(34.dp),
                    contentPadding = PaddingValues(
                        horizontal = 10.dp,
                        vertical = 0.dp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFD32F2F)
                    )
                ) {
                    Text(
                        "SAIR",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF002B5C).copy(alpha = 0.10f)
            ) {
                Text(
                    text = "PERFIL: ALUNO",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    ),
                    color = Color(0xFF002B5C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            val primeiroNomeAluno = SessaoUsuario.usuarioAtual
                ?.nome
                ?.trim()
                ?.takeIf { it.isNotBlank() && !it.contains("@") }
                ?.substringBefore(" ")
                .orEmpty()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (primeiroNomeAluno.isNotBlank()) {
                    "Seja bem-vindo, $primeiroNomeAluno!"
                } else {
                    "Seja bem-vindo!"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF002B5C)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Acompanhe suas anamneses e documentos.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF526579)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 16.dp
                )
        ) {
            if (mensagemSucesso.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
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
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = "Não foi possível identificar o cadastro do aluno. Entre em contato com o profissional.",
                        modifier = Modifier.padding(16.dp),
                        color = Color(0xFFB71C1C)
                    )
                }
            } else if (anamneseSelecionadaId != null) {
                val modoSomenteLeitura =
                    anamneseSelecionada?.status == "RESPONDIDA"
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = anamneseSelecionada?.nomeModelo
                                ?: if (modoSomenteLeitura) {
                                    "Visualização da anamnese"
                                } else {
                                    "Preenchimento da anamnese"
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF002B5C)
                        )
                    }
                }
                if (carregando) {
                    CircularProgressIndicator()
                } else if (modoSomenteLeitura) {
                    if (perguntas.isEmpty()) {
                        Text(
                            text = "Esta anamnese não possui perguntas registradas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF526579)
                        )
                    } else {
                        perguntas.forEachIndexed { index, pergunta ->
                            val respostaBruta = respostas[pergunta.id].orEmpty()
                            val respostaExibida = formatarRespostaAluno(
                                tipoResposta = pergunta.tipoResposta,
                                respostaBruta = respostaBruta
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}. ${pergunta.texto}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF002B5C)
                                    )
                                    Text(
                                        text = respostaExibida.ifBlank {
                                            "Não respondida"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (respostaExibida.isBlank()) {
                                            Color(0xFF757575)
                                        } else {
                                            Color(0xFF212121)
                                        }
                                    )
                                }
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
                        val respostaAtual =
                            respostas[pergunta.id] ?: ""
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                CampoRespostaAnamnese(
                                    pergunta = pergunta,
                                    valor = respostaAtual,
                                    onChange = { novaResposta ->
                                        respostas = respostas +
                                                (pergunta.id to novaResposta)
                                        mensagemErro = ""
                                        salvamentosPendentes[
                                            pergunta.id
                                        ]?.cancel()
                                        salvamentosPendentes[
                                            pergunta.id
                                        ] = scope.launch {
                                            delay(250)
                                            try {
                                                respostaRepository
                                                    .salvarRespostaDoAluno(
                                                        alunoId = idAluno,
                                                        perguntaId = pergunta.id,
                                                        resposta = novaResposta
                                                    )
                                            } catch (e: Exception) {
                                                mensagemErro = e.message
                                                    ?: "Erro ao salvar resposta."
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val perguntasObrigatoriasPendentes =
                        perguntas.count { pergunta ->
                            pergunta.obrigatoria &&
                                    !respostaEstaPreenchida(
                                        tipoResposta = pergunta.tipoResposta,
                                        resposta = respostas[pergunta.id]
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
                                        val respostaAtual =
                                            respostas[pergunta.id] ?: ""
                                        respostaRepository.salvarRespostaDoAluno(
                                            alunoId = idAluno,
                                            perguntaId = pergunta.id,
                                            resposta = respostaAtual
                                        )
                                    }
                                    respostaRepository.concluirDoAluno(
                                        alunoId = idAluno,
                                        anamneseId = anamneseSelecionadaId!!
                                    )
// Vincula e atualiza automaticamente Condições, Restrições e Status no perfil do aluno
                                    sincronizarAcompanhamentoAlunoComRespostas(
                                        alunoId = idAluno,
                                        alunoRepository = alunoRepository,
                                        perguntas = perguntas,
                                        respostas = respostas
                                    )
// Envia imediatamente as respostas e dados atualizados ao Firestore
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val db = FitCareDatabase.getDatabase(context)
                                            val syncService = FirestoreSyncService(db.usuarioDao(), db)
                                            syncService.sincronizarAnamnesesAtribuidasDoAluno()
                                        } catch (_: Exception) {
                                        }
                                    }
                                    AlunoSyncWorker.agendar(context)
                                    anamneseSelecionadaId = null
                                    perguntas = emptyList()
                                    respostas = emptyMap()
                                    mensagemSucesso =
                                        "Anamnese concluída e enviada com sucesso ao professor."
                                } catch (e: Exception) {
                                    mensagemErro = e.message
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
// LISTAGEM DAS ANAMNESES
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Você ainda não possui uma anamnese atribuída. Aguarde o envio pelo profissional.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (pendentes.isNotEmpty()) {
                    Text(
                        text = "Anamneses disponíveis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002B5C)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    pendentes.forEach { anamnese ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = anamnese.nomeModelo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (
                                        anamnese.status == "EM_ANDAMENTO"
                                    ) {
                                        "Preenchimento iniciado"
                                    } else {
                                        "Aguardando preenchimento"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF526579)
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aguardando liberação do profissional",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    naoEnviadas.forEach { anamnese ->
                        Text(
                            text = "• ${anamnese.nomeModelo} — ainda não enviada.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (respondidas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Anamneses concluídas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF002B5C)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    respondidas.forEach { anamnese ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = anamnese.nomeModelo,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF002B5C)
                                )
                                Text(
                                    text = buildString {
                                        append("Concluída")
                                        if (anamnese.dataResposta.isNotBlank()) {
                                            append(" em ${anamnese.dataResposta}")
                                        }
                                    },
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        anamneseSelecionadaId = anamnese.id
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
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
                if (
                    anamneses.isNotEmpty() &&
                    pendentes.isEmpty() &&
                    naoEnviadas.isEmpty() &&
                    respondidas.size == anamneses.size
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Todas as suas anamneses foram concluídas. Seu acesso está liberado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
// DOCUMENTOS E IMAGENS DO ALUNO
            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "MEUS DOCUMENTOS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF002B5C)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Importe exames, laudos e imagens para comparação.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF526579)
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (idAluno != null && idAluno > 0L) {
                BotaoImportarDocumentoAnamnese(
                    alunoId = idAluno,
                    anamneseRepository = documentoRepository,
                    alunoRepository = alunoRepository,
                    onDocumentoImportado = {
                        mensagemDocumento = ""
                    }
                )
            }
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                documentos.forEach { documento ->
                    val ehComparacao =
                        documento.tipoArquivo.startsWith(
                            "comparacao_",
                            ignoreCase = true
                        ) ||
                                documento.nomeArquivo.startsWith(
                                    "comparacao_",
                                    ignoreCase = true
                                )
                    val nomeExibido = documento.nomeArquivo
                        .removePrefix("comparacao_")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        )
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
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (ehComparacao) {
                                        "Imagem para comparação"
                                    } else {
                                        "Documento médico — ${
                                            documento.tipoArquivo.uppercase(
                                                Locale.ROOT
                                            )
                                        }"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF526579)
                                )
                                Text(
                                    text = "Importado em: ${documento.dataImportacao}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                TextButton(
                                    onClick = {
                                        try {
                                            abrirDocumentoAnamnese(
                                                context,
                                                documento
                                            )
                                            mensagemErro = ""
                                        } catch (erro: Exception) {
                                            mensagemErro = erro.message
                                                ?: "Não foi possível abrir o documento."
                                        }
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 0.dp,
                                        vertical = 0.dp
                                    )
                                ) {
                                    Text("ABRIR DOCUMENTO")
                                }
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            documentoRepository.excluirDocumento(
                                                documento.id
                                            )
                                            File(
                                                documento.caminhoArquivo
                                            ).delete()
                                            mensagemErro = ""
                                        } catch (erro: Exception) {
                                            mensagemErro = erro.message
                                                ?: "Não foi possível excluir o documento."
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir documento",
                                    tint = Color(0xFFD32F2F)
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
}
// CAMPO DE RESPOSTA DA ANAMNESE (COM CAIXA DE TEXTO PARA "OUTRO")
@Composable
private fun CampoRespostaAnamnese(
    pergunta: PerguntaAnamneseAtribuidaEntity,
    valor: String,
    onChange: (String) -> Unit
) {
    val alternativas = remember(pergunta.opcoes) {
        try {
            val json = JSONArray(pergunta.opcoes)
            (0 until json.length()).map {
                json.getString(it)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = pergunta.texto,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF002B5C)
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
                        if (novo.matches(Regex("[0-9]([.,][0-9])?"))) {
                            onChange(novo)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Informe um número") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
            }
            "DATA" -> {
                OutlinedTextField(
                    value = valor,
                    onValueChange = onChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Data") },
                    placeholder = { Text("DD/MM/AAAA") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
            "SIM_NAO" -> {
                Row(
                    modifier = Modifier.selectableGroup()
                ) {
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
                            RadioButton(
                                selected = valor == opcao,
                                onClick = null
                            )
                            Text(
                                text = opcao,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            "MULTIPLA_ESCOLHA" -> {
                val outroSelecionado = ehOpcaoOutro(valor) ||
                        valor.startsWith("Outro:", ignoreCase = true)
                val textoOutroAtual = if (valor.startsWith("Outro:", ignoreCase = true)) {
                    valor.substringAfter(":").trimStart()
                } else {
                    ""
                }
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    alternativas.forEach { opcao ->
                        val ehEstaOutro = ehOpcaoOutro(opcao)
                        val selecionada = if (ehEstaOutro) {
                            outroSelecionado
                        } else {
                            valor == opcao
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selecionada,
                                    onClick = {
                                        if (ehEstaOutro) {
                                            if (!outroSelecionado) {
                                                onChange("Outro:")
                                            }
                                        } else {
                                            onChange(opcao)
                                        }
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selecionada,
                                onClick = null
                            )
                            Text(
                                text = opcao,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (outroSelecionado) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = textoOutroAtual,
                            onValueChange = { digitado ->
                                onChange("Outro: $digitado")
                            },
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
                        (0 until json.length()).map {
                            json.getString(it)
                        }.toSet()
                    } catch (_: Exception) {
                        emptySet()
                    }
                }
                val itemOutroAtual = selecionadas.firstOrNull {
                    ehOpcaoOutro(it) || it.startsWith("Outro:", ignoreCase = true)
                }
                val outroMarcado = itemOutroAtual != null
                val textoOutroAtual = if (
                    itemOutroAtual != null &&
                    itemOutroAtual.startsWith("Outro:", ignoreCase = true)
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
                    val marcada = if (ehEstaOutro) {
                        outroMarcado
                    } else {
                        opcao in selecionadas
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val novas = selecionadas.toMutableSet()
                                if (ehEstaOutro) {
                                    if (outroMarcado) {
                                        novas.removeAll {
                                            ehOpcaoOutro(it) ||
                                                    it.startsWith("Outro:", ignoreCase = true)
                                        }
                                    } else {
                                        novas.add("Outro:")
                                    }
                                } else {
                                    if (opcao in novas) {
                                        novas.remove(opcao)
                                    } else {
                                        novas.add(opcao)
                                    }
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
                                            ehOpcaoOutro(it) ||
                                                    it.startsWith("Outro:", ignoreCase = true)
                                        }
                                    }
                                } else {
                                    if (checked) {
                                        novas.add(opcao)
                                    } else {
                                        novas.remove(opcao)
                                    }
                                }
                                salvarConjunto(novas)
                            }
                        )
                        Text(
                            text = opcao,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (outroMarcado) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = textoOutroAtual,
                        onValueChange = { digitado ->
                            val novas = selecionadas.filterNot {
                                ehOpcaoOutro(it) ||
                                        it.startsWith("Outro:", ignoreCase = true)
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