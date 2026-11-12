
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
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class AlunoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Iniciando sincronização de alunos.")

            val database = FitCareDatabase.getDatabase(applicationContext)

            val syncService = FirestoreSyncService(
                usuarioDao = database.usuarioDao(),
                database = database
            )

            // Não encerra como sucesso se a sessão estiver inválida.
            if (!syncService.validarSessao()) {
                Log.w(
                    TAG,
                    "Sessão Firebase inválida ou usuário local não vinculado. Nova tentativa agendada."
                )
                return Result.retry()
            }

            Log.i(TAG, "Sessão validada. Enviando alterações locais.")

            // Primeiro envia as alterações locais.
            val escritaConcluida = syncService.sincronizarAlunos()

            if (!escritaConcluida) {
                Log.e(TAG, "Falha ao enviar alterações locais ao Firestore.")
                return Result.retry()
            }

            Log.i(TAG, "Envio local concluído. Iniciando leitura remota.")

            // Depois recebe as alterações feitas em outros dispositivos.
            val leituraConcluida = syncService.sincronizarLeituraAlunos()

            if (!leituraConcluida) {
                Log.e(TAG, "Falha ao receber alterações do Firestore.")
                return Result.retry()
            }

            Log.i(TAG, "Sincronização concluída com sucesso.")

            Result.success()

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Erro inesperado durante a sincronização.",
                e
            )
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

        /**
         * Agenda uma sincronização imediata.
         * Utilizada após alterações locais e login.
         */
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

            Log.i(TAG, "Solicitação de sincronização imediata enviada ao WorkManager.")
        }

        /**
         * Agenda a sincronização periódica.
         * O intervalo mínimo permitido pelo WorkManager é de 15 minutos.
         */
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

        /**
         * Cancela somente a sincronização periódica.
         * Não apaga dados locais nem operações pendentes.
         */
        fun cancelarPeriodica(context: Context) {

            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(PERIODIC_WORK_NAME)

            Log.i(TAG, "Sincronização periódica cancelada.")
        }
    }
}