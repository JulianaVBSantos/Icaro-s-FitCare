
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PerguntaAnamneseAtribuidaDao {

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM anamneses_atribuidas a
            INNER JOIN alunos al ON al.id = a.alunoId
            WHERE a.id = :anamneseId
            AND al.professorId = :professorId
            AND a.status = 'NAO_ENVIADA'
        )
    """)
    abstract suspend fun anamnesePodeSerEditadaPeloProfessor(
        anamneseId: Long,
        professorId: Long
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun inserirInterno(
        pergunta: PerguntaAnamneseAtribuidaEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun inserirTodasInterno(
        perguntas: List<PerguntaAnamneseAtribuidaEntity>
    ): List<Long>

    // Inserção interna para sincronização.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun inserirParaSincronizacaoInterno(
        pergunta: PerguntaAnamneseAtribuidaEntity
    ): Long

    // Atualiza uma pergunta usando seu identificador remoto.
    @Query("""
        UPDATE perguntas_anamnese_atribuidas
        SET anamneseAtribuidaId = :anamneseAtribuidaId,
            perguntaModeloId = :perguntaModeloId,
            texto = :texto,
            tipoResposta = :tipoResposta,
            opcoes = :opcoes,
            obrigatoria = :obrigatoria,
            ordem = :ordem
        WHERE firestoreId = :firestoreId
    """)
    protected abstract suspend fun atualizarPorFirestoreIdInterno(
        firestoreId: String,
        anamneseAtribuidaId: Long,
        perguntaModeloId: Long?,
        texto: String,
        tipoResposta: String,
        opcoes: String,
        obrigatoria: Boolean,
        ordem: Int
    ): Int

    // Insere ou atualiza sem substituir o registro existente.
    // Isso preserva o ID local usado pela tabela de respostas.
    @Transaction
    open suspend fun inserirOuAtualizarParaSincronizacao(
        pergunta: PerguntaAnamneseAtribuidaEntity
    ): Long {
        require(!pergunta.firestoreId.isNullOrBlank()) {
            "A pergunta precisa possuir um firestoreId."
        }

        inserirParaSincronizacaoInterno(pergunta)

        atualizarPorFirestoreIdInterno(
            firestoreId = pergunta.firestoreId,
            anamneseAtribuidaId = pergunta.anamneseAtribuidaId,
            perguntaModeloId = pergunta.perguntaModeloId,
            texto = pergunta.texto,
            tipoResposta = pergunta.tipoResposta,
            opcoes = pergunta.opcoes,
            obrigatoria = pergunta.obrigatoria,
            ordem = pergunta.ordem
        )

        return buscarPorFirestoreIdInterno(pergunta.firestoreId)?.id
            ?: throw IllegalStateException(
                "Não foi possível salvar a pergunta sincronizada."
            )
    }

    @Query("""
        SELECT *
        FROM perguntas_anamnese_atribuidas
        WHERE firestoreId = :firestoreId
        LIMIT 1
    """)
    protected abstract suspend fun buscarPorFirestoreIdInterno(
        firestoreId: String
    ): PerguntaAnamneseAtribuidaEntity?

    // Busca pública para uso pelo serviço de sincronização.
    @Query("""
        SELECT *
        FROM perguntas_anamnese_atribuidas
        WHERE firestoreId = :firestoreId
        LIMIT 1
    """)
    abstract suspend fun buscarPorFirestoreId(
        firestoreId: String
    ): PerguntaAnamneseAtribuidaEntity?

    // Lista perguntas de uma anamnese para sincronização.
    @Query("""
        SELECT *
        FROM perguntas_anamnese_atribuidas
        WHERE anamneseAtribuidaId = :anamneseId
        ORDER BY ordem ASC, id ASC
    """)
    abstract suspend fun listarParaSincronizacao(
        anamneseId: Long
    ): List<PerguntaAnamneseAtribuidaEntity>

    @Transaction
    open suspend fun inserirDoProfessor(
        pergunta: PerguntaAnamneseAtribuidaEntity,
        professorId: Long
    ): Long {
        require(
            anamnesePodeSerEditadaPeloProfessor(
                pergunta.anamneseAtribuidaId,
                professorId
            )
        ) {
            "Você não tem permissão para adicionar perguntas nesta anamnese."
        }

        return inserirInterno(pergunta)
    }

    @Transaction
    open suspend fun inserirTodasDoProfessor(
        perguntas: List<PerguntaAnamneseAtribuidaEntity>,
        professorId: Long
    ): List<Long> {
        require(perguntas.isNotEmpty()) {
            "Nenhuma pergunta foi informada."
        }

        for (pergunta in perguntas) {
            require(
                anamnesePodeSerEditadaPeloProfessor(
                    pergunta.anamneseAtribuidaId,
                    professorId
                )
            ) {
                "Você não tem permissão para adicionar perguntas nesta anamnese."
            }
        }

        return inserirTodasInterno(perguntas)
    }

    @Query("""
        UPDATE perguntas_anamnese_atribuidas
        SET texto = :texto,
            tipoResposta = :tipoResposta,
            opcoes = :opcoes,
            obrigatoria = :obrigatoria,
            ordem = :ordem
        WHERE id = :perguntaId
        AND EXISTS (
            SELECT 1
            FROM anamneses_atribuidas a
            INNER JOIN alunos al ON al.id = a.alunoId
            WHERE a.id = perguntas_anamnese_atribuidas.anamneseAtribuidaId
            AND al.professorId = :professorId
            AND a.status = 'NAO_ENVIADA'
        )
    """)
    abstract suspend fun atualizarDoProfessor(
        perguntaId: Long,
        texto: String,
        tipoResposta: String,
        opcoes: String,
        obrigatoria: Boolean,
        ordem: Int,
        professorId: Long
    ): Int

    @Query("""
        DELETE FROM perguntas_anamnese_atribuidas
        WHERE id = :perguntaId
        AND EXISTS (
            SELECT 1
            FROM anamneses_atribuidas a
            INNER JOIN alunos al ON al.id = a.alunoId
            WHERE a.id = perguntas_anamnese_atribuidas.anamneseAtribuidaId
            AND al.professorId = :professorId
            AND a.status = 'NAO_ENVIADA'
        )
    """)
    abstract suspend fun excluirDoProfessor(
        perguntaId: Long,
        professorId: Long
    ): Int

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.id = :anamneseId
        AND al.professorId = :professorId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    abstract fun listarPorAnamneseDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): Flow<List<PerguntaAnamneseAtribuidaEntity>>

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        WHERE a.id = :anamneseId
        AND a.alunoId = :alunoId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    abstract fun listarPorAnamneseDoAluno(
        anamneseId: Long,
        alunoId: Long
    ): Flow<List<PerguntaAnamneseAtribuidaEntity>>

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.id = :anamneseId
        AND al.professorId = :professorId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    abstract suspend fun buscarPorAnamneseDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): List<PerguntaAnamneseAtribuidaEntity>

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        WHERE a.id = :anamneseId
        AND a.alunoId = :alunoId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    abstract suspend fun buscarPorAnamneseDoAluno(
        anamneseId: Long,
        alunoId: Long
    ): List<PerguntaAnamneseAtribuidaEntity>

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE p.id = :id
        AND al.professorId = :professorId
        LIMIT 1
    """)
    abstract suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): PerguntaAnamneseAtribuidaEntity?

    @Query("""
        SELECT p.*
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
        WHERE p.id = :id
        AND a.alunoId = :alunoId
        LIMIT 1
    """)
    abstract suspend fun buscarPorIdDoAluno(
        id: Long,
        alunoId: Long
    ): PerguntaAnamneseAtribuidaEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM perguntas_anamnese_atribuidas p
            INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
            INNER JOIN alunos al ON al.id = a.alunoId
            WHERE p.id = :perguntaId
            AND al.professorId = :professorId
        )
    """)
    abstract suspend fun pertenceAoProfessor(
        perguntaId: Long,
        professorId: Long
    ): Boolean

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM perguntas_anamnese_atribuidas p
            INNER JOIN anamneses_atribuidas a ON a.id = p.anamneseAtribuidaId
            WHERE p.id = :perguntaId
            AND a.alunoId = :alunoId
        )
    """)
    abstract suspend fun pertenceAoAluno(
        perguntaId: Long,
        alunoId: Long
    ): Boolean

    @Query("""
        DELETE FROM perguntas_anamnese_atribuidas
        WHERE anamneseAtribuidaId = :anamneseId
        AND EXISTS (
            SELECT 1
            FROM anamneses_atribuidas a
            INNER JOIN alunos al ON al.id = a.alunoId
            WHERE a.id = :anamneseId
            AND al.professorId = :professorId
            AND a.status = 'NAO_ENVIADA'
        )
    """)
    abstract suspend fun excluirTodasDaAnamneseDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): Int
    @Query("""
    UPDATE perguntas_anamnese_atribuidas
    SET firestoreId = :firestoreId
    WHERE id = :id
      AND (firestoreId IS NULL OR firestoreId = '')
""")
    abstract suspend fun atribuirFirestoreId(
        id: Long,
        firestoreId: String
    ): Int
}