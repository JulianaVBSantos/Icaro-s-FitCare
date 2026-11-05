package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnamneseDao {

    @Insert
    suspend fun inserirAnamnese(
        anamnese: AnamneseEntity
    ): Long

    @Query("""
        SELECT * FROM anamneses
        WHERE alunoId = :alunoId
        ORDER BY dataCriacao DESC, id DESC
    """)
    fun listarAnamneses(
        alunoId: Long
    ): Flow<List<AnamneseEntity>>

    @Query("""
        SELECT * FROM anamneses
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(
        id: Long
    ): AnamneseEntity?

    @Query("""
        SELECT * FROM anamneses
        WHERE alunoId = :alunoId
        AND grupoId = :grupoId
        ORDER BY versao DESC
    """)
    fun listarVersoes(
        alunoId: Long,
        grupoId: String
    ): Flow<List<AnamneseEntity>>

    @Query("""
        SELECT MAX(versao) FROM anamneses
        WHERE alunoId = :alunoId
        AND grupoId = :grupoId
    """)
    suspend fun obterUltimaVersao(
        alunoId: Long,
        grupoId: String
    ): Int?

    @Insert
    suspend fun inserirDocumento(
        documento: DocumentoAnamneseEntity
    ): Long

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    fun listarDocumentos(
        alunoId: Long
    ): Flow<List<DocumentoAnamneseEntity>>

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarDocumentoPorId(
        id: Long
    ): DocumentoAnamneseEntity?

    @Query("""
        DELETE FROM documentos_anamnese
        WHERE id = :id
    """)
    suspend fun excluirDocumento(
        id: Long
    )
}