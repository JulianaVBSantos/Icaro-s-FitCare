
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PerguntaModeloAnamneseDao {

    // INSERIR
    @Insert
    suspend fun inserir(
        pergunta: PerguntaModeloAnamneseEntity
    ): Long

    @Insert
    suspend fun inserirTodas(
        perguntas: List<PerguntaModeloAnamneseEntity>
    ): List<Long>

    // ATUALIZAR
    @Update
    suspend fun atualizar(
        pergunta: PerguntaModeloAnamneseEntity
    )

    @Update
    suspend fun atualizarTodas(
        perguntas: List<PerguntaModeloAnamneseEntity>
    )

    // EXCLUIR
    @Delete
    suspend fun excluir(
        pergunta: PerguntaModeloAnamneseEntity
    )

    @Query("""
        DELETE FROM perguntas_modelo_anamnese
        WHERE modeloId = :modeloId
        AND EXISTS (
            SELECT 1
            FROM modelos_anamnese
            WHERE modelos_anamnese.id = :modeloId
            AND modelos_anamnese.categoria = 'PERSONALIZADO'
            AND modelos_anamnese.professorId = :professorId
        )
    """)
    suspend fun excluirTodasDoModeloDoProfessor(
        modeloId: Long,
        professorId: Long
    )

    // LISTAR PERGUNTAS
    @Query("""
        SELECT perguntas_modelo_anamnese.*
        FROM perguntas_modelo_anamnese
        INNER JOIN modelos_anamnese
            ON modelos_anamnese.id = perguntas_modelo_anamnese.modeloId
        WHERE perguntas_modelo_anamnese.modeloId = :modeloId
        AND (
            modelos_anamnese.categoria = 'PADRAO'
            OR (
                modelos_anamnese.categoria = 'PERSONALIZADO'
                AND modelos_anamnese.professorId = :professorId
            )
        )
        ORDER BY perguntas_modelo_anamnese.ordem ASC,
                 perguntas_modelo_anamnese.id ASC
    """)
    fun listarPorModeloDoProfessor(
        modeloId: Long,
        professorId: Long
    ): Flow<List<PerguntaModeloAnamneseEntity>>

    @Query("""
        SELECT perguntas_modelo_anamnese.*
        FROM perguntas_modelo_anamnese
        INNER JOIN modelos_anamnese
            ON modelos_anamnese.id = perguntas_modelo_anamnese.modeloId
        WHERE perguntas_modelo_anamnese.modeloId = :modeloId
        AND (
            modelos_anamnese.categoria = 'PADRAO'
            OR (
                modelos_anamnese.categoria = 'PERSONALIZADO'
                AND modelos_anamnese.professorId = :professorId
            )
        )
        ORDER BY perguntas_modelo_anamnese.ordem ASC,
                 perguntas_modelo_anamnese.id ASC
    """)
    suspend fun buscarPorModeloDoProfessor(
        modeloId: Long,
        professorId: Long
    ): List<PerguntaModeloAnamneseEntity>

    // BUSCAR PERGUNTA POR ID
    @Query("""
        SELECT perguntas_modelo_anamnese.*
        FROM perguntas_modelo_anamnese
        INNER JOIN modelos_anamnese
            ON modelos_anamnese.id = perguntas_modelo_anamnese.modeloId
        WHERE perguntas_modelo_anamnese.id = :id
        AND (
            modelos_anamnese.categoria = 'PADRAO'
            OR (
                modelos_anamnese.categoria = 'PERSONALIZADO'
                AND modelos_anamnese.professorId = :professorId
            )
        )
        LIMIT 1
    """)
    suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): PerguntaModeloAnamneseEntity?

    // OPERAÇÕES INTERNAS
    @Query("""
        DELETE FROM perguntas_modelo_anamnese
        WHERE modeloId = :modeloId
    """)
    suspend fun excluirTodasDoModelo(
        modeloId: Long
    )

    @Query("""
    UPDATE modelos_anamnese
    SET dataAtualizacao = :dataAtualizacao,
        updatedAtMillis = :updatedAtMillis
    WHERE id = :modeloId
      AND professorId = :professorId
      AND categoria = 'PERSONALIZADO'
      AND excluido = 0
""")
    suspend fun marcarModeloAlterado(
        modeloId: Long,
        professorId: Long,
        dataAtualizacao: String,
        updatedAtMillis: Long
    ): Int
}
