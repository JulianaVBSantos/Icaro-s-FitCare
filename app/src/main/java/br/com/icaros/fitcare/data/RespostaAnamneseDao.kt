
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RespostaAnamneseDao {

    // Atualiza uma resposta existente, respeitando o vínculo do aluno.
    @Query("""
        UPDATE respostas_anamnese
        SET
            resposta = :resposta,
            dataAtualizacao = :dataAtualizacao
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
    suspend fun atualizarRespostaDoAluno(
        perguntaId: Long,
        alunoId: Long,
        resposta: String,
        dataAtualizacao: String
    ): Int

    // Verifica se já existe uma resposta para a pergunta do aluno.
    @Query("""
        SELECT r.id
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        INNER JOIN anamneses_atribuidas a
            ON p.anamneseAtribuidaId = a.id
        WHERE p.id = :perguntaId
        AND a.alunoId = :alunoId
        AND a.status IN ('PENDENTE', 'EM_ANDAMENTO')
        LIMIT 1
    """)
    suspend fun buscarIdRespostaDoAluno(
        perguntaId: Long,
        alunoId: Long
    ): Long?

    // Insere uma resposta somente se a pergunta pertencer ao aluno
    // e a anamnese ainda permitir respostas.
    @Query("""
        INSERT OR IGNORE INTO respostas_anamnese (
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
    """)
    suspend fun inserirRespostaDoAluno(
        perguntaId: Long,
        alunoId: Long,
        resposta: String,
        dataAtualizacao: String
    ): Long

    // Atualiza ou insere a resposta dentro de uma única transação.
    @Transaction
    suspend fun salvarRespostaDoAluno(
        perguntaId: Long,
        alunoId: Long,
        resposta: String,
        dataAtualizacao: String
    ): Long {
        atualizarRespostaDoAluno(
            perguntaId = perguntaId,
            alunoId = alunoId,
            resposta = resposta,
            dataAtualizacao = dataAtualizacao
        )

        val respostaExistente = buscarIdRespostaDoAluno(
            perguntaId = perguntaId,
            alunoId = alunoId
        )

        if (respostaExistente != null) {
            return respostaExistente
        }

        return inserirRespostaDoAluno(
            perguntaId = perguntaId,
            alunoId = alunoId,
            resposta = resposta,
            dataAtualizacao = dataAtualizacao
        )
    }

    // Busca uma resposta usando o ID local da pergunta.
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

    // Busca a resposta pelo identificador remoto da pergunta.
    @Query("""
        SELECT r.*
        FROM respostas_anamnese r
        INNER JOIN perguntas_anamnese_atribuidas p
            ON r.perguntaAtribuidaId = p.id
        WHERE p.firestoreId = :perguntaFirestoreId
        LIMIT 1
    """)
    suspend fun buscarPorPerguntaFirestoreId(
        perguntaFirestoreId: String
    ): RespostaAnamneseEntity?

    // Atualiza uma resposta recebida do Firestore.
    @Query("""
        UPDATE respostas_anamnese
        SET
            resposta = :resposta,
            dataAtualizacao = :dataAtualizacao
        WHERE perguntaAtribuidaId = (
            SELECT p.id
            FROM perguntas_anamnese_atribuidas p
            WHERE p.firestoreId = :perguntaFirestoreId
            LIMIT 1
        )
    """)
    suspend fun atualizarRespostaSincronizada(
        perguntaFirestoreId: String,
        resposta: String,
        dataAtualizacao: String
    ): Int

    // Insere uma resposta recebida do Firestore.
    @Query("""
        INSERT OR IGNORE INTO respostas_anamnese (
            perguntaAtribuidaId,
            resposta,
            dataAtualizacao
        )
        SELECT
            p.id,
            :resposta,
            :dataAtualizacao
        FROM perguntas_anamnese_atribuidas p
        WHERE p.firestoreId = :perguntaFirestoreId
    """)
    suspend fun inserirRespostaSincronizada(
        perguntaFirestoreId: String,
        resposta: String,
        dataAtualizacao: String
    ): Long

    // Atualiza ou insere a resposta sincronizada.
    @Transaction
    suspend fun salvarRespostaSincronizada(
        perguntaFirestoreId: String,
        resposta: String,
        dataAtualizacao: String
    ): Long {
        atualizarRespostaSincronizada(
            perguntaFirestoreId = perguntaFirestoreId,
            resposta = resposta,
            dataAtualizacao = dataAtualizacao
        )

        val respostaExistente = buscarPorPerguntaFirestoreId(
            perguntaFirestoreId = perguntaFirestoreId
        )

        if (respostaExistente != null) {
            return respostaExistente.id
        }

        return inserirRespostaSincronizada(
            perguntaFirestoreId = perguntaFirestoreId,
            resposta = resposta,
            dataAtualizacao = dataAtualizacao
        )
    }

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
