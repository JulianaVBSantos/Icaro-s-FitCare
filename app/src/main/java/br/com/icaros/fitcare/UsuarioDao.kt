
package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun inserir(usuario: UsuarioEntity): Long

    @Query("""
        SELECT * FROM usuarios
        WHERE email = :email
        LIMIT 1
    """)
    suspend fun buscarPorEmail(email: String): UsuarioEntity?

    @Query("""
        SELECT * FROM usuarios
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun buscarPorId(id: Long): UsuarioEntity?

    @Query("""
        SELECT * FROM usuarios
        WHERE perfil = :perfil AND ativo = 1
        ORDER BY nome
    """)
    suspend fun listarPorPerfil(perfil: String): List<UsuarioEntity>

    @Query("""
        UPDATE usuarios
        SET ativo = :ativo
        WHERE id = :id
    """)
    suspend fun atualizarStatus(id: Long, ativo: Boolean)

    @Query("""
        UPDATE usuarios
        SET professorId = :professorId
        WHERE id = :usuarioId AND perfil = 'ALUNO'
    """)
    suspend fun vincularProfessor(
        usuarioId: Long,
        professorId: Long
    )

    @Query("""
    SELECT * FROM usuarios
    WHERE alunoId = :alunoId
    LIMIT 1
""")
    suspend fun buscarPorAlunoId(alunoId: Long): UsuarioEntity?

    @Query("""
    SELECT * FROM usuarios
    WHERE professorId = :professorId
    LIMIT 1
""")
    suspend fun buscarPorProfessorId(professorId: Long): UsuarioEntity?

}
