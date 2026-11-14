package br.com.icaros.fitcare.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private const val PREFS_FCM = "fitcare_fcm"
    private const val CHAVE_TOKEN_ENVIADO = "fcm_token_enviado"
    private const val CHAVE_UID_ENVIADO = "fcm_uid_enviado"
    private const val CHAVE_PERFIL_ENVIADO = "fcm_perfil_enviado"

    fun sincronizarTokenSessaoAtual(context: Context) {
        val usuario = SessaoUsuario.usuarioAtual ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    if (jaFoiSincronizado(context, uid, usuario.perfil, token)) {
                        return@addOnSuccessListener
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        registrarTokenNoFirestore(
                            context = context,
                            usuario = usuario,
                            uid = uid,
                            token = token
                        )
                    }
                }
            }
    }

    fun atualizarNovoToken(context: Context, novoToken: String) {
        if (novoToken.isBlank()) return

        val usuario = SessaoUsuario.usuarioAtual ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        if (jaFoiSincronizado(context, uid, usuario.perfil, novoToken)) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            registrarTokenNoFirestore(
                context = context,
                usuario = usuario,
                uid = uid,
                token = novoToken
            )
        }
    }

    private suspend fun registrarTokenNoFirestore(
        context: Context,
        usuario: UsuarioEntity,
        uid: String,
        token: String
    ) {
        val firestore = FirebaseFirestore.getInstance()
        try {
            when (usuario.perfil) {
                "PROFESSOR" -> {
                    firestore.collection("usuarios")
                        .document(uid)
                        .set(
                            mapOf(
                                "fcmToken" to token,
                                "fcmUpdatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                    marcarComoSincronizado(context, uid, usuario.perfil, token)
                }

                "ALUNO" -> {
                    val alunoId = usuario.alunoId ?: return
                    val db = FitCareDatabase.getDatabase(context)
                    val alunoFbId = db.alunoDao().buscarPorIdDireto(alunoId)?.firebaseId
                        ?.takeIf { it.isNotBlank() } ?: return

                    firestore.collection("alunos")
                        .document(alunoFbId)
                        .set(
                            mapOf(
                                "fcmToken" to token,
                                "fcmUpdatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                        .await()
                    marcarComoSincronizado(context, uid, usuario.perfil, token)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun removerTokenAoSair(
        context: Context,
        usuarioAnterior: UsuarioEntity?,
        uidAnterior: String?
    ) {
        limparCacheLocal(context)

        if (usuarioAnterior == null || uidAnterior.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            val firestore = FirebaseFirestore.getInstance()
            try {
                when (usuarioAnterior.perfil) {
                    "PROFESSOR" -> {
                        firestore.collection("usuarios")
                            .document(uidAnterior)
                            .update("fcmToken", FieldValue.delete())
                            .await()
                    }

                    "ALUNO" -> {
                        val alunoId = usuarioAnterior.alunoId ?: return@launch
                        val db = FitCareDatabase.getDatabase(context)
                        val alunoFbId = db.alunoDao().buscarPorIdDireto(alunoId)?.firebaseId
                            ?.takeIf { it.isNotBlank() } ?: return@launch

                        firestore.collection("alunos")
                            .document(alunoFbId)
                            .update("fcmToken", FieldValue.delete())
                            .await()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun jaFoiSincronizado(
        context: Context,
        uid: String,
        perfil: String,
        token: String
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
        val tokenSalvo = prefs.getString(CHAVE_TOKEN_ENVIADO, null)
        val uidSalvo = prefs.getString(CHAVE_UID_ENVIADO, null)
        val perfilSalvo = prefs.getString(CHAVE_PERFIL_ENVIADO, null)

        return token == tokenSalvo && uid == uidSalvo && perfil == perfilSalvo
    }

    private fun marcarComoSincronizado(
        context: Context,
        uid: String,
        perfil: String,
        token: String
    ) {
        context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            .edit()
            .putString(CHAVE_TOKEN_ENVIADO, token)
            .putString(CHAVE_UID_ENVIADO, uid)
            .putString(CHAVE_PERFIL_ENVIADO, perfil)
            .apply()
    }

    private fun limparCacheLocal(context: Context) {
        context.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
            .edit()
            .remove(CHAVE_TOKEN_ENVIADO)
            .remove(CHAVE_UID_ENVIADO)
            .remove(CHAVE_PERFIL_ENVIADO)
            .apply()
    }
}