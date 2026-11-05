
package br.com.icaros.fitcare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.AnamneseEntity
import br.com.icaros.fitcare.data.AnamneseRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.compose.ui.platform.LocalContext
import br.com.icaros.fitcare.data.DocumentoAnamneseEntity

private val condicoesDisponiveis = listOf(
    "Hipertensão",
    "Diabetes",
    "Problemas cardiovasculares",
    "Problemas articulares",
    "Lesões anteriores",
    "Problemas respiratórios",
    "Obesidade",
    "Limitação de mobilidade",
    "Problemas na coluna",
    "Nenhuma condição informada",
    "Outras"
)

private val restricoesDisponiveis = listOf(
    "Restrição a exercícios de impacto",
    "Restrição de carga",
    "Restrição de movimentos específicos",
    "Restrição cardiovascular",
    "Restrição por lesão",
    "Restrição de mobilidade",
    "Sem restrições informadas",
    "Outras"
)

private val riscosDisponiveis = listOf(
    "Risco cardiovascular",
    "Risco de lesão",
    "Limitação funcional",
    "Necessidade de acompanhamento específico",
    "Risco de queda",
    "Nenhum risco identificado",
    "Outros"
)

@Composable
fun TelaCriarAnamnese(
    alunoId: Long,
    anamneseRepository: AnamneseRepository,
    anamneseParaEditar: AnamneseEntity? = null,
    onVoltar: () -> Unit,
    onSalvo: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var dataAvaliacao by remember {
        mutableStateOf(
            anamneseParaEditar?.dataAvaliacao
                ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        )
    }

    var historicoSaude by remember {
        mutableStateOf(anamneseParaEditar?.historicoSaude ?: "")
    }

    var medicamentos by remember {
        mutableStateOf(anamneseParaEditar?.medicamentos ?: "")
    }

    var cirurgias by remember {
        mutableStateOf(anamneseParaEditar?.cirurgias ?: "")
    }

    var acompanhamentoMedico by remember {
        mutableStateOf(anamneseParaEditar?.acompanhamentoMedico ?: "")
    }

    var condicoes by remember {
        mutableStateOf(
            anamneseParaEditar?.condicoesSelecionadas
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet()
        )
    }

    var restricoes by remember {
        mutableStateOf(
            anamneseParaEditar?.restricoesSelecionadas
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet()
        )
    }

    var riscos by remember {
        mutableStateOf(
            anamneseParaEditar?.riscosSelecionados
                ?.split("|")
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: emptySet()
        )
    }

    var observacoes by remember {
        mutableStateOf(anamneseParaEditar?.observacoes ?: "")
    }

    var recomendacoes by remember {
        mutableStateOf(anamneseParaEditar?.recomendacoes ?: "")
    }

    var cuidados by remember {
        mutableStateOf(anamneseParaEditar?.cuidados ?: "")
    }

    var objetivos by remember {
        mutableStateOf(anamneseParaEditar?.objetivos ?: "")
    }

    var profissional by remember {
        mutableStateOf(anamneseParaEditar?.profissionalResponsavel ?: "")
    }

    var salvando by remember { mutableStateOf(false) }
    var mensagemErro by remember { mutableStateOf<String?>(null) }

    val grupoId = remember {
        anamneseParaEditar?.grupoId ?: UUID.randomUUID().toString()
    }

    val corFundo = MaterialTheme.colorScheme.surface
    val corTexto = MaterialTheme.colorScheme.onSurface
    val corSecundaria = MaterialTheme.colorScheme.onSurfaceVariant
    val corPrimaria = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(corFundo)
    ) {

        // CABEÇALHO
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.primaryContainer
                )
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onVoltar) {
                    Text("< VOLTAR")
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "ANAMNESE",
                    fontWeight = FontWeight.Bold,
                    color = corTexto
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(18.dp)
        ) {

            Text(
                text = if (anamneseParaEditar == null)
                    "NOVA ANAMNESE"
                else
                    "NOVA VERSÃO DA ANAMNESE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = corTexto
            )

            Spacer(modifier = Modifier.height(20.dp))

            // IDENTIFICAÇÃO
            tituloSecaoAnamnese("IDENTIFICAÇÃO")

            campoAnamnese(
                valor = dataAvaliacao,
                onValorChange = { dataAvaliacao = it },
                rotulo = "Data da avaliação"
            )

            campoAnamnese(
                valor = profissional,
                onValorChange = { profissional = it },
                rotulo = "Profissional responsável"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // HISTÓRICO DE SAÚDE
            tituloSecaoAnamnese("HISTÓRICO DE SAÚDE")

            campoAnamnese(
                valor = historicoSaude,
                onValorChange = { historicoSaude = it },
                rotulo = "Histórico de saúde",
                linhas = 4
            )

            campoAnamnese(
                valor = medicamentos,
                onValorChange = { medicamentos = it },
                rotulo = "Medicamentos utilizados",
                linhas = 3
            )

            campoAnamnese(
                valor = cirurgias,
                onValorChange = { cirurgias = it },
                rotulo = "Cirurgias e procedimentos anteriores",
                linhas = 3
            )

            campoAnamnese(
                valor = acompanhamentoMedico,
                onValorChange = { acompanhamentoMedico = it },
                rotulo = "Acompanhamento médico",
                linhas = 3
            )

            Spacer(modifier = Modifier.height(20.dp))

            // CONDIÇÕES
            tituloSecaoAnamnese("CONDIÇÕES DE SAÚDE")

            Text(
                text = "Selecione as condições relatadas pelo aluno:",
                color = corSecundaria,
                fontSize = 14.sp
            )

            opcoesAnamnese(
                opcoes = condicoesDisponiveis,
                selecionadas = condicoes,
                aoAlterar = { condicoes = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // RESTRIÇÕES
            tituloSecaoAnamnese("RESTRIÇÕES")

            Text(
                text = "Selecione as restrições aplicáveis:",
                color = corSecundaria,
                fontSize = 14.sp
            )

            opcoesAnamnese(
                opcoes = restricoesDisponiveis,
                selecionadas = restricoes,
                aoAlterar = { restricoes = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // RISCOS
            tituloSecaoAnamnese("RISCOS E CUIDADOS")

            Text(
                text = "Selecione os riscos que precisam de atenção:",
                color = corSecundaria,
                fontSize = 14.sp
            )

            opcoesAnamnese(
                opcoes = riscosDisponiveis,
                selecionadas = riscos,
                aoAlterar = { riscos = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // AVALIAÇÃO PROFISSIONAL
            tituloSecaoAnamnese("AVALIAÇÃO PROFISSIONAL")

            campoAnamnese(
                valor = observacoes,
                onValorChange = { observacoes = it },
                rotulo = "Observações profissionais",
                linhas = 4
            )

            campoAnamnese(
                valor = recomendacoes,
                onValorChange = { recomendacoes = it },
                rotulo = "Recomendações",
                linhas = 4
            )

            campoAnamnese(
                valor = cuidados,
                onValorChange = { cuidados = it },
                rotulo = "Cuidados durante os exercícios",
                linhas = 4
            )

            campoAnamnese(
                valor = objetivos,
                onValorChange = { objetivos = it },
                rotulo = "Objetivos e planejamento",
                linhas = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "As opções selecionadas registram informações relatadas ou avaliadas pelo profissional. Não representam diagnóstico médico.",
                fontSize = 12.sp,
                color = corSecundaria
            )

            if (mensagemErro != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = mensagemErro!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (alunoId <= 0) {
                        mensagemErro = "Aluno inválido."
                        return@Button
                    }

                    if (dataAvaliacao.isBlank()) {
                        mensagemErro = "Informe a data da avaliação."
                        return@Button
                    }

                    if (salvando) return@Button

                    salvando = true
                    mensagemErro = null

                    scope.launch {
                        try {
                            val agora = SimpleDateFormat(
                                "dd/MM/yyyy HH:mm",
                                Locale.getDefault()
                            ).format(Date())

                            val anamnese = AnamneseEntity(
                                id = 0,
                                alunoId = alunoId,
                                grupoId = grupoId,
                                versao = anamneseParaEditar?.versao ?: 1,
                                dataAvaliacao = dataAvaliacao.trim(),
                                historicoSaude = historicoSaude.trim(),
                                medicamentos = medicamentos.trim(),
                                cirurgias = cirurgias.trim(),
                                acompanhamentoMedico = acompanhamentoMedico.trim(),
                                condicoesSelecionadas = condicoes.joinToString("|"),
                                restricoesSelecionadas = restricoes.joinToString("|"),
                                riscosSelecionados = riscos.joinToString("|"),
                                observacoes = observacoes.trim(),
                                recomendacoes = recomendacoes.trim(),
                                cuidados = cuidados.trim(),
                                objetivos = objetivos.trim(),
                                profissionalResponsavel = profissional.trim(),
                                dataCriacao = anamneseParaEditar?.dataCriacao ?: agora,
                                dataAtualizacao = agora
                            )

                            val novoId = anamneseRepository.salvarNovaVersao(anamnese)

                            val ultimaVersao = anamnese.copy(
                                id = novoId,
                                versao = anamneseParaEditar?.let {
                                    it.versao + 1
                                } ?: 1
                            )

                            val arquivoPdf = gerarPdfAnamnese(
                                context = context,
                                alunoId = alunoId,
                                anamnese = ultimaVersao
                            )

                            val documento = DocumentoAnamneseEntity(
                                alunoId = alunoId,
                                nomeArquivo = arquivoPdf.name,
                                caminhoArquivo = arquivoPdf.absolutePath,
                                tipoArquivo = "pdf",
                                tamanhoArquivo = arquivoPdf.length(),
                                dataImportacao = SimpleDateFormat(
                                    "dd/MM/yyyy HH:mm",
                                    Locale.getDefault()
                                ).format(Date())
                            )

                            anamneseRepository.importarDocumento(documento)

                            onSalvo()
                            onSalvo()

                        } catch (e: Exception) {
                            mensagemErro = e.message
                                ?: "Não foi possível salvar a anamnese."
                        } finally {
                            salvando = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = !salvando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = corPrimaria
                )
            ) {
                if (salvando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "SALVAR ANAMNESE",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun tituloSecaoAnamnese(titulo: String) {
    Text(
        text = titulo,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun campoAnamnese(
    valor: String,
    onValorChange: (String) -> Unit,
    rotulo: String,
    linhas: Int = 1
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValorChange,
        label = { Text(rotulo) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        minLines = linhas,
        maxLines = linhas,
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun opcoesAnamnese(
    opcoes: List<String>,
    selecionadas: Set<String>,
    aoAlterar: (Set<String>) -> Unit
) {
    Column {
        opcoes.forEach { opcao ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selecionadas.contains(opcao),
                    onCheckedChange = { marcado ->
                        val novas = selecionadas.toMutableSet()

                        if (marcado) {
                            if (opcao.startsWith("Nenhuma") ||
                                opcao == "Nenhum risco identificado" ||
                                opcao == "Sem restrições informadas"
                            ) {
                                novas.clear()
                            } else {
                                novas.removeAll {
                                    it.startsWith("Nenhuma") ||
                                            it == "Nenhum risco identificado" ||
                                            it == "Sem restrições informadas"
                                }
                            }

                            novas.add(opcao)
                        } else {
                            novas.remove(opcao)
                        }

                        aoAlterar(novas)
                    }
                )

                Text(
                    text = opcao,
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}