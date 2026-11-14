package br.com.icaros.fitcare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.*
import kotlinx.coroutines.launch

private val DIAS_SEMANA_PERIODIZACAO = listOf(
    "SEGUNDA" to "Segunda-feira",
    "TERCA" to "Terça-feira",
    "QUARTA" to "Quarta-feira",
    "QUINTA" to "Quinta-feira",
    "SEXTA" to "Sexta-feira",
    "SABADO" to "Sábado",
    "DOMINGO" to "Domingo"
)

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

    // Estados de diálogos CRUD
    var editarMacro by remember { mutableStateOf<MacrocicloEntity?>(null) }
    var mostrarDialogoMacro by remember { mutableStateOf(false) }

    var editarMeso by remember { mutableStateOf<MesocicloEntity?>(null) }
    var mostrarDialogoMeso by remember { mutableStateOf(false) }

    var editarMicro by remember { mutableStateOf<MicrocicloEntity?>(null) }
    var mostrarDialogoMicro by remember { mutableStateOf(false) }

    var editarSessao by remember { mutableStateOf<SessaoPeriodizacaoEntity?>(null) }
    var mostrarDialogoSessao by remember { mutableStateOf(false) }

    var mensagemErro by remember { mutableStateOf("") }

    // Mantém referências selecionadas sincronizadas se excluídas
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                BotaoVoltarPadrao(
                    onVoltar = {
                        when {
                            microSelecionado != null -> microSelecionado = null
                            mesoSelecionado != null -> mesoSelecionado = null
                            macroSelecionado != null -> macroSelecionado = null
                            else -> onVoltar()
                        }
                    }
                )
            }
            Image(
                painter = painterResource(id = R.drawable.logo_fitcare),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(145.dp)
                    .height(70.dp)
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
                text = "Aluno: ${aluno?.nome ?: "Carregando..."} (Planejamento hierárquico opcional)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trilha de navegação (Breadcrumb hierárquico)
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
                                text = "Nenhum macrociclo cadastrado. A periodização é opcional — você pode criar um ciclo longo ou continuar usando os treinos convencionais.",
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
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir macrociclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para gerenciar os mesociclos ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                // NÍVEL 2: MESOCICLOS
                mesoSelecionado == null -> {
                    val macro = macroSelecionado!!

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
                            text = "2. Mesociclos (${mesociclos.size})",
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
                                text = "Nenhum mesociclo cadastrado em ${macro.nome}. Adicione fases como Adaptação, Hipertrofia ou Força.",
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
                                                    text = "Foco: ${meso.foco}",
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
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir mesociclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para gerenciar os microciclos ›",
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
                        microciclos.forEach { micro ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { microSelecionado = micro },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = micro.nome,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (micro.objetivoSemanal.isNotBlank()) {
                                                Text(
                                                    text = "Meta semanal: ${micro.objetivoSemanal}",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "Semana: ${micro.dataInicio}" +
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
                                                } catch (e: Exception) {
                                                    mensagemErro = e.message ?: "Erro ao excluir microciclo."
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    Text(
                                        text = "Toque para organizar as sessões da semana ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
// NÍVEL 4: SESSÕES POR DIAS DA SEMANA VINCULADAS A TREINOS
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
                                        text = "Descanso / nenhuma sessão vinculada.",
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
                                                                text = "Ficha vinculada: ${treinoVinculado.nome}",
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        if (sessao.observacoes.isNotBlank()) {
                                                            Text(
                                                                text = sessao.observacoes,
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
                                                        Text("Abrir treino e exercícios vinculados")
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
// DIÁLOGO MACROCICLO
    if (mostrarDialogoMacro) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMacro == null) "Novo Macrociclo" else "Editar Macrociclo",
            nomeInicial = editarMacro?.nome ?: "",
            subtituloLabel = "Objetivo geral",
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
                        mensagemErro = ""
                        mostrarDialogoMacro = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar macrociclo."
                    }
                }
            }
        )
    }
// DIÁLOGO MESOCICLO
    if (mostrarDialogoMeso && macroSelecionado != null) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMeso == null) "Novo Mesociclo" else "Editar Mesociclo",
            nomeInicial = editarMeso?.nome ?: "",
            subtituloLabel = "Foco (ex.: Adaptação, Hipertrofia, Força)",
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
                        mensagemErro = ""
                        mostrarDialogoMeso = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar mesociclo."
                    }
                }
            }
        )
    }
// DIÁLOGO MICROCICLO
    if (mostrarDialogoMicro && mesoSelecionado != null) {
        DialogoFormCiclo(
            tituloDialogo = if (editarMicro == null) "Novo Microciclo" else "Editar Microciclo",
            nomeInicial = editarMicro?.nome ?: "",
            subtituloLabel = "Objetivo semanal",
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
                        mensagemErro = ""
                        mostrarDialogoMicro = false
                    } catch (e: Exception) {
                        mensagemErro = e.message ?: "Erro ao salvar microciclo."
                    }
                }
            }
        )
    }
// DIÁLOGO SESSÃO DA SEMANA
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
                                        text = rotulo.substringBefore("-"),
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
                        label = { Text("Título da sessão *") },
                        placeholder = { Text("Ex.: Sessão A - Inferiores") },
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
                        label = { Text("Observações da sessão") },
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tituloDialogo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
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
                OutlinedTextField(
                    value = dataInicio,
                    onValueChange = { dataInicio = it },
                    label = { Text("Data de início * (dd/MM/aaaa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dataFim,
                    onValueChange = { dataFim = it },
                    label = { Text("Data de término (dd/MM/aaaa)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSalvar(nome, subtitulo, dataInicio, dataFim) }) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Visão geral completa: ${macrociclo.nome}",
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Mesociclo: ${meso.nome}" +
                                if (meso.foco.isNotBlank()) " (${meso.foco})" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (microciclos.isEmpty()) {
                        Text(
                            text = "Nenhum microciclo neste mesociclo.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        microciclos.forEach { micro ->
                            val sessoes by periodizacaoRepository
                                .listarSessoes(micro.id)
                                .collectAsState(initial = emptyList())

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "• Microciclo: ${micro.nome} (${micro.dataInicio})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (sessoes.isEmpty()) {
                                    Text(
                                        text = "   Sem sessões semanais cadastradas.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    sessoes.forEach { sessao ->
                                        val treinoVinc = treinosAluno.firstOrNull { it.id == sessao.treinoId }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 12.dp)
                                                .clickable(enabled = treinoVinc != null) {
                                                    treinoVinc?.let { onVisualizarTreino(it.id) }
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${sessao.diaSemana}: ${sessao.titulo}" +
                                                        if (treinoVinc != null) " → ${treinoVinc.nome}" else "",
                                                fontSize = 11.sp,
                                                color = if (treinoVinc != null) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
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
}
