package br.com.icaros.fitcare.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
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
        SELECT * FROM usuarios
        WHERE perfil = 'PROFESSOR'
        ORDER BY ativo DESC, nome
    """)
    suspend fun listarProfissionaisParaAdmin(): List<UsuarioEntity>

    @Query("""
        UPDATE usuarios
        SET ativo = :ativo
        WHERE id = :id AND perfil = 'PROFESSOR'
    """)
    suspend fun atualizarStatus(
        id: Long,
        ativo: Boolean
    ): Int

    @Query("""
        UPDATE usuarios
        SET professorId = :professorId
        WHERE id = :usuarioId AND perfil = 'ALUNO'
    """)
    suspend fun vincularProfessor(
        usuarioId: Long,
        professorId: Long
    ): Int

    @Query("""
        UPDATE alunos
        SET professorId = :professorId
        WHERE id = :alunoId
    """)
    suspend fun atualizarProfessorDoCadastroAluno(
        alunoId: Long,
        professorId: Long
    ): Int

    @Transaction
    suspend fun vincularProfessorCompleto(
        usuarioId: Long,
        alunoId: Long,
        professorId: Long
    ): Boolean {

        val cadastroAtualizado = atualizarProfessorDoCadastroAluno(
            alunoId = alunoId,
            professorId = professorId
        )

        val usuarioAtualizado = vincularProfessor(
            usuarioId = usuarioId,
            professorId = professorId
        )

        check(cadastroAtualizado == 1 && usuarioAtualizado == 1) {
            "Não foi possível atualizar os dois vínculos."
        }

        return true
    }

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
    suspend fun buscarPorProfessorId(
        professorId: Long
    ): UsuarioEntity?

    @Query("""
        UPDATE usuarios
        SET senhaHash = :senhaHash
        WHERE id = :id
    """)
    suspend fun atualizarSenha(
        id: Long,
        senhaHash: String
    ): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM alunos
            WHERE professorId = :professorId
        )
        OR EXISTS(
            SELECT 1 FROM usuarios
            WHERE professorId = :professorId
            AND perfil = 'ALUNO'
        )
    """)
    suspend fun professorPossuiAlunos(
        professorId: Long
    ): Boolean

    @Query("""
        DELETE FROM usuarios
        WHERE id = :professorId
        AND perfil = 'PROFESSOR'
    """)
    suspend fun excluirProfessor(
        professorId: Long
    ): Int

    @Query("""
        SELECT COUNT(*) FROM usuarios
        WHERE perfil = 'ADMIN'
    """)
    suspend fun contarAdmins(): Int

    @Transaction
    suspend fun inserirAdminInicial(
        usuario: UsuarioEntity
    ): Long {
        check(usuario.perfil == "ADMIN") {
            "Perfil administrativo inválido."
        }

        check(contarAdmins() == 0) {
            "A conta administrativa inicial já foi criada."
        }

        return inserir(usuario)
    }

    @Query("""
        UPDATE usuarios
        SET firebaseUid = :firebaseUid
        WHERE id = :usuarioId
    """)
    suspend fun atualizarFirebaseUid(
        usuarioId: Long,
        firebaseUid: String
    ): Int

    @Query("""
        SELECT * FROM usuarios
        WHERE firebaseUid = :firebaseUid
        LIMIT 1
    """)
    suspend fun buscarPorFirebaseUid(
        firebaseUid: String
    ): UsuarioEntity?

    @Query("""
        UPDATE usuarios
        SET nome = :nome,
            email = :email,
            ativo = :ativo,
            firebaseUid = :firebaseUid
        WHERE id = :usuarioId
        AND perfil = 'PROFESSOR'
    """)
    suspend fun atualizarProfessorComDadosFirebase(
        usuarioId: Long,
        nome: String,
        email: String,
        ativo: Boolean,
        firebaseUid: String
    ): Int
}