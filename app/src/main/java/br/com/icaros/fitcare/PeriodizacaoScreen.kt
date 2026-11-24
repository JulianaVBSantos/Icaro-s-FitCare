package br.com.icaros.fitcare

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val DIAS_SEMANA_PERIODIZACAO = listOf(
    "SEGUNDA" to "Segunda",
    "TERCA" to "Terça",
    "QUARTA" to "Quarta",
    "QUINTA" to "Quinta",
    "SEXTA" to "Sexta",
    "SABADO" to "Sábado",
    "DOMINGO" to "Domingo"
)

private fun formatarDataUtcParaBr(utcMillis: Long): String {
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(utcMillis))
}

private fun parseDataBrParaCalendar(dataStr: String): Calendar? {
    if (dataStr.isBlank()) return null
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
        isLenient = false
    }
    val date = runCatching { fmt.parse(dataStr.trim()) }.getOrNull() ?: return null
    return Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

// Publica um resumo estruturado da periodização no Firestore para exibição na Área do Aluno
private suspend fun publicarPeriodizacaoAlunoFirestore(
    aluno: AlunoEntity?,
    periodizacaoRepository: PeriodizacaoRepository,
    treinosAluno: List<TreinoEntity>
) {
    val alunoFbId = aluno?.firebaseId?.takeIf { it.isNotBlank() } ?: return
    try {
        val macros = periodizacaoRepository.listarMacrociclos(aluno.id).first()
        val macroAtual = macros.firstOrNull()
        val docRef = FirebaseFirestore.getInstance()
            .collection("alunos")
            .document(alunoFbId)
            .collection("periodizacao")
            .document("atual")

        if (macroAtual == null) {
            docRef.delete().await()
            return
        }

        val mesos = periodizacaoRepository.listarMesociclos(macroAtual.id).first()
        val fasesLista = mutableListOf<Map<String, Any>>()

        for (meso in mesos) {
            val micros = periodizacaoRepository.listarMicrociclos(meso.id).first()
            val microsLista = mutableListOf<Map<String, Any>>()
            for ((idxMicro, micro) in micros.withIndex()) {
                val sessoes = periodizacaoRepository.listarSessoes(micro.id).first()
                val sessoesLista = sessoes.map { s ->
                    val trNome = treinosAluno.firstOrNull { it.id == s.treinoId }?.nome ?: ""
                    mapOf(
                        "diaSemana" to s.diaSemana,
                        "titulo" to s.titulo,
                        "observacoes" to s.observacoes,
                        "treinoNome" to trNome
                    )
                }
                microsLista.add(
                    mapOf(
                        "semanaNumero" to (idxMicro + 1),
                        "nome" to micro.nome,
                        "objetivoSemanal" to micro.objetivoSemanal,
                        "dataInicio" to micro.dataInicio,
                        "dataFim" to micro.dataFim,
                        "frequencia" to sessoes.size,
                        "sessoes" to sessoesLista
                    )
                )
            }
            fasesLista.add(
                mapOf(
                    "nome" to meso.nome,
                    "foco" to meso.foco,
                    "dataInicio" to meso.dataInicio,
                    "dataFim" to meso.dataFim,
                    "microciclos" to microsLista
                )
            )
        }

        docRef.set(
            mapOf(
                "macroNome" to macroAtual.nome,
                "macroObjetivo" to macroAtual.objetivo,
                "dataInicio" to macroAtual.dataInicio,
                "dataFim" to macroAtual.dataFim,
                "fases" to fasesLista,
                "updatedAtMillis" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    } catch (e: Exception) {
    }
}

@Composable
fun TelaPeriodizacaoAluno(
    alunoId: Long,
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    periodizacaoRepository: PeriodizacaoRepository,
    onVoltar: () -> Unit,
    onVisualizarTreino: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val aluno by repository.buscarPorId(alunoId).collectAsState(initial = null)
    val macrociclos by periodizacaoRepository.listarMacrociclos(alunoId).collectAsState(initial = emptyList())
    val treinosAluno by treinoRepository.listarTreinosDoAluno(alunoId).collectAsState(initial = emptyList())

    var macroSelecionado by remember { mutableStateOf<MacrocicloEntity?>(null) }
    var mesoSelecionado by remember { mutableStateOf<MesocicloEntity?>(null) }
    var microSelecionado by remember { mutableStateOf<MicrocicloEntity?>(null) }

    var editarMacro by remember { mutableStateOf<MacrocicloEntity?>(null) }
    var mostrarDialogoMacro by remember { mutableStateOf(false) }

    var editarMeso by remember { mutableStateOf<MesocicloEntity?>(null) }
    var mostrarDialogoMeso by remember { mutableStateOf(false) }

    var editarMicro by remember { mutableStateOf<MicrocicloEntity?>(null) }
    var mostrarDialogoMicro by remember { mutableStateOf(false) }

    var editarSessao by remember { mutableStateOf<SessaoPeriodizacaoEntity?>(null) }
    var mostrarDialogoSessao by remember { mutableStateOf(false) }

    var mensagemErro by remember { mutableStateOf("") }

    fun sincronizarComAluno() {
        scope.launch(Dispatchers.IO) {
            publicarPeriodizacaoAlunoFirestore(aluno, periodizacaoRepository, treinosAluno)
        }
    }

    fun voltarNivelAnterior() {
        when {
            microSelecionado != null -> microSelecionado = null
            mesoSelecionado != null -> mesoSelecionado = null
            macroSelecionado != null -> macroSelecionado = null
            else -> onVoltar()
        }
    }

    BackHandler {
        voltarNivelAnterior()
    }

    LaunchedEffect(macrociclos) {
        macroSelecionado?.let { sel ->
            macroSelecionado = macrociclos.firstOrNull { it.id == sel.id }
            if (macroSelecionado == null) {
                mesoSelecionado = null
                microSelecionado = null
            }
        }
    }

    val mesociclos by (macroSelecionado?.let { periodizacaoRepository.listarMesociclos(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    LaunchedEffect(mesociclos) {
        mesoSelecionado?.let { sel ->
            mesoSelecionado = mesociclos.firstOrNull { it.id == sel.id }
            if (mesoSelecionado == null) {
                microSelecionado = null
            }
        }
    }

    val microciclos by (mesoSelecionado?.let { periodizacaoRepository.listarMicrociclos(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    LaunchedEffect(microciclos) {
        microSelecionado?.let { sel ->
            microSelecionado = microciclos.firstOrNull { it.id == sel.id }
        }
    }

    val sessoes by (microSelecionado?.let { periodizacaoRepository.listarSessoes(it.id) }
        ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    // Coleta todos os microciclos do macrociclo para destacar no calendário consolidado
    val todosMicrociclosDoMacro by produceState<List<MicrocicloEntity>>(
        initialValue = emptyList(),
        key1 = mesociclos
    ) {
        val lista = mutableListOf<MicrocicloEntity>()
        for (m in mesociclos) {
            val micros = try {
                periodizacaoRepository.listarMicrociclos(m.id).first()
            } catch (e: Exception) {
                emptyList()
            }
            lista.addAll(micros)
        }
        value = lista
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                BotaoVoltarPadrao(
                    onVoltar = { voltarNivelAnterior() }
                )
            }
            LogoFitCarePadrao(
                modifier = Modifier.align(Alignment.Center),
                largura = 135.dp,
                altura = 62.dp
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Periodização do Treinamento",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Aluno: ${aluno?.nome ?: "Carregando..."}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Hierarquia atual:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val trilha = buildList {
                        add("Macrociclos")
                        macroSelecionado?.let { add(it.nome) }
                        mesoSelecionado?.let { add(it.nome) }
                        microSelecionado?.let { add(it.nome) }
                    }.joinToString(" › ")
                    Text(
                        text = trilha,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (mensagemErro.isNotBlank()) {
                Text(
                    text = mensagemErro,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
            when {
// NÍVEL 1: MACROCICLOS
                macroSelecionado == null -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Macrociclos (${macrociclos.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                editarMacro = null
                                mostrarDialogoMacro = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Macrociclo", fontSize = 12.sp)
                        }
                    }
                    if (macrociclos.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Nenhum macrociclo cadastrado. Crie um planejamento para organizar fases, microciclos semanais e variáveis de treino.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        macrociclos.forEach { macro ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { macroSelecionado = macro },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = macro.nome,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (macro.objetivo.isNotBlank()) {
                                                Text(
                                                    text = "Objetivo: ${macro.objetivo}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Período: ${macro.dataInicio}" +
                                                        if (macro.dataFim.isNotBlank()) " até ${macro.dataFim}" else "",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            editarMacro = macro
                                            mostrarDialogoMacro = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                try {
                                                    periodizacaoRepository.excluirMacrociclo(macro)
                                                    sincronizarComAluno()
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir macrociclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para abrir o calendário e o painel visual dos ciclos ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
// NÍVEL 2: CALENDÁRIO CONSOLIDADO (COM MICROCICLOS) + PAINEL VISUAL MODERNO + MESOCICLOS
                mesoSelecionado == null -> {
                    val macro = macroSelecionado!!
                    CalendarioMacrocicloConsolidado(
                        macrociclo = macro,
                        mesociclos = mesociclos,
                        microciclos = todosMicrociclosDoMacro
                    )
                    VisaoCompletaMacrociclo(
                        macrociclo = macro,
                        periodizacaoRepository = periodizacaoRepository,
                        treinosAluno = treinosAluno,
                        onVisualizarTreino = onVisualizarTreino
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Fases / Mesociclos (${mesociclos.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                editarMeso = null
                                mostrarDialogoMeso = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mesociclo", fontSize = 12.sp)
                        }
                    }
                    if (mesociclos.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Nenhuma fase (mesociclo) cadastrada em ${macro.nome}. Adicione fases como Adaptação, Hipertrofia ou Força.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        mesociclos.forEach { meso ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { mesoSelecionado = meso },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = meso.nome,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (meso.foco.isNotBlank()) {
                                                Text(
                                                    text = "Fase / Foco: ${meso.foco}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Período: ${meso.dataInicio}" +
                                                        if (meso.dataFim.isNotBlank()) " até ${meso.dataFim}" else "",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            editarMeso = meso
                                            mostrarDialogoMeso = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                try {
                                                    periodizacaoRepository.excluirMesociclo(meso)
                                                    sincronizarComAluno()
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir mesociclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para gerenciar os microciclos semanais ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
// NÍVEL 3: MICROCICLOS
                microSelecionado == null -> {
                    val meso = mesoSelecionado!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3. Microciclos (${microciclos.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                editarMicro = null
                                mostrarDialogoMicro = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Microciclo", fontSize = 12.sp)
                        }
                    }
                    if (microciclos.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Nenhum microciclo semanal cadastrado em ${meso.nome}.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        microciclos.forEachIndexed { idxMicro, micro ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { microSelecionado = micro },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "SEMANA ${idxMicro + 1}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = micro.nome,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (micro.objetivoSemanal.isNotBlank()) {
                                                Text(
                                                    text = "Variáveis / Meta: ${micro.objetivoSemanal}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Período: ${micro.dataInicio}" +
                                                        if (micro.dataFim.isNotBlank()) " até ${micro.dataFim}" else "",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            editarMicro = micro
                                            mostrarDialogoMicro = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            scope.launch {
                                                try {
                                                    periodizacaoRepository.excluirMicrociclo(micro)
                                                    sincronizarComAluno()
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir microciclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para organizar os dias e treinos da semana ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
// NÍVEL 4: SESSÕES POR DIAS DA SEMANA
                else -> {
                    val micro = microSelecionado!!
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "4. Sessões da Semana (${sessoes.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = {
                                editarSessao = null
                                mostrarDialogoSessao = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sessão", fontSize = 12.sp)
                        }
                    }
                    DIAS_SEMANA_PERIODIZACAO.forEach { (codigoDia, nomeDia) ->
                        val sessoesDoDia = sessoes.filter { it.diaSemana.equals(codigoDia, ignoreCase = true) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = nomeDia,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (sessoesDoDia.isEmpty()) {
                                    Text(
                                        text = "Descanso / sem sessão vinculada.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    sessoesDoDia.forEach { sessao ->
                                        val treinoVinculado = treinosAluno.firstOrNull { it.id == sessao.treinoId }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = sessao.titulo,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (treinoVinculado != null) {
                                                            Text(
                                                                text = "Treino: ${treinoVinculado.nome}",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        if (sessao.observacoes.isNotBlank()) {
                                                            Text(
                                                                text = "Variáveis / Intervalo: ${sessao.observacoes}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    IconButton(onClick = {
                                                        editarSessao = sessao
                                                        mostrarDialogoSessao = true
                                                    }) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                    IconButton(onClick = {
                                                        scope.launch {
                                                            try {
                                                                periodizacaoRepository.excluirSessao(sessao)
                                                                sincronizarComAluno()
                                                            } catch (e: Exception) {
                                                                mensagemErro = e.message ?: "Erro ao excluir sessão."
                                                            }
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                                if (treinoVinculado != null) {
                                                    OutlinedButton(
                                                        onClick = { onVisualizarTreino(treinoVinculado.id) },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("Abrir ficha de treino")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
// DIÁLOGOS COM SELETOR GRÁFICO DE CALENDÁRIO
    if (mostrarDialogoMacro) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMacro == null) "Novo Macrociclo" else "Editar Macrociclo",
            nomeInicial = editarMacro?.nome ?: "",
            subtituloLabel = "Objetivo / Frequência (ex.: Hipertrofia • 3x/semana)",
            subtituloInicial = editarMacro?.objetivo ?: "",
            inicioInicial = editarMacro?.dataInicio ?: "",
            fimInicial = editarMacro?.dataFim ?: "",
            onDismiss = { mostrarDialogoMacro = false },
            onSalvar = { nome, obj, inicio, fim ->
                scope.launch {
                    try {
                        periodizacaoRepository.salvarMacrociclo(
                            MacrocicloEntity(
                                id = editarMacro?.id ?: 0L,
                                alunoId = alunoId,
                                nome = nome.trim(),
                                objetivo = obj.trim(),
                                dataInicio = inicio.trim(),
                                dataFim = fim.trim()
                            )
                        )
                        sincronizarComAluno()
                        mensagemErro = ""
                        mostrarDialogoMacro = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar macrociclo."
                    }
                }
            }
        )
    }
    if (mostrarDialogoMeso && macroSelecionado != null) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMeso == null) "Novo Mesociclo (Fase)" else "Editar Mesociclo",
            nomeInicial = editarMeso?.nome ?: "",
            subtituloLabel = "Fase / Repetições / Intervalo (ex.: Hipertrofia • 8-12 reps • 60s)",
            subtituloInicial = editarMeso?.foco ?: "",
            inicioInicial = editarMeso?.dataInicio ?: "",
            fimInicial = editarMeso?.dataFim ?: "",
            onDismiss = { mostrarDialogoMeso = false },
            onSalvar = { nome, foco, inicio, fim ->
                scope.launch {
                    try {
                        periodizacaoRepository.salvarMesociclo(
                            MesocicloEntity(
                                id = editarMeso?.id ?: 0L,
                                macrocicloId = macroSelecionado!!.id,
                                nome = nome.trim(),
                                foco = foco.trim(),
                                dataInicio = inicio.trim(),
                                dataFim = fim.trim(),
                                ordem = editarMeso?.ordem ?: mesociclos.size
                            )
                        )
                        sincronizarComAluno()
                        mensagemErro = ""
                        mostrarDialogoMeso = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar mesociclo."
                    }
                }
            }
        )
    }
    if (mostrarDialogoMicro && mesoSelecionado != null) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMicro == null) "Novo Microciclo Semanal" else "Editar Microciclo",
            nomeInicial = editarMicro?.nome ?: "",
            subtituloLabel = "Variáveis e meta semanal (ex.: Choque • RIR 1-2 • 90s descanso)",
            subtituloInicial = editarMicro?.objetivoSemanal ?: "",
            inicioInicial = editarMicro?.dataInicio ?: "",
            fimInicial = editarMicro?.dataFim ?: "",
            onDismiss = { mostrarDialogoMicro = false },
            onSalvar = { nome, meta, inicio, fim ->
                scope.launch {
                    try {
                        periodizacaoRepository.salvarMicrociclo(
                            MicrocicloEntity(
                                id = editarMicro?.id ?: 0L,
                                mesocicloId = mesoSelecionado!!.id,
                                nome = nome.trim(),
                                objetivoSemanal = meta.trim(),
                                dataInicio = inicio.trim(),
                                dataFim = fim.trim(),
                                ordem = editarMicro?.ordem ?: microciclos.size
                            )
                        )
                        sincronizarComAluno()
                        mensagemErro = ""
                        mostrarDialogoMicro = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar microciclo."
                    }
                }
            }
        )
    }
    if (mostrarDialogoSessao && microSelecionado != null) {
        var diaSelecionado by remember { mutableStateOf(editarSessao?.diaSemana ?: "SEGUNDA") }
        var tituloSessao by remember { mutableStateOf(editarSessao?.titulo ?: "") }
        var obsSessao by remember { mutableStateOf(editarSessao?.observacoes ?: "") }
        var treinoVincId by remember { mutableStateOf(editarSessao?.treinoId) }
        AlertDialog(
            onDismissRequest = { mostrarDialogoSessao = false },
            title = { Text(if (editarSessao == null) "Nova Sessão Semanal" else "Editar Sessão") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Dia da semana:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    DIAS_SEMANA_PERIODIZACAO.chunked(2).forEach { par ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            par.forEach { (cod, rotulo) ->
                                val sel = diaSelecionado == cod
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (sel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { diaSelecionado = cod }
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rotulo,
                                        fontSize = 11.sp,
                                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = tituloSessao,
                        onValueChange = { tituloSessao = it },
                        label = { Text("Identificação da sessão (ex.: A1, B1) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Vincular ficha de treino (opcional):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { treinoVincId = null },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = treinoVincId == null, onClick = { treinoVincId = null })
                        Text("Sem treino vinculado", fontSize = 12.sp)
                    }
                    treinosAluno.forEach { tr ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { treinoVincId = tr.id },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = treinoVincId == tr.id, onClick = { treinoVincId = tr.id })
                            Text(tr.nome, fontSize = 12.sp)
                        }
                    }
                    OutlinedTextField(
                        value = obsSessao,
                        onValueChange = { obsSessao = it },
                        label = { Text("Variáveis / Intervalo / Repetições") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            periodizacaoRepository.salvarSessao(
                                SessaoPeriodizacaoEntity(
                                    id = editarSessao?.id ?: 0L,
                                    microcicloId = microSelecionado!!.id,
                                    treinoId = treinoVincId,
                                    diaSemana = diaSelecionado,
                                    titulo = tituloSessao.trim(),
                                    observacoes = obsSessao.trim(),
                                    ordem = editarSessao?.ordem ?: sessoes.size
                                )
                            )
                            sincronizarComAluno()
                            mensagemErro = ""
                            mostrarDialogoSessao = false
                        } catch (e: Exception) {
                            mensagemErro = e.message ?: "Erro ao salvar sessão."
                        }
                    }
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSessao = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
// Calendário Consolidado destacando Macrociclo, Fases (Mesociclos) e Microciclos Semanais
@Composable
fun CalendarioMacrocicloConsolidado(
    macrociclo: MacrocicloEntity,
    mesociclos: List<MesocicloEntity>,
    microciclos: List<MicrocicloEntity> = emptyList()
) {
    val escuro = temaEscuroAtivoState
    val corCartao = if (escuro) Color(0xFF172331) else Color.White
    val corTextoPrincipal = if (escuro) Color(0xFFEAF2FA) else Color(0xFF002B5C)
    val corTextoSecundario = if (escuro) Color(0xFFA2B5C8) else Color(0xFF526579)
    val corDestaque = if (escuro) Color(0xFF7AB8F5) else Color(0xFF002B5C)

    val fmt = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
            isLenient = false
        }
    }
    val fmtMesAno = remember {
        SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
    }

    val dataInicioMacro = remember(macrociclo.dataInicio) {
        runCatching { fmt.parse(macrociclo.dataInicio) }.getOrNull()
    }
    val dataFimMacro = remember(macrociclo.dataFim) {
        runCatching { fmt.parse(macrociclo.dataFim) }.getOrNull()
    }

    val coresMicrociclos = listOf(
        Color(0xFF2E7D32), // Verde
        Color(0xFF1565C0), // Azul
        Color(0xFFEF6C00), // Laranja
        Color(0xFF6A1B9A), // Roxo
        Color(0xFF00838F), // Ciano
        Color(0xFFC62828)  // Vermelho
    )

    var mesAtualCal by remember(macrociclo.id, dataInicioMacro) {
        mutableStateOf(
            Calendar.getInstance().apply {
                time = dataInicioMacro ?: Date()
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        )
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
                IconButton(
                    onClick = {
                        mesAtualCal = (mesAtualCal.clone() as Calendar).apply {
                            add(Calendar.MONTH, -1)
                        }
                    }
                ) {
                    Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = corDestaque)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Calendário de Microciclos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = corTextoSecundario
                    )
                    Text(
                        text = fmtMesAno.format(mesAtualCal.time)
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                }

                IconButton(
                    onClick = {
                        mesAtualCal = (mesAtualCal.clone() as Calendar).apply {
                            add(Calendar.MONTH, 1)
                        }
                    }
                ) {
                    Text("›", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = corDestaque)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val diasSemana = listOf("D", "S", "T", "Q", "Q", "S", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                diasSemana.forEach { dia ->
                    Text(
                        text = dia,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoSecundario
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val primeiroDiaSemana = mesAtualCal.get(Calendar.DAY_OF_WEEK) - 1
            val totalDiasNoMes = mesAtualCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val celulas = buildList<Int?> {
                repeat(primeiroDiaSemana) { add(null) }
                for (d in 1..totalDiasNoMes) add(d)
                while (size % 7 != 0) add(null)
            }

            celulas.chunked(7).forEach { semana ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    semana.forEach { dia ->
                        if (dia == null) {
                            Box(modifier = Modifier.weight(1f).height(34.dp))
                        } else {
                            val dataDia = (mesAtualCal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dia)
                            }.time

                            val dentroMacro = dataInicioMacro != null && dataFimMacro != null &&
                                    !dataDia.before(dataInicioMacro) && !dataDia.after(dataFimMacro)

                            val indiceMicroAtivo = microciclos.indexOfFirst { micro ->
                                val ini = runCatching { fmt.parse(micro.dataInicio) }.getOrNull()
                                val fim = runCatching { fmt.parse(micro.dataFim) }.getOrNull()
                                ini != null && fim != null && !dataDia.before(ini) && !dataDia.after(fim)
                            }

                            val dentroMeso = mesociclos.any { meso ->
                                val ini = runCatching { fmt.parse(meso.dataInicio) }.getOrNull()
                                val fim = runCatching { fmt.parse(meso.dataFim) }.getOrNull()
                                ini != null && fim != null && !dataDia.before(ini) && !dataDia.after(fim)
                            }

                            val corFundoDia = when {
                                indiceMicroAtivo >= 0 -> coresMicrociclos[indiceMicroAtivo % coresMicrociclos.size]
                                dentroMeso -> corDestaque.copy(alpha = 0.75f)
                                dentroMacro -> corDestaque.copy(alpha = 0.22f)
                                else -> Color.Transparent
                            }

                            val corTextoDia = when {
                                indiceMicroAtivo >= 0 || dentroMeso -> Color.White
                                else -> corTextoPrincipal
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .padding(horizontal = 2.dp)
                                    .background(corFundoDia, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dia.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (indiceMicroAtivo >= 0 || dentroMeso) FontWeight.Bold else FontWeight.Normal,
                                    color = corTextoDia
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (microciclos.isNotEmpty()) {
                Text(
                    text = "Microciclos (Semanas) no período:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    microciclos.forEachIndexed { idx, micro ->
                        val corMicro = coresMicrociclos[idx % coresMicrociclos.size]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(corMicro.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(corMicro, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${micro.nome} (${micro.dataInicio} – ${micro.dataFim})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = corTextoPrincipal
                                )
                                if (micro.objetivoSemanal.isNotBlank()) {
                                    Text(
                                        text = micro.objetivoSemanal,
                                        fontSize = 11.sp,
                                        color = corTextoSecundario
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(corDestaque.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Macrociclo", fontSize = 11.sp, color = corTextoSecundario)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(corDestaque, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Mesociclo ativo", fontSize = 11.sp, color = corTextoSecundario)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoFormCiclo(
    tituloDialogo: String,
    nomeInicial: String,
    subtituloLabel: String,
    subtituloInicial: String,
    inicioInicial: String,
    fimInicial: String,
    onDismiss: () -> Unit,
    onSalvar: (String, String, String, String) -> Unit
) {
    var nome by remember { mutableStateOf(nomeInicial) }
    var subtitulo by remember { mutableStateOf(subtituloInicial) }
    var dataInicio by remember { mutableStateOf(inicioInicial) }
    var dataFim by remember { mutableStateOf(fimInicial) }
    var erroData by remember { mutableStateOf<String?>(null) }
    var abrirPickerInicio by remember { mutableStateOf(false) }
    var abrirPickerFim by remember { mutableStateOf(false) }

    if (abrirPickerInicio) {
        val stateInicio = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { abrirPickerInicio = false },
            confirmButton = {
                TextButton(onClick = {
                    stateInicio.selectedDateMillis?.let { millis ->
                        dataInicio = formatarDataUtcParaBr(millis)
                        erroData = null
                    }
                    abrirPickerInicio = false
                }) { Text("Selecionar") }
            },
            dismissButton = {
                TextButton(onClick = { abrirPickerInicio = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = stateInicio)
        }
    }

    if (abrirPickerFim) {
        val stateFim = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { abrirPickerFim = false },
            confirmButton = {
                TextButton(onClick = {
                    stateFim.selectedDateMillis?.let { millis ->
                        dataFim = formatarDataUtcParaBr(millis)
                        erroData = null
                    }
                    abrirPickerFim = false
                }) { Text("Selecionar") }
            },
            dismissButton = {
                TextButton(onClick = { abrirPickerFim = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = stateFim)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tituloDialogo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it; erroData = null },
                    label = { Text("Nome *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitulo,
                    onValueChange = { subtitulo = it },
                    label = { Text(subtituloLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { abrirPickerInicio = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dataInicio.isBlank()) "Selecionar data de início *" else "Início: $dataInicio"
                    )
                }
                OutlinedButton(
                    onClick = { abrirPickerFim = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (dataFim.isBlank()) "Selecionar data de término" else "Término: $dataFim"
                    )
                }
                if (erroData != null) {
                    Text(
                        text = erroData!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nome.isBlank()) {
                    erroData = "Informe o nome."
                    return@TextButton
                }
                val calInicio = parseDataBrParaCalendar(dataInicio)
                if (calInicio == null) {
                    erroData = "Selecione uma data de início válida."
                    return@TextButton
                }
                if (dataFim.isNotBlank()) {
                    val calFim = parseDataBrParaCalendar(dataFim)
                    if (calFim == null || !calFim.after(calInicio)) {
                        erroData = "A data de término deve ser maior que a data de início."
                        return@TextButton
                    }
                }
                onSalvar(nome, subtitulo, dataInicio, dataFim)
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Painel Visual Moderno do Macrociclo (substitui o formato de planilha por cards organizados por Semana, Microciclo, Dias/Treino e Variáveis)
@Composable
fun VisaoCompletaMacrociclo(
    macrociclo: MacrocicloEntity,
    periodizacaoRepository: PeriodizacaoRepository,
    treinosAluno: List<TreinoEntity>,
    onVisualizarTreino: (Long) -> Unit
) {
    val mesociclos by periodizacaoRepository
        .listarMesociclos(macrociclo.id)
        .collectAsState(initial = emptyList())
    if (mesociclos.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Painel Semanal • ${macrociclo.nome}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            mesociclos.forEach { meso ->
                val microciclos by periodizacaoRepository
                    .listarMicrociclos(meso.id)
                    .collectAsState(initial = emptyList())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FASE: ${meso.nome.uppercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (meso.foco.isNotBlank()) {
                                Text(
                                    text = meso.foco,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (microciclos.isEmpty()) {
                        Text(
                            text = "Nenhum microciclo nesta fase.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        microciclos.forEachIndexed { idxMicro, micro ->
                            val sessoes by periodizacaoRepository
                                .listarSessoes(micro.id)
                                .collectAsState(initial = emptyList())
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
// Cabeçalho Semana + Nome do Microciclo + Frequência
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF2E7D32), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "SEMANA ${idxMicro + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = micro.nome,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${sessoes.size}x/sem",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
// Chips visuais de Dias + Treino (substituindo as células amarelas da planilha)
                                    if (sessoes.isEmpty()) {
                                        Text(
                                            text = "Sem dias de treino definidos nesta semana.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sessoes.forEach { sessao ->
                                                val treinoVinc = treinosAluno.firstOrNull { it.id == sessao.treinoId }
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable(enabled = treinoVinc != null) {
                                                            treinoVinc?.let { onVisualizarTreino(it.id) }
                                                        },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFFFFB300).copy(alpha = 0.22f)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text(
                                                            text = sessao.diaSemana.take(3),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = sessao.titulo,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        if (treinoVinc != null) {
                                                            Text(
                                                                text = treinoVinc.nome,
                                                                fontSize = 9.sp,
                                                                maxLines = 1,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
// Variáveis e meta semanal
                                    if (micro.objetivoSemanal.isNotBlank()) {
                                        Text(
                                            text = "Variáveis: ${micro.objetivoSemanal}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}