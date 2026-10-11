package br.com.icaros.fitcare

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

// Cores da identidade visual
private val Lilas = Color(0xFF8250C8)
private val LilasClaro = Color(0xFFF0E8FC)
private val Vermelho = Color(0xFFE34B63)

// Tema claro das outras telas
private val Fundo = Color(0xFFF8F6FC)
private val Texto = Color(0xFF30263D)
private val Cinza = Color(0xFF77717F)
private val Branco = Color.White

// Cores da nova tela de Login
private val LoginTopo = Color(0xFF171329)
private val LoginBase = Color(0xFF080B18)
private val LoginCampo = Color(0xFF1D2035)
private val LoginCampoBorda = Color(0xFF343850)
private val LoginTexto = Color(0xFFF7F7FB)
private val LoginTextoSecundario = Color(0xFFA8ACBF)
private val LoginPlaceholder = Color(0xFF858AA1)
private val LoginAzul = Color(0xFF536DFE)
private val LoginAzulClaro = Color(0xFF8CA0FF)

// Activity Principal
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitCareApp()
        }
    }
}

// Mapeamento das telas do aplicativo
private enum class AppScreen {
    Login,
    Dashboard,
    Alunos,
    PerfilAluno,
    Treinos
}

// Fluxo de navegação principal da aplicação
@Composable
fun FitCareApp() {
    var tela by remember { mutableStateOf(AppScreen.Login) }
    var alunoSelecionado by remember { mutableStateOf("Ana Oliveira") }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Lilas,
            secondary = Vermelho,
            background = Fundo,
            surface = Branco
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Fundo
        ) {
            when (tela) {
                AppScreen.Login -> {
                    TelaLogin(onEntrar = { tela = AppScreen.Dashboard })
                }
                AppScreen.Dashboard -> {
                    TelaDashboard(
                        onAlunos = { tela = AppScreen.Alunos },
                        onTreinos = { tela = AppScreen.Treinos },
                        onPerfil = {
                            alunoSelecionado = "Ana Oliveira"
                            tela = AppScreen.PerfilAluno
                        },
                        onSair = { tela = AppScreen.Login }
                    )
                }
                AppScreen.Alunos -> {
                    TelaAlunos(
                        onVoltar = { tela = AppScreen.Dashboard },
                        onSelecionarAluno = {
                            alunoSelecionado = it
                            tela = AppScreen.PerfilAluno
                        },
                        onTreinos = { tela = AppScreen.Treinos }
                    )
                }
                AppScreen.PerfilAluno -> {
                    TelaPerfilAluno(
                        nomeAluno = alunoSelecionado,
                        onVoltar = { tela = AppScreen.Alunos },
                        onDashboard = { tela = AppScreen.Dashboard },
                        onTreinos = { tela = AppScreen.Treinos }
                    )
                }
                AppScreen.Treinos -> {
                    TelaTreinos(
                        onVoltar = { tela = AppScreen.Dashboard },
                        onAlunos = { tela = AppScreen.Alunos },
                        onPerfil = {
                            alunoSelecionado = "Ana Oliveira"
                            tela = AppScreen.PerfilAluno
                        }
                    )
                }
            }
        }
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
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
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
            .background(brush = Brush.verticalGradient(colors = listOf(LoginTopo, Color(0xFF101125), LoginBase)))
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
                modifier = Modifier.width(190.dp).height(125.dp)
            )
            Spacer(modifier = Modifier.height(34.dp))
            Text(text = "Bem-vindo de volta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = LoginTexto)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Acesse sua conta para gerenciar seus alunos.", fontSize = 14.sp, color = LoginTextoSecundario)
            Spacer(modifier = Modifier.height(30.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth().height(58.dp),
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
                modifier = Modifier.fillMaxWidth().height(58.dp),
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
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LoginAzul)
            ) {
                Text(text = "ENTRAR", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(text = "Esqueci minha senha", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LoginAzulClaro, modifier = Modifier.clickable {})
        }
    }
}

// Tela de Dashboard principal
@Composable
fun TelaDashboard(
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onPerfil: () -> Unit,
    onSair: () -> Unit
) {
    var pesquisa by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F6FC))) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            // Cabeçalho roxo compacto - Apenas para a Logo centralizada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF241B50), Color(0xFF33205E))))
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier.width(140.dp).height(70.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable { onPerfil() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "○", color = Color.White, fontSize = 22.sp)
                    }
                }
            }
// Área clara de conteúdo do Dashboard
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    text = "Acompanhe seus alunos e seus progressos.",
                    color = Texto,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = { pesquisa = it },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    singleLine = true,
                    placeholder = { Text(text = "Buscar aluno...", color = Color(0xFF77717F), fontSize = 14.sp) },
                    leadingIcon = { Text(text = "⌕", color = Color(0xFF6F6878), fontSize = 25.sp) },
                    trailingIcon = { Text(text = "☷", color = Color(0xFF6F6878), fontSize = 20.sp) },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = Lilas, unfocusedBorderColor = Color(0xFFD8D3DE),
                        focusedTextColor = Texto, unfocusedTextColor = Texto
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "Total de alunos", valor = "12", corFundo = Color(0xFFE9DFFF), corTexto = Color(0xFF6841A5))
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "Em atenção", valor = "3", corFundo = Color(0xFFFFEAB5), corTexto = Color(0xFFB17B00))
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "Em dia", valor = "9", corFundo = Color(0xFFD8F3DF), corTexto = Color(0xFF36844A))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = "Ações rápidas", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Texto)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardAcao(modifier = Modifier.weight(1f), titulo = "Cadastrar aluno", subtitulo = "Adicionar novo aluno", corFundo = Color(0xFFE6D9FA), corTexto = Color(0xFF5E398D), icone = "+", onClick = onAlunos)
                    DashboardAcao(modifier = Modifier.weight(1f), titulo = "Consultar alunos", subtitulo = "Ver lista e filtros", corFundo = Color(0xFFDCEBFF), corTexto = Color(0xFF416C93), icone = "⌕", onClick = onAlunos)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardAcao(modifier = Modifier.weight(1f), titulo = "Registrar treino", subtitulo = "Frequência e feedback", corFundo = Color(0xFFDDF4E3), corTexto = Color(0xFF39754A), icone = "✓", onClick = onTreinos)
                    DashboardAcao(modifier = Modifier.weight(1f), titulo = "Central de Atenção", subtitulo = "Ver casos prioritários", corFundo = Color(0xFFFFD9DC), corTexto = Color(0xFF9C4350), icone = "!", onClick = onAlunos)
                }
                Spacer(modifier = Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Últimos alunos acessados", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Texto, modifier = Modifier.weight(1f))
                    Text(text = "Ver todos ›", fontSize = 13.sp, color = Lilas, modifier = Modifier.clickable { onAlunos() })
                }
                Spacer(modifier = Modifier.height(10.dp))
                DashboardAluno(nome = "Lucas Carvalho", treino = "Treino: Força e Hipertrofia", status = "Em atenção", statusCor = Color(0xFFFFEAB5), statusTextoCor = Color(0xFF9A7000), onClick = onPerfil)
                Spacer(modifier = Modifier.height(8.dp))
                DashboardAluno(nome = "Ana Souza", treino = "Treino: Emagrecimento", status = "Em dia", statusCor = Color(0xFFD8F3DF), statusTextoCor = Color(0xFF36844A), onClick = onPerfil)
                Spacer(modifier = Modifier.height(8.dp))
                DashboardAluno(nome = "Rafael Martins", treino = "Treino: Em dia", status = "Em dia", statusCor = Color(0xFFD8F3DF), statusTextoCor = Color(0xFF36844A), onClick = onPerfil)
            }
        }
        DashboardBottomBar(telaAtual = AppScreen.Dashboard, onInicio = {}, onAlunos = onAlunos, onTreinos = onTreinos, onRegistros = onPerfil, onMais = onSair)
    }
}
// Indicadores do topo do dashboard
@Composable
private fun DashboardIndicador(modifier: Modifier = Modifier, titulo: String, valor: String, corFundo: Color, corTexto: Color) {
    Box(modifier = modifier.height(94.dp).background(color = corFundo, shape = RoundedCornerShape(13.dp)).padding(10.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = titulo, color = corTexto.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 2)
            Spacer(modifier = Modifier.height(5.dp))
            Text(text = valor, color = corTexto, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
// Botões das ações rápidas - CORRIGIDO CONTRA CORTES
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
            .height(104.dp) // Aumentamos ligeiramente a altura padrão de 92.dp para 104.dp para dar respiro
            .background(color = corFundo, shape = RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp) // Paddings equilibrados
    ) {
        Box(
            modifier = Modifier.size(24.dp).background(Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icone, color = corTexto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = titulo, color = corTexto, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = subtitulo,
            color = corTexto.copy(alpha = 0.85f),
            fontSize = 11.sp, // Ajuste sutil no tamanho para garantir fluidez
            lineHeight = 14.sp, // Adicionado controle de espaçamento entre linhas
            maxLines = 2
        )
    }
}
// Elemento individual da listagem rápida de alunos
@Composable
private fun DashboardAluno(nome: String, treino: String, status: String, statusCor: Color, statusTextoCor: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onClick() }, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(42.dp).background(Color(0xFFE9E7EC), CircleShape), contentAlignment = Alignment.Center) {
                Text(text = nome.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""), color = Color(0xFF706A75), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nome, color = Texto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = treino, color = Cinza, fontSize = 11.sp)
            }
            Box(modifier = Modifier.background(color = statusCor, shape = RoundedCornerShape(20.dp)).padding(horizontal = 9.dp, vertical = 5.dp)) {
                Text(text = status, color = statusTextoCor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "›", color = Color(0xFF77717F), fontSize = 25.sp)
        }
    }
}
// Tela de Lista Completa de Alunos com filtros
@Composable
fun TelaAlunos(onVoltar: () -> Unit, onSelecionarAluno: (String) -> Unit, onTreinos: () -> Unit) {
    var pesquisa by remember { mutableStateOf("") }
    var filtroSelecionado by remember { mutableStateOf("Todos") }
    val alunos = listOf(
        Triple("Lucas Carvalho", "Força e Hipertrofia", "Em atenção"),
        Triple("Ana Souza", "Emagrecimento", "Em dia"),
        Triple("Rafael Martins", "Treinamento funcional", "Em dia"),
        Triple("Mariana Costa", "Condicionamento físico", "Em atenção")
    )
    val filtrados = alunos.filter { (nome, _, status) ->
        nome.contains(pesquisa, ignoreCase = true) && (filtroSelecionado == "Todos" || status == filtroSelecionado)
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F6FC))) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
// Cabeçalho roxo compacto - Apenas para a Logo centralizada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF241B50), Color(0xFF33205E))))
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier.width(140.dp).height(70.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .clickable { onVoltar() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "○", color = Color.White, fontSize = 22.sp)
                    }
                }
            }
// Conteúdo claro da tela de listagem
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    text = "Meus alunos",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Texto,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = { pesquisa = it },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    singleLine = true,
                    placeholder = { Text(text = "Buscar aluno pelo nome...", color = Color(0xFF77717F), fontSize = 14.sp) },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedBorderColor = Lilas, unfocusedBorderColor = Color(0xFFD8D3DE))
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Todos", "Em atenção", "Em dia").forEach { tag ->
                        val selecionado = filtroSelecionado == tag
                        Box(modifier = Modifier.background(color = if (selecionado) Lilas else Color.White, shape = RoundedCornerShape(20.dp)).clickable { filtroSelecionado = tag }.padding(horizontal = 18.dp, vertical = 8.dp)) {
                            Text(text = tag, color = if (selecionado) Color.White else Texto, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                filtrados.forEach { (nome, objetivo, status) ->
                    val corStatus = if (status == "Em atenção") Color(0xFFFFD9DC) else Color(0xFFDDF4E3)
                    val corTextoStatus = if (status == "Em atenção") Color(0xFF9C4350) else Color(0xFF39754A)
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onSelecionarAluno(nome) }, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(44.dp).background(Color(0xFFE9E7EC), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(text = nome.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""), color = Color(0xFF706A75), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = nome, color = Texto, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(text = objetivo, color = Cinza, fontSize = 12.sp)
                                }
                                Text(text = "›", color = Color(0xFF77717F), fontSize = 26.sp)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(color = corStatus, shape = RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                                    Text(text = status, color = corTextoStatus, fontSize = 11.sp)
                                }
                                Text(text = "Ver perfil ›", color = Lilas, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
        DashboardBottomBar(telaAtual = AppScreen.Alunos, onInicio = onVoltar, onAlunos = {}, onTreinos = onTreinos, onRegistros = {}, onMais = {})
    }
}
// Tela de Perfil Individual do Aluno - ATUALIZADA COM CABEÇALHO E RODAPÉ
@Composable
fun TelaPerfilAluno(
    nomeAluno: String,
    onVoltar: () -> Unit,
    onDashboard: () -> Unit,
    onTreinos: () -> Unit
) {
    val objetivoAluno = when (nomeAluno) {
        "Lucas Carvalho" -> "Hipertrofia muscular"
        "Ana Souza" -> "Emagrecimento"
        "Rafael Martins" -> "Treinamento funcional"
        "Mariana Costa" -> "Condicionamento físico"
        else -> "Hipertrofia muscular"
    }
    val statusAluno = if (nomeAluno == "Lucas Carvalho" || nomeAluno == "Mariana Costa") "Em atenção" else "Em dia"
    val corStatus = if (statusAluno == "Em atenção") Color(0xFFFFD9DC) else Color(0xFFDDF4E3)
    val corTextoStatus = if (statusAluno == "Em atenção") Color(0xFF9C4350) else Color(0xFF39754A)
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F6FC))) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
// Cabeçalho Roxo Compacto Padronizado com os botões de controle integrados
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF241B50), Color(0xFF33205E))))
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
// Botão de retornar na extrema esquerda
                    Row(
                        modifier = Modifier.clickable { onVoltar() }.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Voltar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
// Logo perfeitamente centralizada
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier.width(140.dp).height(70.dp)
                    )
// Ícone de Editar na extrema direita
                    Text(
                        text = "✎",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { }.align(Alignment.CenterEnd)
                    )
                }
            }
// Identificação do aluno na área clara da tela
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(Color(0xFFE9E7EC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "👤", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = nomeAluno, color = Texto, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(text = objetivoAluno, color = Cinza, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.background(color = corStatus, shape = RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(text = statusAluno, color = corTextoStatus, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
// Cartões de Informações do Aluno
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👤", color = Lilas, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Dados cadastrais", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Texto)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinhaDado(rotulo = "E-mail", valor = "${nomeAluno.lowercase().replace(" ", "")}@email.com")
                    LinhaDado(rotulo = "Cadastro", valor = "15/09/2026")
                    LinhaDado(rotulo = "Objetivo", valor = objetivoAluno)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Editar dados ›",
                        color = Lilas,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { }
                    )
                }
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📋", color = Lilas, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Anamnese", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Texto)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Importe o documento de anamnese para manter as informações do aluno disponíveis no perfil.",
                        color = Cinza,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Lilas),
                        border = BorderStroke(1.dp, Lilas)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📤", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Importar anamnese", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Formatos aceitos: PDF ou imagem",
                        color = Cinza,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Cartao(modifier = Modifier.padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "❤️", color = Lilas, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Registro de dor e avaliação", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Texto)
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
                                Text(text = "Última avaliação • 08/10/2026", color = Color(0xFF9C4350), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "4/10", color = Color(0xFF9C4350), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Intensidade da dor", color = Color(0xFF9C4350), fontSize = 12.sp)
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
                            Text(text = "Região: lombar", color = Texto, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(text = "Observação: desconforto relatado pelo aluno.", color = Cinza, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Lilas)
                    ) {
                        Text(text = "+ Nova avaliação", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Cartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", color = Lilas, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Histórico do aluno", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Texto)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Evolução da intensidade da dor", color = Cinza, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier.fillMaxHeight().width(24.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("10", fontSize = 10.sp, color = Cinza)
                                Text("7,5", fontSize = 10.sp, color = Cinza)
                                Text("5", fontSize = 10.sp, color = Cinza)
                                Text("2.5", fontSize = 10.sp, color = Cinza)
                                Text("0", fontSize = 10.sp, color = Cinza)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                GraficoHistoricoCustomizado()
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15/09", color = Cinza, fontSize = 10.sp)
                        Text("22/09", color = Cinza, fontSize = 10.sp)
                        Text("01/10", color = Cinza, fontSize = 10.sp)
                        Text("08/10", color = Cinza, fontSize = 10.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Dados ilustrativos • Escala de 0 a 10", color = Cinza, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color(0xFFE9E7EC), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Avaliações anteriores", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Spacer(modifier = Modifier.height(10.dp))
                    ItemAvaliacaoHistorica(data = "08/10/2026", regiao = "Região lombar", nota = "4/10", colorNota = Color(0xFFE34B63))
                    ItemAvaliacaoHistorica(data = "01/10/2026", regiao = "Região lombar", nota = "6/10", colorNota = Color(0xFFE34B63))
                    ItemAvaliacaoHistorica(data = "22/09/2026", regiao = "Região lombar", nota = "5/10", colorNota = Color(0xFFE34B63))
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
// Barra inferior adicionada à TelaPerfilAluno
        DashboardBottomBar(
            telaAtual = AppScreen.PerfilAluno,
            onInicio = onDashboard,
            onAlunos = onVoltar,
            onTreinos = onTreinos,
            onRegistros = {},
            onMais = {}
        )
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
                Text(text = data, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Texto)
                Text(text = regiao, fontSize = 11.sp, color = Cinza)
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
            drawCircle(
                color = Color(0xFFE34B63),
                radius = 4.5.dp.toPx(),
                center = ponto
            )
        }
    }
}
// Tela de Gestão de Treinos e Planejamentos
@Composable
fun TelaTreinos(
    onVoltar: () -> Unit,
    onAlunos: () -> Unit,
    onPerfil: () -> Unit
) {
    var pesquisa by remember { mutableStateOf("") }
    val treinosMock = listOf(
        RowTreinoData("Lucas Carvalho", "Hipertrofia (A/B/C/D)", "01/10/2023 - 31/12/2023", "4x/semana", 0.65f, "Ativo", Color(0xFFD8F3DF), Color(0xFF36844A)),
        RowTreinoData("Ana Souza", "Hipertrofia (A/B/C)", "15/09/2023 - 22/10/2023", "3x/semana", 0.88f, "A vencer", Color(0xFFFFEAB5), Color(0xFFB17B00)),
        RowTreinoData("Rafael Martins", "Hipertrofia (A/B)", "01/08/2023 - 30/09/2023", "2x/semana", 1.00f, "Vencido", Color(0xFFFFD9DC), Color(0xFF9C4350))
    )
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F6FC))) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
// Cabeçalho roxo compacto - Apenas para a Logo centralizada
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF241B50), Color(0xFF33205E))))
                    .padding(vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_fitcare),
                        contentDescription = "Logo Ícaro's FitCare",
                        modifier = Modifier.width(140.dp).height(70.dp)
                    )
                }
            }
// Títulos claros e Botão "Novo Treino" colocados fora da faixa roxa
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Treinos", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Texto)
                        Text(text = "Gerencie os planejamentos dos seus alunos.", color = Cinza, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { /* Iniciar formulário de novo treino */ },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDDF4E3)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("+ Novo treino", color = Color(0xFF39754A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
// Campo de pesquisa rápida da lista de planejamentos
                OutlinedTextField(
                    value = pesquisa,
                    onValueChange = { pesquisa = it },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    singleLine = true,
                    placeholder = { Text(text = "Buscar por aluno ou treino...", color = Color(0xFF77717F), fontSize = 14.sp) },
                    leadingIcon = { Text(text = "⌕", color = Color(0xFF6F6878), fontSize = 25.sp) },
                    trailingIcon = { Text(text = "☷", color = Color(0xFF6F6878), fontSize = 20.sp) },
                    shape = RoundedCornerShape(13.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                        focusedBorderColor = Lilas, unfocusedBorderColor = Color(0xFFD8D3DE)
                    )
                )
                Spacer(modifier = Modifier.height(14.dp))
// Cartões com Indicadores Gerais de Vencimento e Atividade
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "Treinos ativos", valor = "18", corFundo = Color(0xFFDCEBFF), corTexto = Color(0xFF416C93))
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "A vencer (7 dias)", valor = "5", corFundo = Color(0xFFFFEAB5), corTexto = Color(0xFFB17B00))
                    DashboardIndicador(modifier = Modifier.weight(1f), titulo = "Vencidos", valor = "2", corFundo = Color(0xFFFFD9DC), corTexto = Color(0xFF9C4350))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Lista de Treinos", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Text(text = "Filtrar por situação", fontSize = 12.sp, color = Cinza)
                }
                Spacer(modifier = Modifier.height(10.dp))
// Lista de Treinos com indicador linear de progresso (%) realizado
                treinosMock.filter { it.aluno.contains(pesquisa, ignoreCase = true) || it.nomeTreino.contains(pesquisa, ignoreCase = true) }.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onPerfil() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Branco),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(Color(0xFFE9E7EC), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(text = "👤", fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Aluno: ${item.aluno}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Texto)
                                    Text(text = "Treino: ${item.nomeTreino}", fontSize = 13.sp, color = Texto)
                                    Text(text = "Período: ${item.periodo}", fontSize = 11.sp, color = Cinza)
                                    Text(text = "Frequência: ${item.frequencia}", fontSize = 11.sp, color = Cinza)
                                }
                                Box(modifier = Modifier.background(item.corStatus, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                    Text(text = item.statusTxt, color = item.corTxt, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Progresso realizado: ${(item.progresso * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Texto)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LinearProgressIndicator(
                                    progress = { item.progresso },
                                    modifier = Modifier.weight(1f).height(8.dp),
                                    color = Color(0xFF241B50),
                                    trackColor = Color(0xFFE9E7EC),
                                    strokeCap = StrokeCap.Round
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Detalhes ›", color = Lilas, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        DashboardBottomBar(telaAtual = AppScreen.Treinos, onInicio = onVoltar, onAlunos = onAlunos, onTreinos = {}, onRegistros = onPerfil, onMais = {})
    }
}
// Estrutura de Modelagem de Dados para a Lista de Treinos
private data class RowTreinoData(
    val aluno: String, val nomeTreino: String, val periodo: String, val frequencia: String,
    val progresso: Float, val statusTxt: String, val corStatus: Color, val corTxt: Color
)
// Barra de navegação inferior global do aplicativo - SEM EMOJI DE PESO
@Composable
private fun DashboardBottomBar(
    telaAtual: AppScreen,
    onInicio: () -> Unit,
    onAlunos: () -> Unit,
    onTreinos: () -> Unit,
    onRegistros: () -> Unit,
    onMais: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.White, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().height(68.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
            DashboardNavItem(icone = "⌂", titulo = "Início", selecionado = telaAtual == AppScreen.Dashboard, onClick = onInicio)
            DashboardNavItem(icone = "♙", titulo = "Alunos", selecionado = telaAtual == AppScreen.Alunos || telaAtual == AppScreen.PerfilAluno, onClick = onAlunos)

            // ÍCONE ALTERADO DE '🏋' PARA '⚙' (OU OUTRO SÍMBOLO DA SUA PREFERÊNCIA)
            DashboardNavItem(icone = "⚙", titulo = "Treinos", selecionado = telaAtual == AppScreen.Treinos, onClick = onTreinos)

            DashboardNavItem(icone = "▣", titulo = "Registros", selecionado = false, onClick = onRegistros)
            DashboardNavItem(icone = "⋯", titulo = "Mais", selecionado = telaAtual == AppScreen.Login, onClick = onMais)
        }
    }
}
// Item individual da barra inferior
@Composable
private fun DashboardNavItem(icone: String, titulo: String, selecionado: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.width(65.dp).clickable { onClick() }.padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icone, color = if (selecionado) Lilas else Color(0xFF77717F), fontSize = 21.sp)
        Text(text = titulo, color = if (selecionado) Lilas else Color(0xFF77717F), fontSize = 10.sp, fontWeight = if (selecionado) FontWeight.Bold else FontWeight.Normal)
    }
}
// Linha de detalhamento de rótulo e valor
@Composable
private fun LinhaDado(rotulo: String, valor: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(rotulo, fontSize = 12.sp, color = Cinza)
        Text(valor, fontSize = 15.sp, color = Texto)
    }
}
// PREVIEWS
@Preview(showBackground = true, showSystemUi = true, name = "01 - Login")
@Composable
fun PreviewLogin() {
    MaterialTheme {
        TelaLogin(onEntrar = {})
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "02 - Dashboard")
@Composable
fun PreviewDashboard() {
    MaterialTheme {
        TelaDashboard(
            onAlunos = {},
            onTreinos = {},
            onPerfil = {},
            onSair = {}
        )
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "03 - Alunos")
@Composable
fun PreviewAlunos() {
    MaterialTheme {
        TelaAlunos(
            onVoltar = {},
            onSelecionarAluno = {},
            onTreinos = {}
        )
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "04 - Perfil do Aluno")
@Composable
fun PreviewPerfilAluno() {
    MaterialTheme {
        TelaPerfilAluno(
            nomeAluno = "Lucas Carvalho",
            onVoltar = {},
            onDashboard = {},
            onTreinos = {}
        )
    }
}
@Preview(showBackground = true, showSystemUi = true, name = "06 - Tela de Treinos")
@Composable
fun PreviewTreinos() {
    MaterialTheme {
        TelaTreinos(
            onVoltar = {},
            onAlunos = {},
            onPerfil = {}
        )
    }
}