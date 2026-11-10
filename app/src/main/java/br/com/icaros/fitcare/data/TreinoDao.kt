
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

    // TREINOS DO PROFESSOR

    @Query("""
        SELECT treinos.*
        FROM treinos
        INNER JOIN alunos ON alunos.id = treinos.alunoId
        WHERE alunos.professorId = :professorId
        ORDER BY treinos.id DESC
    """)
    fun listarTreinosDoProfessor(
        professorId: Long
    ): Flow<List<TreinoEntity>>

    @Query("""
        SELECT treinos.*
        FROM treinos
        INNER JOIN alunos ON alunos.id = treinos.alunoId
        WHERE alunos.professorId = :professorId
        AND treinos.alunoId = :alunoId
        ORDER BY treinos.id DESC
    """)
    fun listarTreinosDoAlunoPeloProfessor(
        alunoId: Long,
        professorId: Long
    ): Flow<List<TreinoEntity>>

    // TREINOS DO ALUNO AUTENTICADO

    @Query("""
        SELECT * FROM treinos
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    fun listarTreinosDoAluno(
        alunoId: Long
    ): Flow<List<TreinoEntity>>

    // CONSULTA E VALIDAÇÃO DE PROPRIEDADE

    @Query("""
        SELECT treinos.*
        FROM treinos
        INNER JOIN alunos ON alunos.id = treinos.alunoId
        WHERE treinos.id = :treinoId
        AND alunos.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarTreinoDoProfessor(
        treinoId: Long,
        professorId: Long
    ): TreinoEntity?

    @Query("""
        SELECT * FROM treinos
        WHERE id = :treinoId
        AND alunoId = :alunoId
        LIMIT 1
    """)
    suspend fun buscarTreinoDoAluno(
        treinoId: Long,
        alunoId: Long
    ): TreinoEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM treinos
            INNER JOIN alunos ON alunos.id = treinos.alunoId
            WHERE treinos.id = :treinoId
            AND alunos.professorId = :professorId
        )
    """)
    suspend fun treinoPertenceAoProfessor(
        treinoId: Long,
        professorId: Long
    ): Boolean

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM treinos
            WHERE id = :treinoId
            AND alunoId = :alunoId
        )
    """)
    suspend fun treinoPertenceAoAluno(
        treinoId: Long,
        alunoId: Long
    ): Boolean

    // OPERAÇÕES DE TREINO

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirTreino(treino: TreinoEntity): Long

    @Update
    suspend fun atualizarTreino(treino: TreinoEntity)

    @Delete
    suspend fun excluirTreino(treino: TreinoEntity)

    // EXERCÍCIOS

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirExercicio(
        exercicio: ExercicioEntity
    ): Long

    @Update
    suspend fun atualizarExercicio(
        exercicio: ExercicioEntity
    )

    @Delete
    suspend fun excluirExercicio(
        exercicio: ExercicioEntity
    )

    @Query("""
        SELECT exercicios.*
        FROM exercicios
        INNER JOIN treinos ON treinos.id = exercicios.treinoId
        INNER JOIN alunos ON alunos.id = treinos.alunoId
        WHERE treinos.id = :treinoId
        AND alunos.professorId = :professorId
        ORDER BY exercicios.ordem ASC, exercicios.id ASC
    """)
    fun listarExerciciosDoProfessor(
        treinoId: Long,
        professorId: Long
    ): Flow<List<ExercicioEntity>>

    @Query("""
        SELECT exercicios.*
        FROM exercicios
        INNER JOIN treinos ON treinos.id = exercicios.treinoId
        WHERE treinos.id = :treinoId
        AND treinos.alunoId = :alunoId
        ORDER BY exercicios.ordem ASC, exercicios.id ASC
    """)
    fun listarExerciciosDoAluno(
        treinoId: Long,
        alunoId: Long
    ): Flow<List<ExercicioEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM exercicios
            INNER JOIN treinos ON treinos.id = exercicios.treinoId
            INNER JOIN alunos ON alunos.id = treinos.alunoId
            WHERE exercicios.id = :exercicioId
            AND alunos.professorId = :professorId
        )
    """)
    suspend fun exercicioPertenceAoProfessor(
        exercicioId: Long,
        professorId: Long
    ): Boolean

    @Query("""
        SELECT treinoId FROM exercicios
        WHERE id = :exercicioId
        LIMIT 1
    """)
    suspend fun buscarTreinoIdDoExercicio(
        exercicioId: Long
    ): Long?

    @Query("""
        DELETE FROM exercicios
        WHERE treinoId = :treinoId
        AND EXISTS(
            SELECT 1
            FROM treinos
            INNER JOIN alunos ON alunos.id = treinos.alunoId
            WHERE treinos.id = :treinoId
            AND alunos.professorId = :professorId
        )
    """)
    suspend fun excluirExerciciosDoTreino(
        treinoId: Long,
        professorId: Long
    )
    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM alunos
        WHERE id = :alunoId
        AND professorId = :professorId
    )
""")
    suspend fun alunoPertenceAoProfessor(
        alunoId: Long,
        professorId: Long
    ): Boolean
}