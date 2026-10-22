package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlunoDao {

    @Query("SELECT * FROM alunos ORDER BY nome ASC")
    fun listarAlunos(): Flow<List<AlunoEntity>>

    @Query("SELECT * FROM alunos WHERE id = :id LIMIT 1")
    fun buscarPorId(id: Long): Flow<AlunoEntity?>

    @Query("""
        SELECT * FROM alunos
        WHERE nome LIKE '%' || :termo || '%'
        ORDER BY nome ASC
    """)
    fun buscarPorNome(termo: String): Flow<List<AlunoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(aluno: AlunoEntity): Long

    @Update
    suspend fun atualizar(aluno: AlunoEntity)

    @Delete
    suspend fun excluir(aluno: AlunoEntity)

    @Query("SELECT COUNT(*) FROM alunos")
    suspend fun contarAlunos(): Int
}