
package br.com.icaros.fitcare

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

private val formatosDocumentoMedico = setOf("pdf", "doc", "docx")
private val formatosImagemComparacao = setOf("jpg", "jpeg", "png")

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

    return nome ?: "arquivo_anexado"
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

    var mensagem by remember { mutableStateOf("") }
    var processando by remember { mutableStateOf(false) }
    var mostrarOpcoes by remember { mutableStateOf(false) }
    var tipoImportacao by remember { mutableStateOf("") }

    val seletorArquivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->

        if (uri != null) {
            scope.launch {
                processando = true
                mensagem = ""

                var arquivoSalvo: File? = null

                try {
                    if (alunoId <= 0L) {
                        throw IllegalStateException(
                            "Não foi possível identificar o cadastro do aluno. Entre em contato com o profissional."
                        )
                    }

                    val nomeOriginal = obterNomeArquivo(
                        context,
                        uri
                    )

                    val extensao = obterExtensao(nomeOriginal)

                    val formatosPermitidos = when (tipoImportacao) {
                        "MEDICO" -> formatosDocumentoMedico
                        "COMPARACAO" -> formatosImagemComparacao
                        else -> emptySet()
                    }

                    if (extensao !in formatosPermitidos) {
                        val formatosEsperados = if (
                            tipoImportacao == "COMPARACAO"
                        ) {
                            "JPG, JPEG ou PNG"
                        } else {
                            "DOC, DOCX ou PDF"
                        }

                        throw IllegalArgumentException(
                            "Formato de arquivo não suportado. Selecione um arquivo $formatosEsperados."
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

                    val ehComparacao = tipoImportacao == "COMPARACAO"

                    val documento = DocumentoAnamneseEntity(
                        alunoId = alunoId,
                        nomeArquivo = if (ehComparacao) {
                            "comparacao_$nomeOriginal"
                        } else {
                            nomeOriginal
                        },
                        caminhoArquivo = destino.absolutePath,
                        tipoArquivo = if (ehComparacao) {
                            "comparacao_$extensao"
                        } else {
                            extensao
                        },
                        tamanhoArquivo = destino.length(),
                        dataImportacao = SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            Locale.getDefault()
                        ).format(Date())
                    )

                    anamneseRepository.importarDocumento(documento)

                    onDocumentoImportado()

                    mensagem = if (ehComparacao) {
                        "Imagem para comparação importada com sucesso."
                    } else {
                        "Documento médico importado com sucesso."
                    }

                } catch (erro: Exception) {
                    arquivoSalvo?.delete()

                    mensagem = erro.message
                        ?: "Não foi possível importar o arquivo."
                } finally {
                    processando = false
                    tipoImportacao = ""
                }
            }
        } else {
            tipoImportacao = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = {
                mensagem = ""
                mostrarOpcoes = true
            },
            enabled = !processando && alunoId > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (processando) {
                    "IMPORTANDO..."
                } else {
                    "IMPORTAR DOCUMENTO OU IMAGEM"
                }
            )
        }

        if (mensagem.isNotBlank()) {
            Text(
                text = mensagem,
                color = if (
                    mensagem.contains("sucesso", ignoreCase = true)
                ) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        if (mostrarOpcoes) {
            AlertDialog(
                onDismissRequest = {
                    mostrarOpcoes = false
                },
                title = {
                    Text("O que deseja importar?")
                },
                text = {
                    Text(
                        "Escolha o tipo de arquivo. Cada opção aceita formatos diferentes."
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                mostrarOpcoes = false
                                tipoImportacao = "MEDICO"

                                // A validação da extensão acontece após a seleção.
                                seletorArquivo.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Importar documento médico")
                        }

                        OutlinedButton(
                            onClick = {
                                mostrarOpcoes = false
                                tipoImportacao = "COMPARACAO"

                                // A validação da extensão acontece após a seleção.
                                seletorArquivo.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Importar imagem para fins de comparação")
                        }
                    }
                }
            )
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

    val tipoArquivo = documento.tipoArquivo
        .lowercase(Locale.ROOT)
        .removePrefix("comparacao_")

    val tipoMime = when (tipoArquivo) {
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "*/*"
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, tipoMime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(intent, "Abrir arquivo")
    )
}