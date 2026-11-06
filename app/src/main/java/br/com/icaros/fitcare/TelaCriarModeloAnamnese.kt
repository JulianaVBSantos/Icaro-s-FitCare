
package br.com.icaros.fitcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.ModeloAnamneseEntity
import br.com.icaros.fitcare.data.ModeloAnamneseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCriarModeloAnamnese(
    modeloId: Long? = null,
    repository: ModeloAnamneseRepository,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("PERSONALIZADO") }
    var ativo by remember { mutableStateOf(true) }

    var carregando by remember { mutableStateOf(modeloId != null) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val editando = modeloId != null

    LaunchedEffect(modeloId) {
        if (modeloId != null) {
            try {
                val modelo = repository.buscarPorId(modeloId)

                if (modelo != null) {
                    nome = modelo.nome
                    descricao = modelo.descricao
                    categoria = modelo.categoria
                    ativo = modelo.ativo
                } else {
                    erro = "Modelo não encontrado."
                }
            } catch (e: Exception) {
                erro = e.message ?: "Não foi possível carregar o modelo."
            } finally {
                carregando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
    ) {

        // CABEÇALHO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF1565C0)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = if (editando) "Editar modelo" else "Novo modelo",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )

                Text(
                    text = "Configuração da anamnese",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }

        if (carregando) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1565C0)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // NOME
                Column {
                    Text(
                        text = "Nome do modelo *",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    OutlinedTextField(
                        value = nome,
                        onValueChange = {
                            nome = it
                            erro = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Ex.: Anamnese inicial")
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // DESCRIÇÃO
                Column {
                    Text(
                        text = "Descrição",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Descreva a finalidade deste modelo")
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // CATEGORIA
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Categoria",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = categoria == "PADRAO",
                            onClick = { categoria = "PADRAO" }
                        )

                        Text("Padrão")

                        Spacer(modifier = Modifier.width(20.dp))

                        RadioButton(
                            selected = categoria == "PERSONALIZADO",
                            onClick = { categoria = "PERSONALIZADO" }
                        )

                        Text("Personalizado")
                    }

                    Text(
                        text = "Padrão: modelo disponibilizado pelo aplicativo. " +
                                "Personalizado: modelo criado pelo profissional.",
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }

                // STATUS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Modelo ativo",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )

                            Text(
                                text = if (ativo)
                                    "Disponível para utilização"
                                else
                                    "Indisponível para novas utilizações",
                                fontSize = 12.sp,
                                color = Color(0xFF757575)
                            )
                        }

                        Switch(
                            checked = ativo,
                            onCheckedChange = { ativo = it }
                        )
                    }
                }

                if (erro != null) {
                    Text(
                        text = erro!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "* Campo obrigatório",
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            // BOTÃO SALVAR
            Button(
                onClick = {
                    if (nome.isBlank()) {
                        erro = "Informe o nome do modelo."
                        return@Button
                    }

                    scope.launch {
                        salvando = true
                        erro = null

                        try {
                            val modelo = ModeloAnamneseEntity(
                                id = modeloId ?: 0L,
                                nome = nome.trim(),
                                descricao = descricao.trim(),
                                categoria = categoria,
                                ativo = ativo
                            )

                            repository.salvar(modelo)
                            onSalvo()

                        } catch (e: Exception) {
                            erro = e.message
                                ?: "Não foi possível salvar o modelo."
                        } finally {
                            salvando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .height(52.dp),
                enabled = !salvando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0)
                )
            ) {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (editando)
                            "Salvar alterações"
                        else
                            "Criar modelo",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}