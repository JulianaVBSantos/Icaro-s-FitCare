
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RespostaAnamneseDao {

    @Query("""
        INSERT INTO respostas_anamnese (
            perguntaAtribuidaId,
            resposta,
            dataAtualizacao
        )
        SELECT
            :perguntaId,
            :resposta,
            :dataAtualizacao
        WHERE EXISTS (
            SELECT 1
            FROM perguntas_anamnese_atribuidas p
            INNER JOIN anamneses_atribuidas a
                ON p.anamneseAtribuidaId = a.id
            WHERE p.id = :perguntaId
            AND a.alunoId = :alunoId
            AND a.status IN ('PENDENTE', 'EM_ANDAMENTO')
        )
        ON CONFLICT(perguntaAtribuidaId)
        DO UPDATE SET
            resposta = excluded.resposta,
            dataAtualizacao = excluded.dataAtualizacao
    """)
    suspend fun salvarRespostaDoAluno(
        perguntaId: Long,
        alunoId: Long,
        resposta: String,
        dataAtualizacao: String
    ): Long

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        WHERE p.id = :perguntaId
        AND a.alunoId = :alunoId
        LIMIT 1
    """)
    suspend fun buscarPorPerguntaDoAluno(
        perguntaId: Long,
        alunoId: Long
    ): RespostaAnamneseEntity?

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        INNER JOIN alunos al
            ON a.alunoId = al.id
        WHERE p.id = :perguntaId
        AND al.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarPorPerguntaDoProfessor(
        perguntaId: Long,
        professorId: Long
    ): RespostaAnamneseEntity?

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        WHERE a.id = :anamneseId
        AND a.alunoId = :alunoId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    fun listarPorAnamneseDoAluno(
        anamneseId: Long,
        alunoId: Long
    ): Flow<List<RespostaAnamneseEntity>>

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        INNER JOIN alunos al
            ON a.alunoId = al.id
        WHERE a.id = :anamneseId
        AND al.professorId = :professorId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    fun listarPorAnamneseDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): Flow<List<RespostaAnamneseEntity>>

    @Query("""
        SELECT COUNT(*)
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        WHERE a.id = :anamneseId
        AND a.alunoId = :alunoId
        AND TRIM(r.resposta) != ''
    """)
    suspend fun contarRespostasPreenchidasDoAluno(
        anamneseId: Long,
        alunoId: Long
    ): Int

    @Query("""
        SELECT COUNT(*)
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        INNER JOIN alunos al
            ON a.alunoId = al.id
        WHERE a.id = :anamneseId
        AND al.professorId = :professorId
        AND TRIM(r.resposta) != ''
    """)
    suspend fun contarRespostasPreenchidasDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): Int

    @Query("""
        DELETE FROM respostas_anamnese
        WHERE perguntaAtribuidaId = :perguntaId
        AND EXISTS (
            SELECT 1
            FROM perguntas_anamnese_atribuidas p
            INNER JOIN anamneses_atribuidas a
                ON p.anamneseAtribuidaId = a.id
            WHERE p.id = :perguntaId
            AND a.alunoId = :alunoId
            AND a.status IN ('PENDENTE', 'EM_ANDAMENTO')
        )
    """)
    suspend fun excluirPorPerguntaDoAluno(
        perguntaId: Long,
        alunoId: Long
    )

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        WHERE a.id = :anamneseId
        AND a.alunoId = :alunoId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    suspend fun buscarTodasPorAnamneseDoAluno(
        anamneseId: Long,
        alunoId: Long
    ): List<RespostaAnamneseEntity>

    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        INNER JOIN alunos al
            ON a.alunoId = al.id
        WHERE a.id = :anamneseId
        AND al.professorId = :professorId
        ORDER BY p.ordem ASC, p.id ASC
    """)
    suspend fun buscarTodasPorAnamneseDoProfessor(
        anamneseId: Long,
        professorId: Long
    ): List<RespostaAnamneseEntity>
}