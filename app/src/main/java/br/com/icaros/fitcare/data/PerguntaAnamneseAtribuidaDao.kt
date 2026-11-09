
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PerguntaAnamneseAtribuidaDao {

    @Insert
    suspend fun inserir(
        pergunta: PerguntaAnamneseAtribuidaEntity
    ): Long

    @Insert
    suspend fun inserirTodas(
        perguntas: List<PerguntaAnamneseAtribuidaEntity>
    ): List<Long>


    @Update
    suspend fun atualizar(
        pergunta: PerguntaAnamneseAtribuidaEntity
    )

    @Delete
    suspend fun excluir(
        pergunta: PerguntaAnamneseAtribuidaEntity
    )

    @Query("""
        SELECT * FROM perguntas_anamnese_atribuidas
        WHERE anamneseAtribuidaId = :anamneseId
        ORDER BY ordem ASC, id ASC
    """)
    fun listarPorAnamnese(
        anamneseId: Long
    ): Flow<List<PerguntaAnamneseAtribuidaEntity>>

    @Query("""
        SELECT * FROM perguntas_anamnese_atribuidas
        WHERE anamneseAtribuidaId = :anamneseId
        ORDER BY ordem ASC, id ASC
    """)
    suspend fun buscarPorAnamnese(
        anamneseId: Long
    ): List<PerguntaAnamneseAtribuidaEntity>

    @Query("""
        SELECT * FROM perguntas_anamnese_atribuidas
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(
        id: Long
    ): PerguntaAnamneseAtribuidaEntity?

    @Query("""
        DELETE FROM perguntas_anamnese_atribuidas
        WHERE anamneseAtribuidaId = :anamneseId
    """)
    suspend fun excluirTodasDaAnamnese(
        anamneseId: Long
    )

    @Query("""
    SELECT EXISTS(
        SELECT 1
        FROM perguntas_anamnese_atribuidas p
        INNER JOIN anamneses_atribuidas a
            ON a.id = p.anamneseAtribuidaId
        WHERE p.id = :perguntaId
        AND a.alunoId = :alunoId
    )
""")
    suspend fun pertenceAoAluno(
        perguntaId: Long,
        alunoId: Long
    ): Boolean
}
