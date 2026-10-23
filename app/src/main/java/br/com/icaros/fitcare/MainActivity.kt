package br.com.icaros.fitcare

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
import br.com.icaros.fitcare.data.identificarMotivosAcompanhamento
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

// Activity Principal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    EditarAluno,
    Treinos,
    SelecionarAlunoTreino,
    FormularioTreino
}

// Fluxo de navegação principal da aplicação
@Composable
fun FitCareApp(repository: AlunoRepository, treinoRepository: TreinoRepository) {
    var tela by remember { mutableStateOf(AppScreen.Login) }
    var alunoSelecionadoId by remember { mutableStateOf<Long?>(null) }
    var treinoSelecionadoId by remember { mutableStateOf<Long?>(null) }
    var registrarNovoTreino by remember { mutableStateOf(false) }

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
                            onEditar = { tela = AppScreen.EditarAluno }
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
    var condicoes by remember { mutableStateOf("") }
    var restricoes by remember { mutableStateOf("") }

    var erroNome by remember { mutableStateOf(false) }
    var erroEmail by remember { mutableStateOf(false) }
    var erroObjetivo by remember { mutableStateOf(false) }

    var mensagem by remember { mutableStateOf("") }
    var cadastroSucesso by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
        // Cabeçalho claro padronizado (Correção de contraste para a Logo)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CabecalhoTopoClaro, CabecalhoBaseClaro)
                    )
                )
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
// Botão para voltar ao Dashboard
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable { onVoltar() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        color = AzulPrincipal,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Voltar",
                        color = AzulPrincipal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
// Logo ampliada e centralizada
                Image(
                    painter = painterResource(id = R.drawable.logo_fitcare),
                    contentDescription = "Logo Ícaro's FitCare",
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp)
                )
            }
        }
// Conteúdo do formulário
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Cadastro de aluno",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Text(
                text = "Preencha os dados para cadastrar um novo aluno.",
                fontSize = 13.sp,
                color = TextoSecundario
            )
            OutlinedTextField(
                value = nome,
                onValueChange = {
                    nome = it
                    erroNome = false
                },
                label = { Text("Nome completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = erroNome,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            if (erroNome) {
                Text(
                    text = "Informe o nome completo.",
                    color = Vermelho,
                    fontSize = 12.sp
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    erroEmail = false
                },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = erroEmail,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            if (erroEmail) {
                Text(
                    text = "Informe um e-mail válido.",
                    color = Vermelho,
                    fontSize = 12.sp
                )
            }
            OutlinedTextField(
                value = objective,
                onValueChange = {
                    objective = it
                    erroObjetivo = false
                },
                label = { Text("Objetivo do aluno") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                isError = erroObjetivo,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            if (erroObjetivo) {
                Text(
                    text = "Informe o objetivo do aluno.",
                    color = Vermelho,
                    fontSize = 12.sp
                )
            }
            OutlinedTextField(
                value = observacoesProfissionais,
                onValueChange = { observacoesProfissionais = it },
                label = { Text("Observações profissionais") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            OutlinedTextField(
                value = condicoes,
                onValueChange = { condicoes = it },
                label = { Text("Condições") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            OutlinedTextField(
                value = restricoes,
                onValueChange = { restricoes = it },
                label = { Text("Restrições") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(13.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = AzulPrincipal,
                    unfocusedBorderColor = TextoSecundario.copy(alpha = 0.5f)
                )
            )
            Button(
                onClick = {
                    erroNome = nome.isBlank()
                    erroEmail = !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
                    erroObjetivo = objective.isBlank()
                    mensagem = ""
                    cadastroSucesso = false
                    if (!erroNome && !erroEmail && !erroObjetivo && !salvando) {
                        salvando = true
                        coroutineScope.launch {
                            try {
                                val dataAtual = SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale("pt", "BR")
                                ).format(Date())
                                val aluno = br.com.icaros.fitcare.data.AlunoEntity(
                                    nome = nome.trim(),
                                    email = email.trim(),
                                    dataCadastro = dataAtual,
                                    objetivo = objective.trim(),
                                    observacoesProfissionais = observacoesProfissionais.trim(),
                                    condicoes = condicoes.trim(),
                                    restricoes = restricoes.trim()
                                )
                                repository.inserir(aluno)
                                cadastroSucesso = true
                                mensagem = "Aluno cadastrado com sucesso!"
                                onCadastroConcluido()
                            } catch (e: SQLiteConstraintException) {
                                mensagem = "Este e-mail já está cadastrado."
                            } catch (e: Exception) {
                                mensagem = "Não foi possível cadastrar o aluno. Tente novamente."
                            } finally {
                                salvando = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
            ) {
                Text(
                    text = if (salvando) "Cadastrando..." else "Cadastrar aluno",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (mensagem.isNotBlank()) {
                Text(
                    text = mensagem,
                    color = if (cadastroSucesso) Color(0xFF2E7D32) else Vermelho,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal),
                border = BorderStroke(1.dp, AzulPrincipal)
            ) {
                Text("Cancelar")
            }
        }
// Rodapé padronizado
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
            .background(brush = Brush.verticalGradient(colors = listOf(LoginTopo, LoginBase)))
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
// Cabeçalho claro padronizado (Correção de contraste para a Logo)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AzulPrincipal.copy(alpha = 0.1f), CircleShape)
                            .clickable { onAlunos() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "○", color = AzulPrincipal, fontSize = 22.sp)
                    }
                }
            }
// Área clara de conteúdo do Dashboard
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
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
                    leadingIcon = { Text(text = "⌕", color = TextoSecundario, fontSize = 25.sp) },
                    trailingIcon = { Text(text = "☷", color = TextoSecundario, fontSize = 20.sp) },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = AzulPrincipal, unfocusedBorderColor = AzulMuitoClaro,
                        focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal
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
                            statusCor = if (aluno.status == "Atenção") Color(0xFFFFEAB5) else Color(
                                0xFFD8F3DF
                            ),
                            statusTextoCor = if (aluno.status == "Atenção") Color(0xFF9A7000) else Color(
                                0xFF36844A
                            ),
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
            Text(text = valor, color = corTexto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
            Text(text = icone, color = corTexto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
                    text = nome.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
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
    val alunos by repository.listarAlunos().collectAsState(initial = emptyList())
    val termo = pesquisa.trim()
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
        val correspondeStatus = filtroSelecionado == "Todos" || aluno.status == filtroSelecionado
        correspondeBusca && correspondeStatus && correspondeAcompanhamento
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
// Cabeçalho claro padronizado (Correção de contraste para a Logo)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AzulPrincipal.copy(alpha = 0.1f), CircleShape)
                            .clickable { onVoltar() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "○", color = AzulPrincipal, fontSize = 22.sp)
                    }
                }
            }
// Conteúdo claro da tela de listagem
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
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = AzulPrincipal,
                        unfocusedBorderColor = AzulMuitoClaro
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Todos", "Normal", "Atenção", "Prioridade").forEach { tag ->
                        val selecionado = filtroSelecionado == tag
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (selecionado) AzulPrincipal else Color.White,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { filtroSelecionado = tag }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (selecionado) Color.White else TextoPrincipal,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Acompanhamento",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoPrincipal
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Todos", "Com restrições", "Com observações").forEach { filtro ->
                        val selecionado = filtroAcompanhamento == filtro
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selecionado) AzulMuitoClaro else Branco)
                                .clickable { filtroAcompanhamento = filtro }
                                .padding(horizontal = 6.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filtro,
                                fontSize = 11.sp,
                                color = if (selecionado) AzulPrincipal else TextoPrincipal,
                                fontWeight = if (selecionado) FontWeight.SemiBold else FontWeight.Normal
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
                                text = if (pesquisa.isBlank()) "Não há alunos cadastrados." else "Tente buscar por outro nome ou altere o filtro.",
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
                                .clickable { onSelecionarAluno(aluno) },
                            shape = RoundedCornerShape(13.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color(0xFFE9E7EC), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = nome.split(" ").take(2)
                                                .mapNotNull { it.firstOrNull() }.joinToString(""),
                                            color = Color(0xFF706A75),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
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
                                    Text(text = "›", color = Color(0xFF77717F), fontSize = 26.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
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
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
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
                                        }, color = AzulPrincipal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        DashboardBottomBar(
            telaAtual = AppScreen.Alunos,
            onInicio = onVoltar,
            onAlunos = {},
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {})
    }
}

// Tela de Perfil Individual do Aluno
@Composable
fun TelaPerfilAluno(
    alunoId: Long,
    repository: AlunoRepository,
    onVoltar: () -> Unit,
    onDashboard: () -> Unit,
    onTreinos: () -> Unit,
    onEditar: () -> Unit
) {
    var alunoAtual by remember { mutableStateOf<br.com.icaros.fitcare.data.AlunoEntity?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var mostrarDialogoExclusao by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf(false) }
    var erroExclusao by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(alunoId) {
        repository.buscarPorId(alunoId).collect { resultado ->
            alunoAtual = resultado
            carregando = false
        }
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
            Text(text = "Carregando dados do aluno...", color = TextoSecundario, fontSize = 13.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
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
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
// Cabeçalho claro padronizado (Correção de contraste para a Logo)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onVoltar() }
                            .align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "←",
                            color = AzulPrincipal,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Voltar",
                            color = AzulPrincipal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier
                            .width(160.dp)
                            .height(80.dp)
                    )
                }
            }
// Identificação do aluno
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
                    Text(text = "👤", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = nomeAluno,
                        color = TextoPrincipal,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(text = objetivoAluno, color = TextoSecundario, fontSize = 14.sp)
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
// Cartões de Informações
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) { // cartão de dados cadastrais
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👤", color = AzulPrincipal, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dados cadastrais",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinhaDado(rotulo = "E-mail", valor = alunoAtual!!.email)
                    LinhaDado(rotulo = "Cadastro", valor = alunoAtual!!.dataCadastro)
                    LinhaDado(rotulo = "Objetivo", valor = alunoAtual!!.objetivo)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Editar dados ›",
                        color = AzulPrincipal,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onEditar() })
                }
                OutlinedButton(
                    onClick = { erroExclusao = ""; mostrarDialogoExclusao = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Vermelho)
                ) {
                    Text("Excluir aluno")
                }
                Spacer(modifier = Modifier.height(12.dp))
                // cartão de acompanhamento
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🩺",
                            color = AzulPrincipal,
                            fontSize = 15.sp
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
                            .padding(
                                horizontal = 12.dp,
                                vertical = 5.dp
                            )
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
                        valor = observacoesProfissionais.ifBlank { "Não informado." }
                    )

                    LinhaDado(
                        rotulo = "Condições",
                        valor = condicoes.ifBlank { "Não informado." }
                    )

                    LinhaDado(
                        rotulo = "Restrições",
                        valor = restricoes.ifBlank { "Não informado." }
                    )


                    val motivos = identificarMotivosAcompanhamento(
                        condicoes = condicoes,
                        restricoes = restricoes,
                        observacoes = observacoesProfissionais
                    )

                    if (motivos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Alertas identificados",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB3261E)
                            )

                            motivos.forEach { motivo ->
                                Text(
                                    text = "• $motivo",
                                    fontSize = 13.sp,
                                    color = Color(0xFFB3261E)
                                )
                            }
                        }
                    }
                }
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) { // cartão de anamnese
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📋", color = AzulPrincipal, fontSize = 15.sp)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal),
                        border = BorderStroke(1.dp, AzulPrincipal)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📤", fontSize = 13.sp)
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
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) { // cartão de registro de dor e avaliação
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❤️", color = AzulPrincipal, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registro de dor e avaliação",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFD9DC), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Última avaliação • 08/10/2026",
                                    color = Color(0xFF9C4350),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "4/10",
                                    color = Color(0xFF9C4350),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Intensidade da dor",
                                color = Color(0xFF9C4350),
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .height(6.dp)
                                    .background(Color.White.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .fillMaxHeight()
                                        .background(Color(0xFFE34B63), CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Região: lombar",
                                color = TextoPrincipal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Observação: desconforto relatado pelo aluno.",
                                color = TextoSecundario,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal)
                    ) {
                        Text(
                            text = "+ Nova avaliação",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Cartao { // cartão de histórico do aluno
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", color = AzulPrincipal, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico do aluno",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextoPrincipal
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Evolução da intensidade da dor",
                        color = TextoSecundario,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("10", fontSize = 10.sp, color = TextoSecundario)
                                Text("7,5", fontSize = 10.sp, color = TextoSecundario)
                                Text("5", fontSize = 10.sp, color = TextoSecundario)
                                Text("2.5", fontSize = 10.sp, color = TextoSecundario)
                                Text("0", fontSize = 10.sp, color = TextoSecundario)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) { GraficoHistoricoCustomizado() }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15/09", color = TextoSecundario, fontSize = 10.sp)
                        Text("22/09", color = TextoSecundario, fontSize = 10.sp)
                        Text("01/10", color = TextoSecundario, fontSize = 10.sp)
                        Text("08/10", color = TextoSecundario, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Dados ilustrativos • Escala de 0 a 10",
                        color = TextoSecundario,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFFE9E7EC), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Avaliações anteriores",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrincipal
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ItemAvaliacaoHistorica(
                        data = "08/10/2026",
                        regiao = "Região lombar",
                        nota = "4/10",
                        colorNota = Color(0xFFE34B63)
                    )
                    ItemAvaliacaoHistorica(
                        data = "01/10/2026",
                        regiao = "Região lombar",
                        nota = "6/10",
                        colorNota = Color(0xFFE34B63)
                    )
                    ItemAvaliacaoHistorica(
                        data = "22/09/2026",
                        regiao = "Região lombar",
                        nota = "5/10",
                        colorNota = Color(0xFFE34B63)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (mostrarDialogoExclusao) {
                AlertDialog(
                    onDismissRequest = { if (!excluindo) mostrarDialogoExclusao = false },
                    title = { Text("Confirmar exclusão") },
                    text = {
                        Column {
                            Text("Deseja excluir definitivamente os dados deste aluno? Esta ação não poderá ser desfeita.")
                            if (erroExclusao.isNotBlank()) {
                                Text(text = erroExclusao, color = Vermelho, fontSize = 13.sp)
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
                        ) { Text(if (excluindo) "Excluindo..." else "Excluir", color = Vermelho) }
                    },
                    dismissButton = {
                        TextButton(
                            enabled = !excluindo,
                            onClick = { mostrarDialogoExclusao = false }) { Text("Cancelar") }
                    }
                )
            }
            DashboardBottomBar(
                telaAtual = AppScreen.PerfilAluno,
                onInicio = onDashboard,
                onAlunos = onVoltar,
                onTreinos = onTreinos,
                onRegistros = {},
                onMais = {})
        }
    }
}

@Composable
private fun ItemAvaliacaoHistorica(data: String, regiao: String, nota: String, colorNota: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFFFBF9FE), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            Text(text = nota, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colorNota)
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

// Tela do formulário de edição
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
    var alunoOriginal by remember { mutableStateOf<br.com.icaros.fitcare.data.AlunoEntity?>(null) }
    var carregando by remember { mutableStateOf(true) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }
    var observacoes by remember { mutableStateOf("") }
    var condicoes by remember { mutableStateOf("") }
    var restricoes by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(alunoId) {
        repository.buscarPorId(alunoId).collect { aluno ->
            if (alunoOriginal == null && aluno != null) {
                alunoOriginal = aluno
                nome = aluno.nome
                email = aluno.email
                objetivo = aluno.objetivo
                observacoes = aluno.observacoesProfissionais
                condicoes = aluno.condicoes
                restricoes = aluno.restricoes
            }
            carregando = false
        }
    }
    if (carregando) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AzulPrincipal)
        }
        return
    }
    val original = alunoOriginal
    if (original == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Aluno não encontrado.", color = TextoPrincipal)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onVoltar) { Text("Voltar") }
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FundoApp)
    ) {
// CABEÇALHO PADRÃO CLARO
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
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_fitcare),
                contentDescription = "Logo FitCare",
                modifier = Modifier
                    .width(160.dp)
                    .height(80.dp)
            )
            TextButton(
                onClick = onVoltar,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Text(text = "← Voltar", color = AzulPrincipal)
            }
        }
// CONTEÚDO DA TELA
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Editar aluno",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextoPrincipal
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it; erro = "" },
                label = { Text("Nome completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; erro = "" },
                label = { Text("E-mail") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = objetivo,
                onValueChange = { objetivo = it; erro = "" },
                label = { Text("Objetivo do aluno") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações profissionais") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = condicoes,
                onValueChange = { condicoes = it },
                label = { Text("Condições de saúde") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = restricoes,
                onValueChange = { restricoes = it },
                label = { Text("Restrições") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            if (erro.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = erro, color = Color.Red, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    erro = ""
                    when {
                        nome.isBlank() -> erro = "Informe o nome do aluno."
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim())
                            .matches() -> erro = "Informe um e-mail válido."

                        objetivo.isBlank() -> erro = "Informe o objetivo do aluno."
                        else -> {
                            scope.launch {
                                salvando = true
                                try {
                                    repository.atualizar(
                                        original.copy(
                                            nome = nome.trim(),
                                            email = email.trim(),
                                            objetivo = objetivo.trim(),
                                            observacoesProfissionais = observacoes.trim(),
                                            condicoes = condicoes.trim(),
                                            restricoes = restricoes.trim()
                                        )
                                    )
                                    onSalvo()
                                } catch (e: Exception) {
                                    erro = "Não foi possível salvar as alterações."
                                } finally {
                                    salvando = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            ) {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Salvar alterações")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onVoltar,
                modifier = Modifier.fillMaxWidth(),
                enabled = !salvando
            ) { Text("Cancelar") }
            Spacer(modifier = Modifier.height(16.dp))
        }
        DashboardBottomBar(
            telaAtual = AppScreen.EditarAluno,
            onInicio = onDashboard,
            onAlunos = onAlunos,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {})
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
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
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

    val formatoData = remember {
        java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale("pt", "BR")
        ).apply {
            isLenient = false
        }
    }

    val hoje = remember {
        java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale("pt", "BR")
        ).format(java.util.Date())
    }

    val dataHoje = remember(hoje) {
        formatoData.parse(hoje)
    }

    val treinosAtivos = treinos.count { treino ->
        val dataFim = treino.dataFim.trim()

        if (dataFim.isBlank()) {
            true
        } else {
            val data = try {
                formatoData.parse(dataFim)
            } catch (_: Exception) {
                null
            }

            data != null && dataHoje != null && !data.before(dataHoje)
        }
    }

    val treinosAVencer = treinos.count { treino ->
        val dataFim = treino.dataFim.trim()

        if (dataFim.isBlank()) {
            false
        } else {
            val data = try {
                formatoData.parse(dataFim)
            } catch (_: Exception) {
                null
            }

            val limite = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 7)
            }.time

            data != null &&
                    dataHoje != null &&
                    !data.before(dataHoje) &&
                    !data.after(limite)
        }
    }

    val treinosVencidos = treinos.count { treino ->
        val dataFim = treino.dataFim.trim()

        if (dataFim.isBlank()) {
            false
        } else {
            val data = try {
                formatoData.parse(dataFim)
            } catch (_: Exception) {
                null
            }

            data != null && dataHoje != null && data.before(dataHoje)
        }
    }
    val alunos by repository
        .listarAlunos()
        .collectAsState(initial = emptyList())

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
        ) ||
                treino.nome.contains(
                    pesquisa,
                    ignoreCase = true
                )
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
                            text = "Buscar por nome, e-mail ou cadastro..."
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

                        val hoje = java.text.SimpleDateFormat(
                            "dd/MM/yyyy",
                            java.util.Locale("pt", "BR")
                        ).format(java.util.Date())

                        val status = when {
                            dataFim.isBlank() -> "Sem prazo"
                            dataFim < hoje -> "Vencido"
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
            }
        }

        DashboardBottomBar(
            telaAtual = AppScreen.Treinos,
            onInicio = onVoltar,
            onAlunos = onAlunos,
            onRegistros = {},
            onTreinos = {},
            onMais = {}
        )
    }
}

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

    /*
     * Carrega os dados do treino existente.
     */
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

    /*
     * Confirmação da exclusão do treino.
     */
    if (mostrarDialogoExcluir && treinoId != null) {
        AlertDialog(
            onDismissRequest = {
                if (!salvando) {
                    mostrarDialogoExcluir = false
                }
            },
            title = {
                Text(
                    text = "Excluir treino"
                )
            },
            text = {
                Text(
                    text = "Tem certeza que deseja excluir este treino? " +
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
                                            (e.message
                                                ?: "erro inesperado.")

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
    ) {

        // CABEÇALHO
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

            // Botão voltar
            IconButton(
                onClick = onVoltar,
                enabled = !salvando,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = AzulPrincipal
                )
            }

            // Logo
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

            // Lixeira
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

        // CONTEÚDO
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
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
                                        color = MaterialTheme
                                            .colorScheme
                                            .error
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = exercicio.nome,
                            onValueChange = {
                                exercicios[index] =
                                    exercicios[index].copy(
                                        nome = it
                                    )
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
                                        exercicios[index].copy(
                                            series = it
                                        )
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
                                        exercicios[index].copy(
                                            repeticoes = it
                                        )
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
                                    exercicios[index].copy(
                                        carga = it
                                    )
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
                                    exercicios[index].copy(
                                        observacoes = it
                                    )
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

            // Adicionar exercício
            OutlinedButton(
                onClick = {
                    exercicios.add(
                        ExercicioFormulario()
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Adicionar exercício")
            }

            /*
             * Botão de exclusão inferior.
             * Aparece somente na edição.
             */
            if (treinoId != null) {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

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

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

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

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }

        // RODAPÉ
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
                        mensagemErro =
                            "Informe o nome do treino."

                        return@Button
                    }

                    if (dataInicio.isBlank()) {
                        mensagemErro =
                            "Informe a data de início."

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
                        val data =
                            formatoData.parse(dataInicio)

                        data != null &&
                                formatoData.format(data) ==
                                dataInicio

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
                            val data =
                                formatoData.parse(dataFim)

                            data != null &&
                                    formatoData.format(data) ==
                                    dataFim

                        } catch (_: Exception) {
                            false
                        }

                        if (!fimValido) {
                            mensagemErro =
                                "A data de término deve ser válida no formato dd/MM/aaaa."

                            return@Button
                        }

                        val inicio =
                            formatoData.parse(dataInicio)

                        val fim =
                            formatoData.parse(dataFim)

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

                    val exerciciosInvalidos =
                        exercicios.any { exercicio ->

                            exercicio.nome.isBlank() ||
                                    exercicio.series.toIntOrNull() == null ||
                                    exercicio.series
                                        .toIntOrNull()!! <= 0 ||
                                    exercicio.repeticoes.isBlank()
                        }

                    if (exerciciosInvalidos) {
                        mensagemErro =
                            "Preencha o nome, as séries válidas e as repetições de todos os exercícios."

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
                                            descricao =
                                                descricao.trim(),
                                            dataInicio =
                                                dataInicio.trim(),
                                            dataFim =
                                                dataFim.trim(),
                                            observacoes =
                                                observacoes.trim()
                                        )
                                    )

                                } else {

                                    treinoRepository.atualizarTreino(
                                        TreinoEntity(
                                            id = treinoId,
                                            alunoId = alunoId,
                                            nome = nome.trim(),
                                            descricao =
                                                descricao.trim(),
                                            dataInicio =
                                                dataInicio.trim(),
                                            dataFim =
                                                dataFim.trim(),
                                            observacoes =
                                                observacoes.trim()
                                        )
                                    )

                                    treinoId
                                }

                            if (treinoId != null) {
                                treinoRepository
                                    .excluirExerciciosDoTreino(
                                        treinoId
                                    )
                            }

                            exercicios.forEachIndexed { index,
                                                        exercicio ->

                                treinoRepository.inserirExercicio(
                                    ExercicioEntity(
                                        treinoId = idTreinoSalvo,
                                        nome =
                                            exercicio.nome.trim(),
                                        series =
                                            exercicio.series.toInt(),
                                        repeticoes =
                                            exercicio.repeticoes.trim(),
                                        carga =
                                            exercicio.carga.trim(),
                                        observacoes =
                                            exercicio.observacoes
                                                .trim(),
                                        ordem = index
                                    )
                                )
                            }

                            onSalvo()

                        } catch (e: Exception) {

                            mensagemErro =
                                "Não foi possível salvar o treino: " +
                                        (
                                                e.message
                                                    ?: "erro inesperado."
                                                )

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
                    Text(
                        text = "Voltar",
                        color = AzulPrincipal
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