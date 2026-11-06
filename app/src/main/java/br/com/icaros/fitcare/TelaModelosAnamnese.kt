package br.com.icaros.fitcare

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.ModeloAnamneseEntity
import br.com.icaros.fitcare.data.ModeloAnamneseRepository
import kotlinx.coroutines.launch

@Composable
fun TelaModelosAnamnese(
    repository: ModeloAnamneseRepository,
    onVoltar: () -> Unit,
    onNovoModelo: () -> Unit,
    onEditarModelo: (ModeloAnamneseEntity) -> Unit,
    onPerguntas: (ModeloAnamneseEntity) -> Unit
) {
    val modelos by repository.listarTodos()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var modeloParaExcluir by remember {
        mutableStateOf<ModeloAnamneseEntity?>(null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF1565C0)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(id = R.drawable.logo_fitcare),
                contentDescription = "Ícaros FitCare",
                modifier = Modifier.height(48.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Modelos de Anamnese",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Gerencie os modelos utilizados nas avaliações dos alunos.",
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onNovoModelo,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1565C0)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Criar novo modelo")
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Modelos cadastrados (${modelos.size})",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (modelos.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(38.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Nenhum modelo cadastrado",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = "Crie um modelo para começar a montar suas anamneses.",
                        color = Color(0xFF757575),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                modelos.forEach { modelo ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = modelo.nome,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = if (modelo.categoria == "PADRAO") {
                                    "Modelo padrão"
                                } else {
                                    "Modelo personalizado"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF1565C0),
                                fontWeight = FontWeight.SemiBold
                            )

                            if (modelo.descricao.isNotBlank()) {
                                Spacer(modifier = Modifier.height(7.dp))

                                Text(
                                    text = modelo.descricao,
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (modelo.ativo) {
                                    "Ativo"
                                } else {
                                    "Inativo"
                                },
                                fontSize = 12.sp,
                                color = if (modelo.ativo) {
                                    Color(0xFF1565C0)
                                } else {
                                    Color(0xFF757575)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(
                                color = Color(0xFFF5F7FA)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        onPerguntas(modelo)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = null
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text("Perguntas")
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            onEditarModelo(modelo)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Editar modelo",
                                            tint = Color(0xFF1565C0)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            modeloParaExcluir = modelo
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Excluir modelo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    modeloParaExcluir?.let { modelo ->

        AlertDialog(
            onDismissRequest = {
                modeloParaExcluir = null
            },
            title = {
                Text("Excluir modelo?")
            },
            text = {
                Text(
                    "Deseja realmente excluir o modelo \"${modelo.nome}\"? " +
                            "As perguntas vinculadas também serão excluídas. " +
                            "Modelos já atribuídos possuem uma cópia independente das perguntas."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                repository.excluir(modelo)
                            } finally {
                                modeloParaExcluir = null
                            }
                        }
                    }
                ) {
                    Text(
                        "Excluir",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        modeloParaExcluir = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}