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
        RespostaAnamneseEntity::class,
        SyncOperationEntity::class,
        MacrocicloEntity::class,
        MesocicloEntity::class,
        MicrocicloEntity::class,
        SessaoPeriodizacaoEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class FitCareDatabase : RoomDatabase() {
    abstract fun syncOperationDao(): SyncOperationDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun alunoDao(): AlunoDao
    abstract fun documentoAnamneseDao(): DocumentoAnamneseDao
    abstract fun treinoDao(): TreinoDao

    abstract fun periodizacaoDao(): PeriodizacaoDao
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
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // Adiciona o identificador remoto do aluno.
                db.execSQL(
                    "ALTER TABLE alunos ADD COLUMN firebaseId TEXT"
                )

                // Garante que cada aluno tenha um firebaseId único.
                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_alunos_firebaseId
            ON alunos(firebaseId)
            """.trimIndent()
                )

                // Cria a fila de operações de sincronização.
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS sync_operations (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                entityType TEXT NOT NULL,
                localEntityId INTEGER NOT NULL,
                remoteDocumentId TEXT,
                operationType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                attempts INTEGER NOT NULL,
                lastError TEXT
            )
            """.trimIndent()
                )

                // Índice para localizar operações de determinado registro.
                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS
            index_sync_operations_entityType_localEntityId
            ON sync_operations(entityType, localEntityId)
            """.trimIndent()
                )

                // Índice para consultar operações em ordem cronológica.
                db.execSQL(
                    """
            CREATE INDEX IF NOT EXISTS
            index_sync_operations_createdAt
            ON sync_operations(createdAt)
            """.trimIndent()
                )
            }
        }


        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // Adiciona o identificador remoto sem apagar os modelos existentes.
                db.execSQL(
                    "ALTER TABLE modelos_anamnese ADD COLUMN firestoreId TEXT"
                )

                // Gera um identificador único para cada modelo personalizado antigo.
                // Modelos PADRAO continuam sem identificador remoto.
                db.execSQL(
                    """
            UPDATE modelos_anamnese
            SET firestoreId = lower(hex(randomblob(16)))
            WHERE categoria = 'PERSONALIZADO'
              AND firestoreId IS NULL
            """.trimIndent()
                )

                // Impede que dois modelos locais utilizem o mesmo identificador remoto.
                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_modelos_anamnese_firestoreId
            ON modelos_anamnese(firestoreId)
            """.trimIndent()
                )
            }
        }


        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE modelos_anamnese ADD COLUMN excluido INTEGER NOT NULL DEFAULT 0"
                )

                db.execSQL(
                    "ALTER TABLE modelos_anamnese ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0"
                )

                db.execSQL(
                    "ALTER TABLE modelos_anamnese ADD COLUMN deletedAtMillis INTEGER"
                )

                // Inicializa a referência temporal dos modelos existentes.
                db.execSQL(
                    """
            UPDATE modelos_anamnese
            SET updatedAtMillis = CAST(strftime('%s', 'now') AS INTEGER) * 1000
            WHERE updatedAtMillis = 0
            """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE anamneses_atribuidas
            ADD COLUMN firestoreId TEXT
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE anamneses_atribuidas
            ADD COLUMN updatedAtMillis INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )

                db.execSQL(
                    """
            ALTER TABLE anamneses_atribuidas
            ADD COLUMN excluida INTEGER NOT NULL DEFAULT 0
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_anamneses_atribuidas_firestoreId
            ON anamneses_atribuidas(firestoreId)
            """.trimIndent()
                )

                db.execSQL(
                    """
            UPDATE anamneses_atribuidas
            SET updatedAtMillis = CAST(strftime('%s','now') AS INTEGER) * 1000
            WHERE updatedAtMillis = 0
            """.trimIndent()
                )
            }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
            ALTER TABLE perguntas_anamnese_atribuidas
            ADD COLUMN firestoreId TEXT
            """.trimIndent()
                )

                // Gera identificadores remotos únicos para as perguntas existentes.
                db.execSQL(
                    """
            UPDATE perguntas_anamnese_atribuidas
            SET firestoreId = lower(hex(randomblob(16)))
            WHERE firestoreId IS NULL
            """.trimIndent()
                )

                db.execSQL(
                    """
            CREATE UNIQUE INDEX IF NOT EXISTS
            index_perguntas_anamnese_atribuidas_firestoreId
            ON perguntas_anamnese_atribuidas(firestoreId)
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            UPDATE anamneses_atribuidas
            SET firestoreId = lower(hex(randomblob(16)))
            WHERE firestoreId IS NULL OR firestoreId = ''
        """.trimIndent()
                )
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE exercicios_nova (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        treinoId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        series TEXT NOT NULL,
                        repeticoes TEXT NOT NULL,
                        carga TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(treinoId) REFERENCES treinos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO exercicios_nova (id, treinoId, nome, series, repeticoes, carga, observacoes, ordem)
                    SELECT id, treinoId, nome, CAST(series AS TEXT), repeticoes, carga, observacoes, ordem
                    FROM exercicios
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE exercicios")
                db.execSQL("ALTER TABLE exercicios_nova RENAME TO exercicios")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_exercicios_treinoId
                    ON exercicios(treinoId)
                    """.trimIndent()
                )
            }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exercicios ADD COLUMN videoTipo TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercicios ADD COLUMN videoUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE exercicios ADD COLUMN videoNome TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS macrociclos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        alunoId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        objetivo TEXT NOT NULL,
                        dataInicio TEXT NOT NULL,
                        dataFim TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        FOREIGN KEY(alunoId) REFERENCES alunos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_macrociclos_alunoId ON macrociclos(alunoId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS mesociclos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        macrocicloId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        foco TEXT NOT NULL,
                        dataInicio TEXT NOT NULL,
                        dataFim TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(macrocicloId) REFERENCES macrociclos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mesociclos_macrocicloId ON mesociclos(macrocicloId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS microciclos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mesocicloId INTEGER NOT NULL,
                        nome TEXT NOT NULL,
                        objetivoSemanal TEXT NOT NULL,
                        dataInicio TEXT NOT NULL,
                        dataFim TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(mesocicloId) REFERENCES mesociclos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_microciclos_mesocicloId ON microciclos(mesocicloId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sessoes_periodizacao (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        microcicloId INTEGER NOT NULL,
                        treinoId INTEGER,
                        diaSemana TEXT NOT NULL,
                        titulo TEXT NOT NULL,
                        observacoes TEXT NOT NULL,
                        ordem INTEGER NOT NULL,
                        FOREIGN KEY(microcicloId) REFERENCES microciclos(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(treinoId) REFERENCES treinos(id)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessoes_periodizacao_microcicloId ON sessoes_periodizacao(microcicloId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessoes_periodizacao_treinoId ON sessoes_periodizacao(treinoId)")
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
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}