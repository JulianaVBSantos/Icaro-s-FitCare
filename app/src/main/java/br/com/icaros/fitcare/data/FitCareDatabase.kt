
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
    version = 9,
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

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE alunos ADD COLUMN professorId INTEGER"
                )

                db.execSQL(
                    """
                    UPDATE alunos SET professorId = (
                        SELECT id
                        FROM usuarios
                        WHERE LOWER(email) = 'icaroalvesdasilva2005@gmail.com'
                          AND perfil = 'PROFESSOR'
                        LIMIT 1
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_alunos_professorId
                    ON alunos(professorId)
                    """.trimIndent()
                )

                db.execSQL(
                    "ALTER TABLE modelos_anamnese ADD COLUMN professorId INTEGER"
                )

                db.execSQL(
                    """
                    UPDATE modelos_anamnese
                    SET professorId = (
                        SELECT id
                        FROM usuarios
                        WHERE LOWER(email) = 'icaroalvesdasilva2005@gmail.com'
                          AND perfil = 'PROFESSOR'
                        LIMIT 1
                    )
                    WHERE categoria = 'PERSONALIZADO'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_modelos_anamnese_professorId
                    ON modelos_anamnese(professorId)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_modelos_anamnese_categoria
                    ON modelos_anamnese(categoria)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // 1. Fazer backup das três tabelas antes de qualquer alteração.
                db.execSQL(
                    """
            CREATE TABLE respostas_anamnese_backup AS
            SELECT * FROM respostas_anamnese
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE perguntas_anamnese_atribuidas_backup AS
            SELECT * FROM perguntas_anamnese_atribuidas
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE TABLE anamneses_atribuidas_backup AS
            SELECT * FROM anamneses_atribuidas
            """.trimIndent()
                )

                // 2. Excluir primeiro as tabelas dependentes.
                db.execSQL("DROP TABLE respostas_anamnese")
                db.execSQL("DROP TABLE perguntas_anamnese_atribuidas")
                db.execSQL("DROP TABLE anamneses_atribuidas")

                // 3. Recriar anamneses_atribuidas sem FK para modelos_anamnese.
                db.execSQL(
                    """
            CREATE TABLE anamneses_atribuidas (
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
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
                )

                // 4. Recriar perguntas das anamneses atribuídas.
                db.execSQL(
                    """
            CREATE TABLE perguntas_anamnese_atribuidas (
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

                // 5. Recriar respostas.
                db.execSQL(
                    """
            CREATE TABLE respostas_anamnese (
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

                // 6. Restaurar primeiro as anamneses atribuídas.
                db.execSQL(
                    """
            INSERT INTO anamneses_atribuidas (
                id,
                alunoId,
                modeloId,
                nomeModelo,
                status,
                dataCriacao,
                dataEnvio,
                dataInicioResposta,
                dataResposta,
                profissionalResponsavel,
                identificadorExterno
            )
            SELECT
                id,
                alunoId,
                modeloId,
                nomeModelo,
                status,
                dataCriacao,
                dataEnvio,
                dataInicioResposta,
                dataResposta,
                profissionalResponsavel,
                identificadorExterno
            FROM anamneses_atribuidas_backup
            """.trimIndent()
                )

                // 7. Restaurar as perguntas atribuídas.
                db.execSQL(
                    """
            INSERT INTO perguntas_anamnese_atribuidas (
                id,
                anamneseAtribuidaId,
                perguntaModeloId,
                texto,
                tipoResposta,
                opcoes,
                obrigatoria,
                ordem
            )
            SELECT
                id,
                anamneseAtribuidaId,
                perguntaModeloId,
                texto,
                tipoResposta,
                opcoes,
                obrigatoria,
                ordem
            FROM perguntas_anamnese_atribuidas_backup
            """.trimIndent()
                )

                // 8. Restaurar as respostas.
                db.execSQL(
                    """
            INSERT INTO respostas_anamnese (
                id,
                perguntaAtribuidaId,
                resposta,
                dataAtualizacao
            )
            SELECT
                id,
                perguntaAtribuidaId,
                resposta,
                dataAtualizacao
            FROM respostas_anamnese_backup
            """.trimIndent()
                )

                // 9. Remover as tabelas temporárias.
                db.execSQL("DROP TABLE respostas_anamnese_backup")
                db.execSQL("DROP TABLE perguntas_anamnese_atribuidas_backup")
                db.execSQL("DROP TABLE anamneses_atribuidas_backup")

                // 10. Recriar os índices.
                db.execSQL(
                    """
            CREATE INDEX index_anamneses_atribuidas_alunoId
            ON anamneses_atribuidas(alunoId)
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX index_anamneses_atribuidas_modeloId
            ON anamneses_atribuidas(modeloId)
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX index_anamneses_atribuidas_status
            ON anamneses_atribuidas(status)
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE INDEX index_perguntas_anamnese_atribuidas_anamneseAtribuidaId
            ON perguntas_anamnese_atribuidas(anamneseAtribuidaId)
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE UNIQUE INDEX index_respostas_anamnese_perguntaAtribuidaId
            ON respostas_anamnese(perguntaAtribuidaId)
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE usuarios ADD COLUMN firebaseUid TEXT"
                )

                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_usuarios_firebaseUid
            ON usuarios(firebaseUid)
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
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}