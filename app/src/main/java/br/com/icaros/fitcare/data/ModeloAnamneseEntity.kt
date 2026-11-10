
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "modelos_anamnese",
    indices = [
        Index(value = ["professorId"]),
        Index(value = ["categoria"])
    ]
)
data class ModeloAnamneseEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,

    val descricao: String = "",

    // PADRAO ou PERSONALIZADO
    val categoria: String = "PERSONALIZADO",

    val ativo: Boolean = true,

    val dataCriacao: String = "",

    val dataAtualizacao: String = "",

    // NULL = modelo padrão compartilhado
    // ID preenchido = modelo pertencente ao professor
    val professorId: Long? = null
)