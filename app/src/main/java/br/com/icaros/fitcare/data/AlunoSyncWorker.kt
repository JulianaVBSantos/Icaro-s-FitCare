
package br.com.icaros.fitcare.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class AlunoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Iniciando sincronização.")

            val database =
                FitCareDatabase.getDatabase(applicationContext)

            val firebaseUser =
                FirebaseAuth.getInstance().currentUser
                    ?: return Result.retry()

            val usuario = database.usuarioDao()
                .buscarPorFirebaseUid(firebaseUser.uid)
                ?: return Result.retry()

            if (!usuario.ativo) {
                Log.w(TAG, "Usuário local inativo.")
                return Result.failure()
            }

            val syncService = FirestoreSyncService(
                usuarioDao = database.usuarioDao(),
                database = database
            )

            when (usuario.perfil.uppercase()) {

                "PROFESSOR" -> {
                    if (!syncService.validarSessao()) {
                        Log.w(TAG, "Sessão do professor inválida.")
                        return Result.retry()
                    }

                    if (!syncService.sincronizarAlunos()) {
                        Log.e(TAG, "Falha ao enviar alunos.")
                        return Result.retry()
                    }

                    if (!syncService.sincronizarLeituraAlunos()) {
                        Log.e(TAG, "Falha ao receber alunos.")
                        return Result.retry()
                    }

                    if (!syncService.sincronizarModelosAnamnese()) {
                        Log.e(TAG, "Falha ao sincronizar modelos.")
                        return Result.retry()
                    }

                    if (!syncService.sincronizarAnamnesesAtribuidas()) {
                        Log.e(TAG, "Falha ao sincronizar anamneses atribuídas.")
                        return Result.retry()
                    }
                }

                "ALUNO" -> {
                    if (!syncService.sincronizarAnamnesesAtribuidasDoAluno()) {
                        Log.e(TAG, "Falha ao sincronizar anamneses do aluno.")
                        return Result.retry()
                    }
                }

                else -> {
                    Log.e(
                        TAG,
                        "Perfil sem permissão para sincronização: ${usuario.perfil}"
                    )
                    return Result.failure()
                }
            }

            Log.i(TAG, "Sincronização concluída com sucesso.")
            Result.success()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado durante a sincronização.", e)
            Result.retry()
        }
    }

    companion object {

        private const val TAG = "AlunoSyncWorker"

        const val WORK_NAME = "aluno_sync"
        const val PERIODIC_WORK_NAME = "aluno_sync_periodico"

        private const val INTERVALO_PERIODICO_MINUTOS = 15L

        private fun criarRestricoes(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }

        fun agendar(context: Context) {
            val requisicao =
                OneTimeWorkRequestBuilder<AlunoSyncWorker>()
                    .setConstraints(criarRestricoes())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    requisicao
                )

            Log.i(TAG, "Sincronização imediata agendada.")
        }

        fun agendarPeriodica(context: Context) {
            val requisicao =
                PeriodicWorkRequestBuilder<AlunoSyncWorker>(
                    INTERVALO_PERIODICO_MINUTOS,
                    TimeUnit.MINUTES
                )
                    .setConstraints(criarRestricoes())
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        30,
                        TimeUnit.SECONDS
                    )
                    .build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    requisicao
                )

            Log.i(TAG, "Sincronização periódica agendada.")
        }

        fun cancelarPeriodica(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(PERIODIC_WORK_NAME)

            Log.i(TAG, "Sincronização periódica cancelada.")
        }
    }
}