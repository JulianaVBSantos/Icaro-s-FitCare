
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeloAnamneseDao {

    @Insert
    suspend fun inserir(modelo: ModeloAnamneseEntity): Long

    @Update
    suspend fun atualizar(modelo: ModeloAnamneseEntity)

    @Delete
    suspend fun excluir(modelo: ModeloAnamneseEntity)

    // EXCLUSÃO FÍSICA SOMENTE DE MODELOS PADRÃO
    @Query("""
        DELETE FROM modelos_anamnese
        WHERE id = :id
        AND categoria = 'PADRAO'
    """)
    suspend fun excluirModeloPadrao(id: Long): Int

    // EXCLUSÃO LÓGICA DE MODELOS PERSONALIZADOS
    @Query("""
        UPDATE modelos_anamnese
        SET excluido = 1,
            ativo = 0,
            dataAtualizacao = :dataAtualizacao,
            updatedAtMillis = :updatedAtMillis,
            deletedAtMillis = :updatedAtMillis
        WHERE id = :id
          AND professorId = :professorId
          AND categoria = 'PERSONALIZADO'
          AND excluido = 0
    """)
    suspend fun marcarPersonalizadoComoExcluido(
        id: Long,
        professorId: Long,
        dataAtualizacao: String,
        updatedAtMillis: Long
    ): Int

    // LISTAR TODOS OS MODELOS VISÍVEIS
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PADRAO'
        OR (
            categoria = 'PERSONALIZADO'
            AND professorId = :professorId
            AND excluido = 0
        )
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarTodosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // LISTAR MODELOS ATIVOS
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE ativo = 1
        AND (
            categoria = 'PADRAO'
            OR (
                categoria = 'PERSONALIZADO'
                AND professorId = :professorId
                AND excluido = 0
            )
        )
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarAtivosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // LISTAR MODELOS PADRÃO
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PADRAO'
        AND ativo = 1
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarModelosPadrao(): Flow<List<ModeloAnamneseEntity>>

    // LISTAR PERSONALIZADOS NÃO EXCLUÍDOS
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PERSONALIZADO'
        AND professorId = :professorId
        AND excluido = 0
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarPersonalizadosDoProfessor(
        professorId: Long
    ): Flow<List<ModeloAnamneseEntity>>

    // BUSCAR MODELO VISÍVEL
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND (
            categoria = 'PADRAO'
            OR (
                categoria = 'PERSONALIZADO'
                AND professorId = :professorId
                AND excluido = 0
            )
        )
        LIMIT 1
    """)
    suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): ModeloAnamneseEntity?

    // BUSCAR MODELO PADRÃO
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND categoria = 'PADRAO'
        LIMIT 1
    """)
    suspend fun buscarModeloPadrao(
        id: Long
    ): ModeloAnamneseEntity?

    // BUSCAR PERSONALIZADO NÃO EXCLUÍDO
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        AND categoria = 'PERSONALIZADO'
        AND professorId = :professorId
        AND excluido = 0
        LIMIT 1
    """)
    suspend fun buscarPersonalizadoDoProfessor(
        id: Long,
        professorId: Long
    ): ModeloAnamneseEntity?

    // ATUALIZAR STATUS
    @Query("""
        UPDATE modelos_anamnese
        SET ativo = :ativo,
            dataAtualizacao = :dataAtualizacao
        WHERE id = :id
        AND professorId = :professorId
        AND categoria = 'PERSONALIZADO'
        AND excluido = 0
    """)
    suspend fun atualizarStatusPersonalizado(
        id: Long,
        ativo: Boolean,
        dataAtualizacao: String,
        professorId: Long
    )

    // VERIFICAR PROPRIEDADE
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM modelos_anamnese
            WHERE id = :id
            AND categoria = 'PERSONALIZADO'
            AND professorId = :professorId
            AND excluido = 0
        )
    """)
    suspend fun pertenceAoProfessor(
        id: Long,
        professorId: Long
    ): Boolean

    // BUSCAR PELO IDENTIFICADOR FIRESTORE
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE firestoreId = :firestoreId
        LIMIT 1
    """)
    suspend fun buscarPorFirestoreId(
        firestoreId: String
    ): ModeloAnamneseEntity?

    // MODELOS PARA SINCRONIZAÇÃO, INCLUSIVE EXCLUÍDOS
    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE categoria = 'PERSONALIZADO'
        AND professorId = :professorId
        AND firestoreId IS NOT NULL
    """)
    suspend fun listarPersonalizadosParaSincronizacao(
        professorId: Long
    ): List<ModeloAnamneseEntity>

    // ATUALIZAR DATA DE ALTERAÇÃO
    @Query("""
        UPDATE modelos_anamnese
        SET dataAtualizacao = :dataAtualizacao,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :modeloId
        AND professorId = :professorId
        AND categoria = 'PERSONALIZADO'
        AND excluido = 0
    """)
    suspend fun atualizarDataAlteracaoParaSincronizacao(
        modeloId: Long,
        professorId: Long,
        dataAtualizacao: String,
        updatedAtMillis: Long
    )


    @Query("""
    SELECT * FROM modelos_anamnese
    WHERE id = :id
      AND professorId = :professorId
      AND excluido = 0
      AND categoria IN ('PADRAO', 'PERSONALIZADO')
    LIMIT 1
""")
    suspend fun buscarModeloEditavelDoProfessor(
        id: Long,
        professorId: Long
    ): ModeloAnamneseEntity?

    @Query("""
    SELECT * FROM modelos_anamnese
    WHERE professorId = :professorId
      AND firestoreId IS NOT NULL
    ORDER BY id ASC
""")
    suspend fun listarModelosParaSincronizacao(
        professorId: Long
    ): List<ModeloAnamneseEntity>

}
