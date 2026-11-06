
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PerguntaModeloAnamneseDao {

    @Insert
    suspend fun inserir(
        pergunta: PerguntaModeloAnamneseEntity
    ): Long

    @Insert
    suspend fun inserirTodas(
        perguntas: List<PerguntaModeloAnamneseEntity>
    ): List<Long>

    @Update
    suspend fun atualizar(
        pergunta: PerguntaModeloAnamneseEntity
    )

    @Update
    suspend fun atualizarTodas(
        perguntas: List<PerguntaModeloAnamneseEntity>
    )

    @Delete
    suspend fun excluir(
        pergunta: PerguntaModeloAnamneseEntity
    )

    @Query("""
        SELECT * FROM perguntas_modelo_anamnese
        WHERE modeloId = :modeloId
        ORDER BY ordem ASC, id ASC
    """)
    fun listarPorModelo(
        modeloId: Long
    ): Flow<List<PerguntaModeloAnamneseEntity>>

    @Query("""
        SELECT * FROM perguntas_modelo_anamnese
        WHERE modeloId = :modeloId
        ORDER BY ordem ASC, id ASC
    """)
    suspend fun buscarPorModelo(
        modeloId: Long
    ): List<PerguntaModeloAnamneseEntity>

    @Query("""
        SELECT * FROM perguntas_modelo_anamnese
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(
        id: Long
    ): PerguntaModeloAnamneseEntity?

    @Query("""
        DELETE FROM perguntas_modelo_anamnese
        WHERE modeloId = :modeloId
    """)
    suspend fun excluirTodasDoModelo(modeloId: Long)
}
