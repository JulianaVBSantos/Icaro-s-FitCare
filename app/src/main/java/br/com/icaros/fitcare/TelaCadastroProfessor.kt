package br.com.icaros.fitcare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import br.com.icaros.fitcare.data.UsuarioRepository

@Composable
fun TelaCadastroProfessor(
    usuarioRepository: UsuarioRepository,
    onVoltar: () -> Unit,
    onCadastroConcluido: () -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var salvando by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Criar conta profissional",
            style = MaterialTheme.typography.headlineSmall
        )

        Text("Preencha os dados para cadastrar seu acesso ao Ícaros FitCare.")

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            label = { Text("Nome completo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha (mínimo de 8 caracteres)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it },
            label = { Text("Confirme sua senha") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (erro.isNotBlank()) {
            Text(
                text = erro,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                when {
                    nome.isBlank() ->
                        erro = "Informe seu nome completo."

                    email.isBlank() ->
                        erro = "Informe seu e-mail."

                    senha.length < 8 ->
                        erro = "A senha deve ter pelo menos 8 caracteres."

                    senha != confirmarSenha ->
                        erro = "As senhas não coincidem."

                    else -> {
                        erro = ""

                        scope.launch {
                            salvando = true

                            try {
                                usuarioRepository.cadastrarProfessor(
                                    nome = nome.trim(),
                                    email = email.trim(),
                                    senha = senha
                                )

                                onCadastroConcluido()
                            } catch (e: Exception) {
                                erro = e.message
                                    ?: "Não foi possível criar a conta."
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
            Text(if (salvando) "CRIANDO CONTA..." else "CRIAR CONTA")
        }

        TextButton(
            onClick = onVoltar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Voltar")
        }
    }
}