package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TelaEscolherPerfil(
    onSelecionarPerfil: (String) -> Unit
) {
    val escuro = temaEscuroAtivoState
    val corTopo = if (escuro) Color(0xFF141F2E) else Color(0xFFEAF5FF)
    val corBase = if (escuro) Color(0xFF0F1722) else Color(0xFFD8ECFF)
    val corTitulo = if (escuro) Color(0xFFEAF2FA) else Color(0xFF173B5E)
    val corSubtitulo = if (escuro) Color(0xFFA2B5C8) else Color(0xFF38566F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(corTopo, corBase)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoFitCarePadrao(largura = 230.dp, altura = 150.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bem-vindo ao Ícaros FitCare",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = corTitulo
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Como deseja acessar o aplicativo?",
                color = corSubtitulo,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    onSelecionarPerfil("PROFESSOR")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SOU PROFESSOR")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    onSelecionarPerfil("ALUNO")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("SOU ALUNO")
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Acesso administrativo",
                color = corSubtitulo,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    onSelecionarPerfil("ADMIN")
                }
            )
        }
    }
}