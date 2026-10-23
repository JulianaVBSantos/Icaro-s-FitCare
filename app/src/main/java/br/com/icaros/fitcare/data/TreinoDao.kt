
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TreinoDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirTreino(treino: TreinoEntity): Long

    @Update
    suspend fun atualizarTreino(treino: TreinoEntity)

    @Delete
    suspend fun excluirTreino(treino: TreinoEntity)

    @Query(
        "SELECT * FROM treinos WHERE alunoId = :alunoId ORDER BY id DESC"
    )
    fun listarTreinosDoAluno(alunoId: Long): Flow<List<TreinoEntity>>

    @Query("SELECT * FROM treinos ORDER BY id DESC")
    fun listarTodosTreinos(): Flow<List<TreinoEntity>>

    @Query("SELECT * FROM treinos WHERE id = :treinoId LIMIT 1")
    suspend fun buscarTreinoPorId(treinoId: Long): TreinoEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirExercicio(exercicio: ExercicioEntity): Long

    @Update
    suspend fun atualizarExercicio(exercicio: ExercicioEntity)

    @Delete
    suspend fun excluirExercicio(exercicio: ExercicioEntity)

    @Query(
        "SELECT * FROM exercicios WHERE treinoId = :treinoId ORDER BY ordem ASC, id ASC"
    )
    fun listarExercicios(treinoId: Long): Flow<List<ExercicioEntity>>

    @Query("DELETE FROM exercicios WHERE treinoId = :treinoId")
    suspend fun excluirExerciciosDoTreino(treinoId: Long)
}