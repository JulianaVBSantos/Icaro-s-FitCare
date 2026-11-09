
package br.com.icaros.fitcare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UsuarioEntity::class,
        AlunoEntity::class,
        TreinoEntity::class,
        ExercicioEntity::class,
        AnamneseEntity::class,
        DocumentoAnamneseEntity::class,
        ModeloAnamneseEntity::class,
        PerguntaModeloAnamneseEntity::class,
        AnamneseAtribuidaEntity::class,
        PerguntaAnamneseAtribuidaEntity::class,
        RespostaAnamneseEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class FitCareDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao

    abstract fun alunoDao(): AlunoDao

    abstract fun documentoAnamneseDao(): DocumentoAnamneseDao

    abstract fun treinoDao(): TreinoDao

    abstract fun anamneseDao(): AnamneseDao

    abstract fun modeloAnamneseDao(): ModeloAnamneseDao

    abstract fun perguntaModeloAnamneseDao(): PerguntaModeloAnamneseDao

    abstract fun anamneseAtribuidaDao(): AnamneseAtribuidaDao

    abstract fun perguntaAnamneseAtribuidaDao(): PerguntaAnamneseAtribuidaDao

    abstract fun respostaAnamneseDao(): RespostaAnamneseDao

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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS anamneses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alunoId INTEGER NOT NULL,
                        grupoId TEXT NOT NULL,
                        versao INTEGER NOT NULL,
                        dataAvaliacao TEXT NOT NULL,
                        historicoSaude TEXT NOT NULL,
                        medicamentos TEXT NOT NULL,
                        cirurgias TEXT NOT NULL,
                        acompanhamentoMedico TEXT NOT NULL,
                        condicoesSelecionadas TEXT NOT NULL,
                        restricoesSelecionadas TEXT NOT NULL,
                        riscosSelecionados TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        recomendacoes TEXT NOT NULL,
                        cuidados TEXT NOT NULL,
                        objetivos TEXT NOT NULL,
                        profissionalResponsavel TEXT NOT NULL,
                        dataCriacao TEXT NOT NULL,
                        dataAtualizacao TEXT NOT NULL,
                        FOREIGN KEY(alunoId) REFERENCES alunos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_anamneses_alunoId
                    ON anamneses(alunoId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_anamneses_grupoId
                    ON anamneses(grupoId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_anamneses_alunoId_grupoId_versao
                    ON anamneses(alunoId, grupoId, versao)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS documentos_anamnese (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alunoId INTEGER NOT NULL,
                        nomeArquivo TEXT NOT NULL,
                        caminhoArquivo TEXT NOT NULL,
                        tipoArquivo TEXT NOT NULL,
                        tamanhoArquivo INTEGER NOT NULL,
                        dataImportacao TEXT NOT NULL,
                        FOREIGN KEY(alunoId) REFERENCES alunos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_documentos_anamnese_alunoId
                    ON documentos_anamnese(alunoId)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS modelos_anamnese (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nome TEXT NOT NULL,
                        descricao TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        ativo INTEGER NOT NULL,
                        dataCriacao TEXT NOT NULL,
                        dataAtualizacao TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS perguntas_modelo_anamnese (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        modeloId INTEGER NOT NULL,
                        texto TEXT NOT NULL,
                        tipoResposta TEXT NOT NULL,
                        opcoes TEXT NOT NULL,
                        obrigatoria INTEGER NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(modeloId)
                        REFERENCES modelos_anamnese(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_perguntas_modelo_anamnese_modeloId
                    ON perguntas_modelo_anamnese(modeloId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS anamneses_atribuidas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alunoId INTEGER NOT NULL,
                        modeloId INTEGER NOT NULL,
                        nomeModelo TEXT NOT NULL,
                        status TEXT NOT NULL,
                        dataCriacao TEXT NOT NULL,
                        dataEnvio TEXT NOT NULL,
                        dataInicioResposta TEXT NOT NULL,
                        dataResposta TEXT NOT NULL,
                        profissionalResponsavel TEXT NOT NULL,
                        identificadorExterno TEXT NOT NULL,
                        FOREIGN KEY(alunoId)
                        REFERENCES alunos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(modeloId)
                        REFERENCES modelos_anamnese(id)
                        ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_anamneses_atribuidas_alunoId
                    ON anamneses_atribuidas(alunoId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_anamneses_atribuidas_modeloId
                    ON anamneses_atribuidas(modeloId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_anamneses_atribuidas_status
                    ON anamneses_atribuidas(status)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS perguntas_anamnese_atribuidas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        anamneseAtribuidaId INTEGER NOT NULL,
                        perguntaModeloId INTEGER,
                        texto TEXT NOT NULL,
                        tipoResposta TEXT NOT NULL,
                        opcoes TEXT NOT NULL,
                        obrigatoria INTEGER NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(anamneseAtribuidaId)
                        REFERENCES anamneses_atribuidas(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_perguntas_anamnese_atribuidas_anamneseAtribuidaId
                    ON perguntas_anamnese_atribuidas(anamneseAtribuidaId)
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS respostas_anamnese (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        perguntaAtribuidaId INTEGER NOT NULL,
                        resposta TEXT NOT NULL,
                        dataAtualizacao TEXT NOT NULL,
                        FOREIGN KEY(perguntaAtribuidaId)
                        REFERENCES perguntas_anamnese_atribuidas(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_respostas_anamnese_perguntaAtribuidaId
                    ON respostas_anamnese(perguntaAtribuidaId)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS usuarios (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nome TEXT NOT NULL,
                        email TEXT NOT NULL,
                        senhaHash TEXT NOT NULL,
                        perfil TEXT NOT NULL,
                        alunoId INTEGER,
                        professorId INTEGER,
                        ativo INTEGER NOT NULL,
                        dataCriacao INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    index_usuarios_email
                    ON usuarios(email)
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
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}