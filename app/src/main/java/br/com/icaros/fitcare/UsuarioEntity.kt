
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    val email: String,

    // Armazene somente o hash da senha, nunca a senha original.
    val senhaHash: String,

    // PROFESSOR ou ALUNO
    val perfil: String,

    // Identificador do aluno ou professor relacionado, quando aplicável.
    val alunoId: Long? = null,
    val professorId: Long? = null,

    val ativo: Boolean = true,

    val dataCriacao: Long = System.currentTimeMillis()
)