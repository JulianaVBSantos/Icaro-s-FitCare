package br.com.icaros.fitcare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TelaEscolherPerfil(
    onSelecionarPerfil: (String) -> Unit,
    onCadastrarProfessor: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Bem-vindo ao Ícaros FitCare",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Como deseja acessar o aplicativo?")

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSelecionarPerfil("PROFESSOR") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SOU PROFESSOR")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onSelecionarPerfil("ALUNO") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SOU ALUNO")
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onCadastrarProfessor) {
            Text("Criar minha conta profissional")
        }
    }
}