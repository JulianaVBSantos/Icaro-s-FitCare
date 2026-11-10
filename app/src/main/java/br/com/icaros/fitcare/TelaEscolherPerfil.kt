
package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun TelaEscolherPerfil(
    onSelecionarPerfil: (String) -> Unit
) {
    val azulClaro = Color(0xFFEAF5FF)
    val azulFundo = Color(0xFFD8ECFF)

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
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.logo_fitcare),
                contentDescription = "Logo Ícaros FitCare",
                modifier = Modifier
                    .width(250.dp)
                    .height(165.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Bem-vindo ao Ícaros FitCare",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF173B5E)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Como deseja acessar o aplicativo?",
                color = Color(0xFF38566F),
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
                color = Color(0xFF38566F),
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    onSelecionarPerfil("ADMIN")
                }
            )
        }
    }
}