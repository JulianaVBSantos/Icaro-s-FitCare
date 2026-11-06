
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnamneseAtribuidaDao {

    @Insert
    suspend fun inserir(
        anamnese: AnamneseAtribuidaEntity
    ): Long

    @Update
    suspend fun atualizar(
        anamnese: AnamneseAtribuidaEntity
    )

    @Delete
    suspend fun excluir(
        anamnese: AnamneseAtribuidaEntity
    )

    @Query("""
        SELECT * FROM anamneses_atribuidas
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    fun listarPorAluno(
        alunoId: Long
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        SELECT * FROM anamneses_atribuidas
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(
        id: Long
    ): AnamneseAtribuidaEntity?

    @Query("""
        SELECT * FROM anamneses_atribuidas
        WHERE status = :status
        ORDER BY id DESC
    """)
    fun listarPorStatus(
        status: String
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = :status,
            dataEnvio = :dataEnvio
        WHERE id = :id
    """)
    suspend fun atualizarEnvio(
        id: Long,
        status: String,
        dataEnvio: String
    )

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = :status,
            dataInicioResposta = :dataInicioResposta
        WHERE id = :id
    """)
    suspend fun atualizarInicioResposta(
        id: Long,
        status: String,
        dataInicioResposta: String
    )

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = :status,
            dataResposta = :dataResposta
        WHERE id = :id
    """)
    suspend fun atualizarConclusao(
        id: Long,
        status: String,
        dataResposta: String
    )
}
