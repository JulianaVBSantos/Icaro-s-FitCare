
package br.com.icaros.fitcare

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import br.com.icaros.fitcare.data.AnamneseRepository
import androidx.compose.runtime.collectAsState
import br.com.icaros.fitcare.data.AlunoRepository

@Suppress("UNUSED_PARAMETER")
@Composable
fun TelaAnamneseAluno(
    alunoId: Long,
    anamneseRepository: AnamneseRepository,
    alunoRepository: AlunoRepository,
    onVoltar: () -> Unit,
    onCriarAnamnese: () -> Unit,
    onAbrirAnamnese: (Long) -> Unit,
    onImportarDocumento: () -> Unit,
    onAbrirDocumento: (Long) -> Unit
) {

    val context = LocalContext.current

    val anamneses by anamneseRepository
        .listarAnamneses(alunoId)
        .collectAsState(initial = emptyList())

    val documentos by anamneseRepository
        .listarDocumentos(alunoId)
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        // CABEÇALHO PADRÃO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .statusBarsPadding()
                .padding(top = 4.dp, bottom = 12.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
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

                Box(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    BotaoVoltarPadrao(onVoltar = onVoltar)
                }
            }
        }

        // CONTEÚDO ROLÁVEL
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                text = "ANAMNESE DO ALUNO",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cadastre avaliações e consulte os documentos associados a este aluno.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CRIAR ANAMNESE
            Button(
                onClick = onCriarAnamnese,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "+ NOVA ANAMNESE",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // IMPORTAR DOCUMENTO
            BotaoImportarDocumentoAnamnese(
                alunoId = alunoId,
                anamneseRepository = anamneseRepository,
                alunoRepository = alunoRepository,
                onDocumentoImportado = {
                    // A lista será atualizada pelo Flow do Room.
                }
            )

            Spacer(modifier = Modifier.height(26.dp))

            // HISTÓRICO DE ANAMNESES
            Text(
                text = "HISTÓRICO DE ANAMNESES",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (anamneses.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Nenhuma anamnese cadastrada para este aluno.",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {

                anamneses.forEach { anamnese ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Anamnese - Versão ${anamnese.versao}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Avaliação: ${
                                    anamnese.dataAvaliacao.ifBlank {
                                        "Não informada"
                                    }
                                }",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    onAbrirAnamnese(anamnese.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("VISUALIZAR ANAMNESE")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // DOCUMENTOS IMPORTADOS E GERADOS
            Text(
                text = "DOCUMENTOS",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (documentos.isEmpty()) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Nenhum documento associado a este aluno.",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {

                documentos.forEach { documento ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = documento.nomeArquivo,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = "Formato: ${documento.tipoArquivo.uppercase()}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Importação: ${documento.dataImportacao}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    try {
                                        abrirDocumentoAnamnese(
                                            context = context,
                                            documento = documento
                                        )
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context,
                                            e.message ?: "Não foi possível abrir o documento.",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ABRIR DOCUMENTO")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
