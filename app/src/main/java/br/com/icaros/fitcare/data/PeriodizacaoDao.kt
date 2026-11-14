package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodizacaoDao {

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM alunos
            WHERE id = :alunoId AND professorId = :professorId
        )
    """)
    suspend fun alunoPertenceAoProfessor(alunoId: Long, professorId: Long): Boolean

    // MACROCICLOS
    @Query("""
        SELECT macrociclos.* FROM macrociclos
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE macrociclos.alunoId = :alunoId AND alunos.professorId = :professorId
        ORDER BY macrociclos.id DESC
    """)
    fun listarMacrociclosDoAluno(alunoId: Long, professorId: Long): Flow<List<MacrocicloEntity>>

    @Query("""
        SELECT macrociclos.* FROM macrociclos
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE macrociclos.id = :id AND alunos.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarMacrocicloPorId(id: Long, professorId: Long): MacrocicloEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirMacrociclo(macrociclo: MacrocicloEntity): Long

    @Update
    suspend fun atualizarMacrociclo(macrociclo: MacrocicloEntity)

    @Delete
    suspend fun excluirMacrociclo(macrociclo: MacrocicloEntity)

    // MESOCICLOS
    @Query("""
        SELECT mesociclos.* FROM mesociclos
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE mesociclos.macrocicloId = :macrocicloId AND alunos.professorId = :professorId
        ORDER BY mesociclos.ordem ASC, mesociclos.id ASC
    """)
    fun listarMesociclos(macrocicloId: Long, professorId: Long): Flow<List<MesocicloEntity>>

    @Query("""
        SELECT mesociclos.* FROM mesociclos
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE mesociclos.id = :id AND alunos.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarMesocicloPorId(id: Long, professorId: Long): MesocicloEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirMesociclo(mesociclo: MesocicloEntity): Long

    @Update
    suspend fun atualizarMesociclo(mesociclo: MesocicloEntity)

    @Delete
    suspend fun excluirMesociclo(mesociclo: MesocicloEntity)

    // MICROCICLOS
    @Query("""
        SELECT microciclos.* FROM microciclos
        INNER JOIN mesociclos ON mesociclos.id = microciclos.mesocicloId
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE microciclos.mesocicloId = :mesocicloId AND alunos.professorId = :professorId
        ORDER BY microciclos.ordem ASC, microciclos.id ASC
    """)
    fun listarMicrociclos(mesocicloId: Long, professorId: Long): Flow<List<MicrocicloEntity>>

    @Query("""
        SELECT microciclos.* FROM microciclos
        INNER JOIN mesociclos ON mesociclos.id = microciclos.mesocicloId
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE microciclos.id = :id AND alunos.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarMicrocicloPorId(id: Long, professorId: Long): MicrocicloEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirMicrociclo(microciclo: MicrocicloEntity): Long

    @Update
    suspend fun atualizarMicrociclo(microciclo: MicrocicloEntity)

    @Delete
    suspend fun excluirMicrociclo(microciclo: MicrocicloEntity)

    // SESSÕES DE PERIODIZAÇÃO
    @Query("""
        SELECT sessoes_periodizacao.* FROM sessoes_periodizacao
        INNER JOIN microciclos ON microciclos.id = sessoes_periodizacao.microcicloId
        INNER JOIN mesociclos ON mesociclos.id = microciclos.mesocicloId
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE sessoes_periodizacao.microcicloId = :microcicloId AND alunos.professorId = :professorId
        ORDER BY sessoes_periodizacao.ordem ASC, sessoes_periodizacao.id ASC
    """)
    fun listarSessoes(microcicloId: Long, professorId: Long): Flow<List<SessaoPeriodizacaoEntity>>

    @Query("""
        SELECT sessoes_periodizacao.* FROM sessoes_periodizacao
        INNER JOIN microciclos ON microciclos.id = sessoes_periodizacao.microcicloId
        INNER JOIN mesociclos ON mesociclos.id = microciclos.mesocicloId
        INNER JOIN macrociclos ON macrociclos.id = mesociclos.macrocicloId
        INNER JOIN alunos ON alunos.id = macrociclos.alunoId
        WHERE sessoes_periodizacao.id = :id AND alunos.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarSessaoPorId(id: Long, professorId: Long): SessaoPeriodizacaoEntity?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM treinos
            INNER JOIN alunos ON alunos.id = treinos.alunoId
            WHERE treinos.id = :treinoId
            AND treinos.alunoId = :alunoId
            AND alunos.professorId = :professorId
        )
    """)
    suspend fun treinoPertenceAoAlunoEProfessor(
        treinoId: Long,
        alunoId: Long,
        professorId: Long
    ): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserirSessao(sessao: SessaoPeriodizacaoEntity): Long

    @Update
    suspend fun atualizarSessao(sessao: SessaoPeriodizacaoEntity)

    @Delete
    suspend fun excluirSessao(sessao: SessaoPeriodizacaoEntity)
}