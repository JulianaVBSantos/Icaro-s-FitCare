
package br.com.icaros.fitcare

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Lilas = Color(0xFF8250C8)
private val LilasClaro = Color(0xFFF0E8FC)
private val Vermelho = Color(0xFFE34B63)
private val Fundo = Color(0xFFF8F6FC)
private val Texto = Color(0xFF30263D)
private val Cinza = Color(0xFF77717F)
private val Branco = Color.White

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitCareApp()
        }
    }
}

private enum class AppScreen {
    Login, Dashboard, Alunos, PerfilAluno
}

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
                AppScreen.Login -> TelaLogin(
                    onEntrar = { tela = AppScreen.Dashboard }
                )

                AppScreen.Dashboard -> TelaDashboard(
                    onAlunos = { tela = AppScreen.Alunos },
                    onPerfil = {
                        alunoSelecionado = "Ana Oliveira"
                        tela = AppScreen.PerfilAluno
                    },
                    onSair = { tela = AppScreen.Login }
                )

                AppScreen.Alunos -> TelaAlunos(
                    onVoltar = { tela = AppScreen.Dashboard },
                    onSelecionarAluno = {
                        alunoSelecionado = it
                        tela = AppScreen.PerfilAluno
                    }
                )

                AppScreen.PerfilAluno -> TelaPerfilAluno(
                    nomeAluno = alunoSelecionado,
                    onVoltar = { tela = AppScreen.Alunos }
                )
            }
        }
    }
}

@Composable
private fun Cabecalho(
    titulo: String,
    subtitulo: String = "",
    onVoltar: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        if (onVoltar != null) {
            Text(
                text = "← Voltar",
                color = Lilas,
                modifier = Modifier
                    .clickable { onVoltar() }
                    .padding(bottom = 16.dp)
            )
        }

        Text(
            text = titulo,
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Texto
        )

        if (subtitulo.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                text = subtitulo,
                fontSize = 14.sp,
                color = Cinza
            )
        }
    }
}

@Composable
private fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Lilas
        )
    ) {
        Text(texto, fontSize = 16.sp)
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
        Column(
            modifier = Modifier.padding(18.dp),
            content = conteudo
        )
    }
}

// TELA 1 — LOGIN

@Composable
fun TelaLogin(
    onEntrar: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fundo)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(LilasClaro, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("IF", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Lilas)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Ícaro's FitCare",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Lilas
        )

        Text(
            "Cuidado e acompanhamento de alunos",
            fontSize = 14.sp,
            color = Cinza
        )

        Spacer(Modifier.height(36.dp))

        Cartao {
            Text("Bem-vindo!", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(6.dp))
            Text("Entre na sua conta para continuar.", color = Cinza, fontSize = 14.sp)

            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                placeholder = { Text("seuemail@exemplo.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            BotaoPrincipal(
                texto = "Entrar",
                onClick = onEntrar
            )

            Spacer(Modifier.height(14.dp))

            Text(
                "Acesso demonstrativo",
                color = Cinza,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

// TELA 2 — DASHBOARD

@Composable
fun TelaDashboard(
    onAlunos: () -> Unit,
    onPerfil: () -> Unit,
    onSair: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ícaro's FitCare", color = Lilas, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Painel de acompanhamento", color = Cinza, fontSize = 13.sp)
            }

            Text(
                "Sair",
                color = Vermelho,
                modifier = Modifier.clickable { onSair() }.padding(8.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Cabecalho(
            titulo = "Olá, profissional!",
            subtitulo = "Confira o resumo dos seus alunos."
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Cartao(modifier = Modifier.weight(1f)) {
                Text("Alunos", color = Cinza, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("12", fontSize = 32.sp, color = Lilas, fontWeight = FontWeight.Bold)
                Text("cadastrados", color = Cinza, fontSize = 12.sp)
            }

            Cartao(modifier = Modifier.weight(1f)) {
                Text("Atenção", color = Cinza, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text("3", fontSize = 32.sp, color = Vermelho, fontWeight = FontWeight.Bold)
                Text("para acompanhar", color = Cinza, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(22.dp))

        Cartao {
            Text("Acesso rápido", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Texto)
            Spacer(Modifier.height(8.dp))
            Text("Consulte os cadastros e o histórico de acompanhamento.", color = Cinza, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))

            BotaoPrincipal("Ver alunos", onAlunos)

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onPerfil,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Abrir perfil demonstrativo", color = Lilas)
            }
        }

        Spacer(Modifier.height(22.dp))

        Cartao {
            Text("Lembrete", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Texto)
            Spacer(Modifier.height(8.dp))
            Text(
                "Mantenha os registros de avaliações e relatos de dor atualizados para facilitar o acompanhamento.",
                color = Cinza,
                fontSize = 14.sp
            )
        }
    }
}

// TELA 3 — LISTA DE ALUNOS

@Composable
fun TelaAlunos(
    onVoltar: () -> Unit,
    onSelecionarAluno: (String) -> Unit
) {
    var pesquisa by remember { mutableStateOf("") }

    val alunos = listOf(
        "Ana Oliveira" to "Acompanhamento ativo",
        "Bruno Santos" to "Acompanhamento ativo",
        "Carla Mendes" to "Avaliação pendente",
        "Daniel Costa" to "Acompanhamento ativo"
    )

    val filtrados = alunos.filter {
        it.first.contains(pesquisa, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Cabecalho(
            titulo = "Alunos",
            subtitulo = "Consulte os alunos cadastrados.",
            onVoltar = onVoltar
        )

        OutlinedTextField(
            value = pesquisa,
            onValueChange = { pesquisa = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar aluno") },
            placeholder = { Text("Digite um nome") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "${filtrados.size} aluno(s)",
            color = Cinza,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(10.dp))

        filtrados.forEach { aluno ->
            Cartao(
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(LilasClaro, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            aluno.first.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                            color = Lilas,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(aluno.first, fontWeight = FontWeight.Bold, color = Texto, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            aluno.second,
                            color = if (aluno.second.contains("pendente")) Vermelho else Cinza,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        "›",
                        color = Lilas,
                        fontSize = 30.sp,
                        modifier = Modifier.clickable { onSelecionarAluno(aluno.first) }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onSelecionarAluno(aluno.first) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ver perfil")
                }
            }
        }

        if (filtrados.isEmpty()) {
            Text("Nenhum aluno encontrado.", color = Cinza)
        }
    }
}

// TELA 4 — PERFIL DO ALUNO

@Composable
fun TelaPerfilAluno(
    nomeAluno: String,
    onVoltar: () -> Unit
) {
    var relatoDor by remember { mutableStateOf("") }
    var avaliacao by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(22.dp)
    ) {
        Cabecalho(
            titulo = "Perfil do aluno",
            subtitulo = "Dados e histórico de acompanhamento",
            onVoltar = onVoltar
        )

        Cartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .background(LilasClaro, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        nomeAluno.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                        color = Lilas,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(nomeAluno, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Texto)
                    Text("Aluno cadastrado", color = Cinza, fontSize = 13.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Perfil de acompanhamento", color = Lilas, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Cartao {
            TituloSecao("Dados pessoais")
            LinhaDado("E-mail", "aluno@exemplo.com")
            LinhaDado("Telefone", "(00) 00000-0000")
            LinhaDado("Cadastro", "Dados demonstrativos")
        }

        Spacer(Modifier.height(16.dp))

        Cartao {
            TituloSecao("Registro de dor")
            Text(
                "Registre relatos informados pelo aluno.",
                color = Cinza,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = relatoDor,
                onValueChange = { relatoDor = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Local e descrição da dor") },
                placeholder = { Text("Descreva o relato...") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Vermelho)
            ) {
                Text("Registrar relato (demonstração)")
            }
        }

        Spacer(Modifier.height(16.dp))

        Cartao {
            TituloSecao("Avaliações")
            OutlinedTextField(
                value = avaliacao,
                onValueChange = { avaliacao = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Observações da avaliação") },
                placeholder = { Text("Digite as observações...") },
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salvar avaliação (demonstração)")
            }
        }

        Spacer(Modifier.height(16.dp))

        Cartao {
            TituloSecao("Anamnese")
            Text(
                "Anexe ou consulte o documento de anamnese do aluno.",
                color = Cinza,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("＋ Importar documento de anamnese")
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "A importação de arquivos será implementada posteriormente.",
                color = Cinza,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Cartao {
            TituloSecao("Histórico de acompanhamento")
            Text(
                "Exemplo ilustrativo de registros ao longo do tempo.",
                color = Cinza,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))

            GraficoHistorico()

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", color = Cinza, fontSize = 11.sp)
                Text("Out", color = Cinza, fontSize = 11.sp)
                Text("Nov", color = Cinza, fontSize = 11.sp)
                Text("Dez", color = Cinza, fontSize = 11.sp)
                Text("Jan", color = Cinza, fontSize = 11.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "Os valores do gráfico são fictícios e servem apenas para demonstrar o layout.",
                color = Cinza,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TituloSecao(titulo: String) {
    Text(
        text = titulo,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Texto
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LinhaDado(
    rotulo: String,
    valor: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(rotulo, fontSize = 12.sp, color = Cinza)
        Text(valor, fontSize = 15.sp, color = Texto)
    }
}

@Composable
private fun GraficoHistorico() {
    val valores = listOf(0.35f, 0.55f, 0.42f, 0.72f, 0.58f, 0.82f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFFFCFAFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        val largura = size.width
        val altura = size.height

        for (i in 0..3) {
            val y = altura * i / 3f
            drawLine(
                color = Color(0xFFE9E1F3),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(largura, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val pontos = valores.mapIndexed { index, valor ->
            androidx.compose.ui.geometry.Offset(
                x = largura * index / (valores.size - 1).toFloat(),
                y = altura * (1f - valor)
            )
        }

        for (i in 0 until pontos.lastIndex) {
            drawLine(
                color = Lilas,
                start = pontos[i],
                end = pontos[i + 1],
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        pontos.forEach { ponto ->
            drawCircle(
                color = Vermelho,
                radius = 4.dp.toPx(),
                center = ponto
            )
        }
    }
}

// PREVIEWS INDIVIDUAIS

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
            onSelecionarAluno = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "04 - Perfil do Aluno")
@Composable
fun PreviewPerfilAluno() {
    MaterialTheme {
        TelaPerfilAluno(
            nomeAluno = "Ana Oliveira",
            onVoltar = {}
        )
    }
}

@Preview(showBackground = true, name = "05 - Teste de Preview")
@Composable
fun TestePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Fundo)
                .padding(24.dp)
        ) {
            Text(
                "Ícaro's FitCare",
                color = Lilas,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text("Se você consegue ver esta mensagem, o Preview está funcionando.")
        }
    }
}
