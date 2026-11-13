package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "modelos_anamnese",
    indices = [
        Index(value = ["professorId"]),
        Index(value = ["categoria"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class ModeloAnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val categoria: String = "PERSONALIZADO",
    val ativo: Boolean = true,
    val dataCriacao: String = "",
    val dataAtualizacao: String = "",
    val professorId: Long? = null,
    val firestoreId: String? = null,

    // Controle de sincronização e exclusão lógica
    val excluido: Boolean = false,
    val updatedAtMillis: Long = 0L,
    val deletedAtMillis: Long? = null
)