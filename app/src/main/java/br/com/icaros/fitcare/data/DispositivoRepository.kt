
package br.com.icaros.fitcare.data

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import java.util.UUID

data class DispositivoInfo(
    val id: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val appVersion: String,
    val lastSeen: Date?
) {
    fun estaAtivo(): Boolean {
        val ultimaAtividade = lastSeen ?: return false
        return System.currentTimeMillis() - ultimaAtividade.time <= 180_000L
    }
}

class DispositivoRepository(context: Context) {

    private val appContext = context.applicationContext
    private val firestore = FirebaseFirestore.getInstance()

    private val preferencias = appContext.getSharedPreferences(
        "fitcare_dispositivo",
        Context.MODE_PRIVATE
    )

    private fun obterInstallationId(): String {
        val existente = preferencias.getString("installation_id", null)

        if (!existente.isNullOrBlank()) {
            return existente
        }

        val novoId = UUID.randomUUID().toString()

        preferencias.edit()
            .putString("installation_id", novoId)
            .apply()

        return novoId
    }

    @Suppress("DEPRECATION")
    private fun obterVersaoAplicativo(): String {
        return try {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .versionName ?: "Desconhecida"
        } catch (e: Exception) {
            "Desconhecida"
        }
    }

    fun registrarAtividade(firebaseUid: String) {

        if (firebaseUid.isBlank()) return

        // Impede que uma sessão de outro usuário registre atividade
        // em nome deste professor.
        val uidAutenticado = FirebaseAuth.getInstance()
            .currentUser?.uid

        if (uidAutenticado != firebaseUid) return

        val installationId = obterInstallationId()

        val referencia = firestore
            .collection("usuarios")
            .document(firebaseUid)
            .collection("dispositivos")
            .document(installationId)

        referencia.get()
            .addOnSuccessListener { documento ->

                if (documento.exists()) {

                    // Não altera createdAt nem os demais campos.
                    // Isso respeita as regras de atualização do Firestore.
                    referencia.update(
                        "lastSeen",
                        FieldValue.serverTimestamp()
                    )

                } else {

                    val dados = hashMapOf<String, Any>(
                        "installationId" to installationId,
                        "manufacturer" to Build.MANUFACTURER,
                        "model" to Build.MODEL,
                        "androidVersion" to Build.VERSION.RELEASE,
                        "appVersion" to obterVersaoAplicativo(),
                        "platform" to "ANDROID",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "lastSeen" to FieldValue.serverTimestamp()
                    )

                    referencia.set(dados)
                }
            }
    }

    fun listarDispositivos(
        firebaseUid: String,
        callback: (Result<List<DispositivoInfo>>) -> Unit
    ) {

        if (firebaseUid.isBlank()) {
            callback(
                Result.failure(
                    IllegalArgumentException("UID do professor não informado.")
                )
            )
            return
        }

        firestore
            .collection("usuarios")
            .document(firebaseUid)
            .collection("dispositivos")
            .orderBy("lastSeen", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { resultado ->

                val dispositivos = resultado.documents.map { documento ->

                    DispositivoInfo(
                        id = documento.id,
                        manufacturer = documento.getString("manufacturer")
                            ?: "Desconhecido",
                        model = documento.getString("model")
                            ?: "Dispositivo desconhecido",
                        androidVersion = documento.getString("androidVersion")
                            ?: "Desconhecida",
                        appVersion = documento.getString("appVersion")
                            ?: "Desconhecida",
                        lastSeen = documento.getTimestamp("lastSeen")?.toDate()
                    )
                }

                callback(Result.success(dispositivos))
            }
            .addOnFailureListener { erro ->
                callback(Result.failure(erro))
            }
    }
}