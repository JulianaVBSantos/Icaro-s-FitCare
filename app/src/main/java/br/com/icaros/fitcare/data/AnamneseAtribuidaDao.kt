
package br.com.icaros.fitcare.data

import androidx.room.OnConflictStrategy
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AnamneseAtribuidaDao {

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM alunos
            WHERE id = :alunoId
            AND professorId = :professorId
        )
    """)
    abstract suspend fun alunoPertenceAoProfessor(
        alunoId: Long,
        professorId: Long
    ): Boolean

    @Insert
    protected abstract suspend fun inserirInterno(
        anamnese: AnamneseAtribuidaEntity
    ): Long

    @Transaction
    open suspend fun inserirDoProfessor(
        anamnese: AnamneseAtribuidaEntity,
        professorId: Long
    ): Long {
        require(anamnese.alunoId > 0) {
            "Aluno inválido."
        }

        require(anamnese.status == "NAO_ENVIADA") {
            "A anamnese deve ser criada com status não enviada."
        }

        require(
            alunoPertenceAoProfessor(
                anamnese.alunoId,
                professorId
            )
        ) {
            "Este aluno não pertence ao seu cadastro."
        }

        return inserirInterno(anamnese)
    }

    @Query("""
    DELETE FROM anamneses_atribuidas
    WHERE id = :id
    AND status = 'PENDENTE'
    AND alunoId IN (
        SELECT id
        FROM alunos
        WHERE professorId = :professorId
    )
""")
    abstract suspend fun excluirDoProfessor(
        id: Long,
        professorId: Long
    ): Int

    @Query("""
        DELETE FROM anamneses_atribuidas
        WHERE id = :id
        AND alunoId = :alunoId
    """)
    abstract suspend fun excluirDoAluno(
        id: Long,
        alunoId: Long
    ): Int

    @Query("""
        SELECT aa.*
        FROM anamneses_atribuidas aa
        INNER JOIN alunos a ON a.id = aa.alunoId
        WHERE aa.alunoId = :alunoId
        AND a.professorId = :professorId
        ORDER BY aa.id DESC
    """)
    abstract fun listarPorAlunoDoProfessor(
        alunoId: Long,
        professorId: Long
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        SELECT *
        FROM anamneses_atribuidas
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    abstract fun listarPorAluno(
        alunoId: Long
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        SELECT aa.*
        FROM anamneses_atribuidas aa
        INNER JOIN alunos a ON a.id = aa.alunoId
        WHERE aa.id = :id
        AND a.professorId = :professorId
        LIMIT 1
    """)
    abstract suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): AnamneseAtribuidaEntity?

    @Query("""
        SELECT *
        FROM anamneses_atribuidas
        WHERE id = :id
        AND alunoId = :alunoId
        LIMIT 1
    """)
    abstract suspend fun buscarPorIdDoAluno(
        id: Long,
        alunoId: Long
    ): AnamneseAtribuidaEntity?

    @Query("""
        SELECT aa.*
        FROM anamneses_atribuidas aa
        INNER JOIN alunos a ON a.id = aa.alunoId
        WHERE aa.status = :status
        AND a.professorId = :professorId
        ORDER BY aa.id DESC
    """)
    abstract fun listarPorStatusDoProfessor(
        status: String,
        professorId: Long
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        SELECT *
        FROM anamneses_atribuidas
        WHERE status = :status
        AND alunoId = :alunoId
        ORDER BY id DESC
    """)
    abstract fun listarPorStatusDoAluno(
        status: String,
        alunoId: Long
    ): Flow<List<AnamneseAtribuidaEntity>>

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = 'PENDENTE',
            dataEnvio = :dataEnvio
        WHERE id = :id
        AND :status = 'PENDENTE'
        AND status = 'NAO_ENVIADA'
        AND alunoId IN (
            SELECT id
            FROM alunos
            WHERE professorId = :professorId
        )
    """)
    abstract suspend fun atualizarEnvioDoProfessor(
        id: Long,
        status: String,
        dataEnvio: String,
        professorId: Long
    ): Int

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = 'EM_ANDAMENTO',
            dataInicioResposta = CASE
                WHEN dataInicioResposta = '' THEN :dataInicioResposta
                ELSE dataInicioResposta
            END
        WHERE id = :id
        AND :status = 'EM_ANDAMENTO'
        AND status IN ('PENDENTE', 'EM_ANDAMENTO')
        AND alunoId = :alunoId
    """)
    abstract suspend fun atualizarInicioRespostaDoAluno(
        id: Long,
        status: String,
        dataInicioResposta: String,
        alunoId: Long
    ): Int

    @Query("""
        UPDATE anamneses_atribuidas
        SET status = 'RESPONDIDA',
            dataResposta = :dataResposta
        WHERE id = :id
        AND :status = 'RESPONDIDA'
        AND status IN ('PENDENTE', 'EM_ANDAMENTO')
        AND alunoId = :alunoId
    """)
    abstract suspend fun atualizarConclusaoDoAluno(
        id: Long,
        status: String,
        dataResposta: String,
        alunoId: Long
    ): Int
    @Query("""
    SELECT aa.*
    FROM anamneses_atribuidas aa
    INNER JOIN alunos a ON a.id = aa.alunoId
    WHERE a.professorId = :professorId
      AND aa.excluida = 0
""")
    abstract suspend fun listarTodasParaSincronizacaoDoProfessor(
        professorId: Long
    ): List<AnamneseAtribuidaEntity>

    @Query("""
    SELECT *
    FROM anamneses_atribuidas
    WHERE alunoId = :alunoId
      AND excluida = 0
""")
    abstract suspend fun listarTodasParaSincronizacaoDoAluno(
        alunoId: Long
    ): List<AnamneseAtribuidaEntity>

    @Query("""
    SELECT *
    FROM anamneses_atribuidas
    WHERE firestoreId = :firestoreId
    LIMIT 1
""")
    abstract suspend fun buscarPorFirestoreId(
        firestoreId: String
    ): AnamneseAtribuidaEntity?

    @Query("""
    UPDATE anamneses_atribuidas
    SET firestoreId = :firestoreId,
        updatedAtMillis = :updatedAtMillis
    WHERE id = :id
""")
    abstract suspend fun atualizarIdentificadoresSincronizacao(
        id: Long,
        firestoreId: String,
        updatedAtMillis: Long
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun inserirRecebidaDoFirestoreInterno(
        anamnese: AnamneseAtribuidaEntity
    ): Long

    @Transaction
    open suspend fun inserirRecebidaDoFirestore(
        anamnese: AnamneseAtribuidaEntity
    ): Long {
        val existente = buscarPorFirestoreId(
            anamnese.firestoreId ?: return -1L
        )

        if (existente != null) {
            return existente.id
        }

        return inserirRecebidaDoFirestoreInterno(anamnese)
    }
    @Query("""
    UPDATE anamneses_atribuidas
    SET firestoreId = :firestoreId,
        updatedAtMillis = :updatedAtMillis
    WHERE id = :id
""")
    abstract suspend fun atualizarFirestoreId(
        id: Long,
        firestoreId: String,
        updatedAtMillis: Long
    ): Int

    @Query("""
    UPDATE anamneses_atribuidas
    SET nomeModelo = :nomeModelo,
        status = :status,
        dataCriacao = :dataCriacao,
        dataEnvio = :dataEnvio,
        dataInicioResposta = :dataInicioResposta,
        dataResposta = :dataResposta,
        profissionalResponsavel = :profissionalResponsavel,
        identificadorExterno = :identificadorExterno,
        updatedAtMillis = :updatedAtMillis,
        excluida = :excluida
    WHERE firestoreId = :firestoreId
""")
    abstract suspend fun atualizarRecebidaDoFirestore(
        firestoreId: String,
        nomeModelo: String,
        status: String,
        dataCriacao: String,
        dataEnvio: String,
        dataInicioResposta: String,
        dataResposta: String,
        profissionalResponsavel: String,
        identificadorExterno: String,
        updatedAtMillis: Long,
        excluida: Boolean
    ): Int

    @Query("""
    UPDATE anamneses_atribuidas
    SET status = :status,
        dataInicioResposta = :dataInicioResposta,
        dataResposta = :dataResposta,
        updatedAtMillis = :updatedAtMillis
    WHERE firestoreId = :firestoreId
""")
    abstract suspend fun atualizarStatusSincronizado(
        firestoreId: String,
        status: String,
        dataInicioResposta: String,
        dataResposta: String,
        updatedAtMillis: Long
    ): Int
}
