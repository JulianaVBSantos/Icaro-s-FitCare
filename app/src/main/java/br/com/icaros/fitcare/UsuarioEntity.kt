
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["firebaseUid"], unique = true)
    ]
)
data class UsuarioEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    val email: String,

    val senhaHash: String,

    // PROFESSOR, ALUNO ou ADMIN
    val perfil: String,

    val alunoId: Long? = null,

    val professorId: Long? = null,

    val ativo: Boolean = true,

    val dataCriacao: Long = System.currentTimeMillis(),

    // UID do usuário no Firebase Authentication.
    val firebaseUid: String? = null
)
