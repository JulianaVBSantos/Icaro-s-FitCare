
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncOperationDao {

    // Adiciona uma operação à fila de sincronização.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(operacao: SyncOperationEntity): Long

    // Busca todas as operações pendentes, da mais antiga para a mais recente.
    @Query("""
        SELECT * FROM sync_operations
        ORDER BY createdAt ASC, id ASC
    """)
    suspend fun listarPendentes(): List<SyncOperationEntity>

    // Busca operações referentes a um registro específico.
    @Query("""
        SELECT * FROM sync_operations
        WHERE entityType = :entityType
        AND localEntityId = :localEntityId
        ORDER BY createdAt ASC, id ASC
    """)
    suspend fun buscarPorRegistro(
        entityType: String,
        localEntityId: Long
    ): List<SyncOperationEntity>

    // Atualiza a quantidade de tentativas e registra o último erro.
    @Query("""
        UPDATE sync_operations
        SET attempts = attempts + 1,
            lastError = :erro
        WHERE id = :operacaoId
    """)
    suspend fun registrarFalha(
        operacaoId: Long,
        erro: String?
    )

    // Remove uma operação após a sincronização bem-sucedida.
    @Query("""
        DELETE FROM sync_operations
        WHERE id = :operacaoId
    """)
    suspend fun remover(operacaoId: Long)

    // Remove todas as operações de um registro específico.
    @Query("""
        DELETE FROM sync_operations
        WHERE entityType = :entityType
        AND localEntityId = :localEntityId
    """)
    suspend fun removerPorRegistro(
        entityType: String,
        localEntityId: Long
    )

    // Conta quantas operações aguardam sincronização.
    @Query("""
        SELECT COUNT(*) FROM sync_operations
    """)
    suspend fun contarPendentes(): Int
}
