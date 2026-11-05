package br.com.icaros.fitcare

import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.BarChart
import kotlinx.coroutines.delay
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.flow.first
import androidx.compose.ui.platform.LocalContext
import br.com.icaros.fitcare.data.TreinoEntity
import br.com.icaros.fitcare.data.ExercicioEntity
import androidx.compose.ui.text.style.TextAlign
import br.com.icaros.fitcare.data.TreinoRepository
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import android.database.sqlite.SQLiteConstraintException
import br.com.icaros.fitcare.data.AlunoRepository
import br.com.icaros.fitcare.data.FitCareDatabase
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import android.os.Bundle
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource

// Nova Paleta de Cores baseada na Logo
private val AzulPrincipal = Color(0xFF002B5C)
private val AzulSecundario = Color(0xFF164A73)
private val AzulClaro = Color(0xFF5B9BD5)
private val AzulMuitoClaro = Color(0xFFEAF3F9)
private val FundoApp = Color(0xFFF5F9FC)
private val TextoPrincipal = Color(0xFF002B5C)
private val TextoSecundario = Color(0xFF526579)
private val Branco = Color(0xFFFFFFFF)

// Mantido apenas para alertas/erros se necessário
private val Vermelho = Color(0xFFE34B63)

// Cores da nova tela de Login e Cabeçalhos (Ajustadas para dar contraste à logo)
private val LoginTopo = Color(0xFFEAF3F9)         // Azul muito claro para clarear o topo
private val LoginBase = Color(0xFFFFFFFF)         // Fundo limpo e branco na base da tela
private val LoginCampo = Color(0xFFFFFFFF)        // Campos de texto brancos para contraste
private val LoginCampoBorda = Color(0xFF5B9BD5)   // Borda em Azul claro
private val LoginTexto = Color(0xFF002B5C)        // Título principal em Azul escuro
private val LoginTextoSecundario = Color(0xFF526579) // Texto secundário legível
private val LoginPlaceholder = Color(0xFF858AA1)
private val LoginAzul = Color(0xFF002B5C)         // Botão ENTRAR agora usa o azul principal
private val LoginAzulClaro = Color(0xFF164A73)    // Links em Azul secundário

// Cores para os cabeçalhos das outras telas
private val CabecalhoTopoClaro = Color(0xFFEAF3F9) // Fundo claro para destacar a logo escura
private val CabecalhoBaseClaro = Color(0xFFF5F9FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = android.graphics.Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        val database = FitCareDatabase.getDatabase(applicationContext)
        val repository = AlunoRepository(database.alunoDao())
        val treinoRepository = TreinoRepository(database.treinoDao())

        setContent {
            FitCareApp(
                repository = repository,
                treinoRepository = treinoRepository
            )
        }
    }
}

// Mapeamento das telas do aplicativo
enum class AppScreen {
    Login,
    Dashboard,
    Alunos,
    CadastroAluno,
    PerfilAluno,
    LinhaDoTempoAluno,
    RegistroEventoAluno,
    EditarAluno,
    Treinos,
    SelecionarAlunoTreino,
    FormularioTreino
}

// Fluxo de navegação principal da aplicação
@Composable
fun FitCareApp(repository: AlunoRepository, treinoRepository: TreinoRepository) {
    var tela by remember { mutableStateOf<AppScreen>(AppScreen.Login) }
    var alunoSelecionadoId by remember { mutableStateOf<Long?>(null) }
    var treinoSelecionadoId by remember { mutableStateOf<Long?>(null) }
    var registrarNovoTreino by remember { mutableStateOf<Boolean>(false) }
    var eventoEmEdicao by remember { mutableStateOf<EventoAluno?>(null) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AzulPrincipal,
            secondary = AzulSecundario,
            background = FundoApp,
            surface = Branco,
            onBackground = TextoPrincipal,
            onSurface = TextoPrincipal
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = FundoApp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FundoApp)
            ) {
                when (tela) {
                    AppScreen.Login -> {
                        TelaLogin(onEntrar = { tela = AppScreen.Dashboard })
                    }

                    AppScreen.Dashboard -> {
                        TelaDashboard(
                            repository = repository,
                            treinoRepository = treinoRepository,
                            onAlunos = {
                                tela = AppScreen.Alunos
                            },
                            onCadastrarAluno = {
                                tela = AppScreen.CadastroAluno
                            },
                            onTreinos = {
                                registrarNovoTreino = false
                                treinoSelecionadoId = null
                                tela = AppScreen.Treinos
                            },
                            onRegistrarTreino = {
                                registrarNovoTreino = true
                                treinoSelecionadoId = null
                                tela = AppScreen.SelecionarAlunoTreino
                            },
                            onPerfil = { aluno ->
                                alunoSelecionadoId = aluno.id
                                tela = AppScreen.PerfilAluno
                            },
                            onSair = {
                                tela = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.CadastroAluno -> {
                        TelaCadastroAluno(
                            repository = repository,
                            onVoltar = {
                                tela = AppScreen.Alunos
                            },
                            onCadastroConcluido = {
                                tela = AppScreen.Alunos
                            },
                            onTreinos = {
                                tela = AppScreen.Treinos
                            }
                        )
                    }

                    AppScreen.Alunos -> {
                        TelaAlunos(
                            repository = repository,
                            onVoltar = { tela = AppScreen.Dashboard },
                            onSelecionarAluno = { aluno ->
                                alunoSelecionadoId = aluno.id

                                if (registrarNovoTreino) {
                                    tela = AppScreen.FormularioTreino
                                } else {
                                    tela = AppScreen.PerfilAluno
                                }
                            },
                            onTreinos = { tela = AppScreen.Treinos },
                            onCadastrarAluno = {
                                tela = AppScreen.CadastroAluno
                            }
                        )
                    }

                    AppScreen.PerfilAluno -> {
                        alunoSelecionadoId?.let { id ->
                            TelaPerfilAluno(
                                alunoId = id,
                                repository = repository,
                                onVoltar = { tela = AppScreen.Alunos },
                                onDashboard = { tela = AppScreen.Dashboard },
                                onTreinos = { tela = AppScreen.Treinos },
                                onEditar = { tela = AppScreen.EditarAluno },
                                onLinhaDoTempo = { tela = AppScreen.LinhaDoTempoAluno }
                            )
                        }
                    }

                    AppScreen.LinhaDoTempoAluno -> {
                        alunoSelecionadoId?.let { id ->
                            TelaLinhaDoTempoAluno(
                                alunoId = id,
                                repository = repository,

                                onVoltar = {
                                    tela = AppScreen.PerfilAluno
                                },

                                onNovoRegistro = {
                                    eventoEmEdicao = null
                                    tela = AppScreen.RegistroEventoAluno
                                },

                                onEditarRegistro = { evento ->
                                    eventoEmEdicao = evento
                                    tela = AppScreen.RegistroEventoAluno
                                },

                                onDashboard = {
                                    tela = AppScreen.Dashboard
                                },

                                onAlunos = {
                                    tela = AppScreen.Alunos
                                },

                                onTreinos = {
                                    tela = AppScreen.Treinos
                                },

                                onRegistros = {},

                                onMais = {}
                            )
                        }
                    }

                    AppScreen.RegistroEventoAluno -> {
                        alunoSelecionadoId?.let { id ->
                            TelaRegistroEventoAluno(
                                alunoId = id,
                                eventoParaEditar = eventoEmEdicao,
                                onVoltar = {
                                    eventoEmEdicao = null
                                    tela = AppScreen.LinhaDoTempoAluno
                                },
                                onSalvo = {
                                    eventoEmEdicao = null
                                    tela = AppScreen.LinhaDoTempoAluno
                                }
                            )
                        }
                    }

                    AppScreen.EditarAluno -> {
                        alunoSelecionadoId?.let { id ->
                            TelaEditarAluno(
                                alunoId = id,
                                repository = repository,
                                onVoltar = {
                                    tela = AppScreen.PerfilAluno
                                },
                                onSalvo = {
                                    tela = AppScreen.PerfilAluno
                                },
                                onDashboard = {
                                    tela = AppScreen.Dashboard
                                },
                                onAlunos = {
                                    tela = AppScreen.Alunos
                                },
                                onTreinos = {
                                    tela = AppScreen.Treinos
                                }
                            )
                        }
                    }

                    AppScreen.Treinos -> {
                        TelaTreinos(
                            repository = repository,
                            treinoRepository = treinoRepository,
                            onVoltar = {
                                tela = AppScreen.Dashboard
                            },
                            onAlunos = {
                                tela = AppScreen.Alunos
                            },
                            onSelecionarTreino = { treino ->
                                alunoSelecionadoId = treino.alunoId
                                treinoSelecionadoId = treino.id
                                registrarNovoTreino = false
                                tela = AppScreen.FormularioTreino
                            },
                            onNovoTreino = {
                                registrarNovoTreino = true
                                treinoSelecionadoId = null
                                tela = AppScreen.SelecionarAlunoTreino
                            }
                        )
                    }

                    AppScreen.SelecionarAlunoTreino -> {
                        TelaAlunos(
                            repository = repository,
                            modoSelecaoTreino = true,
                            onVoltar = {
                                registrarNovoTreino = false
                                tela = AppScreen.Dashboard
                            },
                            onSelecionarAluno = { aluno ->
                                alunoSelecionadoId = aluno.id
                                treinoSelecionadoId = null
                                tela = AppScreen.FormularioTreino
                            },
                            onTreinos = {
                                registrarNovoTreino = false
                                tela = AppScreen.Treinos
                            }
                        )
                    }

                    AppScreen.FormularioTreino -> {
                        val alunoId = alunoSelecionadoId

                        if (alunoId != null) {
                            TelaFormularioTreino(
                                alunoId = alunoId,
                                treinoId = treinoSelecionadoId,
                                treinoRepository = treinoRepository,
                                onVoltar = {
                                    if (treinoSelecionadoId != null) {
                                        tela = AppScreen.Treinos
                                    } else {
                                        tela = AppScreen.SelecionarAlunoTreino
                                    }
                                },
                                onSalvo = {
                                    registrarNovoTreino = false
                                    treinoSelecionadoId = null
                                    tela = AppScreen.Treinos
                                }
                            )
                        } else {
                            registrarNovoTreino = false
                            tela = AppScreen.Alunos
                        }
                    }
                }


            }
        }
    }
}


@Composable
fun BotaoVoltarPadrao(
    onVoltar: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onVoltar,
        enabled = enabled,
        modifier = modifier.size(44.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AzulPrincipal),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AzulPrincipal
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Voltar",
            tint = AzulPrincipal,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun BotaoSairPadrao(
    onSair: () -> Unit
) {
    OutlinedButton(
        onClick = onSair,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFFD32F2F)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp)
    ) {
        Text(
            text = "SAIR",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD32F2F)
        )
    }
}

// tela cadastro de aluno
@Composable
fun TelaCadastroAluno(
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onCadastroConcluido: () -> Unit,
    onTreinos: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var observacoesProfissionais by remember { mutableStateOf("") }

    val opcoesCondicoes = listOf(
        "Hipertensão",
        "Diabetes",
        "Dor lombar",
        "Dor no joelho",
        "Dor no ombro",
        "Problema cardíaco"
    )

    val opcoesRestricoes = listOf(
        "Evitar exercícios de alto impacto",
        "Evitar agachamentos profundos",
        "Limitar cargas elevadas",
        "Evitar exercícios acima da cabeça",
        "Limitar amplitude de movimento"
    )

    var condicoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var restricoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var outraCondicao by remember {
        mutableStateOf("")
    }

    var outraRestricao by remember {
        mutableStateOf("")
    }

    var erro by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val requesterOutraCondicao = remember {
        BringIntoViewRequester()
    }

    val requesterOutraRestricao = remember {
        BringIntoViewRequester()
    }

    val requesterObservacoes = remember {
        BringIntoViewRequester()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

// Cabeçalho padrão da tela
        CabecalhoPadrao(
            titulo = "Cadastrar aluno",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Dados do aluno",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = objective,
                onValueChange = { objective = it },
                label = { Text("Objetivo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Condições",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Selecione as condições que se aplicam ao aluno.",
                fontSize = 12.sp,
                color = TextoSecundario
            )

            opcoesCondicoes.forEach { condicao ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            condicoesSelecionadas =
                                if (condicoesSelecionadas.contains(condicao)) {
                                    condicoesSelecionadas - condicao
                                } else {
                                    condicoesSelecionadas + condicao
                                }
                        }
                        .padding(vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = condicoesSelecionadas.contains(condicao),
                        onCheckedChange = { marcado ->
                            condicoesSelecionadas =
                                if (marcado) {
                                    condicoesSelecionadas + condicao
                                } else {
                                    condicoesSelecionadas - condicao
                                }
                        }
                    )

                    Text(
                        text = condicao,
                        fontSize = 14.sp,
                        color = TextoPrincipal
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        condicoesSelecionadas =
                            if (condicoesSelecionadas.contains("Outro")) {
                                condicoesSelecionadas - "Outro"
                            } else {
                                condicoesSelecionadas + "Outro"
                            }
                    }
                    .padding(vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = condicoesSelecionadas.contains("Outro"),
                    onCheckedChange = { marcado ->
                        condicoesSelecionadas =
                            if (marcado) {
                                condicoesSelecionadas + "Outro"
                            } else {
                                condicoesSelecionadas - "Outro"
                            }
                    }
                )

                Text(
                    text = "Outro",
                    fontSize = 14.sp,
                    color = TextoPrincipal
                )
            }

            if (condicoesSelecionadas.contains("Outro")) {

                OutlinedTextField(
                    value = outraCondicao,
                    onValueChange = {
                        outraCondicao = it
                    },
                    label = {
                        Text("Descreva outra condição")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(
                            requesterOutraCondicao
                        )
                        .onFocusChanged { estado ->
                            if (estado.isFocused) {
                                scope.launch {
                                    requesterOutraCondicao
                                        .bringIntoView()
                                }
                            }
                        },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Restrições",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Selecione as restrições que devem ser consideradas durante os treinos.",
                fontSize = 12.sp,
                color = TextoSecundario
            )

            opcoesRestricoes.forEach { restricao ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            restricoesSelecionadas =
                                if (restricoesSelecionadas.contains(restricao)) {
                                    restricoesSelecionadas - restricao
                                } else {
                                    restricoesSelecionadas + restricao
                                }
                        }
                        .padding(vertical = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = restricoesSelecionadas.contains(restricao),
                        onCheckedChange = { marcado ->
                            restricoesSelecionadas =
                                if (marcado) {
                                    restricoesSelecionadas + restricao
                                } else {
                                    restricoesSelecionadas - restricao
                                }
                        }
                    )

                    Text(
                        text = restricao,
                        fontSize = 14.sp,
                        color = TextoPrincipal
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        restricoesSelecionadas =
                            if (restricoesSelecionadas.contains("Outro")) {
                                restricoesSelecionadas - "Outro"
                            } else {
                                restricoesSelecionadas + "Outro"
                            }
                    }
                    .padding(vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = restricoesSelecionadas.contains("Outro"),
                    onCheckedChange = { marcado ->
                        restricoesSelecionadas =
                            if (marcado) {
                                restricoesSelecionadas + "Outro"
                            } else {
                                restricoesSelecionadas - "Outro"
                            }
                    }
                )

                Text(
                    text = "Outro",
                    fontSize = 14.sp,
                    color = TextoPrincipal
                )
            }

            if (restricoesSelecionadas.contains("Outro")) {

                OutlinedTextField(
                    value = outraRestricao,
                    onValueChange = {
                        outraRestricao = it
                    },
                    label = {
                        Text("Descreva outra restrição")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(
                            requesterOutraRestricao
                        )
                        .onFocusChanged { estado ->
                            if (estado.isFocused) {
                                scope.launch {
                                    requesterOutraRestricao
                                        .bringIntoView()
                                }
                            }
                        },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            OutlinedTextField(
                value = observacoesProfissionais,
                onValueChange = {
                    observacoesProfissionais = it
                },
                label = {
                    Text("Observações profissionais")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(
                        requesterObservacoes
                    )
                    .onFocusChanged { estado ->
                        if (estado.isFocused) {
                            scope.launch {
                                requesterObservacoes
                                    .bringIntoView()
                            }
                        }
                    },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp)
            )

            if (erro.isNotBlank()) {
                Text(
                    text = erro,
                    color = Vermelho,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {

                    if (nome.isBlank()) {
                        erro = "Informe o nome do aluno."
                        return@Button
                    }
                    if (
                        condicoesSelecionadas.contains("Outro") &&
                        outraCondicao.isBlank()
                    ) {
                        erro = "Descreva a outra condição selecionada."
                        return@Button
                    }

                    if (
                        restricoesSelecionadas.contains("Outro") &&
                        outraRestricao.isBlank()
                    ) {
                        erro = "Descreva a outra restrição selecionada."
                        return@Button
                    }
                    erro = ""

                    scope.launch {
                        salvando = true

                        try {

                            val dataAtual = SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale("pt", "BR")
                            ).format(Date())

                            val condicoesSalvas =
                                condicoesSelecionadas
                                    .filter { it != "Outro" }
                                    .toMutableList()

                            if (
                                condicoesSelecionadas.contains("Outro") &&
                                outraCondicao.isNotBlank()
                            ) {
                                condicoesSalvas.add(
                                    outraCondicao.trim()
                                )
                            }

                            val restricoesSalvas =
                                restricoesSelecionadas
                                    .filter { it != "Outro" }
                                    .toMutableList()

                            if (
                                restricoesSelecionadas.contains("Outro") &&
                                outraRestricao.isNotBlank()
                            ) {
                                restricoesSalvas.add(
                                    outraRestricao.trim()
                                )
                            }

                            val condicoesTexto =
                                condicoesSalvas.joinToString(", ")

                            val restricoesTexto =
                                restricoesSalvas.joinToString(", ")

                            val status = calcularStatusAcompanhamento(
                                condicoes = condicoesTexto,
                                restricoes = restricoesTexto
                            )

                            val aluno =
                                br.com.icaros.fitcare.data.AlunoEntity(
                                    nome = nome.trim(),
                                    email = email.trim(),
                                    dataCadastro = dataAtual,
                                    objetivo = objective.trim(),
                                    observacoesProfissionais =
                                        observacoesProfissionais.trim(),
                                    condicoes = condicoesTexto,
                                    restricoes = restricoesTexto,
                                    status = status
                                )

                            repository.inserir(aluno)

                            onCadastroConcluido()

                        } catch (e: Exception) {

                            erro =
                                "Não foi possível cadastrar o aluno: " +
                                        (e.message
                                            ?: "erro desconhecido")

                        } finally {
                            salvando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !salvando,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPrincipal
                )
            ) {
                Text(
                    text = if (salvando) {
                        "Salvando..."
                    } else {
                        "Cadastrar aluno"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        DashboardBottomBar(
            telaAtual = AppScreen.CadastroAluno,
            onInicio = onVoltar,
            onAlunos = onCadastroConcluido,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
    }
}

// Componente de Cartão customizado reútilizavel
@Composable
private fun Cartao(
    modifier: Modifier = Modifier,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Branco),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            conteudo()
        }
    }
}

// Tela de Login
@Composable
fun TelaLogin(onEntrar: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        LoginTopo,
                        LoginBase
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_fitcare),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .width(220.dp)
                    .height(145.dp)
            )
            Spacer(modifier = Modifier.height(34.dp))
            Text(
                text = "Bem-vindo de volta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = LoginTexto
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Acesse sua conta para gerenciar seus alunos.",
                fontSize = 14.sp,
                color = LoginTextoSecundario
            )
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                singleLine = true,
                placeholder = { Text(text = "E-mail", color = LoginPlaceholder) },
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LoginCampo,
                    unfocusedContainerColor = LoginCampo,
                    focusedBorderColor = LoginAzul,
                    unfocusedBorderColor = LoginCampoBorda,
                    focusedTextColor = LoginTexto,
                    unfocusedTextColor = LoginTexto
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                singleLine = true,
                placeholder = { Text(text = "Senha", color = LoginPlaceholder) },
                trailingIcon = {
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                        Icon(
                            imageVector = if (senhaVisivel) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (senhaVisivel) "Esconder senha" else "Mostrar senha",
                            tint = LoginPlaceholder,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LoginCampo,
                    unfocusedContainerColor = LoginCampo,
                    focusedBorderColor = LoginAzul,
                    unfocusedBorderColor = LoginCampoBorda,
                    focusedTextColor = LoginTexto,
                    unfocusedTextColor = LoginTexto
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onEntrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoginAzul)
            ) {
                Text(
                    text = "ENTRAR",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Esqueci minha senha",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LoginAzulClaro,
                modifier = Modifier.clickable {})
        }
    }
}

// Tela de Dashboard principal
@Composable
fun TelaDashboard(
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    onAlunos: () -> Unit,
    onCadastrarAluno: () -> Unit,
    onTreinos: () -> Unit,
    onRegistrarTreino: () -> Unit,
    onPerfil: (br.com.icaros.fitcare.data.AlunoEntity) -> Unit,
    onSair: () -> Unit
) {
    var pesquisa by remember { mutableStateOf("") }
    val alunos by repository
        .listarAlunos()
        .collectAsState(initial = emptyList())

    val totalAlunos = alunos.size

    val alunosEmAtencao = alunos.count { aluno ->
        aluno.status == "Atenção"
    }

    val alunosEmDia = alunos.count { aluno ->
        aluno.status == "Normal"
    }

    val alunosFiltrados = alunos.filter { aluno ->
        aluno.nome.contains(pesquisa.trim(), ignoreCase = true)
    }
    val treinos by treinoRepository
        .listarTodosTreinos()
        .collectAsState(initial = emptyList())
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

// Cabeçalho claro padronizado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CabecalhoTopoClaro,
                                CabecalhoBaseClaro
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.logo_fitcare
                        ),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                    )

                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        BotaoSairPadrao(
                            onSair = onSair
                        )
                    }
                }
            }

// Área clara de conteúdo do Dashboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "Acompanhe seus alunos e seus progressos.",
                    color = TextoPrincipal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = { pesquisa = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Buscar aluno...",
                            color = TextoSecundario,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "⌕",
                            color = TextoSecundario,
                            fontSize = 25.sp
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "☷",
                            color = TextoSecundario,
                            fontSize = 20.sp
                        )
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro,
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "Total de alunos",
                        valor = totalAlunos.toString(),
                        corFundo = AzulMuitoClaro,
                        corTexto = AzulPrincipal
                    )

                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "Em atenção",
                        valor = alunosEmAtencao.toString(),
                        corFundo = Color(0xFFFFEAB5),
                        corTexto = Color(0xFFB17B00)
                    )

                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "Em dia",
                        valor = alunosEmDia.toString(),
                        corFundo = Color(0xFFD8F3DF),
                        corTexto = Color(0xFF36844A)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Ações rápidas",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardAcao(
                        modifier = Modifier.weight(1f),
                        titulo = "Cadastrar aluno",
                        subtitulo = "Adicionar novo aluno",
                        corFundo = AzulMuitoClaro,
                        corTexto = AzulPrincipal,
                        icone = "+",
                        onClick = onCadastrarAluno
                    )
                    DashboardAcao(
                        modifier = Modifier.weight(1f),
                        titulo = "Consultar alunos",
                        subtitulo = "Ver lista e filtros",
                        corFundo = Color(0xFFDCEBFF),
                        corTexto = Color(0xFF416C93),
                        icone = "⌕",
                        onClick = onAlunos
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardAcao(
                        modifier = Modifier.weight(1f),
                        titulo = "Registrar treino",
                        subtitulo = "Frequência e feedback",
                        corFundo = Color(0xFFDDF4E3),
                        corTexto = Color(0xFF39754A),
                        icone = "✓",
                        onClick = onRegistrarTreino
                    )
                    DashboardAcao(
                        modifier = Modifier.weight(1f),
                        titulo = "Central de Atenção",
                        subtitulo = "Ver casos prioritários",
                        corFundo = Color(0xFFFFD9DC),
                        corTexto = Color(0xFF9C4350),
                        icone = "!",
                        onClick = onAlunos
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimos alunos acessados",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Ver todos ›",
                        fontSize = 13.sp,
                        color = AzulPrincipal,
                        modifier = Modifier.clickable { onAlunos() })
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (alunosFiltrados.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nenhum aluno encontrado.",
                                color = TextoPrincipal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (pesquisa.isBlank()) "Não há alunos cadastrados." else "Tente buscar por outro nome.",
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    alunosFiltrados.forEach { aluno ->
                        DashboardAluno(
                            nome = aluno.nome,
                            treino = "Objetivo: ${aluno.objetivo}",
                            status = aluno.status,
                            statusCor = when (aluno.status) {
                                "Prioridade" -> Color(0xFFFFD9DC)
                                "Atenção" -> Color(0xFFFFEAB5)
                                else -> Color(0xFFD8F3DF)
                            },
                            statusTextoCor = when (aluno.status) {
                                "Prioridade" -> Color(0xFFB42332)
                                "Atenção" -> Color(0xFF9A7000)
                                else -> Color(0xFF36844A)
                            },
                            onClick = { onPerfil(aluno) }
                        )
                    }
                }
            }
        }
        DashboardBottomBar(
            telaAtual = AppScreen.Dashboard,
            onInicio = {},
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = onAlunos,
            onMais = onSair
        )
    }
}

// Indicadores do topo do dashboard
@Composable
private fun DashboardIndicador(
    modifier: Modifier = Modifier,
    titulo: String,
    valor: String,
    corFundo: Color,
    corTexto: Color
) {
    Box(
        modifier = modifier
            .height(94.dp)
            .background(color = corFundo, shape = RoundedCornerShape(13.dp))
            .padding(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = titulo,
                color = corTexto.copy(alpha = 0.85f),
                fontSize = 11.sp,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = valor,
                color = corTexto,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Botões das ações rápidas
@Composable
private fun DashboardAcao(
    modifier: Modifier = Modifier,
    titulo: String,
    subtitulo: String,
    corFundo: Color,
    corTexto: Color,
    icone: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(104.dp)
            .background(color = corFundo, shape = RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icone,
                color = corTexto,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = titulo,
            color = corTexto,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = subtitulo,
            color = corTexto.copy(alpha = 0.85f),
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2
        )
    }
}

// Elemento individual da listagem rápida de alunos
@Composable
private fun DashboardAluno(
    nome: String,
    treino: String,
    status: String,
    statusCor: Color,
    statusTextoCor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFE9E7EC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nome.split(" ").take(2).mapNotNull { it.firstOrNull() }
                        .joinToString(""),
                    color = Color(0xFF706A75),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nome,
                    color = TextoPrincipal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = treino, color = TextoSecundario, fontSize = 11.sp)
            }
            Box(
                modifier = Modifier
                    .background(color = statusCor, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(
                    text = status,
                    color = statusTextoCor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "›", color = Color(0xFF77717F), fontSize = 25.sp)
        }
    }
}

// Tela de Lista Completa de Alunos com filtros
@Composable
fun TelaAlunos(
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onSelecionarAluno: (br.com.icaros.fitcare.data.AlunoEntity) -> Unit,
    onTreinos: () -> Unit,
    modoSelecaoTreino: Boolean = false,
    onCadastrarAluno: () -> Unit = {}
) {
    var pesquisa by remember { mutableStateOf("") }
    var filtroSelecionado by remember { mutableStateOf("Todos") }
    var filtroAcompanhamento by remember { mutableStateOf("Todos") }

    val alunos by repository
        .listarAlunos()
        .collectAsState(initial = emptyList())

    val termo = pesquisa.trim()

    // Filtragem dos alunos
    val filtrados = alunos.filter { aluno ->

        val correspondeAcompanhamento = when (filtroAcompanhamento) {
            "Com restrições" -> aluno.restricoes.isNotBlank()
            "Com observações" -> aluno.observacoesProfissionais.isNotBlank()
            else -> true
        }

        val correspondeBusca = termo.isBlank() ||
                aluno.nome.contains(termo, ignoreCase = true) ||
                aluno.email.contains(termo, ignoreCase = true) ||
                aluno.id.toString().contains(termo, ignoreCase = true) ||
                aluno.dataCadastro.contains(termo, ignoreCase = true) ||
                aluno.objetivo.contains(termo, ignoreCase = true) ||
                aluno.observacoesProfissionais.contains(termo, ignoreCase = true) ||
                aluno.condicoes.contains(termo, ignoreCase = true) ||
                aluno.restricoes.contains(termo, ignoreCase = true)

        val correspondeStatus =
            filtroSelecionado == "Todos" ||
                    aluno.status == filtroSelecionado

        correspondeBusca &&
                correspondeStatus &&
                correspondeAcompanhamento
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // Conteúdo rolável da tela
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            // Cabeçalho com logo e botão voltar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CabecalhoTopoClaro,
                                CabecalhoBaseClaro
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(top = 4.dp, bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.logo_fitcare
                        ),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                    )

                    // Botão voltar à esquerda
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        BotaoVoltarPadrao(
                            onVoltar = onVoltar
                        )
                    }
                }
            }

            // Conteúdo da listagem
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {

                // Título e botão de cadastro
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (modoSelecaoTreino) {
                            "Selecione o aluno"
                        } else {
                            "Meus alunos"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    if (!modoSelecaoTreino) {
                        IconButton(
                            onClick = onCadastrarAluno,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = AzulPrincipal,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Adicionar novo aluno",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Campo de pesquisa
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = { pesquisa = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Buscar aluno pelo nome...",
                            color = TextoSecundario,
                            fontSize = 14.sp
                        )
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filtros de status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Todos",
                        "Normal",
                        "Atenção",
                        "Prioridade"
                    ).forEach { tag ->

                        val selecionado = filtroSelecionado == tag

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selecionado) {
                                        AzulPrincipal
                                    } else {
                                        Color.White
                                    },
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    filtroSelecionado = tag
                                }
                                .padding(
                                    horizontal = 18.dp,
                                    vertical = 8.dp
                                )
                        ) {
                            Text(
                                text = tag,
                                color = if (selecionado) {
                                    Color.White
                                } else {
                                    TextoPrincipal
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Filtros de acompanhamento
                Text(
                    text = "Acompanhamento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoPrincipal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Todos",
                        "Com restrições",
                        "Com observações"
                    ).forEach { filtro ->

                        val selecionado =
                            filtroAcompanhamento == filtro

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selecionado) {
                                        AzulMuitoClaro
                                    } else {
                                        Branco
                                    }
                                )
                                .clickable {
                                    filtroAcompanhamento = filtro
                                }
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 12.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filtro,
                                fontSize = 11.sp,
                                color = if (selecionado) {
                                    AzulPrincipal
                                } else {
                                    TextoPrincipal
                                },
                                fontWeight = if (selecionado) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Resultado da filtragem
                if (filtrados.isEmpty()) {

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Nenhum aluno encontrado.",
                                color = TextoPrincipal,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (pesquisa.isBlank()) {
                                    "Não há alunos cadastrados."
                                } else {
                                    "Tente buscar por outro nome ou altere o filtro."
                                },
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                        }
                    }

                } else {

                    // Cartões dos alunos
                    filtrados.forEach { aluno ->

                        val nome = aluno.nome
                        val objetivo = aluno.objetivo
                        val status = aluno.status

                        val corStatus = when (status) {
                            "Prioridade" -> Color(0xFFFFD9DC)
                            "Atenção" -> Color(0xFFFFEAB5)
                            else -> Color(0xFFDDF4E3)
                        }

                        val corTextoStatus = when (status) {
                            "Prioridade" -> Color(0xFF9C4350)
                            "Atenção" -> Color(0xFF9A7000)
                            else -> Color(0xFF39754A)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable {
                                    onSelecionarAluno(aluno)
                                },
                            shape = RoundedCornerShape(13.dp),
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

                                    // Iniciais do aluno
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                Color(0xFFE9E7EC),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = nome.split(" ")
                                                .take(2)
                                                .mapNotNull {
                                                    it.firstOrNull()
                                                }
                                                .joinToString(""),
                                            color = Color(0xFF706A75),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )

                                    // Nome e objetivo
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = nome,
                                            color = TextoPrincipal,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = objetivo,
                                            color = TextoSecundario,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Text(
                                        text = "›",
                                        color = Color(0xFF77717F),
                                        fontSize = 26.sp
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // Status do aluno
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = corStatus,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 5.dp
                                            )
                                    ) {
                                        Text(
                                            text = status,
                                            color = corTextoStatus,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Text(
                                        text = if (modoSelecaoTreino) {
                                            "Selecionar ›"
                                        } else {
                                            "Ver perfil ›"
                                        },
                                        color = AzulPrincipal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Barra inferior fixa
        DashboardBottomBar(
            telaAtual = AppScreen.Alunos,
            onInicio = onVoltar,
            onAlunos = {},
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
    }
}

// Modelo de eventos do acompanhamento individual.
// A persistência usa SharedPreferences para integrar a funcionalidade ao código atual
// sem exigir alterações no FitCareDatabase já existente.
data class EventoAluno(
    val id: Long,
    val alunoId: Long,
    val tipo: String,
    val data: String,
    val titulo: String,
    val descricao: String = "",
    val intensidade: Int? = null,
    val localizacao: String = "",
    val momentoOcorrencia: String = "",
    val relacaoTreinamento: String = "",
    val limitacoes: String = "",
    val proximaAvaliacao: String = "",
    val contatoAcao: String = ""
)

class EventoAlunoStorage(context: Context) {
    private val preferences =
        context.getSharedPreferences("fitcare_eventos_aluno", Context.MODE_PRIVATE)
    private val chave = "eventos"

    fun listarTodos(): List<EventoAluno> {
        val json = preferences.getString(chave, "[]") ?: "[]"
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    EventoAluno(
                        id = item.optLong("id"),
                        alunoId = item.optLong("alunoId"),
                        tipo = item.optString("tipo"),
                        data = item.optString("data"),
                        titulo = item.optString("titulo"),
                        descricao = item.optString("descricao"),
                        intensidade = if (item.has("intensidade") && !item.isNull("intensidade")) item.optInt(
                            "intensidade"
                        ) else null,
                        localizacao = item.optString("localizacao"),
                        momentoOcorrencia = item.optString("momentoOcorrencia"),
                        relacaoTreinamento = item.optString("relacaoTreinamento"),
                        limitacoes = item.optString("limitacoes"),
                        proximaAvaliacao = item.optString("proximaAvaliacao"),
                        contatoAcao = item.optString("contatoAcao")
                    )
                )
            }
        }.sortedWith(compareByDescending<EventoAluno> { it.data }.thenByDescending { it.id })
    }

    fun listarPorAluno(alunoId: Long): List<EventoAluno> =
        listarTodos().filter { it.alunoId == alunoId }

    fun inserir(evento: EventoAluno) {
        val eventos = listarTodos().toMutableList()

        val indice = eventos.indexOfFirst {
            it.id == evento.id
        }

        if (indice >= 0) {
            eventos[indice] = evento
        } else {
            eventos.add(evento)
        }

        salvar(eventos)
    }

    fun excluir(id: Long) {
        val eventos = listarTodos().toMutableList()
        eventos.removeAll { it.id == id }
        salvar(eventos)
    }

    private fun salvar(eventos: List<EventoAluno>) {
        val array = JSONArray()
        eventos.forEach { evento ->
            array.put(
                JSONObject().apply {
                    put("id", evento.id)
                    put("alunoId", evento.alunoId)
                    put("tipo", evento.tipo)
                    put("data", evento.data)
                    put("titulo", evento.titulo)
                    put("descricao", evento.descricao)
                    if (evento.intensidade != null) put(
                        "intensidade",
                        evento.intensidade
                    ) else put(
                        "intensidade",
                        JSONObject.NULL
                    )
                    put("localizacao", evento.localizacao)
                    put("momentoOcorrencia", evento.momentoOcorrencia)
                    put("relacaoTreinamento", evento.relacaoTreinamento)
                    put("limitacoes", evento.limitacoes)
                    put("proximaAvaliacao", evento.proximaAvaliacao)
                    put("contatoAcao", evento.contatoAcao)
                }
            )
        }
        preferences.edit().putString(chave, array.toString()).apply()
    }
}


@Composable
fun TelaPerfilAluno(
    alunoId: Long,
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onDashboard: () -> Unit,
    onTreinos: () -> Unit,
    onEditar: () -> Unit,
    onLinhaDoTempo: () -> Unit
) {
    var alunoAtual by remember {
        mutableStateOf<br.com.icaros.fitcare.data.AlunoEntity?>(null)
    }

    var carregando by remember { mutableStateOf(true) }
    var mostrarDialogoExclusao by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf(false) }
    var erroExclusao by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val contextoEventosPerfil = LocalContext.current

    val storageEventosPerfil = remember(contextoEventosPerfil) {
        EventoAlunoStorage(contextoEventosPerfil)
    }

    var eventosAluno by remember(alunoId) {
        mutableStateOf(storageEventosPerfil.listarPorAluno(alunoId))
    }

    LaunchedEffect(alunoId) {
        repository.buscarPorId(alunoId).collect { resultado ->
            alunoAtual = resultado
            carregando = false
        }
    }

    LaunchedEffect(alunoId) {
        eventosAluno = storageEventosPerfil.listarPorAluno(alunoId)
    }

    // CARREGAMENTO
    if (carregando) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FundoApp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = AzulPrincipal)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Carregando dados do aluno...",
                color = TextoSecundario,
                fontSize = 13.sp
            )
        }
        return
    }

    // ALUNO NÃO ENCONTRADO
    if (alunoAtual == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FundoApp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Aluno não encontrado.",
                color = TextoPrincipal,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Não foi possível localizar os dados deste aluno.",
                color = TextoSecundario,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onVoltar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPrincipal
                )
            ) {
                Text("Voltar para alunos")
            }
        }
        return
    }

    val nomeAluno = alunoAtual!!.nome
    val objetivoAluno = alunoAtual!!.objetivo
    val statusAluno = alunoAtual!!.status
    val observacoesProfissionais = alunoAtual!!.observacoesProfissionais
    val condicoes = alunoAtual!!.condicoes
    val restricoes = alunoAtual!!.restricoes

    val corStatus = when (statusAluno) {
        "Prioridade" -> Color(0xFFFFD9DC)
        "Atenção" -> Color(0xFFFFEAB5)
        else -> Color(0xFFDDF4E3)
    }

    val corTextoStatus = when (statusAluno) {
        "Prioridade" -> Color(0xFF9C4350)
        "Atenção" -> Color(0xFF9A7000)
        else -> Color(0xFF39754A)
    }

    // ESTRUTURA PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // CABEÇALHO COM LOGO DO APLICATIVO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                // LOGO CENTRALIZADA
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )

                // BOTÃO VOLTAR À ESQUERDA
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            // IDENTIFICAÇÃO DO ALUNO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFE9E7EC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👤",
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = nomeAluno,
                        color = TextoPrincipal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = objetivoAluno,
                        color = TextoSecundario,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = corStatus,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusAluno,
                            color = corTextoStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // CARTÕES DE INFORMAÇÕES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // DADOS CADASTRAIS
                Cartao(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Dados cadastrais",
                            tint = AzulPrincipal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Dados cadastrais",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinhaDado(
                        rotulo = "E-mail",
                        valor = alunoAtual!!.email
                    )

                    LinhaDado(
                        rotulo = "Cadastro",
                        valor = alunoAtual!!.dataCadastro
                    )

                    LinhaDado(
                        rotulo = "Objetivo",
                        valor = alunoAtual!!.objetivo
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onEditar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulPrincipal
                        )
                    ) {
                        Text(
                            text = "Editar dados",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // EXCLUIR ALUNO
                OutlinedButton(
                    onClick = {
                        erroExclusao = ""
                        mostrarDialogoExclusao = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Vermelho
                    )
                ) {
                    Text("Excluir aluno")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ACOMPANHAMENTO
                Cartao(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Histórico do aluno",
                            tint = AzulPrincipal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Acompanhamento",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .background(
                                color = corStatus,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = statusAluno.uppercase(),
                            color = corTextoStatus,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinhaDado(
                        rotulo = "Observações profissionais",
                        valor = observacoesProfissionais.ifBlank {
                            "Não informado."
                        }
                    )

                    LinhaDado(
                        rotulo = "Condições",
                        valor = condicoes.ifBlank {
                            "Não informado."
                        }
                    )

                    LinhaDado(
                        rotulo = "Restrições",
                        valor = restricoes.ifBlank {
                            "Não informado."
                        }
                    )
                }

                // ANAMNESE
                Cartao(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = "Anamnese",
                            tint = AzulPrincipal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Anamnese",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Importe o documento de anamnese para manter as informações do aluno disponíveis no perfil.",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AzulPrincipal
                        ),
                        border = BorderStroke(1.dp, AzulPrincipal)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = "Importar anamnese",
                                tint = AzulPrincipal,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = "Importar anamnese",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Formatos aceitos: PDF ou imagem",
                        color = TextoSecundario,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                // HISTÓRICO DO ALUNO
                Cartao(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Gráfico de acompanhamento",
                            tint = AzulPrincipal,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Histórico do aluno",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (eventosAluno.isEmpty()) {
                            "Ainda não há registros de acompanhamento para este aluno."
                        } else {
                            "${eventosAluno.size} registro(s) disponível(is) na linha do tempo individual."
                        },
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Evolução da intensidade da dor",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextoPrincipal
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GraficoIntensidadeDor(eventosAluno)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (eventosAluno.isNotEmpty()) {
                        val ultimo = eventosAluno.first()

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = AzulMuitoClaro
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Último registro • ${ultimo.data}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulPrincipal
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${ultimo.tipo}: ${ultimo.titulo}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoPrincipal
                                )

                                if (ultimo.descricao.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = ultimo.descricao,
                                        fontSize = 12.sp,
                                        color = TextoSecundario,
                                        maxLines = 3
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = onLinhaDoTempo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulPrincipal
                        )
                    ) {
                        Text(
                            "Abrir linha do tempo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        enabled = false
                    ) {
                        Text("Registros: ${eventosAluno.size}")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // RODAPÉ - MESMO PADRÃO DA TELA DE ALUNOS
        DashboardBottomBar(
            telaAtual = AppScreen.PerfilAluno,
            onInicio = onDashboard,
            onAlunos = onVoltar,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
    }

    // DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO
    if (mostrarDialogoExclusao) {
        AlertDialog(
            onDismissRequest = {
                if (!excluindo) {
                    mostrarDialogoExclusao = false
                }
            },
            title = {
                Text("Confirmar exclusão")
            },
            text = {
                Column {
                    Text(
                        "Deseja excluir definitivamente os dados deste aluno? Esta ação não poderá ser desfeita."
                    )

                    if (erroExclusao.isNotBlank()) {
                        Text(
                            text = erroExclusao,
                            color = Vermelho,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !excluindo,
                    onClick = {
                        val aluno = alunoAtual

                        if (aluno != null && !excluindo) {
                            excluindo = true

                            scope.launch {
                                try {
                                    repository.excluir(aluno)
                                    mostrarDialogoExclusao = false
                                    onVoltar()
                                } catch (e: Exception) {
                                    erroExclusao =
                                        "Não foi possível excluir o aluno. Tente novamente."
                                } finally {
                                    excluindo = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (excluindo) "Excluindo..." else "Excluir",
                        color = Vermelho
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !excluindo,
                    onClick = {
                        mostrarDialogoExclusao = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun GraficoIntensidadeDor(eventos: List<EventoAluno>) {

    val formatoData = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    }

    val registrosDor = eventos
        .filter {
            it.tipo == "Dor / desconforto" &&
                    it.intensidade != null
        }
        .sortedBy {
            runCatching {
                formatoData.parse(it.data)
            }.getOrNull()
        }

    if (registrosDor.isEmpty()) {
        Text(
            text = "Ainda não existem registros de intensidade da dor.",
            color = TextoSecundario,
            fontSize = 12.sp
        )
        return
    }

    val pontos = registrosDor.mapNotNull { evento ->
        evento.intensidade?.coerceIn(0, 10)
    }

    if (pontos.isEmpty()) return

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {

        val largura = size.width
        val altura = size.height

        val margemEsquerda = 32.dp.toPx()
        val margemDireita = 8.dp.toPx()
        val margemSuperior = 12.dp.toPx()
        val margemInferior = 30.dp.toPx()

        val larguraGrafico =
            largura - margemEsquerda - margemDireita

        val alturaGrafico =
            altura - margemSuperior - margemInferior

        val corLinha = Color(0xFFFF5570)
        val corGrade = Color(0xFFE8E8EF)

        val paint = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.dp.toPx()
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }

        // Linhas horizontais e escala de intensidade
        for (nivel in 0..10 step 2) {

            val y = margemSuperior +
                    alturaGrafico * (10 - nivel) / 10f

            drawLine(
                color = corGrade,
                start = Offset(margemEsquerda, y),
                end = Offset(largura - margemDireita, y),
                strokeWidth = 1.dp.toPx()
            )

            drawContext.canvas.nativeCanvas.drawText(
                nivel.toString(),
                margemEsquerda - 8.dp.toPx(),
                y + 3.dp.toPx(),
                paint
            )
        }

        // Posição horizontal e vertical dos registros
        val coordenadas = pontos.mapIndexed { index, intensidade ->

            val x = if (pontos.size == 1) {
                margemEsquerda + larguraGrafico / 2
            } else {
                margemEsquerda +
                        larguraGrafico * index / (pontos.size - 1)
            }

            val y = margemSuperior +
                    alturaGrafico * (10 - intensidade) / 10f

            Offset(x, y)
        }

        // Linhas que conectam os registros
        for (index in 0 until coordenadas.size - 1) {

            drawLine(
                color = corLinha,
                start = coordenadas[index],
                end = coordenadas[index + 1],
                strokeWidth = 2.dp.toPx()
            )
        }

        // Pontos de intensidade
        coordenadas.forEach { ponto ->

            drawCircle(
                color = corLinha,
                radius = 4.dp.toPx(),
                center = ponto
            )
        }

        // Datas dos registros
        val paintData = Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 8.dp.toPx()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        coordenadas.forEachIndexed { index, ponto ->

            val data = registrosDor[index].data

            drawContext.canvas.nativeCanvas.drawText(
                data,
                ponto.x,
                altura - 5.dp.toPx(),
                paintData
            )
        }
    }
}

// tela linha do tempo
@Composable
fun TelaLinhaDoTempoAluno(
    alunoId: Long,
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onNovoRegistro: () -> Unit,
    onEditarRegistro: (EventoAluno) -> Unit,
    onDashboard: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onRegistros: () -> Unit,
    onMais: () -> Unit
) {
    val context = LocalContext.current

    val storage = remember {
        EventoAlunoStorage(context)
    }

    var eventos by remember(alunoId) {
        mutableStateOf(storage.listarPorAluno(alunoId))
    }

    var eventoSelecionado by remember {
        mutableStateOf<EventoAluno?>(null)
    }

    val aluno by repository.buscarPorId(alunoId).collectAsState(initial = null)

    LaunchedEffect(alunoId) {
        eventos = storage.listarPorAluno(alunoId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

// CABEÇALHO PADRÃO COM LOGO E BOTÃO VOLTAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {

                // LOGO CENTRALIZADA
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )

                // BOTÃO VOLTAR À ESQUERDA
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                text = aluno?.nome ?: "Aluno",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Histórico individual de acompanhamento",
                fontSize = 13.sp,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onNovoRegistro,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPrincipal
                )
            ) {
                Text(
                    "+ Novo registro",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (eventos.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Branco
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            "Nenhum evento registrado",
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Registre dores, observações, avaliações ou contatos para iniciar o histórico.",
                            color = TextoSecundario,
                            fontSize = 13.sp
                        )
                    }
                }

            } else {

                eventos.forEachIndexed { index, evento ->

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(28.dp)
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        AzulPrincipal,
                                        CircleShape
                                    )
                            )

                            if (index < eventos.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(92.dp)
                                        .background(AzulClaro)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable {
                                    eventoSelecionado = evento
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Branco
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        evento.tipo,
                                        color = AzulPrincipal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        evento.data,
                                        color = TextoSecundario,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    evento.titulo,
                                    color = TextoPrincipal,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (evento.descricao.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        evento.descricao,
                                        color = TextoSecundario,
                                        fontSize = 12.sp,
                                        maxLines = 3
                                    )
                                }

                                if (evento.intensidade != null) {
                                    Spacer(modifier = Modifier.height(5.dp))

                                    Text(
                                        "Intensidade: ${evento.intensidade}/10",
                                        color = TextoPrincipal,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "Toque para consultar os detalhes",
                                    color = AzulClaro,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // RODAPÉ PADRÃO
        DashboardBottomBar(
            telaAtual = AppScreen.LinhaDoTempoAluno,
            onInicio = onDashboard,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = onRegistros,
            onMais = onMais
        )
    }

    // DIÁLOGO DE DETALHES DO EVENTO
    eventoSelecionado?.let { evento ->

        AlertDialog(
            onDismissRequest = {
                eventoSelecionado = null
            },

            title = {
                Text(
                    evento.titulo,
                    color = TextoPrincipal
                )
            },

            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {

                    Text(
                        "Tipo: ${evento.tipo}",
                        fontWeight = FontWeight.Bold,
                        color = AzulPrincipal
                    )

                    Text(
                        "Data: ${evento.data}",
                        color = TextoSecundario
                    )

                    EventoDetalhe("Descrição", evento.descricao)

                    EventoDetalhe(
                        "Intensidade",
                        evento.intensidade?.let { "$it/10" } ?: ""
                    )

                    EventoDetalhe("Localização", evento.localizacao)

                    EventoDetalhe(
                        "Momento da ocorrência",
                        evento.momentoOcorrencia
                    )

                    EventoDetalhe(
                        "Relação com o treinamento",
                        evento.relacaoTreinamento
                    )

                    EventoDetalhe("Limitações", evento.limitacoes)

                    EventoDetalhe(
                        "Próxima avaliação",
                        evento.proximaAvaliacao
                    )

                    EventoDetalhe(
                        "Contato / ação",
                        evento.contatoAcao
                    )
                }
            },

            confirmButton = {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    TextButton(
                        onClick = {
                            storage.excluir(evento.id)
                            eventos = storage.listarPorAluno(alunoId)
                            eventoSelecionado = null
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir registro",
                            tint = Color(0xFFC62828)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            "Excluir",
                            color = Color(0xFFC62828)
                        )
                    }

                    TextButton(
                        onClick = {
                            eventoSelecionado = null
                            onEditarRegistro(evento)
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar registro",
                            tint = AzulPrincipal
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            "Editar",
                            color = AzulPrincipal
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun EventoDetalhe(
    rotulo: String,
    valor: String
) {
    if (valor.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))

        Column {
            Text(
                text = rotulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = valor,
                fontSize = 13.sp,
                color = TextoSecundario
            )
        }
    }
}

// Tela de edição de registro da linha do tempo
@Composable
fun TelaRegistroEventoAluno(
    alunoId: Long,
    eventoParaEditar: EventoAluno? = null,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    val tipos = listOf(
        "Dor / desconforto",
        "Avaliação física",
        "Observação profissional",
        "Contato / acompanhamento"
    )
    var tipo by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.tipo ?: tipos.first())
    }

    var data by remember(eventoParaEditar) {
        mutableStateOf(
            eventoParaEditar?.data
                ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        )
    }

    var titulo by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.titulo ?: "")
    }

    var descricao by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.descricao ?: "")
    }

    var intensidadeTexto by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.intensidade?.toString() ?: "")
    }

    var localizacao by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.localizacao ?: "")
    }

    var momento by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.momentoOcorrencia ?: "")
    }

    var relacaoTreino by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.relacaoTreinamento ?: "")
    }

    var limitacoes by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.limitacoes ?: "")
    }

    var proximaAvaliacao by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.proximaAvaliacao ?: "")
    }

    var contatoAcao by remember(eventoParaEditar) {
        mutableStateOf(eventoParaEditar?.contatoAcao ?: "")
    }
    val context = LocalContext.current
    val storage = remember { EventoAlunoStorage(context) }


    var erro by remember { mutableStateOf("") }

    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
// CABEÇALHO PADRÃO COM LOGO E BOTÃO VOLTAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )

                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(onVoltar = onVoltar)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .imePadding()
                .padding(18.dp)
        ) {
            Text(
                "Tipo de registro",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Spacer(modifier = Modifier.height(6.dp))
            tipos.forEach { opcao ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tipo = opcao }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = tipo == opcao, onClick = { tipo = opcao })
                    Text(opcao, fontSize = 13.sp, color = TextoPrincipal)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            CampoEvento("Data", data) { data = it }
            CampoEvento("Título", titulo) { titulo = it }
            CampoEvento("Descrição / observação", descricao, minLines = 3) {
                descricao = it
            }

            if (tipo == "Dor / desconforto") {
                CampoEvento(
                    label = "Intensidade (0 a 10)",
                    value = intensidadeTexto,
                    placeholder = "Ex.: 6",
                    keyboardType = KeyboardType.Number
                ) {
                    intensidadeTexto = it.filter(Char::isDigit).take(2)
                }

                CampoEvento(
                    label = "Localização da dor",
                    value = localizacao,
                    placeholder = "Ex.: Região lombar, joelho direito ou ombro esquerdo"
                ) {
                    localizacao = it
                }

                CampoEvento(
                    label = "Momento da ocorrência",
                    value = momento,
                    placeholder = "Ex.: Durante o agachamento ou após o treino"
                ) {
                    momento = it
                }

                CampoEvento(
                    label = "Relação com o treinamento",
                    value = relacaoTreino,
                    placeholder = "Ex.: Dor iniciada após aumento da carga no exercício"
                ) {
                    relacaoTreino = it
                }

                CampoEvento(
                    label = "Limitações",
                    value = limitacoes,
                    placeholder = "Ex.: Dificuldade para agachar e realizar movimentos de flexão do tronco",
                    minLines = 2
                ) {
                    limitacoes = it
                }
            }
            if (tipo == "Avaliação física") {
                CampoEvento(
                    "Data / previsão da próxima avaliação",
                    proximaAvaliacao
                ) { proximaAvaliacao = it }
            }
            if (tipo == "Contato / acompanhamento") {
                CampoEvento("Contato e ação realizada", contatoAcao, minLines = 3) {
                    contatoAcao = it
                }
            }

            if (erro.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(erro, color = Vermelho, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val intensidade = intensidadeTexto.toIntOrNull()
                    when {
                        titulo.isBlank() -> erro = "Informe um título para o registro."
                        data.isBlank() -> erro = "Informe a data do registro."
                        tipo == "Dor / desconforto" && (intensidade == null || intensidade !in 0..10) -> erro =
                            "A intensidade deve estar entre 0 e 10."

                        else -> {
                            storage.inserir(
                                EventoAluno(
                                    id = eventoParaEditar?.id ?: System.currentTimeMillis(),
                                    alunoId = alunoId,
                                    tipo = tipo,
                                    data = data,
                                    titulo = titulo.trim(),
                                    descricao = descricao.trim(),
                                    intensidade = if (tipo == "Dor / desconforto") intensidade else null,
                                    localizacao = localizacao.trim(),
                                    momentoOcorrencia = momento.trim(),
                                    relacaoTreinamento = relacaoTreino.trim(),
                                    limitacoes = limitacoes.trim(),
                                    proximaAvaliacao = proximaAvaliacao.trim(),
                                    contatoAcao = contatoAcao.trim()
                                )
                            )
                            onSalvo()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
            ) { Text("Salvar registro", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) { Text("Cancelar") }
        }
    }
}

@Composable
fun CampoEvento(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    placeholder: String = "",
    onValueChange: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotBlank()) {
                Text(placeholder)
            }
        },
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AzulPrincipal,
            focusedLabelColor = AzulPrincipal,
            cursorColor = AzulPrincipal
        )
    )
}

@Composable
fun CabecalhoPadrao(
    titulo: String,
    onVoltar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CabecalhoTopoClaro,
                        CabecalhoBaseClaro
                    )
                )
            )
            .padding(
                start = 8.dp,
                end = 18.dp,
                top = 4.dp,
                bottom = 12.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp),
            contentAlignment = Alignment.Center
        ) {
            // Logo centralizada
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .width(145.dp)
                    .height(65.dp)
            )

            // Botão voltar alinhado à esquerda
            BotaoVoltarPadrao(
                onVoltar = onVoltar,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        Text(
            text = titulo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            textAlign = TextAlign.Center,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextoPrincipal
        )
    }
}

private fun calcularStatusAcompanhamento(
    condicoes: String,
    restricoes: String
): String {
    fun contarItens(texto: String): Int {
        return texto.split(",")
            .count { it.trim().isNotEmpty() }
    }

    val total =
        contarItens(condicoes) + contarItens(restricoes)

    return when {
        total == 0 -> "Normal"
        total == 1 -> "Atenção"
        else -> "Prioridade"
    }
}

@Composable
private fun ItemAvaliacaoHistorica(
    data: String,
    regiao: String,
    nota: String,
    colorNota: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFFBF9FE), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFFEBE6F7), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📋", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )
                Text(text = regiao, fontSize = 11.sp, color = TextoSecundario)
            }
            Text(
                text = nota,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorNota
            )
        }
    }
}

@Composable
private fun GraficoHistoricoCustomizado() {
    val valoresProporcionais = listOf(0.7f, 0.5f, 0.6f, 0.4f)
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAFF), RoundedCornerShape(8.dp))
            .padding(start = 14.dp, top = 6.dp, end = 14.dp, bottom = 6.dp)
    ) {
        val largura = size.width
        val altura = size.height
        for (i in 0..4) {
            val y = altura * i / 4f
            drawLine(
                color = Color(0xFFF0EBF7),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(largura, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        val pontos = valoresProporcionais.mapIndexed { index, valor ->
            androidx.compose.ui.geometry.Offset(
                x = largura * index / (valoresProporcionais.size - 1).toFloat(),
                y = altura * (1f - valor)
            )
        }
        for (i in 0 until pontos.lastIndex) {
            drawLine(
                color = Color(0xFFE34B63),
                start = pontos[i],
                end = pontos[i + 1],
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        pontos.forEach { ponto ->
            drawCircle(color = Color(0xFFE34B63), radius = 4.5.dp.toPx(), center = ponto)
        }
    }
}

// Tela do formulário de edição do aluno
@Composable
fun TelaEditarAluno(
    alunoId: Long,
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit,
    onDashboard: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit
) {
    var alunoOriginal by remember {
        mutableStateOf<br.com.icaros.fitcare.data.AlunoEntity?>(null)
    }

    var carregando by remember { mutableStateOf(true) }

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    val opcoesCondicoes = listOf(
        "Hipertensão",
        "Diabetes",
        "Dor lombar",
        "Dor no joelho",
        "Dor no ombro",
        "Problema cardíaco"
    )

    val opcoesRestricoes = listOf(
        "Evitar exercícios de alto impacto",
        "Evitar agachamentos profundos",
        "Limitar cargas elevadas",
        "Evitar exercícios acima da cabeça",
        "Limitar amplitude de movimento"
    )

    var condicoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var restricoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var outraCondicao by remember {
        mutableStateOf("")
    }

    var outraRestricao by remember {
        mutableStateOf("")
    }

    var erro by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val requesterObservacoes = remember {
        BringIntoViewRequester()
    }

    val requesterOutraCondicao = remember {
        BringIntoViewRequester()
    }

    val requesterOutraRestricao = remember {
        BringIntoViewRequester()
    }

    // CARREGAR DADOS DO ALUNO
    LaunchedEffect(alunoId) {
        repository.buscarPorId(alunoId).collect { aluno ->

            if (alunoOriginal == null && aluno != null) {

                alunoOriginal = aluno

                nome = aluno.nome
                email = aluno.email
                objetivo = aluno.objetivo
                observacoes = aluno.observacoesProfissionais

                val condicoesSalvas = aluno.condicoes
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val restricoesSalvas = aluno.restricoes
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val condicoesPadrao = condicoesSalvas
                    .filter { opcoesCondicoes.contains(it) }
                    .toSet()

                val outrasCondicoes = condicoesSalvas
                    .filter { !opcoesCondicoes.contains(it) }

                val restricoesPadrao = restricoesSalvas
                    .filter { opcoesRestricoes.contains(it) }
                    .toSet()

                val outrasRestricoes = restricoesSalvas
                    .filter { !opcoesRestricoes.contains(it) }

                condicoesSelecionadas =
                    if (outrasCondicoes.isNotEmpty()) {
                        condicoesPadrao + "Outro"
                    } else {
                        condicoesPadrao
                    }

                restricoesSelecionadas =
                    if (outrasRestricoes.isNotEmpty()) {
                        restricoesPadrao + "Outro"
                    } else {
                        restricoesPadrao
                    }

                outraCondicao = outrasCondicoes.joinToString(", ")
                outraRestricao = outrasRestricoes.joinToString(", ")
            }

            carregando = false
        }
    }

    // TELA DE CARREGAMENTO
    if (carregando) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FundoApp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AzulPrincipal)
        }

        return
    }

    val original = alunoOriginal ?: return

    // ESTRUTURA PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // CABEÇALHO COM LOGO E BOTÃO VOLTAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {

                // LOGO CENTRALIZADA
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )

                // BOTÃO VOLTAR À ESQUERDA
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 18.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Editar aluno",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AzulPrincipal,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = objetivo,
                onValueChange = { objetivo = it },
                label = { Text("Objetivo") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Condições",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Selecione as condições que se aplicam ao aluno.",
                fontSize = 12.sp,
                color = TextoSecundario
            )

            opcoesCondicoes.forEach { condicao ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            condicoesSelecionadas =
                                if (condicoesSelecionadas.contains(condicao)) {
                                    condicoesSelecionadas - condicao
                                } else {
                                    condicoesSelecionadas + condicao
                                }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = condicoesSelecionadas.contains(condicao),
                        onCheckedChange = { marcado ->
                            condicoesSelecionadas =
                                if (marcado) {
                                    condicoesSelecionadas + condicao
                                } else {
                                    condicoesSelecionadas - condicao
                                }
                        }
                    )

                    Text(
                        text = condicao,
                        fontSize = 14.sp,
                        color = TextoPrincipal
                    )
                }
            }

            // OUTRA CONDIÇÃO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        condicoesSelecionadas =
                            if (condicoesSelecionadas.contains("Outro")) {
                                condicoesSelecionadas - "Outro"
                            } else {
                                condicoesSelecionadas + "Outro"
                            }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = condicoesSelecionadas.contains("Outro"),
                    onCheckedChange = { marcado ->
                        condicoesSelecionadas =
                            if (marcado) {
                                condicoesSelecionadas + "Outro"
                            } else {
                                condicoesSelecionadas - "Outro"
                            }
                    }
                )

                Text(
                    text = "Outro",
                    fontSize = 14.sp,
                    color = TextoPrincipal
                )
            }

            if (condicoesSelecionadas.contains("Outro")) {

                OutlinedTextField(
                    value = outraCondicao,
                    onValueChange = { outraCondicao = it },
                    label = {
                        Text("Descreva outra condição")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(requesterOutraCondicao)
                        .onFocusChanged { estado ->
                            if (estado.isFocused) {
                                scope.launch {
                                    requesterOutraCondicao.bringIntoView()
                                }
                            }
                        },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // RESTRIÇÕES
            Text(
                text = "Restrições",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Selecione as restrições que devem ser consideradas durante os treinos.",
                fontSize = 12.sp,
                color = TextoSecundario
            )

            opcoesRestricoes.forEach { restricao ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            restricoesSelecionadas =
                                if (restricoesSelecionadas.contains(restricao)) {
                                    restricoesSelecionadas - restricao
                                } else {
                                    restricoesSelecionadas + restricao
                                }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = restricoesSelecionadas.contains(restricao),
                        onCheckedChange = { marcado ->
                            restricoesSelecionadas =
                                if (marcado) {
                                    restricoesSelecionadas + restricao
                                } else {
                                    restricoesSelecionadas - restricao
                                }
                        }
                    )

                    Text(
                        text = restricao,
                        fontSize = 14.sp,
                        color = TextoPrincipal
                    )
                }
            }

            // OUTRA RESTRIÇÃO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        restricoesSelecionadas =
                            if (restricoesSelecionadas.contains("Outro")) {
                                restricoesSelecionadas - "Outro"
                            } else {
                                restricoesSelecionadas + "Outro"
                            }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = restricoesSelecionadas.contains("Outro"),
                    onCheckedChange = { marcado ->
                        restricoesSelecionadas =
                            if (marcado) {
                                restricoesSelecionadas + "Outro"
                            } else {
                                restricoesSelecionadas - "Outro"
                            }
                    }
                )

                Text(
                    text = "Outro",
                    fontSize = 14.sp,
                    color = TextoPrincipal
                )
            }

            if (restricoesSelecionadas.contains("Outro")) {

                OutlinedTextField(
                    value = outraRestricao,
                    onValueChange = {
                        outraRestricao = it
                    },
                    label = {
                        Text("Descreva outra restrição")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(requesterOutraRestricao)
                        .onFocusChanged { estado ->
                            if (estado.isFocused) {
                                scope.launch {
                                    delay(300)
                                    requesterOutraRestricao.bringIntoView()
                                    delay(100)
                                    requesterOutraRestricao.bringIntoView()
                                }
                            }
                        },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // OBSERVAÇÕES PROFISSIONAIS
            OutlinedTextField(
                value = observacoes,
                onValueChange = {
                    observacoes = it
                },
                label = {
                    Text("Observações profissionais")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(requesterObservacoes)
                    .onFocusChanged { estado ->
                        if (estado.isFocused) {
                            scope.launch {
                                requesterObservacoes.bringIntoView()
                            }
                        }
                    },
                minLines = 4,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp)
            )

            // MENSAGEM DE ERRO
            if (erro.isNotBlank()) {
                Text(
                    text = erro,
                    color = Vermelho,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // SALVAR ALTERAÇÕES
            Button(
                onClick = {

                    if (nome.isBlank()) {
                        erro = "Informe o nome do aluno."
                        return@Button
                    }

                    if (
                        condicoesSelecionadas.contains("Outro") &&
                        outraCondicao.isBlank()
                    ) {
                        erro = "Descreva a outra condição selecionada."
                        return@Button
                    }

                    if (
                        restricoesSelecionadas.contains("Outro") &&
                        outraRestricao.isBlank()
                    ) {
                        erro = "Descreva a outra restrição selecionada."
                        return@Button
                    }

                    erro = ""

                    scope.launch {
                        salvando = true

                        try {

                            val condicoesSalvas =
                                condicoesSelecionadas
                                    .filter { it != "Outro" }
                                    .toMutableList()

                            if (
                                condicoesSelecionadas.contains("Outro") &&
                                outraCondicao.isNotBlank()
                            ) {
                                condicoesSalvas.add(
                                    outraCondicao.trim()
                                )
                            }

                            val restricoesSalvas =
                                restricoesSelecionadas
                                    .filter { it != "Outro" }
                                    .toMutableList()

                            if (
                                restricoesSelecionadas.contains("Outro") &&
                                outraRestricao.isNotBlank()
                            ) {
                                restricoesSalvas.add(
                                    outraRestricao.trim()
                                )
                            }

                            val condicoesTexto =
                                condicoesSalvas.joinToString(", ")

                            val restricoesTexto =
                                restricoesSalvas.joinToString(", ")

                            val status = calcularStatusAcompanhamento(
                                condicoes = condicoesTexto,
                                restricoes = restricoesTexto
                            )

                            repository.atualizar(
                                original.copy(
                                    nome = nome.trim(),
                                    email = email.trim(),
                                    objetivo = objetivo.trim(),
                                    observacoesProfissionais = observacoes.trim(),
                                    condicoes = condicoesTexto,
                                    restricoes = restricoesTexto,
                                    status = status
                                )
                            )

                            onSalvo()

                        } catch (e: Exception) {

                            erro =
                                "Não foi possível salvar as alterações: " +
                                        (e.message ?: "erro desconhecido")

                        } finally {
                            salvando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = !salvando,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPrincipal
                )
            ) {
                Text(
                    text = if (salvando) {
                        "Salvando..."
                    } else {
                        "Salvar alterações"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // RODAPÉ FIXADO NA PARTE INFERIOR
        DashboardBottomBar(
            telaAtual = AppScreen.EditarAluno,
            onInicio = onDashboard,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
    }
}

private data class RowTreinoData(
    val aluno: String,
    val nomeTreino: String,
    val periodo: String,
    val frequencia: String,
    val progresso: Float,
    val statusTxt: String,
    val corStatus: Color,
    val corTxt: Color
)

@Composable
private fun DashboardBottomBar(
    telaAtual: AppScreen, // CORRIGIDO: Adicionado explicitamente o tipo do parâmetro ': AppScreen'
    onInicio: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onRegistros: () -> Unit,
    onMais: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DashboardNavItem(
                icone = "⌂",
                titulo = "Início",
                selecionado = telaAtual == AppScreen.Dashboard,
                onClick = onInicio
            )
            DashboardNavItem(
                icone = "♙",
                titulo = "Alunos",
                selecionado = telaAtual == AppScreen.Alunos || telaAtual == AppScreen.PerfilAluno,
                onClick = onAlunos
            )
            DashboardNavItem(
                icone = "⚙",
                titulo = "Treinos",
                selecionado = telaAtual == AppScreen.Treinos,
                onClick = onTreinos
            )
            DashboardNavItem(
                icone = "▣",
                titulo = "Registros",
                selecionado = false,
                onClick = onRegistros
            )
            DashboardNavItem(
                icone = "⋯",
                titulo = "Mais",
                selecionado = telaAtual == AppScreen.Login,
                onClick = onMais
            )
        }
    }
}

@Composable
private fun DashboardNavItem(
    icone: String,
    titulo: String,
    selecionado: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(65.dp)
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icone,
            color = if (selecionado) AzulPrincipal else Color(0xFF77717F),
            fontSize = 21.sp
        )
        Text(
            text = titulo,
            color = if (selecionado) AzulPrincipal else Color(0xFF77717F),
            fontSize = 10.sp,
            fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LinhaDado(rotulo: String, valor: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(rotulo, fontSize = 12.sp, color = TextoSecundario)
        Text(valor, fontSize = 15.sp, color = TextoPrincipal)
    }
}

private data class ExercicioFormulario(
    val nome: String = "",
    val series: String = "3",
    val repeticoes: String = "12",
    val carga: String = "",
    val observacoes: String = ""
)


// TELA DE TREINOS
@Composable
fun TelaTreinos(
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    onVoltar: () -> Unit,
    onAlunos: () -> Unit,
    onSelecionarTreino: (br.com.icaros.fitcare.data.TreinoEntity) -> Unit,
    onNovoTreino: () -> Unit
) {
    var pesquisa by remember {
        mutableStateOf("")
    }

    val treinos by treinoRepository
        .listarTodosTreinos()
        .collectAsState(initial = emptyList())

    val alunos by repository
        .listarAlunos()
        .collectAsState(initial = emptyList())

    val formatoData = remember {
        java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale("pt", "BR")
        ).apply {
            isLenient = false
        }
    }

    val hoje = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
    }

    val limiteVencimento = remember {
        java.util.Calendar.getInstance().apply {
            time = hoje
            add(java.util.Calendar.DAY_OF_YEAR, 7)
        }.time
    }

    // CONVERTE A DATA PARA COMPARAÇÃO
    fun converterData(dataTexto: String): java.util.Date? {
        if (dataTexto.isBlank()) return null

        return try {
            formatoData.parse(dataTexto)
        } catch (_: Exception) {
            null
        }
    }

    // TREINOS ATIVOS
    val treinosAtivos = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)

        dataFim == null || !dataFim.before(hoje)
    }

    // TREINOS A VENCER NOS PRÓXIMOS 7 DIAS
    val treinosAVencer = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)

        dataFim != null &&
                !dataFim.before(hoje) &&
                !dataFim.after(limiteVencimento)
    }

    // TREINOS VENCIDOS
    val treinosVencidos = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)

        dataFim != null && dataFim.before(hoje)
    }

    // FILTRO DE TREINOS
    val treinosFiltrados = treinos.filter { treino ->

        val nomeAluno = alunos
            .firstOrNull { aluno ->
                aluno.id == treino.alunoId
            }
            ?.nome
            .orEmpty()

        nomeAluno.contains(
            pesquisa,
            ignoreCase = true
        ) || treino.nome.contains(
            pesquisa,
            ignoreCase = true
        )
    }

    // ESTRUTURA PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // CABEÇALHO COM LOGO ABAIXO DA BARRA DE STATUS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {

            // BOTÃO VOLTAR
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                BotaoVoltarPadrao(
                    onVoltar = onVoltar
                )
            }

            // LOGO CENTRALIZADA
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(160.dp)
                    .height(80.dp)
            )
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    )
            ) {

                // TÍTULO E BOTÃO NOVO TREINO
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Treinos",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )

                        Text(
                            text = "Gerencie os planejamentos dos seus alunos.",
                            color = TextoSecundario,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onNovoTreino,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDDF4E3)
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "+ Novo treino",
                            color = Color(0xFF39754A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // CAMPO DE PESQUISA
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = {
                        pesquisa = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Buscar por nome do aluno ou treino..."
                        )
                    },
                    leadingIcon = {
                        Text(
                            text = "⌕",
                            color = TextoSecundario,
                            fontSize = 25.sp
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "☷",
                            color = TextoSecundario,
                            fontSize = 20.sp
                        )
                    },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro
                    )
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // INDICADORES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {

                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "Treinos ativos",
                        valor = treinosAtivos.toString(),
                        corFundo = Color(0xFFDCEBFF),
                        corTexto = Color(0xFF416C93)
                    )

                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "A vencer (7 dias)",
                        valor = treinosAVencer.toString(),
                        corFundo = Color(0xFFFFEAB5),
                        corTexto = Color(0xFFB17B00)
                    )

                    DashboardIndicador(
                        modifier = Modifier.weight(1f),
                        titulo = "Vencidos",
                        valor = treinosVencidos.toString(),
                        corFundo = Color(0xFFFFD9DC),
                        corTexto = Color(0xFF9C4350)
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                // TÍTULO DA LISTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lista de Treinos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Filtrar por situação",
                        fontSize = 12.sp,
                        color = TextoSecundario
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                // LISTA VAZIA
                if (treinosFiltrados.isEmpty()) {

                    Text(
                        text = if (pesquisa.isBlank()) {
                            "Nenhum treino cadastrado ainda."
                        } else {
                            "Nenhum treino encontrado para essa pesquisa."
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center,
                        color = TextoSecundario,
                        fontSize = 14.sp
                    )

                } else {

                    // CARTÕES DOS TREINOS
                    treinosFiltrados.forEach { item ->

                        val nomeAluno = alunos
                            .firstOrNull { aluno ->
                                aluno.id == item.alunoId
                            }
                            ?.nome
                            ?: "Aluno não encontrado"

                        val dataFim = item.dataFim.trim()
                        val dataFimConvertida = converterData(dataFim)

                        val status = when {
                            dataFim.isBlank() -> "Sem prazo"
                            dataFimConvertida == null -> "Data inválida"
                            dataFimConvertida.before(hoje) -> "Vencido"
                            else -> "Ativo"
                        }

                        val corStatus = when (status) {
                            "Vencido" -> Color(0xFFFFD9DC)
                            "Ativo" -> Color(0xFFD8F3DF)
                            else -> Color(0xFFE5EAF0)
                        }

                        val corTextoStatus = when (status) {
                            "Vencido" -> Color(0xFF9C4350)
                            "Ativo" -> Color(0xFF36844A)
                            else -> TextoSecundario
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable {
                                    onSelecionarTreino(item)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Branco
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    // ÍCONE DO ALUNO
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                Color(0xFFE9E7EC),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "👤",
                                            fontSize = 16.sp
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = "Aluno: $nomeAluno",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextoPrincipal
                                        )

                                        Text(
                                            text = "Treino: ${item.nome}",
                                            fontSize = 13.sp,
                                            color = TextoPrincipal
                                        )

                                        Text(
                                            text = "Início: ${item.dataInicio}",
                                            fontSize = 11.sp,
                                            color = TextoSecundario
                                        )

                                        Text(
                                            text = "Término: ${
                                                if (dataFim.isBlank()) {
                                                    "Não definido"
                                                } else {
                                                    dataFim
                                                }
                                            }",
                                            fontSize = 11.sp,
                                            color = TextoSecundario
                                        )
                                    }

                                    Spacer(
                                        modifier = Modifier.width(8.dp)
                                    )

                                    // STATUS
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = corStatus,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = 4.dp
                                            )
                                    ) {
                                        Text(
                                            text = status,
                                            color = corTextoStatus,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // DESCRIÇÃO
                                if (item.descricao.isNotBlank()) {

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )

                                    Text(
                                        text = item.descricao,
                                        fontSize = 12.sp,
                                        color = TextoSecundario
                                    )
                                }

                                // OBSERVAÇÕES
                                if (item.observacoes.isNotBlank()) {

                                    Spacer(
                                        modifier = Modifier.height(6.dp)
                                    )

                                    Text(
                                        text = "Observações: ${item.observacoes}",
                                        fontSize = 12.sp,
                                        color = TextoSecundario
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "Detalhes ›",
                                        color = AzulPrincipal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }
        }

        // RODAPÉ FIXADO NA PARTE INFERIOR
        DashboardBottomBar(
            telaAtual = AppScreen.Treinos,
            onInicio = onVoltar,
            onAlunos = onAlunos,
            onTreinos = {},
            onRegistros = {},
            onMais = {}
        )
    }
}


// TELA FORMULÁRIO DE TREINO
@Composable
fun TelaFormularioTreino(
    alunoId: Long,
    treinoId: Long?,
    treinoRepository: TreinoRepository,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var dataInicio by remember { mutableStateOf("") }
    var dataFim by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }

    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }
    var mostrarDialogoExcluir by remember { mutableStateOf(false) }

    val exercicios = remember {
        mutableStateListOf(ExercicioFormulario())
    }

    val scope = rememberCoroutineScope()
    val contextoEventos = LocalContext.current

    val eventoStorage = remember(contextoEventos) {
        EventoAlunoStorage(contextoEventos)
    }

    // CARREGA OS DADOS DO TREINO EXISTENTE
    LaunchedEffect(treinoId) {
        if (treinoId != null) {
            val treino = treinoRepository.buscarTreinoPorId(treinoId)

            if (treino != null) {
                nome = treino.nome
                descricao = treino.descricao
                dataInicio = treino.dataInicio
                dataFim = treino.dataFim
                observacoes = treino.observacoes

                val exerciciosExistentes = treinoRepository
                    .listarExercicios(treinoId)
                    .first()

                exercicios.clear()

                exerciciosExistentes
                    .sortedBy { it.ordem }
                    .forEach { exercicio ->
                        exercicios.add(
                            ExercicioFormulario(
                                nome = exercicio.nome,
                                series = exercicio.series.toString(),
                                repeticoes = exercicio.repeticoes,
                                carga = exercicio.carga,
                                observacoes = exercicio.observacoes
                            )
                        )
                    }

                if (exercicios.isEmpty()) {
                    exercicios.add(ExercicioFormulario())
                }
            }
        }
    }

    // DIÁLOGO DE CONFIRMAÇÃO DE EXCLUSÃO
    if (mostrarDialogoExcluir && treinoId != null) {
        AlertDialog(
            onDismissRequest = {
                if (!salvando) {
                    mostrarDialogoExcluir = false
                }
            },
            title = {
                Text("Excluir treino")
            },
            text = {
                Text(
                    "Tem certeza que deseja excluir este treino? " +
                            "Todos os exercícios vinculados a ele também serão excluídos."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                salvando = true

                                val treino = treinoRepository
                                    .buscarTreinoPorId(treinoId)

                                if (treino != null) {
                                    treinoRepository.excluirTreino(treino)
                                }

                                mostrarDialogoExcluir = false
                                onSalvo()

                            } catch (e: Exception) {
                                mensagemErro =
                                    "Não foi possível excluir o treino: " +
                                            (e.message ?: "erro inesperado.")

                                mostrarDialogoExcluir = false

                            } finally {
                                salvando = false
                            }
                        }
                    },
                    enabled = !salvando
                ) {
                    Text(
                        text = "Excluir",
                        color = Color(0xFFC62828)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoExcluir = false
                    },
                    enabled = !salvando
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // ESTRUTURA PRINCIPAL
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // CABEÇALHO COM LOGO ABAIXO DA BARRA DE STATUS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .statusBarsPadding()
                .padding(vertical = 12.dp)
        ) {

            // BOTÃO VOLTAR
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                BotaoVoltarPadrao(
                    onVoltar = onVoltar
                )
            }

            // LOGO CENTRALIZADA
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(160.dp)
                    .height(80.dp)
            )

            // LIXEIRA
            if (treinoId != null) {
                IconButton(
                    onClick = {
                        mostrarDialogoExcluir = true
                    },
                    enabled = !salvando,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir treino",
                        tint = Color(0xFFC62828)
                    )
                }
            }
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = if (treinoId == null) {
                    "Novo treino"
                } else {
                    "Editar treino"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = if (treinoId == null) {
                    "Cadastre o planejamento e os exercícios do aluno."
                } else {
                    "Visualize e altere o planejamento e os exercícios do aluno."
                },
                fontSize = 14.sp,
                color = TextoSecundario
            )

            // NOME DO TREINO
            OutlinedTextField(
                value = nome,
                onValueChange = {
                    nome = it
                    mensagemErro = null
                },
                label = {
                    Text("Nome do treino *")
                },
                placeholder = {
                    Text("Ex.: Treino A - Superiores")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // DESCRIÇÃO
            OutlinedTextField(
                value = descricao,
                onValueChange = {
                    descricao = it
                },
                label = {
                    Text("Descrição")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // DATA DE INÍCIO
            OutlinedTextField(
                value = dataInicio,
                onValueChange = {
                    dataInicio = it
                    mensagemErro = null
                },
                label = {
                    Text("Data de início *")
                },
                placeholder = {
                    Text("dd/MM/aaaa")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii
                )
            )

            // DATA DE TÉRMINO
            OutlinedTextField(
                value = dataFim,
                onValueChange = {
                    dataFim = it
                    mensagemErro = null
                },
                label = {
                    Text("Data de término")
                },
                placeholder = {
                    Text("dd/MM/aaaa")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii
                )
            )

            // OBSERVAÇÕES
            OutlinedTextField(
                value = observacoes,
                onValueChange = {
                    observacoes = it
                },
                label = {
                    Text("Observações do treino")
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            HorizontalDivider()

            // EXERCÍCIOS
            Text(
                text = "Exercícios",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            exercicios.forEachIndexed { index, exercicio ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Branco
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Exercício ${index + 1}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.Bold,
                                color = TextoPrincipal
                            )

                            if (exercicios.size > 1) {
                                TextButton(
                                    onClick = {
                                        exercicios.removeAt(index)
                                    }
                                ) {
                                    Text(
                                        "Remover",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        // NOME DO EXERCÍCIO
                        OutlinedTextField(
                            value = exercicio.nome,
                            onValueChange = {
                                exercicios[index] =
                                    exercicios[index].copy(nome = it)
                            },
                            label = {
                                Text("Nome do exercício *")
                            },
                            placeholder = {
                                Text("Ex.: Agachamento")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // SÉRIES E REPETIÇÕES
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            OutlinedTextField(
                                value = exercicio.series,
                                onValueChange = {
                                    exercicios[index] =
                                        exercicios[index].copy(series = it)
                                },
                                label = {
                                    Text("Séries *")
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                )
                            )

                            OutlinedTextField(
                                value = exercicio.repeticoes,
                                onValueChange = {
                                    exercicios[index] =
                                        exercicios[index].copy(repeticoes = it)
                                },
                                label = {
                                    Text("Repetições *")
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        // CARGA
                        OutlinedTextField(
                            value = exercicio.carga,
                            onValueChange = {
                                exercicios[index] =
                                    exercicios[index].copy(carga = it)
                            },
                            label = {
                                Text("Carga")
                            },
                            placeholder = {
                                Text("Ex.: 20 kg")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // OBSERVAÇÕES DO EXERCÍCIO
                        OutlinedTextField(
                            value = exercicio.observacoes,
                            onValueChange = {
                                exercicios[index] =
                                    exercicios[index].copy(observacoes = it)
                            },
                            label = {
                                Text("Observações do exercício")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }

            // ADICIONAR EXERCÍCIO
            OutlinedButton(
                onClick = {
                    exercicios.add(ExercicioFormulario())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Adicionar exercício")
            }

            // EXCLUIR TREINO
            if (treinoId != null) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        mostrarDialogoExcluir = true
                    },
                    enabled = !salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFC62828)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFC62828)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Excluir treino",
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // MENSAGEM DE ERRO
            if (mensagemErro != null) {
                Text(
                    text = mensagemErro.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // RODAPÉ COM BOTÕES
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Branco)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            OutlinedButton(
                onClick = onVoltar,
                enabled = !salvando,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {

                    // VALIDAÇÃO DO NOME
                    if (nome.isBlank()) {
                        mensagemErro = "Informe o nome do treino."
                        return@Button
                    }

                    // VALIDAÇÃO DA DATA DE INÍCIO
                    if (dataInicio.isBlank()) {
                        mensagemErro = "Informe a data de início."
                        return@Button
                    }

                    val formatoData =
                        java.text.SimpleDateFormat(
                            "dd/MM/yyyy",
                            java.util.Locale("pt", "BR")
                        ).apply {
                            isLenient = false
                        }

                    val inicioValido = try {
                        val data = formatoData.parse(dataInicio)

                        data != null &&
                                formatoData.format(data) == dataInicio
                    } catch (_: Exception) {
                        false
                    }

                    if (!inicioValido) {
                        mensagemErro =
                            "A data de início deve ser válida no formato dd/MM/aaaa."
                        return@Button
                    }

                    // VALIDAÇÃO DA DATA DE TÉRMINO
                    if (dataFim.isNotBlank()) {

                        val fimValido = try {
                            val data = formatoData.parse(dataFim)

                            data != null &&
                                    formatoData.format(data) == dataFim
                        } catch (_: Exception) {
                            false
                        }

                        if (!fimValido) {
                            mensagemErro =
                                "A data de término deve ser válida no formato dd/MM/aaaa."
                            return@Button
                        }

                        val inicio = formatoData.parse(dataInicio)
                        val fim = formatoData.parse(dataFim)

                        if (
                            inicio != null &&
                            fim != null &&
                            fim.before(inicio)
                        ) {
                            mensagemErro =
                                "A data de término não pode ser anterior à data de início."
                            return@Button
                        }
                    }

                    // VALIDAÇÃO DOS EXERCÍCIOS
                    val exerciciosInvalidos = exercicios.any { exercicio ->
                        exercicio.nome.isBlank() ||
                                exercicio.series.toIntOrNull() == null ||
                                exercicio.series.toIntOrNull()!! <= 0 ||
                                exercicio.repeticoes.isBlank()
                    }

                    if (exerciciosInvalidos) {
                        mensagemErro =
                            "Preencha o nome, as séries válidas e as repetições de todos os exercícios."
                        return@Button
                    }

                    mensagemErro = null
                    salvando = true

                    // SALVAR TREINO
                    scope.launch {
                        try {

                            val idTreinoSalvo =
                                if (treinoId == null) {

                                    treinoRepository.inserirTreino(
                                        TreinoEntity(
                                            alunoId = alunoId,
                                            nome = nome.trim(),
                                            descricao = descricao.trim(),
                                            dataInicio = dataInicio.trim(),
                                            dataFim = dataFim.trim(),
                                            observacoes = observacoes.trim()
                                        )
                                    )

                                } else {

                                    treinoRepository.atualizarTreino(
                                        TreinoEntity(
                                            id = treinoId,
                                            alunoId = alunoId,
                                            nome = nome.trim(),
                                            descricao = descricao.trim(),
                                            dataInicio = dataInicio.trim(),
                                            dataFim = dataFim.trim(),
                                            observacoes = observacoes.trim()
                                        )
                                    )

                                    treinoId
                                }

                            // ATUALIZA OS EXERCÍCIOS NA EDIÇÃO
                            if (treinoId != null) {
                                treinoRepository.excluirExerciciosDoTreino(
                                    treinoId
                                )
                            }

                            // CADASTRA OS EXERCÍCIOS
                            exercicios.forEachIndexed { index, exercicio ->

                                treinoRepository.inserirExercicio(
                                    ExercicioEntity(
                                        treinoId = idTreinoSalvo,
                                        nome = exercicio.nome.trim(),
                                        series = exercicio.series.toInt(),
                                        repeticoes = exercicio.repeticoes.trim(),
                                        carga = exercicio.carga.trim(),
                                        observacoes = exercicio.observacoes.trim(),
                                        ordem = index
                                    )
                                )
                            }

                            // REGISTRA EVENTO NA LINHA DO TEMPO
                            if (treinoId == null) {
                                eventoStorage.inserir(
                                    EventoAluno(
                                        id = System.currentTimeMillis(),
                                        alunoId = alunoId,
                                        tipo = "Treino",
                                        data = dataInicio.trim(),
                                        titulo = "Treino: ${nome.trim()}",
                                        descricao = descricao.trim(),
                                        contatoAcao = observacoes.trim().ifBlank {
                                            "Treino cadastrado no aplicativo."
                                        }
                                    )
                                )
                            }

                            onSalvo()

                        } catch (e: Exception) {

                            mensagemErro =
                                "Não foi possível salvar o treino: " +
                                        (e.message ?: "erro inesperado.")

                        } finally {
                            salvando = false
                        }
                    }
                },
                enabled = !salvando,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPrincipal
                )
            ) {
                Text(
                    text = if (salvando) {
                        "Salvando..."
                    } else if (treinoId == null) {
                        "Salvar treino"
                    } else {
                        "Salvar alterações"
                    }
                )
            }
        }
    }
}

@Composable
fun TelaDetalhesTreino(
    treinoId: Long,
    treinoRepository: TreinoRepository,
    repository: AlunoRepository,
    onVoltar: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var treino by remember {
        mutableStateOf<br.com.icaros.fitcare.data.TreinoEntity?>(null)
    }

    val exercicios by treinoRepository
        .listarExercicios(treinoId)
        .collectAsState(initial = emptyList())

    var carregando by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(treinoId) {
        treino = treinoRepository.buscarTreinoPorId(treinoId)
        carregando = false
    }

    val alunoId = treino?.alunoId

    val aluno by if (alunoId != null) {
        repository.buscarPorId(alunoId)
            .collectAsState(initial = null)
    } else {
        remember {
            mutableStateOf(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

        // Cabeçalho padrão
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            CabecalhoTopoClaro,
                            CabecalhoBaseClaro
                        )
                    )
                )
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.logo_fitcare
                    ),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detalhes do treino",
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )

                TextButton(
                    onClick = onVoltar
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (carregando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AzulPrincipal
                    )
                }
            } else if (treino == null) {
                Text(
                    text = "Treino não encontrado.",
                    color = TextoSecundario
                )
            } else {

                val treinoAtual = treino!!

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Branco
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = treinoAtual.nome,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Aluno: ${aluno?.nome ?: "Aluno não encontrado"}",
                            fontSize = 14.sp,
                            color = TextoPrincipal
                        )

                        Text(
                            text = "Data de início: ${treinoAtual.dataInicio}",
                            fontSize = 13.sp,
                            color = TextoSecundario
                        )

                        if (treinoAtual.dataFim.isNotBlank()) {
                            Text(
                                text = "Data de término: ${treinoAtual.dataFim}",
                                fontSize = 13.sp,
                                color = TextoSecundario
                            )
                        }

                        if (treinoAtual.descricao.isNotBlank()) {
                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = treinoAtual.descricao,
                                fontSize = 13.sp,
                                color = TextoSecundario
                            )
                        }

                        if (treinoAtual.observacoes.isNotBlank()) {
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Observações: ${treinoAtual.observacoes}",
                                fontSize = 13.sp,
                                color = TextoSecundario
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Text(
                    text = "Exercícios",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                if (exercicios.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Branco
                        )
                    ) {
                        Text(
                            text = "Nenhum exercício cadastrado.",
                            modifier = Modifier.padding(16.dp),
                            color = TextoSecundario
                        )
                    }
                } else {
                    exercicios.forEachIndexed { index, exercicio ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Branco
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp)
                            ) {

                                Text(
                                    text = "${index + 1}. ${exercicio.nome}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoPrincipal
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
                                )

                                Text(
                                    text = "${exercicio.series} séries × ${exercicio.repeticoes}",
                                    fontSize = 13.sp,
                                    color = TextoPrincipal
                                )

                                if (exercicio.carga.isNotBlank()) {
                                    Text(
                                        text = "Carga: ${exercicio.carga}",
                                        fontSize = 12.sp,
                                        color = TextoSecundario
                                    )
                                }

                                if (exercicio.observacoes.isNotBlank()) {
                                    Text(
                                        text = "Observações: ${exercicio.observacoes}",
                                        fontSize = 12.sp,
                                        color = TextoSecundario
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