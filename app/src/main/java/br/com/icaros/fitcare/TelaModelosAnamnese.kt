
package br.com.icaros.fitcare

import android.content.Intent
import android.content.ClipData
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import br.com.icaros.fitcare.data.AlunoRepository
import br.com.icaros.fitcare.data.ModeloAnamneseEntity
import br.com.icaros.fitcare.data.ModeloAnamneseRepository
import br.com.icaros.fitcare.data.AnamneseAtribuidaRepository

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

@Composable
fun TelaModelosAnamnese(
    repository: ModeloAnamneseRepository,
    alunoRepository: AlunoRepository,
    anamneseAtribuidaRepository: AnamneseAtribuidaRepository,
    alunoId: Long?,
    onVoltar: () -> Unit,
    onNovoModelo: () -> Unit,
    onEditarModelo: (ModeloAnamneseEntity) -> Unit,
    onPerguntas: (ModeloAnamneseEntity) -> Unit,
    onAbrirAnamnese: (Long) -> Unit
) {

    val modelos by repository.listarTodos()
        .collectAsState(initial = emptyList())

    var modeloEmEnvioId by remember {
        mutableStateOf<Long?>(null)
    }

    // Controla a criação da avaliação digital.
    var modeloEmPreenchimentoId by remember {
        mutableStateOf<Long?>(null)
    }

    var mensagemEnvio by remember {
        mutableStateOf<String?>(null)
    }

    var envioSucesso by remember {
        mutableStateOf(false)
    }

    var modeloParaExcluir by remember {
        mutableStateOf<ModeloAnamneseEntity?>(null)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {

        Spacer(modifier = Modifier.height(12.dp))

        // Cabeçalho
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color(0xFF002B5C)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(
                    id = R.drawable.logo_fitcare
                ),
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

        // Mensagem de resultado
        mensagemEnvio?.let { mensagem ->

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (envioSucesso) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFEBEE)
                    }
                )
            ) {

                Text(
                    text = mensagem,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (envioSucesso) {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Criar modelo
        Button(
            onClick = onNovoModelo,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF002B5C),
                contentColor = Color.White
            )
        ) {

            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Criar novo modelo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
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
                                color = Color(0xFF002B5C),
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
                                text = if (modelo.ativo) "Ativo" else "Inativo",
                                fontSize = 12.sp,
                                color = if (modelo.ativo) {
                                    Color(0xFF002B5C)
                                } else {
                                    Color(0xFF757575)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            HorizontalDivider(
                                color = Color(0xFFF5F7FA)
                            )

                            // Ações do modelo
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

                            // Ações de avaliação para um aluno
                            if (alunoId != null) {

                                Spacer(modifier = Modifier.height(8.dp))

                                // BOTÃO 1 - PREENCHER ANAMNESE DIGITAL
                                OutlinedButton(
                                    onClick = {

                                        val idAluno = alunoId

                                        if (
                                            modeloEmPreenchimentoId == null &&
                                            modeloEmEnvioId == null &&
                                            modelo.ativo
                                        ) {

                                            modeloEmPreenchimentoId = modelo.id
                                            mensagemEnvio = null
                                            envioSucesso = false

                                            scope.launch {

                                                try {

                                                    // Cria uma nova avaliação independente.
                                                    val novaAnamneseId =
                                                        anamneseAtribuidaRepository.atribuir(
                                                            alunoId = idAluno,
                                                            modeloId = modelo.id
                                                        )

                                                    // Libera a avaliação para preenchimento.
                                                    anamneseAtribuidaRepository.enviar(
                                                        novaAnamneseId
                                                    )

                                                    // Abre a tela de preenchimento.
                                                    onAbrirAnamnese(
                                                        novaAnamneseId
                                                    )

                                                } catch (e: Exception) {

                                                    mensagemEnvio =
                                                        e.message
                                                            ?: "Não foi possível criar a avaliação digital."

                                                    envioSucesso = false

                                                } finally {

                                                    modeloEmPreenchimentoId = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = modeloEmPreenchimentoId == null &&
                                            modeloEmEnvioId == null &&
                                            modelo.ativo,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF002B5C)
                                    )
                                ) {

                                    if (modeloEmPreenchimentoId == modelo.id) {

                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF002B5C)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text("Abrindo formulário...")

                                    } else {

                                        Text(
                                            text = if (modelo.ativo) {
                                                "Preencher anamnese digital"
                                            } else {
                                                "Modelo indisponível"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // BOTÃO 2 - EXPORTAR / COMPARTILHAR PDF
                                Button(
                                    onClick = {

                                        val idAluno = alunoId

                                        if (
                                            modeloEmEnvioId == null &&
                                            modeloEmPreenchimentoId == null &&
                                            modelo.ativo
                                        ) {

                                            modeloEmEnvioId = modelo.id
                                            mensagemEnvio = null
                                            envioSucesso = false

                                            scope.launch {

                                                var compartilhamentoIniciado = false

                                                try {

                                                    // 1. Buscar aluno
                                                    val aluno = alunoRepository
                                                        .buscarPorId(idAluno)
                                                        .first()
                                                        ?: throw IllegalArgumentException(
                                                            "Aluno não encontrado."
                                                        )

                                                    // 2. Criar avaliação independente
                                                    val novaAnamneseId =
                                                        anamneseAtribuidaRepository.atribuir(
                                                            alunoId = idAluno,
                                                            modeloId = modelo.id
                                                        )

                                                    // 3. Buscar avaliação
                                                    val anamnese =
                                                        anamneseAtribuidaRepository
                                                            .buscarPorId(novaAnamneseId)
                                                            ?: throw IllegalStateException(
                                                                "Não foi possível recuperar a avaliação criada."
                                                            )

                                                    // 4. Buscar perguntas
                                                    val perguntas =
                                                        anamneseAtribuidaRepository
                                                            .buscarPerguntas(novaAnamneseId)

                                                    // 5. Gerar PDF
                                                    val arquivo =
                                                        AnamnesePdfGenerator.gerar(
                                                            context = context,
                                                            aluno = aluno,
                                                            anamnese = anamnese,
                                                            perguntas = perguntas
                                                        )

                                                    // 6. Criar URI seguro
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        arquivo
                                                    )

                                                    // 7. Preparar compartilhamento
                                                    val intent = Intent(
                                                        Intent.ACTION_SEND
                                                    ).apply {

                                                        type = "application/pdf"

                                                        putExtra(
                                                            Intent.EXTRA_STREAM,
                                                            uri
                                                        )

                                                        putExtra(
                                                            Intent.EXTRA_SUBJECT,
                                                            "Anamnese - ${aluno.nome}"
                                                        )

                                                        putExtra(
                                                            Intent.EXTRA_TEXT,
                                                            "Olá! Segue o documento de anamnese."
                                                        )

                                                        addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )

                                                        clipData = ClipData.newUri(
                                                            context.contentResolver,
                                                            "Anamnese",
                                                            uri
                                                        )
                                                    }

                                                    // 8. Abrir seletor
                                                    context.startActivity(
                                                        Intent.createChooser(
                                                            intent,
                                                            "Compartilhar anamnese em PDF"
                                                        )
                                                    )

                                                    compartilhamentoIniciado = true

                                                    // 9. Atualizar status
                                                    anamneseAtribuidaRepository
                                                        .enviar(novaAnamneseId)

                                                    mensagemEnvio =
                                                        "PDF gerado e seletor de compartilhamento aberto! " +
                                                                "Avaliação nº $novaAnamneseId. " +
                                                                "O status PENDENTE indica que o compartilhamento foi iniciado, não que o documento foi recebido."

                                                    envioSucesso = true

                                                } catch (e: Exception) {

                                                    mensagemEnvio = if (
                                                        compartilhamentoIniciado
                                                    ) {
                                                        "O seletor de compartilhamento foi aberto, mas não foi possível atualizar o status da avaliação: " +
                                                                (e.message ?: "Erro desconhecido.")
                                                    } else {
                                                        e.message
                                                            ?: "Não foi possível gerar ou compartilhar a anamnese."
                                                    }

                                                    envioSucesso = false

                                                } finally {

                                                    modeloEmEnvioId = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = modeloEmEnvioId == null &&
                                            modeloEmPreenchimentoId == null &&
                                            modelo.ativo,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF002B5C),
                                        contentColor = Color.White
                                    )
                                ) {

                                    if (modeloEmEnvioId == modelo.id) {

                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text("Preparando PDF...")

                                    } else {

                                        Text(
                                            text = if (modelo.ativo) {
                                                "Exportar / compartilhar PDF"
                                            } else {
                                                "Modelo indisponível"
                                            },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
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

    // CONFIRMAÇÃO DE EXCLUSÃO
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

                                mensagemEnvio = null

                            } catch (e: Exception) {

                                mensagemEnvio =
                                    e.message
                                        ?: "Não foi possível excluir o modelo."

                                envioSucesso = false

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