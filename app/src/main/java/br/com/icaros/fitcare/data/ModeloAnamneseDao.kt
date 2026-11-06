
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

    @Query("""
        SELECT * FROM modelos_anamnese
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarTodos(): Flow<List<ModeloAnamneseEntity>>

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE ativo = 1
        ORDER BY nome COLLATE NOCASE ASC
    """)
    fun listarAtivos(): Flow<List<ModeloAnamneseEntity>>

    @Query("""
        SELECT * FROM modelos_anamnese
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(id: Long): ModeloAnamneseEntity?

    @Query("""
        UPDATE modelos_anamnese
        SET ativo = :ativo,
            dataAtualizacao = :dataAtualizacao
        WHERE id = :id
    """)
    suspend fun atualizarStatus(
        id: Long,
        ativo: Boolean,
        dataAtualizacao: String
    )
}
