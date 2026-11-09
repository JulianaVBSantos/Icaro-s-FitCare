package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoAnamneseDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(documento: DocumentoAnamneseEntity): Long

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE alunoId = :alunoId
        ORDER BY id DESC
    """)
    fun listarPorAluno(alunoId: Long): Flow<List<DocumentoAnamneseEntity>>

    @Query("""
        SELECT * FROM documentos_anamnese
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(id: Long): DocumentoAnamneseEntity?

    @Query("""
        DELETE FROM documentos_anamnese
        WHERE id = :id
    """)
    suspend fun excluirPorId(id: Long)
}