
package br.com.icaros.fitcare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import br.com.icaros.fitcare.data.UsuarioRepository
import kotlinx.coroutines.launch

@Composable
fun TelaCadastroAdminInicial(
    usuarioRepository: UsuarioRepository,
    onVoltar: () -> Unit,
    onCadastroConcluido: () -> Unit
) {

    var nome by remember {
        mutableStateOf("")
    }

    var senha by remember {
        mutableStateOf("")
    }

    var confirmarSenha by remember {
        mutableStateOf("")
    }

    var emailProfessor by remember {
        mutableStateOf("")
    }

    var senhaProfessor by remember {
        mutableStateOf("")
    }

    var mensagemErro by remember {
        mutableStateOf<String?>(null)
    }

    var carregando by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val azul = Color(0xFF1769AA)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Criar administrador",
            fontWeight = FontWeight.Bold,
            color = azul
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Cadastre o administrador inicial do Ícaros FitCare."
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        OutlinedTextField(
            value = nome,
            onValueChange = {
                nome = it
            },
            label = {
                Text("Nome do administrador")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = "julianavieirabsantos@gmail.com",
            onValueChange = {},
            readOnly = true,
            label = {
                Text("E-mail administrativo")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = senha,
            onValueChange = {
                senha = it
            },
            label = {
                Text("Senha do administrador")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = {
                confirmarSenha = it
            },
            label = {
                Text("Confirmar senha do administrador")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "Confirmação do professor inicial",
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = emailProfessor,
            onValueChange = {
                emailProfessor = it
            },
            label = {
                Text("E-mail do professor inicial")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !carregando
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = senhaProfessor,
            onValueChange = {
                senhaProfessor = it
            },
            label = {
                Text("Senha do professor inicial")
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !carregando
        )

        if (mensagemErro != null) {

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = mensagemErro!!,
                color = Color.Red
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                if (
                    nome.isBlank() ||
                    senha.isBlank() ||
                    confirmarSenha.isBlank() ||
                    emailProfessor.isBlank() ||
                    senhaProfessor.isBlank()
                ) {
                    mensagemErro = "Preencha todos os campos."
                    return@Button
                }

                if (senha != confirmarSenha) {
                    mensagemErro =
                        "As senhas do administrador não coincidem."
                    return@Button
                }

                if (senha.length < 8) {
                    mensagemErro =
                        "A senha do administrador deve possuir pelo menos 8 caracteres."
                    return@Button
                }

                carregando = true
                mensagemErro = null

                scope.launch {

                    try {

                        usuarioRepository.criarAdminInicial(
                            nome = nome.trim(),
                            email = "julianavieirabsantos@gmail.com",
                            senha = senha,
                            emailProfessorInicial = emailProfessor.trim(),
                            senhaProfessorInicial = senhaProfessor
                        )

                        onCadastroConcluido()

                    } catch (e: Exception) {

                        mensagemErro = e.message
                            ?: "Não foi possível criar o administrador."

                    } finally {
                        carregando = false
                    }
                }
            },
            enabled = !carregando,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = azul
            )
        ) {

            Text(
                text = if (carregando) {
                    "Criando..."
                } else {
                    "Criar administrador"
                }
            )
        }

        TextButton(
            onClick = onVoltar,
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando
        ) {
            Text("Voltar")
        }
    }
}
