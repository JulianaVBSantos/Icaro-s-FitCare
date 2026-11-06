
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RespostaAnamneseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(
        resposta: RespostaAnamneseEntity
    ): Long

    @Update
    suspend fun atualizar(
        resposta: RespostaAnamneseEntity
    )

    @Delete
    suspend fun excluir(
        resposta: RespostaAnamneseEntity
    )

    @Query("""
        SELECT * FROM respostas_anamnese
        WHERE perguntaAtribuidaId = :perguntaId
        LIMIT 1
    """)
    suspend fun buscarPorPergunta(
        perguntaId: Long
    ): RespostaAnamneseEntity?

    @Query("""
        SELECT respostas_anamnese.*
        FROM respostas_anamnese
        INNER JOIN perguntas_anamnese_atribuidas
        ON respostas_anamnese.perguntaAtribuidaId =
           perguntas_anamnese_atribuidas.id
        WHERE perguntas_anamnese_atribuidas.anamneseAtribuidaId =
              :anamneseId
        ORDER BY perguntas_anamnese_atribuidas.ordem ASC
    """)
    fun listarPorAnamnese(
        anamneseId: Long
    ): Flow<List<RespostaAnamneseEntity>>

    @Query("""
        SELECT COUNT(*)
        FROM respostas_anamnese
        INNER JOIN perguntas_anamnese_atribuidas
        ON respostas_anamnese.perguntaAtribuidaId =
           perguntas_anamnese_atribuidas.id
        WHERE perguntas_anamnese_atribuidas.anamneseAtribuidaId =
              :anamneseId
        AND TRIM(respostas_anamnese.resposta) != ''
    """)
    suspend fun contarRespostasPreenchidas(
        anamneseId: Long
    ): Int

    @Query("""
        DELETE FROM respostas_anamnese
        WHERE perguntaAtribuidaId = :perguntaId
    """)
    suspend fun excluirPorPergunta(
        perguntaId: Long
    )
}
