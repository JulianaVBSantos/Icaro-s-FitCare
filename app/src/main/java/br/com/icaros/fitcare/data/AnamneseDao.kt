
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnamneseDao {

    // INSERIR ANAMNESE
    @Insert
    suspend fun inserirAnamnese(
        anamnese: AnamneseEntity
    ): Long

    // LISTAR ANAMNESES DO ALUNO
    @Query("""
        SELECT a.* FROM anamneses a
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.alunoId = :alunoId
        AND al.professorId = :professorId
        ORDER BY a.dataCriacao DESC, a.id DESC
    """)
    fun listarAnamnesesDoProfessor(
        alunoId: Long,
        professorId: Long
    ): Flow<List<AnamneseEntity>>

    @Query("""
        SELECT * FROM anamneses
        WHERE alunoId = :alunoId
        ORDER BY dataCriacao DESC, id DESC
    """)
    fun listarAnamnesesDoAluno(
        alunoId: Long
    ): Flow<List<AnamneseEntity>>

    // BUSCAR ANAMNESE POR ID
    @Query("""
        SELECT a.* FROM anamneses a
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.id = :id
        AND al.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): AnamneseEntity?

    @Query("""
        SELECT * FROM anamneses
        WHERE id = :id
        AND alunoId = :alunoId
        LIMIT 1
    """)
    suspend fun buscarPorIdDoAluno(
        id: Long,
        alunoId: Long
    ): AnamneseEntity?

    // HISTÓRICO DE VERSÕES
    @Query("""
        SELECT a.* FROM anamneses a
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.alunoId = :alunoId
        AND a.grupoId = :grupoId
        AND al.professorId = :professorId
        ORDER BY a.versao DESC
    """)
    fun listarVersoesDoProfessor(
        alunoId: Long,
        grupoId: String,
        professorId: Long
    ): Flow<List<AnamneseEntity>>

    @Query("""
        SELECT * FROM anamneses
        WHERE alunoId = :alunoId
        AND grupoId = :grupoId
        ORDER BY versao DESC
    """)
    fun listarVersoesDoAluno(
        alunoId: Long,
        grupoId: String
    ): Flow<List<AnamneseEntity>>

    // OBTER ÚLTIMA VERSÃO
    @Query("""
        SELECT MAX(a.versao)
        FROM anamneses a
        INNER JOIN alunos al ON al.id = a.alunoId
        WHERE a.alunoId = :alunoId
        AND a.grupoId = :grupoId
        AND al.professorId = :professorId
    """)
    suspend fun obterUltimaVersaoDoProfessor(
        alunoId: Long,
        grupoId: String,
        professorId: Long
    ): Int?

    @Query("""
        SELECT MAX(versao)
        FROM anamneses
        WHERE alunoId = :alunoId
        AND grupoId = :grupoId
    """)
    suspend fun obterUltimaVersaoDoAluno(
        alunoId: Long,
        grupoId: String
    ): Int?

    // INSERIR DOCUMENTO
    @Insert
    suspend fun inserirDocumento(
        documento: DocumentoAnamneseEntity
    ): Long

    // LISTAR DOCUMENTOS

    @Query("""
        SELECT d.* FROM documentos_anamnese d
        INNER JOIN alunos al ON al.id = d.alunoId
        WHERE d.alunoId = :alunoId
        AND al.professorId = :professorId
        ORDER BY d.id DESC
    """)
    fun listarDocumentosDoProfessor(
        alunoId: Long,
        professorId: Long
    ): Flow<List<DocumentoAnamneseEntity>>

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    fun listarDocumentosDoAluno(
        alunoId: Long
    ): Flow<List<DocumentoAnamneseEntity>>

    // BUSCAR DOCUMENTO POR ID

    @Query("""
        SELECT d.* FROM documentos_anamnese d
        INNER JOIN alunos al ON al.id = d.alunoId
        WHERE d.id = :id
        AND al.professorId = :professorId
        LIMIT 1
    """)
    suspend fun buscarDocumentoPorIdDoProfessor(
        id: Long,
        professorId: Long
    ): DocumentoAnamneseEntity?

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE id = :id
        AND alunoId = :alunoId
        LIMIT 1
    """)
    suspend fun buscarDocumentoPorIdDoAluno(
        id: Long,
        alunoId: Long
    ): DocumentoAnamneseEntity?

    // EXCLUIR DOCUMENTO
    @Query("""
        DELETE FROM documentos_anamnese
        WHERE id = :id
        AND alunoId IN (
            SELECT id FROM alunos
            WHERE professorId = :professorId
        )
    """)
    suspend fun excluirDocumentoDoProfessor(
        id: Long,
        professorId: Long
    )

    @Query("""
        DELETE FROM documentos_anamnese
        WHERE id = :id
        AND alunoId = :alunoId
    """)
    suspend fun excluirDocumentoDoAluno(
        id: Long,
        alunoId: Long
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