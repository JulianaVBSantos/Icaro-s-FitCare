package br.com.icaros.fitcare

import android.content.Context
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.icaros.fitcare.data.AlunoRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RegistroCargaExercicio(
    val data: String,
    val carga: String,
    val timestamp: Long = System.currentTimeMillis()
)

object HistoricoCargasAlunoStorage {

    private fun normalizarNome(nome: String): String =
        nome.trim().lowercase(Locale.ROOT)

    fun registrarCarga(
        context: Context,
        alunoId: Long,
        nomeExercicio: String,
        carga: String,
        data: String = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date()),
        alunoFirebaseId: String? = null
    ) {
        val nomeLimpo = nomeExercicio.trim()
        val cargaLimpa = carga.trim()
        if (nomeLimpo.isBlank() || cargaLimpa.isBlank()) return

        val prefs = context.getSharedPreferences("fitcare_hist_cargas_permanente_$alunoId", Context.MODE_PRIVATE)
        val raiz = runCatching { JSONObject(prefs.getString("mapa_exercicios", "{}") ?: "{}") }
            .getOrDefault(JSONObject())

        val chave = normalizarNome(nomeLimpo)
        val objEx = raiz.optJSONObject(chave) ?: JSONObject().apply {
            put("nomeExibicao", nomeLimpo)
            put("registros", JSONArray())
        }
        objEx.put("nomeExibicao", nomeLimpo)
        val arr = objEx.optJSONArray("registros") ?: JSONArray()

        // Evita duplicata idêntica consecutiva na mesma data
        val ultimo = if (arr.length() > 0) arr.optJSONObject(arr.length() - 1) else null
        if (ultimo == null || ultimo.optString("carga") != cargaLimpa || ultimo.optString("data") != data) {
            arr.put(
                JSONObject().apply {
                    put("data", data)
                    put("carga", cargaLimpa)
                    put("timestamp", System.currentTimeMillis())
                }
            )
        }
        objEx.put("registros", arr)
        raiz.put(chave, objEx)

        val jsonFinal = raiz.toString()
        prefs.edit().putString("mapa_exercicios", jsonFinal).apply()

        if (!alunoFirebaseId.isNullOrBlank()) {
            FirebaseFirestore.getInstance()
                .collection("alunos")
                .document(alunoFirebaseId)
                .set(mapOf("historicoCargasJson" to jsonFinal), SetOptions.merge())
        }
    }

    fun obterUltimaCargaSalva(context: Context, alunoId: Long, nomeExercicio: String): String? {
        val prefs = context.getSharedPreferences("fitcare_hist_cargas_permanente_$alunoId", Context.MODE_PRIVATE)
        val raiz = runCatching { JSONObject(prefs.getString("mapa_exercicios", "{}") ?: "{}") }
            .getOrNull() ?: return null
        val objEx = raiz.optJSONObject(normalizarNome(nomeExercicio)) ?: return null
        val arr = objEx.optJSONArray("registros") ?: return null
        if (arr.length() == 0) return null
        return arr.optJSONObject(arr.length() - 1)?.optString("carga")?.takeIf { it.isNotBlank() }
    }

    suspend fun sincronizarDoFirestore(context: Context, alunoId: Long, alunoFirebaseId: String?) {
        if (alunoFirebaseId.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("alunos")
                    .document(alunoFirebaseId)
                    .get()
                    .await()
                val remotoStr = doc.getString("historicoCargasJson").orEmpty()
                if (remotoStr.isNotBlank()) {
                    val prefs = context.getSharedPreferences("fitcare_hist_cargas_permanente_$alunoId", Context.MODE_PRIVATE)
                    val localObj = runCatching { JSONObject(prefs.getString("mapa_exercicios", "{}") ?: "{}") }.getOrDefault(JSONObject())
                    val remotoObj = runCatching { JSONObject(remotoStr) }.getOrDefault(JSONObject())
                    val keys = remotoObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val exRem = remotoObj.optJSONObject(k) ?: continue
                        val exLoc = localObj.optJSONObject(k)
                        val lenRem = exRem.optJSONArray("registros")?.length() ?: 0
                        val lenLoc = exLoc?.optJSONArray("registros")?.length() ?: 0
                        if (lenRem >= lenLoc) {
                            localObj.put(k, exRem)
                        }
                    }
                    prefs.edit().putString("mapa_exercicios", localObj.toString()).apply()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun listarTodosExerciciosComHistorico(
        context: Context,
        alunoId: Long
    ): List<Pair<String, List<RegistroCargaExercicio>>> {
        val prefs = context.getSharedPreferences("fitcare_hist_cargas_permanente_$alunoId", Context.MODE_PRIVATE)
        val raiz = runCatching { JSONObject(prefs.getString("mapa_exercicios", "{}") ?: "{}") }
            .getOrDefault(JSONObject())
        val resultado = mutableListOf<Pair<String, List<RegistroCargaExercicio>>>()
        val keys = raiz.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val objEx = raiz.optJSONObject(k) ?: continue
            val nomeExib = objEx.optString("nomeExibicao").ifBlank { k }
            val arr = objEx.optJSONArray("registros") ?: continue
            val regs = buildList {
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val d = item.optString("data")
                    val c = item.optString("carga")
                    if (d.isNotBlank() && c.isNotBlank()) {
                        add(RegistroCargaExercicio(d, c, item.optLong("timestamp")))
                    }
                }
            }
            if (regs.isNotEmpty()) {
                resultado.add(nomeExib to regs)
            }
        }
        return resultado.sortedBy { it.first.lowercase(Locale.ROOT) }
    }
}

@Composable
fun TelaProgressaoCargaAluno(
    alunoId: Long,
    repository: AlunoRepository,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val aluno by repository.buscarPorId(alunoId).collectAsState(initial = null)
    var busca by remember { mutableStateOf("") }
    var listaHistorico by remember(alunoId) {
        mutableStateOf(HistoricoCargasAlunoStorage.listarTodosExerciciosComHistorico(context, alunoId))
    }
    var exercicioSelecionado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(alunoId, aluno?.firebaseId) {
        HistoricoCargasAlunoStorage.sincronizarDoFirestore(context, alunoId, aluno?.firebaseId)
        listaHistorico = HistoricoCargasAlunoStorage.listarTodosExerciciosComHistorico(context, alunoId)
        if (exercicioSelecionado == null && listaHistorico.isNotEmpty()) {
            exercicioSelecionado = listaHistorico.first().first
        }
    }

    val filtrados = remember(busca, listaHistorico) {
        val t = busca.trim()
        if (t.isBlank()) listaHistorico
        else listaHistorico.filter { it.first.contains(t, ignoreCase = true) }
    }

    val itemAtivo = filtrados.firstOrNull { it.first == exercicioSelecionado } ?: filtrados.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (temaEscuroAtivoState) Color(0xFF0F1722) else Color(0xFFF5F9FC))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CabecalhoPadrao(
            titulo = "Progressão de carga",
            onVoltar = onVoltar
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aluno: ${aluno?.nome ?: "Carregando..."}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (temaEscuroAtivoState) Color(0xFFEAF2FA) else Color(0xFF002B5C)
            )
            Text(
                text = "Histórico permanente de cargas por exercício (mantido mesmo após troca de ficha).",
                fontSize = 12.sp,
                color = if (temaEscuroAtivoState) Color(0xFFA2B5C8) else Color(0xFF526579)
            )

            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                label = { Text("Filtrar exercício...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (filtrados.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (temaEscuroAtivoState) Color(0xFF172331) else Color.White
                    )
                ) {
                    Text(
                        text = "Nenhum histórico de carga registrado para este aluno ainda.",
                        modifier = Modifier.padding(18.dp),
                        fontSize = 13.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filtrados.forEach { (nomeEx, _) ->
                        FilterChip(
                            selected = itemAtivo?.first == nomeEx,
                            onClick = { exercicioSelecionado = nomeEx },
                            label = { Text(nomeEx, fontSize = 11.sp) }
                        )
                    }
                }
                itemAtivo?.let { (nomeEx, registros) ->
                    val ultimo = registros.last()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (temaEscuroAtivoState) Color(0xFF172331) else Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = nomeEx,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Última carga salva: ${ultimo.carga} (em ${ultimo.data})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
// Gráfico Carga x Data
                            val pontosNumericos = registros.mapNotNull { reg ->
                                val num = Regex("[0-9]+([.,][0-9]+)?")
                                    .find(reg.carga)
                                    ?.value
                                    ?.replace(",", ".")
                                    ?.toFloatOrNull()
                                if (num != null) Triple(reg.data, num, reg.carga) else null
                            }
                            if (pontosNumericos.isNotEmpty()) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                ) {
                                    val padEsq = 36.dp.toPx()
                                    val padDir = 16.dp.toPx()
                                    val padTop = 20.dp.toPx()
                                    val padBot = 28.dp.toPx()
                                    val w = size.width - padEsq - padDir
                                    val h = size.height - padTop - padBot
                                    val minV = (pontosNumericos.minOf { it.second } - 5f).coerceAtLeast(0f)
                                    val maxV = (pontosNumericos.maxOf { it.second } + 5f).coerceAtLeast(minV + 1f)
                                    val paintTexto = Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 9.dp.toPx()
                                        isAntiAlias = true
                                        textAlign = Paint.Align.CENTER
                                    }
                                    val coords = pontosNumericos.mapIndexed { idx, (_, valor, _) ->
                                        val x = if (pontosNumericos.size == 1) padEsq + w / 2f
                                        else padEsq + w * idx / (pontosNumericos.size - 1)
                                        val y = padTop + h * (1f - ((valor - minV) / (maxV - minV)))
                                        Offset(x, y)
                                    }
                                    for (i in 0 until coords.size - 1) {
                                        drawLine(
                                            color = Color(0xFF164A73),
                                            start = coords.getOrNull(i) ?: continue,
                                            end = coords.getOrNull(i + 1) ?: continue,
                                            strokeWidth = 2.5.dp.toPx()
                                        )
                                    }
                                    coords.forEachIndexed { idx, pt ->
                                        drawCircle(
                                            color = Color(0xFF002B5C),
                                            radius = 4.5.dp.toPx(),
                                            center = pt
                                        )
                                        val info = pontosNumericos.getOrNull(idx)
                                        if (info != null) {
                                            drawContext.canvas.nativeCanvas.drawText(
                                                info.third,
                                                pt.x,
                                                pt.y - 8.dp.toPx(),
                                                paintTexto
                                            )
                                            drawContext.canvas.nativeCanvas.drawText(
                                                info.first.take(5),
                                                pt.x,
                                                size.height - 6.dp.toPx(),
                                                paintTexto
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Registros cronológicos (${registros.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            registros.asReversed().forEach { reg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = reg.data, fontSize = 12.sp)
                                    Text(
                                        text = reg.carga,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}