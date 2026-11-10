
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeloAnamneseDao {

    // INSERIR
    @Insert
    suspend fun inserir(
        modelo: ModeloAnamneseEntity
    ): Long

    // ATUALIZAR

    @Update
    suspend fun atualizar(
        modelo: ModeloAnamneseEntity
    )

    // EXCLUIR
    @Delete
    suspend fun excluir(
        modelo: ModeloAnamneseEntity
    )

    @Query("""
        DELETE FROM modelos_anamnese
        WHERE id = :id
        AND professorId = :professorId
        AND categoria = 'PERSONALIZADO'
    """)
    suspend fun excluirPersonalizado(
        id: Long,
        professorId: Long
    )

    // LISTAR TODOS OS MODELOS VISÍVEIS AO PROFESSOR

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PADRAO'
        OR (
            categoria = 'PERSONALIZADO'
            AND professorId = :professorId
        )
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarTodosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // LISTAR MODELOS ATIVOS DO PROFESSOR
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE ativo = 1
        AND (
            categoria = 'PADRAO'
            OR (
                categoria = 'PERSONALIZADO'
                AND professorId = :professorId
            )
        )
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarAtivosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // =====================================================
    // LISTAR MODELOS PADRÃO
    // =====================================================

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PADRAO'
        AND ativo = 1
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarModelosPadrao(): Flow<List<ModeloAnamneseEntity>>

    // =====================================================
    // LISTAR MODELOS PERSONALIZADOS DO PROFESSOR
    // =====================================================

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PERSONALIZADO'
        AND professorId = :professorId
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarPersonalizadosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // =====================================================
    // BUSCAR MODELO VISÍVEL AO PROFESSOR
    // =====================================================

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND (
            categoria = 'PADRAO'
            OR (
                categoria = 'PERSONALIZADO'
                AND professorId = :professorId
            )
        )
        LIMIT 1
    """)
    suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): ModeloAnamneseEntity?

    // =====================================================
    // BUSCAR MODELO PADRÃO
    // =====================================================

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND categoria = 'PADRAO'
        LIMIT 1
    """)
    suspend fun buscarModeloPadrao(
        id: Long
    ): ModeloAnamneseEntity?

    // =====================================================
    // BUSCAR MODELO PERSONALIZADO DO PROFESSOR
    // =====================================================

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND categoria = 'PERSONALIZADO'
        AND professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarPersonalizadoDoProfessor(
        id: Long,
        professorId: Long
    ): ModeloAnamneseEntity?

    // =====================================================
    // ATUALIZAR STATUS DE MODELO PERSONALIZADO
    // =====================================================

    @Query("""
        UPDATE modelos_anamnese
        SET ativo = :ativo,
            dataAtualizacao = :dataAtualizacao
        WHERE id = :id
        AND professorId = :professorId
        AND categoria = 'PERSONALIZADO'
    """)
    suspend fun atualizarStatusPersonalizado(
        id: Long,
        ativo: Boolean,
        dataAtualizacao: String,
        professorId: Long
    )

    // VERIFICAR PROPRIEDADE DO MODELO

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM modelos_anamnese
            WHERE id = :id
            AND categoria = 'PERSONALIZADO'
            AND professorId = :professorId
        )
    """)
    suspend fun pertenceAoProfessor(
        id: Long,
        professorId: Long
    ): Boolean
}
