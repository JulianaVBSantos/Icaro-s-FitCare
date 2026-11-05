
package br.com.icaros.fitcare

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import br.com.icaros.fitcare.data.AlunoRepository
import br.com.icaros.fitcare.data.AnamneseRepository
import br.com.icaros.fitcare.data.DocumentoAnamneseEntity
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatosPermitidos =
    setOf("pdf", "jpg", "jpeg", "png")

private fun obterNomeArquivo(
    context: Context,
    uri: Uri
): String {

    var nome: String? = null

    context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            val indice = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )

            if (indice >= 0) {
                nome = cursor.getString(indice)
            }
        }
    }

    return nome ?: "documento_anamnese"
}

private fun obterExtensao(nome: String): String {
    return nome.substringAfterLast(".", "")
        .lowercase(Locale.ROOT)
}

@Composable
fun BotaoImportarDocumentoAnamnese(
    alunoId: Long,
    anamneseRepository: AnamneseRepository,
    alunoRepository: AlunoRepository,
    onDocumentoImportado: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mensagem by remember {
        mutableStateOf("")
    }

    var processando by remember {
        mutableStateOf(false)
    }

    var mostrarConferencia by remember {
        mutableStateOf(false)
    }

    var condicoes by remember {
        mutableStateOf("")
    }

    var restricoes by remember {
        mutableStateOf("")
    }

    var observacoes by remember {
        mutableStateOf("")
    }

    var textoReconhecido by remember {
        mutableStateOf("")
    }

    var atualizando by remember {
        mutableStateOf(false)
    }

    val seletorDocumento =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            if (uri != null) {

                scope.launch {

                    processando = true
                    mostrarConferencia = false
                    mensagem = ""

                    var arquivoSalvo: File? = null

                    try {

                        val nomeOriginal =
                            obterNomeArquivo(context, uri)

                        val extensao =
                            obterExtensao(nomeOriginal)

                        if (extensao !in formatosPermitidos) {
                            throw IllegalArgumentException(
                                "Formato não permitido. Selecione PDF, JPG, JPEG ou PNG."
                            )
                        }

                        val pasta = File(
                            context.filesDir,
                            "anamneses/aluno_$alunoId"
                        )

                        if (!pasta.exists() && !pasta.mkdirs()) {
                            throw IllegalStateException(
                                "Não foi possível criar a pasta do aluno."
                            )
                        }

                        val nomeSeguro = nomeOriginal.replace(
                            Regex("[^a-zA-Z0-9._-]"),
                            "_"
                        )

                        val destino = File(
                            pasta,
                            "${System.currentTimeMillis()}_$nomeSeguro"
                        )

                        arquivoSalvo = destino

                        val entrada = context.contentResolver
                            .openInputStream(uri)
                            ?: throw IllegalArgumentException(
                                "Não foi possível acessar o arquivo selecionado."
                            )

                        entrada.use { fluxoEntrada ->

                            destino.outputStream().use { fluxoSaida ->
                                fluxoEntrada.copyTo(fluxoSaida)
                            }
                        }

                        // Registra o documento no banco de dados.
                        val documento = DocumentoAnamneseEntity(
                            alunoId = alunoId,
                            nomeArquivo = nomeOriginal,
                            caminhoArquivo = destino.absolutePath,
                            tipoArquivo = extensao,
                            tamanhoArquivo = destino.length(),
                            dataImportacao = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date())
                        )

                        anamneseRepository.importarDocumento(documento)

                        // Atualiza a listagem assim que o documento é salvo.
                        onDocumentoImportado()

                        mensagem =
                            "Documento armazenado. Iniciando leitura..."

                        // Realiza a extração do texto ou OCR.
                        try {

                            val texto = lerDocumentoAnamnese(
                                context = context,
                                arquivo = destino,
                                extensao = extensao
                            )

                            if (texto.isBlank()) {
                                throw IllegalArgumentException(
                                    "Nenhum texto foi reconhecido. Verifique a qualidade do documento."
                                )
                            }

                            val dados =
                                interpretarTextoAnamnese(texto)

                            condicoes = dados.condicoes
                            restricoes = dados.restricoes
                            observacoes = dados.observacoes
                            textoReconhecido = dados.textoCompleto

                            mostrarConferencia = true

                            mensagem =
                                "Leitura concluída. Confira as informações antes de atualizar o aluno."

                        } catch (erroLeitura: Exception) {

                            textoReconhecido = ""
                            condicoes = ""
                            restricoes = ""
                            observacoes = ""

                            mostrarConferencia = true

                            mensagem =
                                "Documento armazenado, mas não foi possível reconhecer o texto: " +
                                        (erroLeitura.message ?: "erro na leitura.")
                        }

                    } catch (erro: Exception) {

                        arquivoSalvo?.delete()

                        mensagem = erro.message
                            ?: "Não foi possível importar o documento."

                    } finally {

                        processando = false
                    }
                }
            }
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Button(
            onClick = {
                seletorDocumento.launch(
                    arrayOf(
                        "application/pdf",
                        "image/jpeg",
                        "image/png"
                    )
                )
            },
            enabled = !processando &&
                    !atualizando &&
                    alunoId > 0,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                if (processando) {
                    "IMPORTANDO E LENDO..."
                } else {
                    "Importar documento"
                }
            )
        }

        if (mensagem.isNotBlank()) {

            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mostrarConferencia) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Conferência da anamnese",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Confira e corrija as informações reconhecidas. Somente os campos preenchidos serão utilizados na atualização do acompanhamento.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = condicoes,
                        onValueChange = { condicoes = it },
                        label = {
                            Text("Condições de saúde")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = restricoes,
                        onValueChange = { restricoes = it },
                        label = {
                            Text("Restrições e cuidados")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = observacoes,
                        onValueChange = { observacoes = it },
                        label = {
                            Text("Medicamentos, cirurgias e observações")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (textoReconhecido.isNotBlank()) {

                        Text(
                            text = "Texto completo reconhecido",
                            style = MaterialTheme.typography.titleSmall
                        )

                        OutlinedTextField(
                            value = textoReconhecido,
                            onValueChange = {
                                textoReconhecido = it
                            },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8
                        )
                    }

                    Button(
                        onClick = {

                            scope.launch {

                                atualizando = true

                                try {

                                    alunoRepository
                                        .atualizarAcompanhamentoPorAnamnese(
                                            alunoId = alunoId,
                                            condicoes = condicoes,
                                            restricoes = restricoes,
                                            observacoes = observacoes
                                        )

                                    mensagem =
                                        "Acompanhamento do aluno atualizado com sucesso."

                                    mostrarConferencia = false

                                } catch (erro: Exception) {

                                    mensagem = erro.message
                                        ?: "Não foi possível atualizar o acompanhamento."

                                } finally {

                                    atualizando = false
                                }
                            }
                        },
                        enabled = !atualizando &&
                                (
                                        condicoes.isNotBlank() ||
                                                restricoes.isNotBlank() ||
                                                observacoes.isNotBlank()
                                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            if (atualizando) {
                                "ATUALIZANDO..."
                            } else {
                                "Confirmar e atualizar acompanhamento"
                            }
                        )
                    }

                    TextButton(
                        onClick = {
                            mostrarConferencia = false
                            mensagem =
                                "Documento armazenado. O acompanhamento não foi alterado."
                        },
                        enabled = !atualizando,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text("Cancelar atualização")
                    }
                }
            }
        }
    }
}

fun abrirDocumentoAnamnese(
    context: Context,
    documento: DocumentoAnamneseEntity
) {

    val arquivo = File(documento.caminhoArquivo)

    require(arquivo.exists()) {
        "O arquivo não foi encontrado no armazenamento."
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        arquivo
    )

    val tipoMime = when (
        documento.tipoArquivo.lowercase(Locale.ROOT)
    ) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "*/*"
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, tipoMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Abrir documento")
    )
}
