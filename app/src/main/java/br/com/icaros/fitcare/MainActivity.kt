package br.com.icaros.fitcare

import br.com.icaros.fitcare.data.FcmTokenManager
import com.google.firebase.firestore.FirebaseFirestore
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import br.com.icaros.fitcare.data.DispositivoRepository
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import br.com.icaros.fitcare.data.SessaoUsuario
import br.com.icaros.fitcare.data.UsuarioRepository
import androidx.compose.runtime.rememberCoroutineScope
import br.com.icaros.fitcare.data.*
import androidx.compose.runtime.saveable.rememberSaveable
import br.com.icaros.fitcare.data.AnamneseRepository
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository
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
import br.com.icaros.fitcare.data.UsuarioEntity

import kotlinx.coroutines.tasks.await
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
import br.com.icaros.fitcare.data.AlunoSyncWorker

// Paleta de Cores Dinâmica (Tema White e Tema Dark)
private var temaEscuroAtivoState by mutableStateOf(false)

private val AzulPrincipal: Color
    get() = if (temaEscuroAtivoState) Color(0xFF7AB8F5) else Color(0xFF002B5C)
private val AzulSecundario: Color
    get() = if (temaEscuroAtivoState) Color(0xFF5B9BD5) else Color(0xFF164A73)
private val AzulClaro = Color(0xFF5B9BD5)
private val AzulMuitoClaro: Color
    get() = if (temaEscuroAtivoState) Color(0xFF1E2D3D) else Color(0xFFEAF3F9)
private val FundoApp: Color
    get() = if (temaEscuroAtivoState) Color(0xFF0F1722) else Color(0xFFF5F9FC)
private val TextoPrincipal: Color
    get() = if (temaEscuroAtivoState) Color(0xFFEAF2FA) else Color(0xFF002B5C)
private val TextoSecundario: Color
    get() = if (temaEscuroAtivoState) Color(0xFFA2B5C8) else Color(0xFF526579)
private val Branco: Color
    get() = if (temaEscuroAtivoState) Color(0xFF172331) else Color(0xFFFFFFFF)
private val Vermelho = Color(0xFFE34B63)

private val CabecalhoTopoClaro: Color
    get() = if (temaEscuroAtivoState) Color(0xFF162434) else Color(0xFFEAF3F9)
private val CabecalhoBaseClaro: Color
    get() = if (temaEscuroAtivoState) Color(0xFF0F1722) else Color(0xFFF5F9FC)

private const val PREFERENCIAS_SESSAO = "fitcare_sessao"
const val CHAVE_MANTER_CONECTADO = "manter_conectado"
const val CHAVE_USUARIO_ID = "usuario_id"
const val CHAVE_PERFIL = "perfil"

// Checklist unificado entre Acompanhamento do Aluno e Anamnese
val OPCOES_CONDICOES_PADRAO = listOf(
    "Hipertensão",
    "Diabetes",
    "Dor lombar",
    "Dor no joelho",
    "Dor no ombro",
    "Problema cardíaco"
)

val OPCOES_RESTRICOES_PADRAO = listOf(
    "Evitar exercícios de alto impacto",
    "Evitar agachamentos profundos",
    "Limitar cargas elevadas",
    "Evitar exercícios acima da cabeça",
    "Limitar amplitude de movimento"
)

class MainActivity : ComponentActivity() {

    private lateinit var dispositivoRepository: DispositivoRepository

    private val monitorDispositivo = Handler(Looper.getMainLooper())

    private val tarefaMonitoramento = object : Runnable {
        override fun run() {
            registrarDispositivoSeProfessor()

            monitorDispositivo.postDelayed(
                this,
                60_000L
            )
        }
    }

    private fun registrarDispositivoSeProfessor() {
        val usuarioFirebase = FirebaseAuth.getInstance().currentUser
            ?: return

        val uid = usuarioFirebase.uid

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .document(uid)
            .get()
            .addOnSuccessListener { documento ->

                val perfil = documento.getString("perfil")
                val ativo = documento.getBoolean("ativo") == true

                if (perfil == "PROFESSOR" && ativo) {
                    dispositivoRepository.registrarAtividade(uid)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FitCareMessagingService.criarCanalNotificacao(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.BLACK

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = !temaEscuroAtivoState
            isAppearanceLightNavigationBars = false
        }

        dispositivoRepository = DispositivoRepository(applicationContext)

        val database = FitCareDatabase.getDatabase(applicationContext)

        val usuarioRepository = UsuarioRepository(
            database.usuarioDao(),
            applicationContext
        )

        val repository = AlunoRepository(
            database = database,
            context = applicationContext
        )

        val treinoRepository = TreinoRepository(
            database.treinoDao()
        )

        val periodizacaoRepository = PeriodizacaoRepository(
            database.periodizacaoDao()
        )

        val anamneseRepository = AnamneseRepository(
            database.anamneseDao()
        )

        val modeloAnamneseRepository =
            ModeloAnamneseRepository(
                dao = database.modeloAnamneseDao(),
                perguntaDao = database.perguntaModeloAnamneseDao()
            )

        val perguntaModeloAnamneseRepository =
            PerguntaModeloAnamneseRepository(
                database.perguntaModeloAnamneseDao()
            )

        val anamneseAtribuidaRepository =
            AnamneseAtribuidaRepository(
                database = database,
                modeloDao = database.modeloAnamneseDao(),
                perguntaModeloDao = database.perguntaModeloAnamneseDao(),
                anamneseDao = database.anamneseAtribuidaDao(),
                perguntaAtribuidaDao =
                    database.perguntaAnamneseAtribuidaDao()
            )

        val respostaAnamneseRepository =
            RespostaAnamneseRepository(
                respostaDao = database.respostaAnamneseDao(),
                perguntaDao = database.perguntaAnamneseAtribuidaDao(),
                anamneseDao = database.anamneseAtribuidaDao()
            )

        setContent {
            FitCareApp(
                repository = repository,
                treinoRepository = treinoRepository,
                periodizacaoRepository = periodizacaoRepository,
                anamneseRepository = anamneseRepository,
                modeloAnamneseRepository = modeloAnamneseRepository,
                perguntaModeloAnamneseRepository = perguntaModeloAnamneseRepository,
                anamneseAtribuidaRepository = anamneseAtribuidaRepository,
                respostaAnamneseRepository = respostaAnamneseRepository,
                usuarioRepository = usuarioRepository
            )
        }
    }

    override fun onStart() {
        super.onStart()

        monitorDispositivo.removeCallbacks(tarefaMonitoramento)
        monitorDispositivo.post(tarefaMonitoramento)
    }

    override fun onStop() {
        monitorDispositivo.removeCallbacks(tarefaMonitoramento)
        super.onStop()
    }
}

enum class AppScreen {
    Login,
    CadastroProfessor,
    Dashboard,
    Alunos,
    CadastroAluno,
    PerfilAluno,
    LinhaDoTempoAluno,
    RegistroEventoAluno,
    EditarAluno,
    Treinos,
    SelecionarAlunoTreino,
    FormularioTreino,
    DetalhesTreino,
    CentralAtencao,
    ModelosAnamnese,
    CriarModeloAnamnese,
    FormularioAnamnese,
    VisualizarAnamnese,
    AdminDashboard,
    CadastroAdminInicial,
    Periodizacao
}

// Fluxo do aplicativo
@Composable
fun FitCareApp(
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    periodizacaoRepository: PeriodizacaoRepository,
    anamneseRepository: AnamneseRepository,
    modeloAnamneseRepository: ModeloAnamneseRepository,
    perguntaModeloAnamneseRepository: PerguntaModeloAnamneseRepository,
    anamneseAtribuidaRepository: AnamneseAtribuidaRepository,
    respostaAnamneseRepository: RespostaAnamneseRepository,
    usuarioRepository: UsuarioRepository
) {
    var tela by rememberSaveable {
        mutableStateOf(AppScreen.Login)
    }
    var sessaoVersao by rememberSaveable {
        mutableIntStateOf(0)
    }
    var alunoSelecionadoId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var treinoSelecionadoId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var registrarNovoTreino by rememberSaveable {
        mutableStateOf(false)
    }
    var eventoEmEdicao by remember {
        mutableStateOf<EventoAluno?>(null)
    }
    var modeloAnamneseSelecionadoId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var voltarModelosParaPerfil by rememberSaveable {
        mutableStateOf(false)
    }
    var voltarVisualizacaoParaPerfil by rememberSaveable {
        mutableStateOf(false)
    }
    var anamneseAtribuidaSelecionadaId by rememberSaveable {
        mutableStateOf<Long?>(null)
    }
    var perfilSelecionado by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var filtroInicialAlunos by rememberSaveable {
        mutableStateOf("Todos")
    }
    var alunoAutenticado by rememberSaveable {
        mutableStateOf(false)
    }
    val context = LocalContext.current

    val view = androidx.compose.ui.platform.LocalView.current
    LaunchedEffect(temaEscuroAtivoState) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            window.navigationBarColor = android.graphics.Color.BLACK
            WindowInsetsControllerCompat(window, view).apply {
                isAppearanceLightStatusBars = !temaEscuroAtivoState
                isAppearanceLightNavigationBars = false
            }
        }
    }

    val preferenciasSessao = remember {
        context.getSharedPreferences(
            PREFERENCIAS_SESSAO,
            Context.MODE_PRIVATE
        )
    }

    LaunchedEffect(Unit) {
        temaEscuroAtivoState = preferenciasSessao.getBoolean(
            "tema_escuro_ativo",
            false
        )
        val manterConectado = preferenciasSessao.getBoolean(
            CHAVE_MANTER_CONECTADO,
            false
        )
        val usuarioIdSalvo = preferenciasSessao.getLong(
            CHAVE_USUARIO_ID,
            -1L
        )
        if (manterConectado && usuarioIdSalvo > 0L) {
            val usuario = usuarioRepository.buscarUsuarioAtivoPorId(
                usuarioIdSalvo
            )
            if (usuario != null) {
                SessaoUsuario.iniciar(usuario)
                sessaoVersao++
                if (usuario.perfil == "PROFESSOR" || usuario.perfil == "ALUNO") {
                    FcmTokenManager.sincronizarTokenSessaoAtual(context)
                    AlunoSyncWorker.agendar(context)
                    AlunoSyncWorker.agendarPeriodica(context)
                }

                if (usuario.perfil == "PROFESSOR") {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (!uid.isNullOrBlank()) {
                        DispositivoRepository(context).registrarAtividade(uid)
                    }
                }
                if (usuario.perfil == "PROFESSOR" || usuario.perfil == "ALUNO") {
                    AlunoSyncWorker.agendar(context)
                    AlunoSyncWorker.agendarPeriodica(context)
                }
                when (usuario.perfil) {
                    "PROFESSOR" -> {
                        alunoAutenticado = false
                        tela = AppScreen.Dashboard
                    }

                    "ALUNO" -> {
                        alunoAutenticado = true
                        tela = AppScreen.Login
                    }

                    "ADMIN" -> {
                        alunoAutenticado = false
                        tela = AppScreen.AdminDashboard
                    }

                    else -> {
                        SessaoUsuario.encerrar()
                        sessaoVersao++
                        preferenciasSessao.edit()
                            .remove(CHAVE_USUARIO_ID)
                            .remove(CHAVE_PERFIL)
                            .apply()
                        perfilSelecionado = null
                        tela = AppScreen.Login
                    }
                }
            } else {
                preferenciasSessao.edit()
                    .remove(CHAVE_USUARIO_ID)
                    .remove(CHAVE_PERFIL)
                    .apply()
            }
        }
    }

    val scopeGlobal = rememberCoroutineScope()

    DisposableEffect(sessaoVersao, tela) {
        val usuarioLogado = SessaoUsuario.usuarioAtual
        if (usuarioLogado != null && usuarioLogado.perfil == "PROFESSOR" && tela != AppScreen.Login) {
            val db = FitCareDatabase.getDatabase(context)
            val syncService = FirestoreSyncService(db.usuarioDao(), db)
            var listenersAtivos: List<com.google.firebase.firestore.ListenerRegistration> =
                emptyList()

            fun ativarSincronizacao() {
                if (listenersAtivos.isNotEmpty()) return
                if (FirebaseAuth.getInstance().currentUser == null) return

                scopeGlobal.launch(Dispatchers.IO) {
                    try {
                        syncService.sincronizarAlunos()
                        syncService.sincronizarLeituraAlunos()
                        syncService.sincronizarModelosAnamnese()
                        syncService.sincronizarAnamnesesAtribuidas()
                        syncService.sincronizarTreinos()
                    } catch (_: Exception) {
                    }
                }

                listenersAtivos = syncService.iniciarMonitoramentoTempoRealProfessor(
                    scope = scopeGlobal,
                    onContaBloqueada = {
                        val usuarioBloqueado = SessaoUsuario.usuarioAtual
                        val uidBloqueado = FirebaseAuth.getInstance().currentUser?.uid
                        FcmTokenManager.removerTokenAoSair(context, usuarioBloqueado, uidBloqueado)
                        AlunoSyncWorker.cancelarPeriodica(context)
                        Toast.makeText(
                            context,
                            "Seu acesso profissional foi bloqueado pelo administrador.",
                            Toast.LENGTH_LONG
                        ).show()
                        preferenciasSessao.edit()
                            .remove(CHAVE_USUARIO_ID)
                            .remove(CHAVE_PERFIL)
                            .apply()
                        sessaoVersao++
                        alunoAutenticado = false
                        perfilSelecionado = "PROFESSOR"
                        tela = AppScreen.Login
                    }
                )
            }

            val authListener = FirebaseAuth.AuthStateListener { auth ->
                if (auth.currentUser != null) {
                    ativarSincronizacao()
                }
            }

            FirebaseAuth.getInstance().addAuthStateListener(authListener)
            ativarSincronizacao()

            onDispose {
                FirebaseAuth.getInstance().removeAuthStateListener(authListener)
                listenersAtivos.forEach { it.remove() }
                listenersAtivos = emptyList()
            }
        } else {
            onDispose { }
        }
    }

    MaterialTheme(
        colorScheme = if (temaEscuroAtivoState) {
            darkColorScheme(
                primary = AzulPrincipal,
                secondary = AzulSecundario,
                background = FundoApp,
                surface = Branco,
                onBackground = TextoPrincipal,
                onSurface = TextoPrincipal
            )
        } else {
            lightColorScheme(
                primary = AzulPrincipal,
                secondary = AzulSecundario,
                background = FundoApp,
                surface = Branco,
                onBackground = TextoPrincipal,
                onSurface = TextoPrincipal
            )
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            color = FundoApp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(FundoApp)
            ) {
                when (tela) {
                    AppScreen.Periodizacao -> {
                        alunoSelecionadoId?.let { id ->
                            TelaPeriodizacaoAluno(
                                alunoId = id,
                                repository = repository,
                                treinoRepository = treinoRepository,
                                periodizacaoRepository = periodizacaoRepository,
                                onVoltar = { tela = AppScreen.PerfilAluno },
                                onVisualizarTreino = { treinoId ->
                                    treinoSelecionadoId = treinoId
                                    tela = AppScreen.DetalhesTreino
                                }
                            )
                        }
                    }

                    AppScreen.CentralAtencao -> {
                        TelaCentralAtencao(
                            repository = repository,
                            treinoRepository = treinoRepository,
                            onVoltar = {
                                tela = AppScreen.Dashboard
                            },
                            onAbrirPerfil = { aluno ->
                                alunoSelecionadoId = aluno.id
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
                            },
                            onSair = {
                                val usuarioSaindo = SessaoUsuario.usuarioAtual
                                val uidSaindo = FirebaseAuth.getInstance().currentUser?.uid
                                FcmTokenManager.removerTokenAoSair(context, usuarioSaindo, uidSaindo)
                                AlunoSyncWorker.cancelarPeriodica(context)
                                preferenciasSessao.edit()
                                    .remove(CHAVE_USUARIO_ID)
                                    .remove(CHAVE_PERFIL)
                                    .apply()
                                SessaoUsuario.encerrar()
                                sessaoVersao++
                                alunoAutenticado = false
                                perfilSelecionado = "PROFESSOR"
                                tela = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.DetalhesTreino -> {
                        val treinoId = treinoSelecionadoId
                        if (treinoId != null) {
                            TelaDetalhesTreino(
                                treinoId = treinoId,
                                treinoRepository = treinoRepository,
                                repository = repository,
                                onVoltar = {
                                    tela = AppScreen.PerfilAluno
                                },
                                onEditar = { treinoParaEditar ->
                                    alunoSelecionadoId = treinoParaEditar.alunoId
                                    treinoSelecionadoId = treinoParaEditar.id
                                    registrarNovoTreino = false
                                    tela = AppScreen.FormularioTreino
                                }
                            )
                        } else {
                            tela = AppScreen.PerfilAluno
                        }
                    }

                    AppScreen.CadastroAdminInicial -> {
                        TelaCadastroAdminInicial(
                            usuarioRepository = usuarioRepository,
                            onVoltar = {
                                perfilSelecionado = null
                                tela = AppScreen.Login
                            },
                            onCadastroConcluido = {
                                sessaoVersao++
                                alunoAutenticado = false
                                perfilSelecionado = "ADMIN"
                                tela = AppScreen.AdminDashboard
                            }
                        )
                    }

                    AppScreen.AdminDashboard -> {
                        TelaAdminDashboard(
                            usuarioRepository = usuarioRepository,
                            onSair = {
                                preferenciasSessao.edit()
                                    .remove(CHAVE_USUARIO_ID)
                                    .remove(CHAVE_PERFIL)
                                    .apply()
                                SessaoUsuario.encerrar()
                                sessaoVersao++
                                alunoAutenticado = false
                                perfilSelecionado = null
                                tela = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.Login -> {
                        if (alunoAutenticado) {
                            TelaAreaAluno(
                                alunoId = SessaoUsuario.usuarioAtual?.alunoId,
                                anamneseRepository = anamneseAtribuidaRepository,
                                respostaRepository = respostaAnamneseRepository,
                                documentoRepository = anamneseRepository,
                                alunoRepository = repository,
                                onSair = {
                                    val usuarioSaindo = SessaoUsuario.usuarioAtual
                                    val uidSaindo = FirebaseAuth.getInstance().currentUser?.uid
                                    FcmTokenManager.removerTokenAoSair(
                                        context,
                                        usuarioSaindo,
                                        uidSaindo
                                    )
                                    AlunoSyncWorker.cancelarPeriodica(context)
                                    preferenciasSessao.edit()
                                        .remove(CHAVE_USUARIO_ID)
                                        .remove(CHAVE_PERFIL)
                                        .apply()
                                    SessaoUsuario.encerrar()
                                    sessaoVersao++
                                    alunoAutenticado = false
                                    perfilSelecionado = "ALUNO"
                                    tela = AppScreen.Login
                                }
                            )
                        } else if (perfilSelecionado == null) {
                            TelaEscolherPerfil(
                                onSelecionarPerfil = { perfil ->
                                    if (perfil == "ADMIN") {
                                        perfilSelecionado = "ADMIN"
                                        tela = AppScreen.Login
                                    } else {
                                        perfilSelecionado = perfil
                                    }
                                }
                            )
                        } else {
                            TelaLogin(
                                usuarioRepository = usuarioRepository,
                                perfil = perfilSelecionado!!,
                                onVoltar = {
                                    perfilSelecionado = null
                                },
                                onEntrar = { usuario ->
                                    SessaoUsuario.iniciar(usuario)
                                    sessaoVersao++
                                    if (usuario.perfil == "PROFESSOR" || usuario.perfil == "ALUNO") {
                                        FcmTokenManager.sincronizarTokenSessaoAtual(context)
                                        AlunoSyncWorker.agendar(context)
                                        AlunoSyncWorker.agendarPeriodica(context)
                                    }

                                    if (usuario.perfil == "PROFESSOR") {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                        if (!uid.isNullOrBlank()) {
                                            DispositivoRepository(context).registrarAtividade(uid)
                                        }
                                    }
                                    if (
                                        preferenciasSessao.getBoolean(
                                            CHAVE_MANTER_CONECTADO,
                                            false
                                        )
                                    ) {
                                        preferenciasSessao.edit()
                                            .putLong(CHAVE_USUARIO_ID, usuario.id)
                                            .putString(CHAVE_PERFIL, usuario.perfil)
                                            .apply()
                                    } else {
                                        preferenciasSessao.edit()
                                            .remove(CHAVE_USUARIO_ID)
                                            .remove(CHAVE_PERFIL)
                                            .apply()
                                    }
                                    perfilSelecionado = usuario.perfil
                                    when (usuario.perfil) {
                                        "PROFESSOR" -> {
                                            alunoAutenticado = false
                                            tela = AppScreen.Dashboard
                                        }

                                        "ALUNO" -> {
                                            alunoAutenticado = true
                                        }

                                        "ADMIN" -> {
                                            alunoAutenticado = false
                                            tela = AppScreen.AdminDashboard
                                        }

                                        else -> {
                                            SessaoUsuario.encerrar()
                                            sessaoVersao++
                                            perfilSelecionado = null
                                            tela = AppScreen.Login
                                        }
                                    }
                                }
                            )
                        }
                    }

                    AppScreen.CadastroProfessor -> {
                        TelaCadastroProfessor(
                            usuarioRepository = usuarioRepository,
                            onVoltar = {
                                tela = AppScreen.Login
                                perfilSelecionado = null
                            },
                            onCadastroConcluido = {
                                perfilSelecionado = "PROFESSOR"
                                tela = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.Dashboard -> {
                        TelaDashboard(
                            repository = repository,
                            treinoRepository = treinoRepository,
                            anamneseAtribuidaRepository = anamneseAtribuidaRepository,
                            onAlunos = {
                                filtroInicialAlunos = "Todos"
                                tela = AppScreen.Alunos
                            },
                            onFiltrarAlunos = { filtro ->
                                filtroInicialAlunos = filtro
                                tela = AppScreen.Alunos
                            },
                            onCadastrarAluno = {
                                tela = AppScreen.CadastroAluno
                            },
                            onTreinos = {
                                tela = AppScreen.Treinos
                            },
                            onRegistrarTreino = {
                                tela = AppScreen.SelecionarAlunoTreino
                            },
                            onModelosAnamnese = {
                                tela = AppScreen.ModelosAnamnese
                            },
                            onCentralAtencao = {
                                tela = AppScreen.CentralAtencao
                            },
                            onPerfil = { aluno ->
                                alunoSelecionadoId = aluno.id
                                tela = AppScreen.PerfilAluno
                            },
                            onSair = {
                                val usuarioSaindo = SessaoUsuario.usuarioAtual
                                val uidSaindo = FirebaseAuth.getInstance().currentUser?.uid
                                FcmTokenManager.removerTokenAoSair(
                                    context,
                                    usuarioSaindo,
                                    uidSaindo
                                )
                                AlunoSyncWorker.cancelarPeriodica(context)
                                preferenciasSessao.edit()
                                    .remove(CHAVE_USUARIO_ID)
                                    .remove(CHAVE_PERFIL)
                                    .apply()
                                SessaoUsuario.encerrar()
                                sessaoVersao++
                                alunoAutenticado = false
                                perfilSelecionado = "PROFESSOR"
                                tela = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.CadastroAluno -> {
                        TelaCadastroAluno(
                            repository = repository,
                            usuarioRepository = usuarioRepository,
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
                            filtroInicial = filtroInicialAlunos,
                            onVoltar = {
                                filtroInicialAlunos = "Todos"
                                tela = AppScreen.Dashboard
                            },
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
                                usuarioRepository = usuarioRepository,
                                alunoId = id,
                                repository = repository,
                                treinoRepository = treinoRepository,
                                anamneseRepository = anamneseRepository,
                                anamneseAtribuidaRepository = anamneseAtribuidaRepository,
                                onVoltar = {
                                    tela = AppScreen.Alunos
                                },
                                onDashboard = {
                                    tela = AppScreen.Dashboard
                                },
                                onTreinos = {
                                    tela = AppScreen.Treinos
                                },
                                onEditar = {
                                    tela = AppScreen.EditarAluno
                                },
                                onLinhaDoTempo = {
                                    tela = AppScreen.LinhaDoTempoAluno
                                },
                                onModelosAnamnese = {
                                    voltarModelosParaPerfil = true
                                    tela = AppScreen.ModelosAnamnese
                                },
                                onVisualizarAnamnese = { anamneseId ->
                                    anamneseAtribuidaSelecionadaId = anamneseId
                                    voltarVisualizacaoParaPerfil = true
                                    tela = AppScreen.VisualizarAnamnese
                                },
                                onVisualizarTreino = { treinoId ->
                                    treinoSelecionadoId = treinoId
                                    tela = AppScreen.DetalhesTreino
                                },
                                onPeriodizacao = { tela = AppScreen.Periodizacao }
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

                    AppScreen.ModelosAnamnese -> {
                        TelaModelosAnamnese(
                            repository = modeloAnamneseRepository,
                            alunoRepository = repository,
                            anamneseAtribuidaRepository = anamneseAtribuidaRepository,
                            perguntaModeloRepository = perguntaModeloAnamneseRepository,
                            alunoId = alunoSelecionadoId,
                            onVoltar = {
                                if (voltarModelosParaPerfil) {
                                    voltarModelosParaPerfil = false
                                    tela = AppScreen.PerfilAluno
                                } else {
                                    tela = AppScreen.Dashboard
                                }
                            },
                            onNovoModelo = {
                                modeloAnamneseSelecionadoId = null
                                tela = AppScreen.CriarModeloAnamnese
                            },
                            onEditarModelo = { modelo ->
                                modeloAnamneseSelecionadoId = modelo.id
                                tela = AppScreen.CriarModeloAnamnese
                            },
                            onPerguntas = { modelo ->
                                modeloAnamneseSelecionadoId = modelo.id
                                tela = AppScreen.FormularioAnamnese
                            },
                            onAbrirAnamnese = { anamneseId ->
                                anamneseAtribuidaSelecionadaId = anamneseId
                                voltarVisualizacaoParaPerfil = false
                                tela = AppScreen.VisualizarAnamnese
                            },
                            onAnamneseEnviada = {
                                if (voltarModelosParaPerfil) {
                                    voltarModelosParaPerfil = false
                                    tela = AppScreen.PerfilAluno
                                }
                            }
                        )
                    }

                    AppScreen.CriarModeloAnamnese -> {
                        TelaCriarModeloAnamnese(
                            modeloId = modeloAnamneseSelecionadoId,
                            repository = modeloAnamneseRepository,
                            perguntaRepository = perguntaModeloAnamneseRepository,
                            onVoltar = {
                                tela = AppScreen.ModelosAnamnese
                            },
                            onSalvo = {
                                modeloAnamneseSelecionadoId = null
                                tela = AppScreen.ModelosAnamnese
                            }
                        )
                    }

                    AppScreen.FormularioAnamnese -> {
                        val modeloId = modeloAnamneseSelecionadoId
                        if (modeloId != null) {
                            TelaPerguntasModeloAnamnese(
                                modeloId = modeloId,
                                repository = perguntaModeloAnamneseRepository,
                                onVoltar = {
                                    tela = AppScreen.ModelosAnamnese
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Nenhum modelo de anamnese foi selecionado.")
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        tela = AppScreen.ModelosAnamnese
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AzulPrincipal
                                    )
                                ) {
                                    Text("Voltar aos modelos")
                                }
                            }
                        }
                    }

                    AppScreen.VisualizarAnamnese -> {
                        val anamneseId = anamneseAtribuidaSelecionadaId
                        if (anamneseId != null) {
                            TelaVisualizarAnamnese(
                                anamneseId = anamneseId,
                                anamneseRepository = anamneseAtribuidaRepository,
                                respostaRepository = respostaAnamneseRepository,
                                onVoltar = {
                                    if (voltarVisualizacaoParaPerfil) {
                                        voltarVisualizacaoParaPerfil = false
                                        tela = AppScreen.PerfilAluno
                                    } else {
                                        tela = AppScreen.ModelosAnamnese
                                    }
                                },
                                onAnamneseExcluida = {
                                    AlunoSyncWorker.agendar(context)
                                    if (voltarVisualizacaoParaPerfil) {
                                        voltarVisualizacaoParaPerfil = false
                                        tela = AppScreen.PerfilAluno
                                    } else {
                                        tela = AppScreen.ModelosAnamnese
                                    }
                                }
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Nenhuma anamnese foi selecionada.")
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (voltarVisualizacaoParaPerfil) {
                                            voltarVisualizacaoParaPerfil = false
                                            tela = AppScreen.PerfilAluno
                                        } else {
                                            tela = AppScreen.ModelosAnamnese
                                        }
                                    }
                                ) {
                                    Text("VOLTAR")
                                }
                            }
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
    usuarioRepository: UsuarioRepository,
    onVoltar: () -> Unit,
    onCadastroConcluido: () -> Unit,
    onTreinos: () -> Unit
) {
    val context = LocalContext.current

    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var objective by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var observacoesProfissionais by remember { mutableStateOf("") }

    var mostrarSenha by remember { mutableStateOf(false) }
    var mostrarConfirmarSenha by remember { mutableStateOf(false) }

    val opcoesCondicoes = OPCOES_CONDICOES_PADRAO
    val opcoesRestricoes = OPCOES_RESTRICOES_PADRAO

    var condicoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var restricoesSelecionadas by remember {
        mutableStateOf(setOf<String>())
    }

    var outraCondicao by remember { mutableStateOf("") }
    var outraRestricao by remember { mutableStateOf("") }

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
            .statusBarsPadding()
    ) {

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

            Text(
                text = "Acesso do aluno",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )

            Text(
                text = "Cadastre uma senha para que o aluno possa acessar o aplicativo.",
                fontSize = 12.sp,
                color = TextoSecundario
            )

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha de acesso") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                visualTransformation = if (mostrarSenha) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            mostrarSenha = !mostrarSenha
                        }
                    ) {
                        Text(
                            if (mostrarSenha) "Ocultar" else "Mostrar"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = confirmarSenha,
                onValueChange = { confirmarSenha = it },
                label = { Text("Confirmar senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                visualTransformation = if (mostrarConfirmarSenha) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            mostrarConfirmarSenha = !mostrarConfirmarSenha
                        }
                    ) {
                        Text(
                            if (mostrarConfirmarSenha) "Ocultar" else "Mostrar"
                        )
                    }
                }
            )

            Text(
                text = "A senha deve possuir pelo menos 8 caracteres.",
                fontSize = 12.sp,
                color = TextoSecundario
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
                text = "Selecione as condições que se aplicam ao aluno (mesmo checklist da anamnese).",
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
                    onValueChange = {
                        outraCondicao = it
                    },
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
                                    requesterOutraRestricao.bringIntoView()
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
                    if (email.isBlank()) {
                        erro = "Informe o e-mail do aluno."
                        return@Button
                    }
                    if (objective.isBlank()) {
                        erro = "Informe o objetivo do aluno."
                        return@Button
                    }
                    if (senha.length < 8) {
                        erro = "A senha deve possuir pelo menos 8 caracteres."
                        return@Button
                    }
                    if (senha != confirmarSenha) {
                        erro = "As senhas não coincidem."
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
                            val aluno = AlunoEntity(
                                nome = nome.trim(),
                                email = email.trim().lowercase(),
                                dataCadastro = dataAtual,
                                objetivo = objective.trim(),
                                observacoesProfissionais =
                                    observacoesProfissionais.trim(),
                                condicoes = condicoesTexto,
                                restricoes = restricoesTexto,
                                status = status
                            )
                            val alunoId = repository.inserir(aluno)
                            try {
                                usuarioRepository.cadastrarAluno(
                                    nome = nome.trim(),
                                    email = email.trim().lowercase(),
                                    senha = senha,
                                    alunoId = alunoId
                                )
                            } catch (e: Exception) {
                                repository.excluir(
                                    aluno.copy(id = alunoId)
                                )
                                throw e
                            }
                            AlunoSyncWorker.agendar(context)
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = FitCareDatabase.getDatabase(context)
                                    FirestoreSyncService(db.usuarioDao(), db).sincronizarAlunos()
                                } catch (_: Exception) {
                                }
                            }
                            onCadastroConcluido()

                        } catch (e: Exception) {
                            erro =
                                "Não foi possível cadastrar o aluno: " +
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

@Composable
fun TelaLogin(
    usuarioRepository: UsuarioRepository,
    perfil: String,
    onVoltar: () -> Unit,
    onEntrar: (UsuarioEntity) -> Unit
) {
    val context = LocalContext.current
    val preferencias = remember {
        context.getSharedPreferences(
            PREFERENCIAS_SESSAO,
            Context.MODE_PRIVATE
        )
    }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var manterConectado by remember {
        mutableStateOf(
            preferencias.getBoolean(
                CHAVE_MANTER_CONECTADO,
                false
            )
        )
    }
    var carregando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }
    var mostrarAjudaSenha by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val azulClaro = if (temaEscuroAtivoState) Color(0xFF162434) else Color(0xFFEAF5FF)
    val azulFundo = if (temaEscuroAtivoState) Color(0xFF0F1722) else Color(0xFFD8ECFF)
    val descricaoPerfil = when (perfil) {
        "PROFESSOR" -> "Acesse sua conta profissional."
        "ALUNO" -> "Acesse sua conta de aluno."
        "ADMIN" -> "Acesse sua conta administrativa."
        else -> "Informe seus dados de acesso."
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(azulClaro, azulFundo)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
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
                painter = painterResource(R.drawable.logo_fitcare),
                contentDescription = "Logo Ícaros FitCare",
                modifier = Modifier
                    .width(220.dp)
                    .height(145.dp)
            )
            Text(
                text = "Bem-vindo de volta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descricaoPerfil,
                color = TextoSecundario
            )
            Spacer(modifier = Modifier.height(28.dp))
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    mensagemErro = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("E-mail") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = senha,
                onValueChange = {
                    senha = it
                    mensagemErro = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Senha") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                visualTransformation = if (senhaVisivel) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(
                        onClick = {
                            senhaVisivel = !senhaVisivel
                        }
                    ) {
                        Text(
                            if (senhaVisivel) "Ocultar" else "Mostrar"
                        )
                    }
                }
            )
            if (perfil == "ALUNO") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            mostrarAjudaSenha = true
                        }
                    ) {
                        Text(
                            text = "Esqueci minha senha",
                            color = AzulPrincipal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = manterConectado,
                    onCheckedChange = { marcado ->
                        manterConectado = marcado
                        preferencias.edit()
                            .putBoolean(
                                CHAVE_MANTER_CONECTADO,
                                marcado
                            )
                            .apply()
                        if (!marcado) {
                            preferencias.edit()
                                .remove(CHAVE_USUARIO_ID)
                                .remove(CHAVE_PERFIL)
                                .apply()
                        }
                    }
                )
                Text(
                    text = "Manter-me conectado",
                    color = TextoPrincipal
                )
            }
            if (mensagemErro != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mensagemErro!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (email.isBlank() || senha.isBlank()) {
                        mensagemErro =
                            "Informe seu e-mail e sua senha."
                        return@Button
                    }
                    scope.launch {
                        carregando = true
                        mensagemErro = null
                        try {
                            val usuario = usuarioRepository.autenticar(
                                email = email.trim(),
                                senha = senha,
                                perfilEsperado = perfil
                            )
                            if (usuario == null) {
                                mensagemErro =
                                    "E-mail, senha ou perfil incorreto."
                            } else if (usuario.perfil != perfil) {
                                mensagemErro =
                                    "Esta conta não pertence ao perfil selecionado."
                            } else {
                                preferencias.edit().apply {
                                    putBoolean(
                                        CHAVE_MANTER_CONECTADO,
                                        manterConectado
                                    )
                                    if (manterConectado) {
                                        putLong(
                                            CHAVE_USUARIO_ID,
                                            usuario.id
                                        )
                                        putString(
                                            CHAVE_PERFIL,
                                            usuario.perfil
                                        )
                                    } else {
                                        remove(CHAVE_USUARIO_ID)
                                        remove(CHAVE_PERFIL)
                                    }
                                    apply()
                                }
                                onEntrar(usuario)
                            }
                        } catch (e: Exception) {
                            mensagemErro =
                                e.message
                                    ?: "Não foi possível realizar o login."
                        } finally {
                            carregando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !carregando,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    if (carregando) "AGUARDE..." else "ENTRAR"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onVoltar
            ) {
                Text(
                    "Voltar",
                    color = AzulPrincipal
                )
            }
        }
    }
    if (mostrarAjudaSenha && perfil == "ALUNO") {
        AlertDialog(
            onDismissRequest = {
                mostrarAjudaSenha = false
            },
            title = {
                Text(
                    text = "Recuperar acesso",
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal
                )
            },
            text = {
                Text(
                    text = "Para redefinir sua senha, entre em contato " +
                            "com seu professor e solicite a recuperação " +
                            "do acesso à sua conta.\n\n" +
                            "Após a redefinição, você poderá entrar " +
                            "normalmente utilizando sua nova senha."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarAjudaSenha = false
                    }
                ) {
                    Text("ENTENDI")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// Tela de Dashboard principal do professor (com sino zerando notificações lidas e Central de Atenção)
@Composable
fun TelaDashboard(
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    anamneseAtribuidaRepository: AnamneseAtribuidaRepository,
    onAlunos: () -> Unit,
    onFiltrarAlunos: (String) -> Unit = { onAlunos() },
    onCadastrarAluno: () -> Unit,
    onTreinos: () -> Unit,
    onRegistrarTreino: () -> Unit,
    onPerfil: (br.com.icaros.fitcare.data.AlunoEntity) -> Unit,
    onModelosAnamnese: () -> Unit = {},
    onSair: () -> Unit,
    onCentralAtencao: () -> Unit = {}
) {
    val context = LocalContext.current
    var pesquisa by remember { mutableStateOf("") }
    var mostrarNotificacoes by remember { mutableStateOf(false) }
    var mostrarDialogoCentralAtencao by remember { mutableStateOf(false) }
    var statusSincronizacao by remember { mutableStateOf("Sincronizado") }
    var nuvemSincronizada by remember { mutableStateOf(true) }

    val prefsNotificacoes = remember {
        context.getSharedPreferences("fitcare_notificacoes", Context.MODE_PRIVATE)
    }

    var idsNotificacoesLidas by remember {
        mutableStateOf(
            prefsNotificacoes.getStringSet("notificacoes_lidas_ids", emptySet())
                ?.toSet()
                ?: emptySet()
        )
    }

    fun marcarNotificacaoComoLida(chaveNotificacao: String) {
        val atualizado = idsNotificacoesLidas + chaveNotificacao
        idsNotificacoesLidas = atualizado
        prefsNotificacoes.edit()
            .putStringSet("notificacoes_lidas_ids", atualizado)
            .apply()
    }

    fun verificarStatusNuvem() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val redeAtiva = cm?.activeNetwork != null
        if (!redeAtiva) {
            nuvemSincronizada = false
            statusSincronizacao = "Offline • Aguardando conexão"
        } else {
            nuvemSincronizada = true
            statusSincronizacao = "Sincronizado com a nuvem"
        }
    }

    LaunchedEffect(Unit) {
        verificarStatusNuvem()
        withContext(Dispatchers.IO) {
            try {
                val db = FitCareDatabase.getDatabase(context)
                val agora = System.currentTimeMillis()
                val ultimaSync = prefsNotificacoes.getLong("ultima_sync_dashboard_ms", 0L)
                // Só força sincronização completa se passaram mais de 5 minutos (300.000 ms)
                if (agora - ultimaSync > 300_000L) {
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarModelosAnamnese()
                    syncService.sincronizarAnamnesesAtribuidas()
                    prefsNotificacoes.edit()
                        .putLong("ultima_sync_dashboard_ms", agora)
                        .apply()
                }
                val pendentes = db.syncOperationDao().listarPendentes().size
                withContext(Dispatchers.Main) {
                    if (pendentes > 0) {
                        nuvemSincronizada = false
                        statusSincronizacao = "Sincronizando ($pendentes pendente(s))..."
                    } else {
                        verificarStatusNuvem()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    verificarStatusNuvem()
                }
            }
        }
    }

    val alunos by repository
        .listarAlunos()
        .collectAsState(initial = emptyList())

    val todosTreinos by treinoRepository
        .listarTodosTreinos()
        .collectAsState(initial = emptyList())

    // Alertas de treinos a vencer (7 dias) e vencidos
    val alertasTreinos = remember(todosTreinos, alunos) {
        val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
            isLenient = false
        }
        val hoje = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.time
        val limite7Dias = java.util.Calendar.getInstance().apply {
            time = hoje
            add(java.util.Calendar.DAY_OF_YEAR, 7)
        }.time

        todosTreinos.mapNotNull { treino ->
            val dataFimStr = treino.dataFim.trim()
            if (dataFimStr.isBlank()) return@mapNotNull null
            val dataFim = runCatching { formatoData.parse(dataFimStr) }.getOrNull() ?: return@mapNotNull null
            val aluno = alunos.firstOrNull { it.id == treino.alunoId } ?: return@mapNotNull null

            when {
                dataFim.before(hoje) -> Triple(aluno, treino, "VENCIDO")
                !dataFim.after(limite7Dias) -> Triple(aluno, treino, "A_VENCER")
                else -> null
            }
        }
    }

    val notificacoesRespondidas by produceState(
        initialValue = emptyList<Pair<AlunoEntity, AnamneseAtribuidaEntity>>(),
        key1 = alunos
    ) {
        val lista = mutableListOf<Pair<AlunoEntity, AnamneseAtribuidaEntity>>()
        for (aluno in alunos) {
            try {
                val atribuicoes = anamneseAtribuidaRepository
                    .listarPorAluno(aluno.id)
                    .first()
                atribuicoes
                    .filter { it.status == "RESPONDIDA" }
                    .forEach { anamnese ->
                        lista.add(aluno to anamnese)
                    }
            } catch (_: Exception) {
            }
        }
        value = lista.sortedByDescending { it.second.updatedAtMillis }
    }

    val notificacoesAnamneseNaoLidas = notificacoesRespondidas.filter { (_, anamnese) ->
        "anamnese_${anamnese.id}" !in idsNotificacoesLidas && anamnese.id.toString() !in idsNotificacoesLidas
    }

    val alertasTreinosNaoLidos = alertasTreinos.filter { (_, treino, tipo) ->
        "treino_${tipo}_${treino.id}_${treino.dataFim}" !in idsNotificacoesLidas
    }

    val totalNotificacoesNaoLidas = notificacoesAnamneseNaoLidas.size + alertasTreinosNaoLidos.size

    // Notificações na barra de status para anamneses concluídas
    LaunchedEffect(notificacoesAnamneseNaoLidas) {
        if (notificacoesAnamneseNaoLidas.isNotEmpty()) {
            val jaNotificadas = prefsNotificacoes
                .getStringSet("notificacoes_push_exibidas_ids", emptySet())
                ?.toMutableSet()
                ?: mutableSetOf()

            var houveNova = false
            for ((aluno, anamnese) in notificacoesAnamneseNaoLidas) {
                val idStr = "anamnese_${anamnese.id}"
                if (idStr !in jaNotificadas) {
                    FitCareMessagingService.exibirNotificacao(
                        context = context,
                        titulo = "Anamnese concluída",
                        mensagem = "${aluno.nome} respondeu a anamnese \"${anamnese.nomeModelo}\".",
                        notificacaoId = (anamnese.id % Int.MAX_VALUE).toInt()
                    )
                    jaNotificadas.add(idStr)
                    houveNova = true
                }
            }

            if (houveNova) {
                prefsNotificacoes.edit()
                    .putStringSet("notificacoes_push_exibidas_ids", jaNotificadas)
                    .apply()
            }
        }
    }

    // Notificações na barra de status para treinos a vencer (7 dias) e vencidos
    LaunchedEffect(alertasTreinosNaoLidos) {
        if (alertasTreinosNaoLidos.isNotEmpty()) {
            val jaNotificadas = prefsNotificacoes
                .getStringSet("notificacoes_push_exibidas_ids", emptySet())
                ?.toMutableSet()
                ?: mutableSetOf()

            var houveNova = false
            for ((aluno, treino, tipo) in alertasTreinosNaoLidos) {
                val chave = "treino_${tipo}_${treino.id}_${treino.dataFim}"
                if (chave !in jaNotificadas) {
                    val tituloPush = if (tipo == "VENCIDO") {
                        "Treino vencido"
                    } else {
                        "Treino próximo do vencimento"
                    }
                    val msgPush = if (tipo == "VENCIDO") {
                        "O treino \"${treino.nome}\" de ${aluno.nome} venceu em ${treino.dataFim}."
                    } else {
                        "O treino \"${treino.nome}\" de ${aluno.nome} vence em ${treino.dataFim} (menos de 7 dias)."
                    }
                    FitCareMessagingService.exibirNotificacao(
                        context = context,
                        titulo = tituloPush,
                        mensagem = msgPush,
                        notificacaoId = ((treino.id + 50000L) % Int.MAX_VALUE).toInt()
                    )
                    jaNotificadas.add(chave)
                    houveNova = true
                }
            }

            if (houveNova) {
                prefsNotificacoes.edit()
                    .putStringSet("notificacoes_push_exibidas_ids", jaNotificadas)
                    .apply()
            }
        }
    }

    val totalAlunos = alunos.size
    val alunosEmAtencao = alunos.count { aluno ->
        aluno.status == "Atenção" || aluno.status == "Prioridade"
    }
    val alunosEmDia = alunos.count { aluno ->
        aluno.status == "Normal"
    }
    val alunosFiltrados = alunos.filter { aluno ->
        aluno.nome.contains(pesquisa.trim(), ignoreCase = true)
    }

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
                    .padding(top = 4.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        IconButton(
                            onClick = { mostrarNotificacoes = true }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (totalNotificacoesNaoLidas > 0) {
                                        Badge(
                                            containerColor = Color(0xFFD32F2F),
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = totalNotificacoesNaoLidas.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = "Notificações",
                                    tint = AzulPrincipal,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                    Image(
                        painter = painterResource(
                            id = R.drawable.logo_fitcare
                        ),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(145.dp)
                            .height(70.dp)
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
// Selo de Perfil + Indicador discreto de Sincronização
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 18.dp, end = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AzulPrincipal.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "PERFIL: PROFESSOR",
                        modifier = Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 5.dp
                        ),
                        color = AzulPrincipal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (nuvemSincronizada) Color(0xFFD8F3DF) else Color(0xFFFFEAB5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    color = if (nuvemSincronizada) Color(0xFF2E7D32) else Color(0xFFB17B00),
                                    shape = CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusSincronizacao,
                            color = if (nuvemSincronizada) Color(0xFF2E7D32) else Color(0xFF9A7000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.clickable {
                        val novoTema = !temaEscuroAtivoState
                        temaEscuroAtivoState = novoTema
                        context.getSharedPreferences(PREFERENCIAS_SESSAO, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("tema_escuro_ativo", novoTema)
                            .apply()

                    },
                    shape = RoundedCornerShape(20.dp),
                    color = AzulMuitoClaro
                ) {
                    Text(
                        text = if (temaEscuroAtivoState) "☀ Claro" else "☾ Escuro",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = AzulPrincipal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            var nomeRealProfessor by remember {
                mutableStateOf(
                    SessaoUsuario.usuarioAtual?.nome
                        ?.trim()
                        ?.takeIf { it.isNotBlank() && !it.contains("@") }
                        .orEmpty()
                )
            }
            LaunchedEffect(Unit) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (!uid.isNullOrBlank() && nomeRealProfessor.isBlank()) {
                    try {
                        val doc = FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(uid)
                            .get()
                            .await()
                        val nomeFirestore = doc.getString("nome")?.trim().orEmpty()
                        if (nomeFirestore.isNotBlank()) {
                            nomeRealProfessor = nomeFirestore
                        }
                    } catch (_: Exception) {
                    }
                }
            }
            val primeiroNomeProfessor = when {
                nomeRealProfessor.equals("icaroalvesdasilva2005", ignoreCase = true) -> "Ícaro"
                SessaoUsuario.usuarioAtual?.email.equals(
                    "icaroalvesdasilva2005@gmail.com",
                    ignoreCase = true
                ) &&
                        (nomeRealProfessor.isBlank() || nomeRealProfessor.startsWith(
                            "icaroalves",
                            ignoreCase = true
                        )) -> "Ícaro"
                else -> nomeRealProfessor.substringBefore(" ").trim()
            }
            Text(
                text = if (primeiroNomeProfessor.isNotBlank()) {
                    "Bem-vindo de volta, $primeiroNomeProfessor!"
                } else {
                    "Bem-vindo de volta!"
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextoPrincipal,
                modifier = Modifier.padding(
                    start = 22.dp,
                    end = 22.dp,
                    top = 12.dp,
                    bottom = 0.dp
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp)
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
                        focusedContainerColor = Branco,
                        unfocusedContainerColor = Branco,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro,
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal
                    )
                )
                val alunosStatusNormal = alunos.count { it.status == "Normal" }
                val alunosStatusAtencao = alunos.count { it.status == "Atenção" }
                val alunosStatusPrioridade = alunos.count { it.status == "Prioridade" }

                val eventosStorage = remember { EventoAlunoStorage(context) }
                val todosEventos = remember(alunos, todosTreinos) {
                    eventosStorage.listarTodos()
                }

                val metricasFrequencia = remember(alunos, todosEventos, todosTreinos) {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
                        isLenient = false
                    }
                    val agoraCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 23)
                        set(java.util.Calendar.MINUTE, 59)
                        set(java.util.Calendar.SECOND, 59)
                    }
                    val hojeMs = agoraCal.timeInMillis

                    val inicioSemanaCal = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    val inicioSemanaMs = inicioSemanaCal.timeInMillis
                    val seteDiasAtrasMs = hojeMs - (7L * 24L * 60L * 60L * 1000L)

                    val idsAlunosProfessor = alunos.map { it.id }.toSet()
                    val eventosTreinoCarteira = todosEventos.filter { ev ->
                        ev.alunoId in idsAlunosProfessor &&
                                ev.tipo.equals("Treino", ignoreCase = true)
                    }

                    val frequenciaSemanalGeral = eventosTreinoCarteira.count { ev ->
                        val dataMs = runCatching { fmt.parse(ev.data)?.time }.getOrNull() ?: 0L
                        dataMs in inicioSemanaMs..hojeMs
                    }

                    val mediaSemanal = if (totalAlunos > 0) {
                        frequenciaSemanalGeral.toFloat() / totalAlunos.toFloat()
                    } else {
                        0f
                    }

                    val semTreinarMais7Dias = alunos.count { aluno ->
                        val ultimoEventoTreinoMs = eventosTreinoCarteira
                            .filter { it.alunoId == aluno.id }
                            .mapNotNull { runCatching { fmt.parse(it.data)?.time }.getOrNull() }
                            .maxOrNull()

                        val ultimoInicioFichaMs = todosTreinos
                            .filter { it.alunoId == aluno.id }
                            .mapNotNull { runCatching { fmt.parse(it.dataInicio)?.time }.getOrNull() }
                            .maxOrNull()

                        val ultimaAtividadeMs = listOfNotNull(ultimoEventoTreinoMs, ultimoInicioFichaMs).maxOrNull()
                        ultimaAtividadeMs == null || ultimaAtividadeMs < seteDiasAtrasMs
                    }

                    Triple(frequenciaSemanalGeral, mediaSemanal, semTreinarMais7Dias)
                }

                GraficoCarteiraAlunos(
                    totalAlunos = totalAlunos,
                    emDia = alunosStatusNormal,
                    emAtencao = alunosStatusAtencao,
                    prioridade = alunosStatusPrioridade,
                    frequenciaSemanalGeral = metricasFrequencia.first,
                    mediaFrequenciaSemanal = metricasFrequencia.second,
                    alunosSemTreinar7Dias = metricasFrequencia.third
                )
                Spacer(modifier = Modifier.height(14.dp))
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
                        titulo = "Central de atenção",
                        subtitulo = "Alertas e prioridades",
                        corFundo = Color(0xFFFFD9DC),
                        corTexto = Color(0xFF9C4350),
                        icone = "!",
                        onClick = onCentralAtencao
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
                        modifier = Modifier.clickable { onAlunos() }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (alunosFiltrados.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(containerColor = Branco)
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
    if (mostrarDialogoCentralAtencao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCentralAtencao = false },
            title = {
                Text(
                    text = "Central de Atenção",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
            },
            text = {
                Text(
                    text = "Esta tela dedicada aos alertas clínicos e prioridades está em desenvolvimento. " +
                            "Você já pode filtrar os alunos com status 'Atenção' ou 'Prioridade' na tela Consultar Alunos.",
                    fontSize = 13.sp,
                    color = TextoSecundario
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoCentralAtencao = false
                        onAlunos()
                    }
                ) {
                    Text("Ver alunos", color = AzulPrincipal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarDialogoCentralAtencao = false }
                ) {
                    Text("Fechar")
                }
            }
        )
    }
// Diálogo do sino de notificações (Anamneses respondidas + Treinos a vencer / vencidos)
    if (mostrarNotificacoes) {
        AlertDialog(
            onDismissRequest = { mostrarNotificacoes = false },
            title = {
                Text(
                    text = "Notificações e Alertas",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulPrincipal
                )
            },
            text = {
                if (notificacoesRespondidas.isEmpty() && alertasTreinos.isEmpty()) {
                    Text(
                        text = "Nenhuma notificação ou alerta de vencimento no momento.",
                        fontSize = 13.sp,
                        color = TextoSecundario
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
// Alertas de treinos vencidos e a vencer (7 dias)
                        alertasTreinos.forEach { (aluno, treino, tipo) ->
                            val chave = "treino_${tipo}${treino.id}${treino.dataFim}"
                            val jaLida = chave in idsNotificacoesLidas
                            val ehVencido = tipo == "VENCIDO"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        marcarNotificacaoComoLida(chave)
                                        mostrarNotificacoes = false
                                        onPerfil(aluno)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        jaLida -> Color(0xFFF2F4F7)
                                        ehVencido -> Color(0xFFFFD9DC)
                                        else -> Color(0xFFFFEAB5)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = if (ehVencido) {
                                            "Treino vencido • ${aluno.nome}"
                                        } else {
                                            "Treino vence em até 7 dias • ${aluno.nome}"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ehVencido) Color(0xFFB42332) else Color(0xFF9A7000)
                                    )
                                    Text(
                                        text = "Ficha: ${treino.nome} (Vencimento: ${treino.dataFim})",
                                        fontSize = 12.sp,
                                        color = TextoPrincipal
                                    )
                                    Text(
                                        text = "Toque para abrir o perfil ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AzulPrincipal
                                    )
                                }
                            }
                        }
// Notificações de anamneses respondidas
                        notificacoesRespondidas.forEach { (aluno, anamnese) ->
                            val chave = "anamnese_${anamnese.id}"
                            val jaLida = chave in idsNotificacoesLidas || anamnese.id.toString() in idsNotificacoesLidas
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        marcarNotificacaoComoLida(chave)
                                        mostrarNotificacoes = false
                                        onPerfil(aluno)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (jaLida) Color(0xFFF2F4F7) else AzulMuitoClaro
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = "${aluno.nome} respondeu a anamnese" +
                                                if (!jaLida) " (Nova)" else "",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AzulPrincipal
                                    )
                                    Text(
                                        text = anamnese.nomeModelo,
                                        fontSize = 12.sp,
                                        color = TextoPrincipal
                                    )
                                    if (anamnese.dataResposta.isNotBlank()) {
                                        Text(
                                            text = "Concluída em: ${anamnese.dataResposta}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Text(
                                        text = "Toque para abrir o perfil ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AzulPrincipal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { mostrarNotificacoes = false }
                ) {
                    Text("Fechar", color = AzulPrincipal)
                }
            }
        )
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Branco),
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
        filtroInicial: String = "Todos",
        onVoltar: () -> Unit,
        onSelecionarAluno: (br.com.icaros.fitcare.data.AlunoEntity) -> Unit,
        onTreinos: () -> Unit,
        modoSelecaoTreino: Boolean = false,
        onCadastrarAluno: () -> Unit = {}
    ) {
        var pesquisa by remember { mutableStateOf("") }
        var filtroSelecionado by remember(filtroInicial) { mutableStateOf(filtroInicial) }
        var filtroAcompanhamento by remember { mutableStateOf("Todos") }

        val alunos by repository
            .listarAlunos()
            .collectAsState(initial = emptyList())

        val termo = pesquisa.trim()

        val contextAlunos = LocalContext.current
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                try {
                    val db = FitCareDatabase.getDatabase(contextAlunos)
                    val syncService = FirestoreSyncService(db.usuarioDao(), db)
                    syncService.sincronizarAlunos()
                    syncService.sincronizarLeituraAlunos()
                } catch (_: Exception) {
                }
            }
        }

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
                        .padding(top = 4.dp, bottom = 10.dp)
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
                                .width(145.dp)
                                .height(70.dp)
                        )

                        Box(
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            BotaoVoltarPadrao(
                                onVoltar = onVoltar
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
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
                            focusedContainerColor = Branco,
                            unfocusedContainerColor = Branco,
                            focusedBorderColor = AzulPrincipal,
                            unfocusedBorderColor = AzulMuitoClaro,
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                                            Branco
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
                            val selecionado = filtroAcompanhamento == filtro
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
                    if (filtrados.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Branco
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
                                    containerColor = Branco
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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
        }.sortedWith(
            compareByDescending<EventoAluno> { e ->
                runCatching {
                    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).parse(e.data)?.time
                }.getOrNull() ?: 0L
            }.thenByDescending { e -> e.id }
        )
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

// Tela perfil aluno
@Composable
fun TelaPerfilAluno(
    alunoId: Long,
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    anamneseRepository: AnamneseRepository,
    anamneseAtribuidaRepository: AnamneseAtribuidaRepository,
    usuarioRepository: br.com.icaros.fitcare.data.UsuarioRepository,
    onVoltar: () -> Unit,
    onDashboard: () -> Unit,
    onTreinos: () -> Unit,
    onEditar: () -> Unit,
    onLinhaDoTempo: () -> Unit,
    onModelosAnamnese: () -> Unit,
    onVisualizarAnamnese: (Long) -> Unit = {},
    onVisualizarTreino: (Long) -> Unit = {},
    onPeriodizacao: () -> Unit = {}
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
// Sincroniza imediatamente ao abrir o perfil do aluno para trazer atualizações de outros aparelhos
    DisposableEffect(alunoId) {
        var subListener: com.google.firebase.firestore.ListenerRegistration? = null
        scope.launch(Dispatchers.IO) {
            try {
                val db = FitCareDatabase.getDatabase(contextoEventosPerfil)
                val syncService = FirestoreSyncService(db.usuarioDao(), db)
                syncService.sincronizarLeituraAlunos()
                syncService.sincronizarAnamnesesAtribuidas()

                val fbId = db.alunoDao().buscarPorIdDireto(alunoId)?.firebaseId
                if (!fbId.isNullOrBlank()) {
                    subListener = FirebaseFirestore.getInstance()
                        .collection("alunos")
                        .document(fbId)
                        .collection("anamneses_atribuidas")
                        .addSnapshotListener { snap, err ->
                            if (err == null && snap != null) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        syncService.sincronizarAnamnesesAtribuidas()
                                    } catch (_: Exception) {
                                    }
                                }
                            }
                        }
                }
            } catch (_: Exception) {
            }
        }
        onDispose {
            subListener?.remove()
        }
    }

    val documentosAnamnese by anamneseRepository
        .listarDocumentos(alunoId)
        .collectAsState(initial = emptyList())
    val documentosMedicos = documentosAnamnese.filterNot { documento ->
        documento.tipoArquivo.startsWith("comparacao_", ignoreCase = true) ||
                documento.nomeArquivo.startsWith("comparacao_", ignoreCase = true)
    }
    val imagensComparacao = documentosAnamnese.filter { documento ->
        documento.tipoArquivo.startsWith("comparacao_", ignoreCase = true) ||
                documento.nomeArquivo.startsWith("comparacao_", ignoreCase = true)
    }
    val avaliacoesAnamnese by anamneseAtribuidaRepository
        .listarPorAluno(alunoId)
        .collectAsState(initial = emptyList())
    val storageEventosPerfil = remember(contextoEventosPerfil) {
        EventoAlunoStorage(contextoEventosPerfil)
    }
    var eventosAluno by remember(alunoId) {
        mutableStateOf(storageEventosPerfil.listarPorAluno(alunoId))
    }
    var documentoParaExcluir by remember {
        mutableStateOf<br.com.icaros.fitcare.data.DocumentoAnamneseEntity?>(null)
    }
    var excluindoDocumento by remember {
        mutableStateOf(false)
    }
    var erroExclusaoDocumento by remember {
        mutableStateOf("")
    }
    var mostrarCadastroAcessoAluno by remember {
        mutableStateOf(false)
    }
    var nomeAcessoAluno by remember { mutableStateOf("") }
    var emailAcessoAluno by remember { mutableStateOf("") }
    var senhaAcessoAluno by remember { mutableStateOf("") }
    var confirmarSenhaAcessoAluno by remember { mutableStateOf("") }
    var carregandoCadastroAcesso by remember {
        mutableStateOf(false)
    }
    var erroCadastroAcesso by remember { mutableStateOf("") }
    var sucessoCadastroAcesso by remember { mutableStateOf("") }
    var mostrarDialogoRedefinirSenha by remember {
        mutableStateOf(false)
    }
    var novaSenhaAluno by remember {
        mutableStateOf("")
    }
    var confirmarNovaSenhaAluno by remember {
        mutableStateOf("")
    }
    var carregandoRedefinicaoSenha by remember {
        mutableStateOf(false)
    }
    var erroRedefinicaoSenha by remember {
        mutableStateOf("")
    }
    var sucessoRedefinicaoSenha by remember {
        mutableStateOf("")
    }
    var verificandoAcessoAluno by remember {
        mutableStateOf(false)
    }
    var anamneseParaExcluirId by remember {
        mutableStateOf<Long?>(null)
    }
    var excluindoAnamnese by remember {
        mutableStateOf(false)
    }
    var erroExclusaoAnamnese by remember {
        mutableStateOf("")
    }
    LaunchedEffect(alunoId) {
        repository.buscarPorId(alunoId).collect { resultado ->
            alunoAtual = resultado
            if (resultado != null) {
                nomeAcessoAluno = resultado.nome
                emailAcessoAluno = resultado.email
            }
            carregando = false
        }
    }
    LaunchedEffect(alunoId) {
        eventosAluno = storageEventosPerfil.listarPorAluno(alunoId)
    }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
// Cabeçalho compacto com espaçamento reduzido entre logo e selo PERFIL: PROFESSOR
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
                .padding(top = 2.dp, bottom = 4.dp)
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
                        .width(125.dp)
                        .height(58.dp)
                )
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(onVoltar = onVoltar)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AzulPrincipal.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "PERFIL: PROFESSOR",
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 5.dp
                    ),
                    color = AzulPrincipal,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFE9E7EC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = nomeAluno,
                        color = TextoPrincipal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = objetivoAluno,
                        color = TextoSecundario,
                        fontSize = 13.sp
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
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
                    Spacer(modifier = Modifier.height(10.dp))
                    LinhaDado("E-mail", alunoAtual!!.email)
                    LinhaDado("Cadastro", alunoAtual!!.dataCadastro)
                    LinhaDado("Objetivo", alunoAtual!!.objetivo)
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
                Button(
                    onClick = {
// Puxa sempre os dados cadastrados do aluno ao abrir o gerenciamento de acesso
                        nomeAcessoAluno = alunoAtual?.nome.orEmpty()
                        emailAcessoAluno = alunoAtual?.email.orEmpty()
                        senhaAcessoAluno = ""
                        confirmarSenhaAcessoAluno = ""
                        novaSenhaAluno = ""
                        confirmarNovaSenhaAluno = ""
                        erroCadastroAcesso = ""
                        erroRedefinicaoSenha = ""
                        scope.launch {
                            verificandoAcessoAluno = true
                            try {
                                val possuiAcesso =
                                    usuarioRepository.alunoPossuiAcesso(alunoId)
                                if (possuiAcesso) {
                                    mostrarDialogoRedefinirSenha = true
                                } else {
                                    mostrarCadastroAcessoAluno = true
                                }
                            } catch (e: Exception) {
                                erroCadastroAcesso = e.message
                                    ?: "Não foi possível verificar o acesso do aluno."
                            } finally {
                                verificandoAcessoAluno = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(48.dp),
                    enabled = !verificandoAcessoAluno,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF36844A),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (verificandoAcessoAluno) {
                            "VERIFICANDO ACESSO..."
                        } else {
                            "Gerenciar acesso do aluno"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        erroExclusao = ""
                        mostrarDialogoExclusao = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Vermelho
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Vermelho
                    )
                ) {
                    Text("Excluir aluno")
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
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
                    Spacer(modifier = Modifier.height(12.dp))
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
                // Cartão histórico de treinos
                val treinosDoAluno by treinoRepository
                    .listarTreinosDoAluno(alunoId)
                    .collectAsState(initial = emptyList())

                val treinosOrdenados = remember(treinosDoAluno) {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
                    treinosDoAluno.sortedByDescending { t ->
                        runCatching { fmt.parse(t.dataInicio)?.time }.getOrNull() ?: t.id
                    }
                }

                Cartao(
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Assignment,
                            contentDescription = "Histórico de treinos",
                            tint = AzulPrincipal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico de treinos (${treinosOrdenados.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (treinosOrdenados.isEmpty()) {
                        Text(
                            text = "Nenhum treino registrado para este aluno.",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    } else {
                        treinosOrdenados.forEach { treino ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .clickable {
                                        onVisualizarTreino(treino.id)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AzulMuitoClaro
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = treino.nome,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipal
                                    )
                                    Text(
                                        text = "Início: ${treino.dataInicio}" +
                                                if (treino.dataFim.isNotBlank()) " • Término: ${treino.dataFim}" else "",
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Toque para ver exercícios e cargas ›",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AzulPrincipal
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onPeriodizacao,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Periodização (Macro / Meso / Micro)")
                    }
                }
// ANAMNESE E DOCUMENTOS (BOTÕES SEPARADOS: ATRIBUIR E VISUALIZAR)
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
                        text = "Atribua uma anamnese ao aluno e consulte as respostas e os documentos médicos anexados por ele.",
                        color = TextoSecundario,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
// BOTÃO 1: ATRIBUIR ANAMNESE PARA O ALUNO
                    Button(
                        onClick = onModelosAnamnese,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulPrincipal
                        )
                    ) {
                        Text(
                            text = "Gerenciar documentos de anamnese",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Anamneses atribuídas (${avaliacoesAnamnese.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (avaliacoesAnamnese.isEmpty()) {
                        Text(
                            text = "Nenhuma anamnese foi atribuída a este aluno.",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    } else {
                        avaliacoesAnamnese.forEach { avaliacao ->
                            val (rotuloStatus, fundoStatus, textoStatus) =
                                when (avaliacao.status) {
                                    "NAO_ENVIADA" -> Triple(
                                        "Não enviada",
                                        Color(0xFFFFF3E0),
                                        Color(0xFF8D5A00)
                                    )

                                    "PENDENTE" -> Triple(
                                        "Pendente",
                                        Color(0xFFFFF3E0),
                                        Color(0xFF8D5A00)
                                    )

                                    "EM_ANDAMENTO" -> Triple(
                                        "Em andamento",
                                        Color(0xFFE3F2FD),
                                        Color(0xFF1565C0)
                                    )

                                    "RESPONDIDA" -> Triple(
                                        "Respondida",
                                        Color(0xFFE8F5E9),
                                        Color(0xFF2E7D32)
                                    )

                                    else -> Triple(
                                        avaliacao.status,
                                        Color.LightGray,
                                        Color.DarkGray
                                    )
                                }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AzulMuitoClaro
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = avaliacao.nomeModelo,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextoPrincipal
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = fundoStatus,
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = 5.dp
                                            )
                                    ) {
                                        Text(
                                            text = rotuloStatus,
                                            color = textoStatus,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (avaliacao.dataCriacao.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Criada em: ${avaliacao.dataCriacao}",
                                            fontSize = 11.sp,
                                            color = TextoSecundario
                                        )
                                    }
                                    if (avaliacao.dataResposta.isNotBlank()) {
                                        Text(
                                            text = "Respondida em: ${avaliacao.dataResposta}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            onVisualizarAnamnese(avaliacao.id)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AzulPrincipal,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Visualizar anamnese")
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = {
                                            anamneseParaExcluirId = avaliacao.id
                                            erroExclusaoAnamnese = ""
                                        },
                                        enabled = !excluindoAnamnese,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Vermelho
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Vermelho
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Excluir anamnese")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Documentos médicos anexados pelo aluno:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (documentosMedicos.isEmpty()) {
                        Text(
                            text = "Nenhum documento médico foi anexado pelo aluno.",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    } else {
                        documentosMedicos.forEach { documento ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AzulMuitoClaro
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = documento.nomeArquivo,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextoPrincipal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                documentoParaExcluir = documento
                                                erroExclusaoDocumento = ""
                                            },
                                            enabled = !excluindoDocumento
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Remover documento",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Formato: ${documento.tipoArquivo.uppercase()}",
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                    Text(
                                        text = "Anexado em: ${documento.dataImportacao}",
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                abrirDocumentoAnamnese(
                                                    context = contextoEventosPerfil,
                                                    documento = documento
                                                )
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    contextoEventosPerfil,
                                                    e.message
                                                        ?: "Não foi possível abrir o documento.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("ABRIR DOCUMENTO")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.LightGray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Imagens para comparação anexadas pelo aluno:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (imagensComparacao.isEmpty()) {
                        Text(
                            text = "Nenhuma imagem de comparação foi anexada pelo aluno.",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    } else {
                        imagensComparacao.forEach { documento ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AzulMuitoClaro
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = documento.nomeArquivo,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextoPrincipal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                documentoParaExcluir = documento
                                                erroExclusaoDocumento = ""
                                            },
                                            enabled = !excluindoDocumento
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "Remover imagem",
                                                tint = Color.Red
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Formato: ${documento.tipoArquivo.uppercase()}",
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                    Text(
                                        text = "Anexada em: ${documento.dataImportacao}",
                                        fontSize = 11.sp,
                                        color = TextoSecundario
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                abrirDocumentoAnamnese(
                                                    context = contextoEventosPerfil,
                                                    documento = documento
                                                )
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(
                                                    contextoEventosPerfil,
                                                    e.message
                                                        ?: "Não foi possível abrir a imagem.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("ABRIR IMAGEM")
                                    }
                                }
                            }
                        }
                    }
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
                    GraficoRegistrosAluno(eventosAluno)
                    Spacer(modifier = Modifier.height(14.dp))
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
        DashboardBottomBar(
            telaAtual = AppScreen.PerfilAluno,
            onInicio = onDashboard,
            onAlunos = onVoltar,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
    }
// Diálogo exclusão de anamnese (com sincronização imediata)
    anamneseParaExcluirId?.let { idAnamnese ->
        AlertDialog(
            onDismissRequest = {
                if (!excluindoAnamnese) {
                    anamneseParaExcluirId = null
                    erroExclusaoAnamnese = ""
                }
            },
            title = {
                Text("Excluir anamnese?")
            },
            text = {
                Column {
                    Text(
                        "Deseja realmente excluir esta anamnese atribuída? " +
                                "Ela será removida da lista do professor e do aluno."
                    )
                    if (erroExclusaoAnamnese.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = erroExclusaoAnamnese,
                            color = Vermelho,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !excluindoAnamnese,
                    onClick = {
                        if (!excluindoAnamnese) {
                            excluindoAnamnese = true
                            scope.launch {
                                try {
                                    anamneseAtribuidaRepository.excluir(
                                        idAnamnese
                                    )
                                    AlunoSyncWorker.agendar(contextoEventosPerfil)
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            val db =
                                                FitCareDatabase.getDatabase(contextoEventosPerfil)
                                            FirestoreSyncService(
                                                db.usuarioDao(),
                                                db
                                            ).sincronizarAnamnesesAtribuidas()
                                        } catch (_: Exception) {
                                        }
                                    }
                                    anamneseParaExcluirId = null

                                    android.widget.Toast.makeText(
                                        contextoEventosPerfil,
                                        "Anamnese excluída com sucesso.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    erroExclusaoAnamnese = e.message
                                        ?: "Não foi possível excluir a anamnese."
                                } finally {
                                    excluindoAnamnese = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (excluindoAnamnese) {
                            "Excluindo..."
                        } else {
                            "Excluir"
                        },
                        color = Vermelho
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !excluindoAnamnese,
                    onClick = {
                        anamneseParaExcluirId = null
                        erroExclusaoAnamnese = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    if (documentoParaExcluir != null) {
        AlertDialog(
            onDismissRequest = {
                if (!excluindoDocumento) {
                    documentoParaExcluir = null
                    erroExclusaoDocumento = ""
                }
            },
            title = {
                Text("Remover documento?")
            },
            text = {
                Column {
                    Text(
                        "Deseja realmente excluir este documento? O arquivo será removido permanentemente."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = documentoParaExcluir?.nomeArquivo.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )
                    if (erroExclusaoDocumento.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = erroExclusaoDocumento,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !excluindoDocumento,
                    onClick = {
                        val documento = documentoParaExcluir
                        if (documento != null && !excluindoDocumento) {
                            excluindoDocumento = true
                            scope.launch {
                                try {
                                    val arquivo = java.io.File(
                                        documento.caminhoArquivo
                                    )
                                    if (arquivo.exists() && !arquivo.delete()) {
                                        throw Exception(
                                            "Não foi possível remover o arquivo."
                                        )
                                    }
                                    anamneseRepository.excluirDocumento(
                                        documento.id
                                    )
                                    documentoParaExcluir = null
                                    android.widget.Toast.makeText(
                                        contextoEventosPerfil,
                                        "Documento removido com sucesso.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    erroExclusaoDocumento =
                                        "Não foi possível excluir o documento. Tente novamente."
                                } finally {
                                    excluindoDocumento = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (excluindoDocumento) {
                            "Excluindo..."
                        } else {
                            "Excluir"
                        },
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !excluindoDocumento,
                    onClick = {
                        documentoParaExcluir = null
                        erroExclusaoDocumento = ""
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
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
// Diálogo Criar Acesso do Aluno (já puxa nome e e-mail do cadastro do aluno)
    if (mostrarCadastroAcessoAluno) {
        AlertDialog(
            onDismissRequest = {
                if (!carregandoCadastroAcesso) {
                    mostrarCadastroAcessoAluno = false
                }
            },
            title = {
                Text("Criar acesso do aluno")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    Text(
                        text = "Defina a senha inicial para que ${alunoAtual?.nome ?: "o aluno"} acesse o aplicativo:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nomeAcessoAluno,
                        onValueChange = { nomeAcessoAluno = it },
                        label = { Text("Nome") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailAcessoAluno,
                        onValueChange = { emailAcessoAluno = it },
                        label = { Text("E-mail de acesso") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        readOnly = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senhaAcessoAluno,
                        onValueChange = { senhaAcessoAluno = it },
                        label = { Text("Senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmarSenhaAcessoAluno,
                        onValueChange = { confirmarSenhaAcessoAluno = it },
                        label = { Text("Confirmar senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (erroCadastroAcesso.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = erroCadastroAcesso,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    if (sucessoCadastroAcesso.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = sucessoCadastroAcesso,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !carregandoCadastroAcesso,
                    onClick = {
                        if (senhaAcessoAluno != confirmarSenhaAcessoAluno) {
                            erroCadastroAcesso = "As senhas não coincidem."
                            return@TextButton
                        }
                        carregandoCadastroAcesso = true
                        erroCadastroAcesso = ""
                        scope.launch {
                            try {
                                usuarioRepository.cadastrarAluno(
                                    nome = nomeAcessoAluno,
                                    email = emailAcessoAluno,
                                    senha = senhaAcessoAluno,
                                    alunoId = alunoId
                                )
                                sucessoCadastroAcesso =
                                    "Acesso do aluno criado com sucesso."
                                senhaAcessoAluno = ""
                                confirmarSenhaAcessoAluno = ""
                                android.widget.Toast.makeText(
                                    contextoEventosPerfil,
                                    "Conta do aluno criada com sucesso.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                mostrarCadastroAcessoAluno = false
                            } catch (e: Exception) {
                                erroCadastroAcesso = e.message
                                    ?: "Não foi possível criar o acesso."
                            } finally {
                                carregandoCadastroAcesso = false
                            }
                        }
                    }
                ) {
                    Text(
                        if (carregandoCadastroAcesso) {
                            "Criando..."
                        } else {
                            "Criar acesso"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !carregandoCadastroAcesso,
                    onClick = {
                        mostrarCadastroAcessoAluno = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    // Diálogo Redefinir Senha do Aluno (já exibe nome e e-mail do cadastro do aluno)
    if (mostrarDialogoRedefinirSenha) {
        AlertDialog(
            onDismissRequest = {
                if (!carregandoRedefinicaoSenha) {
                    mostrarDialogoRedefinirSenha = false
                }
            },
            title = {
                Text("Redefinir senha do aluno")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(
                        rememberScrollState()
                    )
                ) {
                    Text(
                        text = "Aluno: ${alunoAtual?.nome.orEmpty()}\nE-mail: ${alunoAtual?.email.orEmpty()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = novaSenhaAluno,
                        onValueChange = { novaSenhaAluno = it },
                        label = { Text("Nova senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmarNovaSenhaAluno,
                        onValueChange = { confirmarNovaSenhaAluno = it },
                        label = { Text("Confirmar nova senha") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    if (erroRedefinicaoSenha.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = erroRedefinicaoSenha,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    if (sucessoRedefinicaoSenha.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sucessoRedefinicaoSenha,
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !carregandoRedefinicaoSenha,
                    onClick = {
                        erroRedefinicaoSenha = ""
                        if (novaSenhaAluno.length < 8) {
                            erroRedefinicaoSenha =
                                "A senha deve ter pelo menos 8 caracteres."
                            return@TextButton
                        }
                        if (novaSenhaAluno != confirmarNovaSenhaAluno) {
                            erroRedefinicaoSenha =
                                "As senhas não coincidem."
                            return@TextButton
                        }
                        carregandoRedefinicaoSenha = true
                        scope.launch {
                            try {
                                usuarioRepository.redefinirSenhaDoAluno(
                                    alunoId = alunoId,
                                    novaSenha = novaSenhaAluno
                                )
                                sucessoRedefinicaoSenha =
                                    "Senha redefinida com sucesso."
                                novaSenhaAluno = ""
                                confirmarNovaSenhaAluno = ""
                                android.widget.Toast.makeText(
                                    contextoEventosPerfil,
                                    "Senha do aluno atualizada.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                mostrarDialogoRedefinirSenha = false
                            } catch (e: Exception) {
                                erroRedefinicaoSenha = e.message
                                    ?: "Não foi possível redefinir a senha."
                            } finally {
                                carregandoRedefinicaoSenha = false
                            }
                        }
                    }
                ) {
                    Text(
                        if (carregandoRedefinicaoSenha) {
                            "Salvando..."
                        } else {
                            "Salvar nova senha"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !carregandoRedefinicaoSenha,
                    onClick = {
                        mostrarDialogoRedefinirSenha = false
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

        for (index in 0 until coordenadas.size - 1) {

            drawLine(
                color = corLinha,
                start = coordenadas[index],
                end = coordenadas[index + 1],
                strokeWidth = 2.dp.toPx()
            )
        }

        coordenadas.forEach { ponto ->

            drawCircle(
                color = corLinha,
                radius = 4.dp.toPx(),
                center = ponto
            )
        }

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
/// Gráfico visual da situação geral da carteira de alunos e indicadores de frequência (RF10 / CA06)
@Composable
fun GraficoCarteiraAlunos(
    totalAlunos: Int,
    emDia: Int,
    emAtencao: Int,
    prioridade: Int,
    frequenciaSemanalGeral: Int,
    mediaFrequenciaSemanal: Float,
    alunosSemTreinar7Dias: Int
) {
    Cartao(
        modifier = Modifier.padding(top = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = "Gráfico da carteira",
                tint = AzulPrincipal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Distribuição da carteira de alunos",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (totalAlunos == 0) {
            Text(
                text = "Cadastre alunos para visualizar os indicadores gráficos da carteira.",
                fontSize = 12.sp,
                color = TextoSecundario
            )
        } else {
            val pctEmDia = (emDia * 100f / totalAlunos).toInt()
            val pctAtencao = (emAtencao * 100f / totalAlunos).toInt()
            val pctPrioridade = (prioridade * 100f / totalAlunos).toInt()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
            ) {
                val larguraTotal = size.width
                val altura = size.height
                val wEmDia = larguraTotal * (emDia.toFloat() / totalAlunos)
                val wAtencao = larguraTotal * (emAtencao.toFloat() / totalAlunos)
                val wPrioridade = larguraTotal * (prioridade.toFloat() / totalAlunos)

                var xAtual = 0f
                if (wEmDia > 0f) {
                    drawRect(
                        color = Color(0xFF36844A),
                        topLeft = Offset(xAtual, 0f),
                        size = androidx.compose.ui.geometry.Size(wEmDia, altura)
                    )
                    xAtual += wEmDia
                }
                if (wAtencao > 0f) {
                    drawRect(
                        color = Color(0xFFE5A50A),
                        topLeft = Offset(xAtual, 0f),
                        size = androidx.compose.ui.geometry.Size(wAtencao, altura)
                    )
                    xAtual += wAtencao
                }
                if (wPrioridade > 0f) {
                    drawRect(
                        color = Color(0xFFE34B63),
                        topLeft = Offset(xAtual, 0f),
                        size = androidx.compose.ui.geometry.Size(wPrioridade, altura)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendaIndicador(
                    cor = Color(0xFF36844A),
                    rotulo = "Em dia",
                    detalhe = "$emDia ($pctEmDia%)"
                )
                LegendaIndicador(
                    cor = Color(0xFFE5A50A),
                    rotulo = "Atenção",
                    detalhe = "$emAtencao ($pctAtencao%)"
                )
                LegendaIndicador(
                    cor = Color(0xFFE34B63),
                    rotulo = "Prioridade",
                    detalhe = "$prioridade ($pctPrioridade%)"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = AzulMuitoClaro)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Indicadores de frequência e assiduidade",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextoPrincipal
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFDCEBFF), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = frequenciaSemanalGeral.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF416C93)
                    )
                    Text(
                        text = "Treinos na semana",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF416C93)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFD8F3DF), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale("pt", "BR"), "%.1f", mediaFrequenciaSemanal),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF36844A)
                    )
                    Text(
                        text = "Média semanal",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF36844A)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFFFE5D0), RoundedCornerShape(10.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = alunosSemTreinar7Dias.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC25900)
                    )
                    Text(
                        text = "+7d sem treinar",
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFC25900)
                    )
                }
            }
        }
    }
}


@Composable
private fun LegendaIndicador(
    cor: Color,
    rotulo: String,
    detalhe: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(cor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text = rotulo,
                fontSize = 11.sp,
                color = TextoSecundario
            )
            Text(
                text = detalhe,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
        }
    }
}

// Gráfico de barras de acompanhamento individual por categoria de registro
@Composable
fun GraficoRegistrosAluno(eventos: List<EventoAluno>) {
    if (eventos.isEmpty()) return

    val categorias = listOf(
        Triple("Treinos", eventos.count { it.tipo == "Treino" }, AzulPrincipal),
        Triple("Avaliações", eventos.count { it.tipo == "Avaliação física" }, Color(0xFF36844A)),
        Triple("Dor", eventos.count { it.tipo == "Dor / desconforto" }, Vermelho),
        Triple(
            "Outros",
            eventos.count { it.tipo != "Treino" && it.tipo != "Avaliação física" && it.tipo != "Dor / desconforto" },
            AzulClaro
        )
    )

    val maximo = (categorias.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AzulMuitoClaro.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Distribuição dos registros do aluno",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextoPrincipal
        )
        categorias.forEach { (rotulo, quantidade, corBarra) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rotulo,
                    fontSize = 11.sp,
                    color = TextoPrincipal,
                    modifier = Modifier.width(74.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                ) {
                    val fracao = (quantidade.toFloat() / maximo).coerceIn(0f, 1f)
                    if (fracao > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fracao)
                                .fillMaxHeight()
                                .background(corBarra, RoundedCornerShape(6.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = quantidade.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoPrincipal,
                    modifier = Modifier.width(22.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// MOTOR DE CLASSIFICAÇÃO E CENTRAL DE ATENÇÃO (RF06, RF07, RF08, RF09, RF12)
data class MotivoAcompanhamento(
    val categoria: String,
    val titulo: String,
    val descricao: String,
    val ehPrioritario: Boolean
)

data class DiagnosticoAcompanhamentoAluno(
    val aluno: AlunoEntity,
    val classificacaoCalculada: String,
    val motivos: List<MotivoAcompanhamento>
)

object CentralAtencaoAnalyzer {

    private fun parseDataBr(dataStr: String): Long? {
        if (dataStr.isBlank()) return null
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).apply {
            isLenient = false
        }
        return runCatching { fmt.parse(dataStr.trim())?.time }.getOrNull()
    }

    fun avaliarCarteira(
        alunos: List<AlunoEntity>,
        todosTreinos: List<TreinoEntity>,
        todosEventos: List<EventoAluno>
    ): List<DiagnosticoAcompanhamentoAluno> {
        val calHoje = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val hojeMs = calHoje.timeInMillis
        val mesAtual = calHoje.get(java.util.Calendar.MONTH)
        val anoAtual = calHoje.get(java.util.Calendar.YEAR)

        val calInicioSemana = java.util.Calendar.getInstance().apply {
            timeInMillis = hojeMs
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
        }
        val inicioSemanaMs = calInicioSemana.timeInMillis
        val diaMs = 24L * 60L * 60L * 1000L

        return alunos.map { aluno ->
            val motivos = mutableListOf<MotivoAcompanhamento>()
            val treinosAluno = todosTreinos.filter { it.alunoId == aluno.id }
            val eventosAluno = todosEventos
                .filter { it.alunoId == aluno.id }
                .sortedByDescending { parseDataBr(it.data) ?: it.id }

            // 1. Regra de Baixa Frequência: 2+ treinos previstos não realizados na semana
            val treinosAtivosSemana = treinosAluno.filter { t ->
                val inicio = parseDataBr(t.dataInicio) ?: 0L
                val fim = parseDataBr(t.dataFim) ?: Long.MAX_VALUE
                inicio <= hojeMs + diaMs && fim >= inicioSemanaMs
            }
            if (treinosAtivosSemana.isNotEmpty()) {
                val diasDecorridosSemana = (((hojeMs - inicioSemanaMs) / diaMs).toInt() + 1).coerceIn(1, 7)
                val metaPrevista = (diasDecorridosSemana / 2).coerceAtLeast(2)
                val realizadosSemana = eventosAluno.count { ev ->
                    val dMs = parseDataBr(ev.data) ?: 0L
                    ev.tipo.equals("Treino", ignoreCase = true) && dMs in inicioSemanaMs..(hojeMs + diaMs)
                }
                val faltantes = metaPrevista - realizadosSemana
                if (faltantes >= 2) {
                    motivos.add(
                        MotivoAcompanhamento(
                            categoria = "Baixa frequência",
                            titulo = "Baixa frequência semanal",
                            descricao = "$faltantes treino(s) previsto(s) não realizado(s) nesta semana ($realizadosSemana de $metaPrevista).",
                            ehPrioritario = false
                        )
                    )
                }
            }

            // 2. Regra de Dor Recorrente: 2 treinos consecutivos com dor ou 3 ocorrências no mesmo mês
            val eventosComDor = eventosAluno.filter { ev ->
                ev.tipo.equals("Dor / desconforto", ignoreCase = true) || (ev.intensidade ?: 0) >= 4
            }
            val dorNoMes = eventosComDor.count { ev ->
                val dMs = parseDataBr(ev.data) ?: return@count false
                val c = java.util.Calendar.getInstance().apply { timeInMillis = dMs }
                c.get(java.util.Calendar.MONTH) == mesAtual && c.get(java.util.Calendar.YEAR) == anoAtual
            }
            val doisConsecutivosComDor = eventosAluno.take(2).size == 2 &&
                    eventosAluno.take(2).all { ev ->
                        ev.tipo.equals("Dor / desconforto", ignoreCase = true) || (ev.intensidade ?: 0) >= 4
                    }
            val dorIntensa = eventosComDor.any { (it.intensidade ?: 0) >= 8 }

            if (doisConsecutivosComDor || dorNoMes >= 3 || dorIntensa) {
                val maxDor = eventosComDor.maxOfOrNull { it.intensidade ?: 0 } ?: 0
                val descricaoDor = when {
                    doisConsecutivosComDor -> "Dois registros consecutivos com relato de dor ou desconforto."
                    dorNoMes >= 3 -> "$dorNoMes ocorrências de dor registradas no mês atual."
                    else -> "Relato de dor com intensidade elevada ($maxDor/10)."
                }
                motivos.add(
                    MotivoAcompanhamento(
                        categoria = "Dor",
                        titulo = "Dor recorrente ou elevada",
                        descricao = descricaoDor,
                        ehPrioritario = true
                    )
                )
            } else if (eventosComDor.isNotEmpty()) {
                val ultimaDorMs = parseDataBr(eventosComDor.first().data) ?: 0L
                if (hojeMs - ultimaDorMs <= 7 * diaMs) {
                    motivos.add(
                        MotivoAcompanhamento(
                            categoria = "Dor",
                            titulo = "Registro recente de desconforto",
                            descricao = "Ocorrência de dor registrada nos últimos 7 dias (${eventosComDor.first().data}).",
                            ehPrioritario = false
                        )
                    )
                }
            }

            // 3. Regra de Avaliação Pendente (7 dias após definição) e 4. Avaliação Atrasada (5 dias após data prevista)
            val avaliacoesFisicas = eventosAluno.filter {
                it.tipo.equals("Avaliação física", ignoreCase = true)
            }
            val ultimaAvaliacao = avaliacoesFisicas.firstOrNull()
            if (ultimaAvaliacao == null) {
                val cadastroMs = parseDataBr(aluno.dataCadastro)
                if (cadastroMs != null && (hojeMs - cadastroMs) >= 7 * diaMs) {
                    motivos.add(
                        MotivoAcompanhamento(
                            categoria = "Avaliação",
                            titulo = "Avaliação física pendente",
                            descricao = "Nenhum registro de avaliação física realizado após 7 dias do cadastro (${aluno.dataCadastro}).",
                            ehPrioritario = false
                        )
                    )
                }
            } else {
                val proximaPrevistaMs = parseDataBr(ultimaAvaliacao.proximaAvaliacao)
                if (proximaPrevistaMs != null && (hojeMs - proximaPrevistaMs) >= 5 * diaMs) {
                    val diasAtraso = ((hojeMs - proximaPrevistaMs) / diaMs).toInt()
                    motivos.add(
                        MotivoAcompanhamento(
                            categoria = "Avaliação",
                            titulo = "Reavaliação física atrasada",
                            descricao = "Data prevista em ${ultimaAvaliacao.proximaAvaliacao} ultrapassada há $diasAtraso dia(s).",
                            ehPrioritario = false
                        )
                    )
                }
            }

            // 5. Regra de Ausência de Contato: 10 dias consecutivos sem contato ou ação
            val ultimoContatoMs = eventosAluno.firstOrNull()?.let { parseDataBr(it.data) }
                ?: parseDataBr(aluno.dataCadastro)
            if (ultimoContatoMs != null && (hojeMs - ultimoContatoMs) >= 10 * diaMs) {
                val diasSemContato = ((hojeMs - ultimoContatoMs) / diaMs).toInt()
                motivos.add(
                    MotivoAcompanhamento(
                        categoria = "Sem contato",
                        titulo = "Ausência de contato / acompanhamento",
                        descricao = "$diasSemContato dias consecutivos sem registro de contato ou ação.",
                        ehPrioritario = false
                    )
                )
            }

            // 6. Regra de Proximidade do Vencimento do Treinamento (7 dias antes sem renovação)
            if (treinosAluno.isNotEmpty()) {
                val treinoMaisRecente = treinosAluno.maxByOrNull { parseDataBr(it.dataFim) ?: 0L }
                val dataFimMs = treinoMaisRecente?.let { parseDataBr(it.dataFim) }
                if (treinoMaisRecente != null && dataFimMs != null) {
                    val temRenovacaoPosterior = treinosAluno.any { outro ->
                        outro.id != treinoMaisRecente.id &&
                                (parseDataBr(outro.dataInicio) ?: 0L) >= dataFimMs
                    }
                    if (!temRenovacaoPosterior) {
                        val diffDias = ((dataFimMs - hojeMs) / diaMs).toInt()
                        if (diffDias < 0) {
                            motivos.add(
                                MotivoAcompanhamento(
                                    categoria = "Vencimento",
                                    titulo = "Treino vencido sem renovação",
                                    descricao = "A ficha \"${treinoMaisRecente.nome}\" venceu em ${treinoMaisRecente.dataFim}.",
                                    ehPrioritario = true
                                )
                            )
                        } else if (diffDias <= 7) {
                            motivos.add(
                                MotivoAcompanhamento(
                                    categoria = "Vencimento",
                                    titulo = "Treino próximo do vencimento",
                                    descricao = "A ficha \"${treinoMaisRecente.nome}\" vence em $diffDias dia(s) (${treinoMaisRecente.dataFim}).",
                                    ehPrioritario = false
                                )
                            )
                        }
                    }
                }
            }
// 7. Integração de Condições e Restrições Clínicas (RF12)
            val textoClinico = "${aluno.condicoes} ${aluno.restricoes} ${aluno.observacoesProfissionais}".lowercase()
            val possuiCondicao = aluno.condicoes.isNotBlank() &&
                    !aluno.condicoes.equals("Nenhuma", ignoreCase = true)
            val possuiRestricao = aluno.restricoes.isNotBlank() &&
                    !aluno.restricoes.equals("Nenhuma", ignoreCase = true)
            val palavrasPrioritarias = listOf(
                "cardiopatia", "cirurgia", "hérnia", "hernia", "fratura",
                "lesão grave", "lesao grave", "hipertensão descompensada", "grave", "severa"
            )
            val clinicoPrioritario = palavrasPrioritarias.any { textoClinico.contains(it) }
            if (possuiCondicao || possuiRestricao) {
                val resumoClinico = listOfNotNull(
                    aluno.condicoes.takeIf { possuiCondicao }?.let { "Condições: $it" },
                    aluno.restricoes.takeIf { possuiRestricao }?.let { "Restrições: $it" }
                ).joinToString(" • ")
                motivos.add(
                    MotivoAcompanhamento(
                        categoria = "Clínico",
                        titulo = "Condição / restrição clínica ativa",
                        descricao = resumoClinico,
                        ehPrioritario = clinicoPrioritario
                    )
                )
            }
// Critério final de classificação:
// - Prioridade: >= 1 regra prioritária OU >= 2 regras de atenção simultâneas
// - Atenção: 1 regra de atenção
// - Normal: nenhuma regra ativa
            val totalPrioritarias = motivos.count { it.ehPrioritario }
            val totalAtencao = motivos.count { !it.ehPrioritario }
            val classificacaoFinal = when {
                totalPrioritarias >= 1 || totalAtencao >= 2 -> "Prioridade"
                totalAtencao == 1 -> "Atenção"
                else -> "Normal"
            }
            DiagnosticoAcompanhamentoAluno(
                aluno = aluno,
                classificacaoCalculada = classificacaoFinal,
                motivos = motivos
            )
        }
    }
}
@Composable
fun TelaCentralAtencao(
    repository: AlunoRepository,
    treinoRepository: TreinoRepository,
    onVoltar: () -> Unit,
    onAbrirPerfil: (AlunoEntity) -> Unit,
    onDashboard: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onSair: () -> Unit
) {
    val context = LocalContext.current
    val alunos by repository.listarAlunos().collectAsState(initial = emptyList())
    val todosTreinos by treinoRepository.listarTodosTreinos().collectAsState(initial = emptyList())
    val eventosStorage = remember { EventoAlunoStorage(context) }
    val diagnosticos = remember(alunos, todosTreinos) {
        val eventos = eventosStorage.listarTodos()
        CentralAtencaoAnalyzer.avaliarCarteira(
            alunos = alunos,
            todosTreinos = todosTreinos,
            todosEventos = eventos
        )
    }
// Sincroniza automaticamente a classificação calculada com o banco local se houver divergência
    LaunchedEffect(diagnosticos) {
        withContext(Dispatchers.IO) {
            for (diag in diagnosticos) {
                if (diag.aluno.status != diag.classificacaoCalculada) {
                    try {
                        repository.atualizar(
                            diag.aluno.copy(status = diag.classificacaoCalculada)
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
    var filtroSituacao by remember { mutableStateOf("Todos") }
    val filtrosDisponiveis = listOf(
        "Todos",
        "Prioridade",
        "Atenção",
        "Baixa frequência",
        "Dor",
        "Avaliação",
        "Vencimento",
        "Sem contato",
        "Clínico"
    )
    val alunosRequeremAcompanhamento = remember(diagnosticos, filtroSituacao) {
        val comparador = Comparator { a: DiagnosticoAcompanhamentoAluno, b: DiagnosticoAcompanhamentoAluno ->
            val pA = when (a.classificacaoCalculada) {
                "Prioridade" -> 0
                "Atenção" -> 1
                else -> 2
            }
            val pB = when (b.classificacaoCalculada) {
                "Prioridade" -> 0
                "Atenção" -> 1
                else -> 2
            }
            if (pA != pB) pA.compareTo(pB) else a.aluno.nome.lowercase().compareTo(b.aluno.nome.lowercase())
        }

        val base = diagnosticos
            .filter { it.classificacaoCalculada != "Normal" || it.motivos.isNotEmpty() }
            .sortedWith(comparador)

        when (filtroSituacao) {
            "Todos" -> base
            "Prioridade" -> base.filter { it.classificacaoCalculada == "Prioridade" }
            "Atenção" -> base.filter { it.classificacaoCalculada == "Atenção" }
            else -> base.filter { diag ->
                diag.motivos.any { it.categoria.equals(filtroSituacao, ignoreCase = true) }
            }
        }
    }
    val qtdPrioridade = diagnosticos.count { it.classificacaoCalculada == "Prioridade" }
    val qtdAtencao = diagnosticos.count { it.classificacaoCalculada == "Atenção" }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CabecalhoTopoClaro, CabecalhoBaseClaro)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                BotaoVoltarPadrao(onVoltar = onVoltar)
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
                .padding(18.dp)
        ) {
            Text(
                text = "Central de Atenção",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Text(
                text = "Classificação automatizada e motivos clínicos/operacionais.",
                fontSize = 13.sp,
                color = TextoSecundario
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardIndicador(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filtroSituacao = "Prioridade" },
                    titulo = "Prioridade",
                    valor = qtdPrioridade.toString(),
                    corFundo = Color(0xFFFFD9DC),
                    corTexto = Color(0xFFB42332)
                )
                DashboardIndicador(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filtroSituacao = "Atenção" },
                    titulo = "Atenção",
                    valor = qtdAtencao.toString(),
                    corFundo = Color(0xFFFFEAB5),
                    corTexto = Color(0xFF9A7000)
                )
                DashboardIndicador(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filtroSituacao = "Todos" },
                    titulo = "Em alerta",
                    valor = (qtdPrioridade + qtdAtencao).toString(),
                    corFundo = AzulMuitoClaro,
                    corTexto = AzulPrincipal
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
// Filtros por categoria e classificação
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtrosDisponiveis.chunked(3).forEach { linhaFiltros ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        linhaFiltros.forEach { opcao ->
                            val selecionado = filtroSituacao == opcao
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selecionado) AzulPrincipal else Branco)
                                    .clickable { filtroSituacao = opcao }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opcao,
                                    color = if (selecionado) Color.White else TextoPrincipal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (alunosRequeremAcompanhamento.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Branco)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nenhum aluno neste filtro",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Todos os alunos acompanhados estão em situação normal para este critério.",
                            fontSize = 12.sp,
                            color = TextoSecundario,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                alunosRequeremAcompanhamento.forEach { diag ->
                    val ehPrioridade = diag.classificacaoCalculada == "Prioridade"
                    val corBadgeFundo = if (ehPrioridade) Color(0xFFFFD9DC) else Color(0xFFFFEAB5)
                    val corBadgeTexto = if (ehPrioridade) Color(0xFFB42332) else Color(0xFF9A7000)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable { onAbrirPerfil(diag.aluno) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Branco),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = diag.aluno.nome,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoPrincipal,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = corBadgeFundo
                                ) {
                                    Text(
                                        text = diag.classificacaoCalculada.uppercase(),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = corBadgeTexto
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            diag.motivos.forEach { motivo ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 6.dp)
                                        .background(
                                            color = if (motivo.ehPrioritario) {
                                                Color(0xFFFFF1F2)
                                            } else {
                                                AzulMuitoClaro.copy(alpha = 0.6f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(8.dp)
                                            .background(
                                                color = if (motivo.ehPrioritario) Color(0xFFB42332) else AzulPrincipal,
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${motivo.titulo} (${motivo.categoria})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextoPrincipal
                                        )
                                        Text(
                                            text = motivo.descricao,
                                            fontSize = 11.sp,
                                            color = TextoSecundario
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Toque para abrir o perfil e agir ›",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AzulPrincipal
                            )
                        }
                    }
                }
            }
        }
        DashboardBottomBar(
            telaAtual = AppScreen.Dashboard,
            onInicio = onDashboard,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = onAlunos,
            onMais = onSair
        )
    }
}

// tela linha do tempo com filtro de histórico de avaliações e treinos
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

    var filtroTipo by remember {
        mutableStateOf("Todos")
    }

    var eventoSelecionado by remember {
        mutableStateOf<EventoAluno?>(null)
    }

    val aluno by repository.buscarPorId(alunoId).collectAsState(initial = null)

    LaunchedEffect(alunoId) {
        eventos = storage.listarPorAluno(alunoId)
    }

    val opcoesFiltro = listOf(
        "Todos",
        "Avaliação física",
        "Treino",
        "Dor / desconforto"
    )

    val eventosFiltrados = remember(eventos, filtroTipo) {
        if (filtroTipo == "Todos") {
            eventos
        } else {
            eventos.filter { it.tipo.equals(filtroTipo, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {

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
                .padding(top = 4.dp, bottom = 10.dp)
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
                        .width(145.dp)
                        .height(70.dp)
                )

                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }
        }

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
                text = "Histórico individual de avaliações e treinos",
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

            Spacer(modifier = Modifier.height(14.dp))

            // Filtros por categoria de histórico
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcoesFiltro.take(2).forEach { opcao ->
                        val selecionado = filtroTipo == opcao
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selecionado) AzulPrincipal else Branco)
                                .clickable { filtroTipo = opcao }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opcao,
                                color = if (selecionado) Color.White else TextoPrincipal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcoesFiltro.drop(2).forEach { opcao ->
                        val selecionado = filtroTipo == opcao
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selecionado) AzulPrincipal else Branco)
                                .clickable { filtroTipo = opcao }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opcao,
                                color = if (selecionado) Color.White else TextoPrincipal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (eventosFiltrados.isEmpty()) {

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
                            text = if (filtroTipo == "Todos") {
                                "Nenhum evento registrado"
                            } else {
                                "Nenhum registro em \"$filtroTipo\""
                            },
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "Registre avaliações físicas, treinos, dores ou observações para acompanhar a evolução.",
                            color = TextoSecundario,
                            fontSize = 13.sp
                        )
                    }
                }

            } else {

                eventosFiltrados.forEachIndexed { index, evento ->

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

                            if (index < eventosFiltrados.lastIndex) {
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
        DashboardBottomBar(
            telaAtual = AppScreen.LinhaDoTempoAluno,
            onInicio = onDashboard,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = onRegistros,
            onMais = onMais
        )
    }
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
            .navigationBarsPadding()
    ) {
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
                .padding(top = 4.dp, bottom = 10.dp)
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
                        .width(145.dp)
                        .height(70.dp)
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
                bottom = 10.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .width(140.dp)
                    .height(60.dp)
            )
            BotaoVoltarPadrao(
                onVoltar = onVoltar,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        Text(
            text = titulo,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
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
    val opcoesCondicoes = OPCOES_CONDICOES_PADRAO
    val opcoesRestricoes = OPCOES_RESTRICOES_PADRAO
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

    val context = LocalContext.current
    val requesterObservacoes = remember {
        BringIntoViewRequester()
    }
    val requesterOutraCondicao = remember {
        BringIntoViewRequester()
    }
    val requesterOutraRestricao = remember {
        BringIntoViewRequester()
    }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
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
                .padding(top = 4.dp, bottom = 10.dp)
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
                        .width(145.dp)
                        .height(70.dp)
                )
                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(
                        onVoltar = onVoltar
                    )
                }
            }
        }
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
                text = "Selecione as condições que se aplicam ao aluno (sincronizado com a anamnese).",
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
                            AlunoSyncWorker.agendar(context)
                            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val db = FitCareDatabase.getDatabase(context)
                                    FirestoreSyncService(db.usuarioDao(), db).sincronizarAlunos()
                                } catch (_: Exception) {
                                }
                            }
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
        }
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

@Composable
private fun DashboardBottomBar(
    telaAtual: AppScreen,
    onInicio: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onRegistros: () -> Unit,
    onMais: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Branco,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
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
    val observacoes: String = "",
    val videoTipo: String = "",
    val videoUrl: String = "",
    val videoNome: String = ""
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

    fun converterData(dataTexto: String): java.util.Date? {
        if (dataTexto.isBlank()) return null
        return try {
            formatoData.parse(dataTexto)
        } catch (_: Exception) {
            null
        }
    }

    val treinosAtivos = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)
        dataFim == null || !dataFim.before(hoje)
    }
    val treinosAVencer = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)
        dataFim != null &&
                !dataFim.before(hoje) &&
                !dataFim.after(limiteVencimento)
    }
    val treinosVencidos = treinos.count { treino ->
        val dataFim = converterData(treino.dataFim)
        dataFim != null && dataFim.before(hoje)
    }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
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
                .padding(top = 4.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                BotaoVoltarPadrao(
                    onVoltar = onVoltar
                )
            }
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 14.dp
                    )
            ) {
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
                        focusedContainerColor = Branco,
                        unfocusedContainerColor = Branco,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro,
                        focusedTextColor = TextoPrincipal,
                        unfocusedTextColor = TextoPrincipal
                    )
                )
                Spacer(
                    modifier = Modifier.height(14.dp)
                )
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

// TELA FORMULÁRIO DE TREINO (COM ASSOCIAÇÃO DE VÍDEOS: LINK OU ARQUIVO IMPORTADO)
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
    var indiceExercicioImportandoVideo by remember { mutableIntStateOf(-1) }

    val exercicios = remember {
        mutableStateListOf(ExercicioFormulario())
    }
    val scope = rememberCoroutineScope()
    val contextoEventos = LocalContext.current
    val eventoStorage = remember(contextoEventos) {
        EventoAlunoStorage(contextoEventos)
    }

    val seletorVideo = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        val idx = indiceExercicioImportandoVideo
        indiceExercicioImportandoVideo = -1
        if (uri != null && idx in exercicios.indices) {
            scope.launch(Dispatchers.IO) {
                try {
                    val mime = contextoEventos.contentResolver.getType(uri).orEmpty()
                    if (!mime.startsWith("video/", ignoreCase = true)) {
                        withContext(Dispatchers.Main) {
                            mensagemErro = "Selecione um arquivo de vídeo válido (MP4, MOV, MKV, WEBM)."
                        }
                        return@launch
                    }

                    var nomeOriginal = "video_exercicio.mp4"
                    contextoEventos.contentResolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val col = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (col >= 0) {
                                nomeOriginal = cursor.getString(col) ?: nomeOriginal
                            }
                        }
                    }

                    val pastaVideos = java.io.File(contextoEventos.filesDir, "videos_exercicios")
                    if (!pastaVideos.exists()) {
                        pastaVideos.mkdirs()
                    }
                    val nomeSeguro = nomeOriginal.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                    val destino = java.io.File(pastaVideos, "${System.currentTimeMillis()}_$nomeSeguro")

                    contextoEventos.contentResolver.openInputStream(uri)?.use { entrada ->
                        destino.outputStream().use { saida ->
                            entrada.copyTo(saida)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        exercicios[idx] = exercicios[idx].copy(
                            videoTipo = "LOCAL",
                            videoUrl = destino.absolutePath,
                            videoNome = nomeOriginal
                        )
                        mensagemErro = null
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        mensagemErro = "Não foi possível importar o vídeo: ${e.message ?: "erro inesperado"}"
                    }
                }
            }
        }
    }

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
                                series = exercicio.series,
                                repeticoes = exercicio.repeticoes,
                                carga = exercicio.carga,
                                observacoes = exercicio.observacoes,
                                videoTipo = exercicio.videoTipo,
                                videoUrl = exercicio.videoUrl,
                                videoNome = exercicio.videoNome
                            )
                        )
                    }
                if (exercicios.isEmpty()) {
                    exercicios.add(ExercicioFormulario())
                }
            }
        }
    }
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
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FitCareDatabase.getDatabase(contextoEventos)
                                        FirestoreSyncService(db.usuarioDao(), db).sincronizarTreinos()
                                    } catch (_: Exception) {
                                    }
                                }
                                mostrarDialogoExcluir = false
                                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val db = FitCareDatabase.getDatabase(contextoEventos)
                                        FirestoreSyncService(db.usuarioDao(), db).sincronizarTreinos()
                                    } catch (_: Exception) {
                                    }
                                }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
            .navigationBarsPadding()
    ) {
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
                .padding(vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
            ) {
                BotaoVoltarPadrao(
                    onVoltar = onVoltar
                )
            }
            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(145.dp)
                    .height(70.dp)
            )
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
                                placeholder = {
                                    Text("Ex.: 3 ou 3-4")
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true
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
// Seção de Vídeo Demonstrativo (Opcional: Link YouTube ou Importar do Celular)
                        Text(
                            text = "Vídeo demonstrativo (opcional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoPrincipal
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    exercicios[index] = exercicios[index].copy(
                                        videoTipo = "LINK",
                                        videoUrl = if (exercicio.videoTipo == "LINK") exercicio.videoUrl else "",
                                        videoNome = "Link externo"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Link (YouTube)", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    indiceExercicioImportandoVideo = index
                                    seletorVideo.launch(arrayOf("video/*"))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Importar vídeo", fontSize = 11.sp)
                            }
                        }
                        if (exercicio.videoTipo == "LINK") {
                            OutlinedTextField(
                                value = exercicio.videoUrl,
                                onValueChange = {
                                    exercicios[index] = exercicios[index].copy(
                                        videoTipo = "LINK",
                                        videoUrl = it,
                                        videoNome = "Link externo"
                                    )
                                },
                                label = { Text("URL do vídeo (YouTube / Vimeo / HTTP)") },
                                placeholder = { Text("youtube.com...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        } else if (exercicio.videoTipo == "LOCAL" && exercicio.videoUrl.isNotBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AzulMuitoClaro, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Vídeo importado: ${exercicio.videoNome.ifBlank { "Arquivo local" }}",
                                    fontSize = 12.sp,
                                    color = TextoPrincipal,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        exercicios[index] = exercicios[index].copy(
                                            videoTipo = "",
                                            videoUrl = "",
                                            videoNome = ""
                                        )
                                    }
                                ) {
                                    Text("Remover", color = Vermelho, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    exercicios.add(ExercicioFormulario())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Adicionar exercício")
            }
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
            if (mensagemErro != null) {
                Text(
                    text = mensagemErro.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
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
                    if (nome.isBlank()) {
                        mensagemErro = "Informe o nome do treino."
                        return@Button
                    }
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
                    val exerciciosInvalidos = exercicios.any { exercicio ->
                        exercicio.nome.isBlank() ||
                                exercicio.series.isBlank() ||
                                exercicio.repeticoes.isBlank()
                    }
                    if (exerciciosInvalidos) {
                        mensagemErro =
                            "Preencha o nome, as séries válidas e as repetições de todos os exercícios."
                        return@Button
                    }
                    val linkInvalido = exercicios.any { exercicio ->
                        exercicio.videoTipo == "LINK" &&
                                exercicio.videoUrl.isNotBlank() &&
                                !exercicio.videoUrl.trim().startsWith("http://", ignoreCase = true) &&
                                !exercicio.videoUrl.trim().startsWith("https://", ignoreCase = true)
                    }
                    if (linkInvalido) {
                        mensagemErro =
                            "O link do vídeo deve iniciar com http:// ou https://."
                        return@Button
                    }
                    mensagemErro = null
                    salvando = true
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
                            if (treinoId != null) {
                                treinoRepository.excluirExerciciosDoTreino(
                                    treinoId
                                )
                            }
                            exercicios.forEachIndexed { index, exercicio ->
                                val tipoVid = if (exercicio.videoUrl.isBlank()) "" else exercicio.videoTipo.trim()
                                treinoRepository.inserirExercicio(
                                    ExercicioEntity(
                                        treinoId = idTreinoSalvo,
                                        nome = exercicio.nome.trim(),
                                        series = exercicio.series.trim(),
                                        repeticoes = exercicio.repeticoes.trim(),
                                        carga = exercicio.carga.trim(),
                                        observacoes = exercicio.observacoes.trim(),
                                        ordem = index,
                                        videoTipo = tipoVid,
                                        videoUrl = exercicio.videoUrl.trim(),
                                        videoNome = exercicio.videoNome.trim()
                                    )
                                )
                            }
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
    onVoltar: () -> Unit,
    onEditar: (TreinoEntity) -> Unit = {}
) {
    val context = LocalContext.current
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
            .navigationBarsPadding()
    ) {
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
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                BotaoVoltarPadrao(
                    onVoltar = onVoltar
                )
            }

            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
                contentDescription = "Logo Ícaro's FitCare",
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(145.dp)
                    .height(70.dp)
            )

            treino?.let { treinoParaEditar ->
                IconButton(
                    onClick = { onEditar(treinoParaEditar) },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar treino",
                        tint = AzulPrincipal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {
            Text(
                text = "Detalhes do treino",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Spacer(modifier = Modifier.height(14.dp))
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
                                if (exercicio.videoUrl.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            try {
                                                if (exercicio.videoTipo == "LINK") {
                                                    val intent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW,
                                                        android.net.Uri.parse(exercicio.videoUrl)
                                                    )
                                                    context.startActivity(intent)
                                                } else {
                                                    val arquivo = java.io.File(exercicio.videoUrl)
                                                    require(arquivo.exists()) {
                                                        "Arquivo de vídeo não encontrado."
                                                    }
                                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        arquivo
                                                    )
                                                    val intent = android.content.Intent(
                                                        android.content.Intent.ACTION_VIEW
                                                    ).apply {
                                                        setDataAndType(uri, "video/*")
                                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(
                                                        android.content.Intent.createChooser(intent, "Assistir vídeo")
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    e.message ?: "Não foi possível abrir o vídeo.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AzulPrincipal
                                        )
                                    ) {
                                        Text(
                                            text = if (exercicio.videoTipo == "LINK") {
                                                "▶ Assistir vídeo (Link)"
                                            } else {
                                                "▶ Assistir vídeo importado"
                                            },
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
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