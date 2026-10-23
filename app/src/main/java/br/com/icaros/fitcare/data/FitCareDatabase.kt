
package br.com.icaros.fitcare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlunoEntity::class,
        TreinoEntity::class,
        ExercicioEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FitCareDatabase : RoomDatabase() {

    abstract fun alunoDao(): AlunoDao

    abstract fun treinoDao(): TreinoDao

    companion object {

        @Volatile
        private var INSTANCE: FitCareDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE alunos ADD COLUMN observacoesProfissionais TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE alunos ADD COLUMN condicoes TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE alunos ADD COLUMN restricoes TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS treinos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alunoId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        descricao TEXT NOT NULL,
                        dataInicio TEXT NOT NULL,
                        dataFim TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        FOREIGN KEY(alunoId) REFERENCES alunos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_treinos_alunoId
                    ON treinos(alunoId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercicios (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        treinoId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        series INTEGER NOT NULL,
                        repeticoes TEXT NOT NULL,
                        carga TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(treinoId) REFERENCES treinos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_exercicios_treinoId
                    ON exercicios(treinoId)
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): FitCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitCareDatabase::class.java,
                    "fitcare_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}