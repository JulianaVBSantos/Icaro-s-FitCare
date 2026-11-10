
package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import br.com.icaros.fitcare.data.AnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
import br.com.icaros.fitcare.data.AnamneseRepository
import br.com.icaros.fitcare.data.DocumentoAnamneseEntity
import br.com.icaros.fitcare.data.PerguntaAnamneseAtribuidaEntity
import br.com.icaros.fitcare.data.RespostaAnamneseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.util.Locale

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

    val idAluno = alunoId

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

    var mensagemDocumento by remember {
        mutableStateOf("")
    }

    val salvamentosPendentes = remember {
        mutableMapOf<Long, Job>()
    }

    val anamneseSelecionada = anamneses.firstOrNull {
        it.id == anamneseSelecionadaId
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

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF002B5C).copy(alpha = 0.10f)
            ) {
                Text(
                    text = "PERFIL: ALUNO",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    ),
                    color = Color(0xFF002B5C),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

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

                // FORMULÁRIO DA ANAMNESE

                Text(
                    text = anamneseSelecionada?.nomeModelo
                        ?: "Preenchimento da anamnese",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF002B5C)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        scope.launch {
                            aguardarSalvamentos()
                            anamneseSelecionadaId = null
                            mensagemErro = ""
                        }
                    }
                ) {
                    Text("VOLTAR ÀS ANAMNESES")
                }

                if (carregando) {
                    CircularProgressIndicator()
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
                                            delay(300)

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
                                    respostas[pergunta.id].isNullOrBlank()
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

                                    respostaRepository.concluirDoAluno(
                                        alunoId = idAluno,
                                        anamneseId = anamneseSelecionadaId!!
                                    )

                                    anamneseSelecionadaId = null
                                    perguntas = emptyList()
                                    respostas = emptyMap()

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

                    respondidas.forEach { anamnese ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Text(
                                    text = anamnese.nomeModelo,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Concluída",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
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

// CAMPO DE RESPOSTA DA ANAMNESE
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
        } catch (e: Exception) {
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
                        if (
                            novo.matches(
                                Regex("[0-9]*([.,][0-9]*)?")
                            )
                        ) {
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
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    alternativas.forEach { opcao ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = valor == opcao,
                                    onClick = { onChange(opcao) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 3.dp),
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

            "CHECKBOX" -> {
                val selecionadas = remember(valor) {
                    try {
                        val json = JSONArray(valor)

                        (0 until json.length()).map {
                            json.getString(it)
                        }.toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                }

                alternativas.forEach { opcao ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val novas = selecionadas.toMutableSet()

                                if (opcao in novas) {
                                    novas.remove(opcao)
                                } else {
                                    novas.add(opcao)
                                }

                                val json = JSONArray()
                                novas.forEach { json.put(it) }

                                onChange(json.toString())
                            }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = opcao in selecionadas,
                            onCheckedChange = { marcado ->
                                val novas = selecionadas.toMutableSet()

                                if (marcado) {
                                    novas.add(opcao)
                                } else {
                                    novas.remove(opcao)
                                }

                                val json = JSONArray()
                                novas.forEach { json.put(it) }

                                onChange(json.toString())
                            }
                        )

                        Text(
                            text = opcao,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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