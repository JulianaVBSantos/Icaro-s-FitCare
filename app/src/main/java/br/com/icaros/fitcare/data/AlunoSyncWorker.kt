
package br.com.icaros.fitcare.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class AlunoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        return try {

            val database = FitCareDatabase.getDatabase(
                applicationContext
            )

            val syncService = FirestoreSyncService(
                usuarioDao = database.usuarioDao(),
                database = database
            )

            // Verifica se existe uma sessão Firebase válida
            if (!syncService.validarSessao()) {
                // Mantém as operações pendentes no Room.
                // A sincronização será agendada novamente após o login.
                return Result.success()
            }

            // Executa a sincronização dos alunos
            val sincronizacaoConcluida =
                syncService.sincronizarAlunos()

            if (sincronizacaoConcluida) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {

        const val WORK_NAME = "aluno_sync"

        /**
         * Agenda a sincronização dos alunos.
         *
         * O WorkManager executará o trabalho somente
         * quando houver conexão com a internet.
         */
        fun agendar(context: Context) {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val requisicao =
                OneTimeWorkRequestBuilder<AlunoSyncWorker>()
                    .setConstraints(constraints)
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
        }
    }
}