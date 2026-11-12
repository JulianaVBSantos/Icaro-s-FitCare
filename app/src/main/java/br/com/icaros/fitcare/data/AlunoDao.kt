
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

    @Query("""
        SELECT * FROM alunos
        WHERE professorId = :professorId
        ORDER BY nome ASC
    """)
    fun listarAlunosDoProfessor(
        professorId: Long
    ): Flow<List<AlunoEntity>>

    @Query("""
        SELECT * FROM alunos
        WHERE id = :alunoId
        LIMIT 1
    """)
    fun listarAlunoDoAluno(
        alunoId: Long
    ): Flow<AlunoEntity?>

    @Query("""
        SELECT * FROM alunos
        WHERE id = :id AND professorId = :professorId
        LIMIT 1
    """)
    fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): Flow<AlunoEntity?>

    @Query("""
        SELECT * FROM alunos
        WHERE id = :id AND id = :alunoId
        LIMIT 1
    """)
    fun buscarPorIdDoAluno(
        id: Long,
        alunoId: Long
    ): Flow<AlunoEntity?>

    @Query("""
        SELECT * FROM alunos
        WHERE professorId = :professorId
        AND nome LIKE '%' || :termo || '%'
        ORDER BY nome ASC
    """)
    fun buscarPorNomeDoProfessor(
        termo: String,
        professorId: Long
    ): Flow<List<AlunoEntity>>

    @Query("""
        SELECT * FROM alunos
        WHERE id = :alunoId
        AND nome LIKE '%' || :termo || '%'
        LIMIT 1
    """)
    fun buscarPorNomeDoAluno(
        termo: String,
        alunoId: Long
    ): Flow<List<AlunoEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(aluno: AlunoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirDoFirestore(aluno: AlunoEntity): Long

    @Update
    suspend fun atualizar(aluno: AlunoEntity)

    @Delete
    suspend fun excluir(aluno: AlunoEntity)

    @Query("""
        SELECT COUNT(*) FROM alunos
        WHERE professorId = :professorId
    """)
    suspend fun contarAlunosDoProfessor(
        professorId: Long
    ): Int

    @Query("""
        SELECT * FROM alunos
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorIdDireto(id: Long): AlunoEntity?

    @Query("""
        SELECT * FROM alunos
        WHERE id = :id AND professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarPorIdDoProfessorDireto(
        id: Long,
        professorId: Long
    ): AlunoEntity?

    @Query("""
        SELECT * FROM alunos
        WHERE firebaseId = :firebaseId
        LIMIT 1
    """)
    suspend fun buscarPorFirebaseId(
        firebaseId: String
    ): AlunoEntity?
}