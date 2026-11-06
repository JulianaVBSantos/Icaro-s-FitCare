package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modelos_anamnese")
data class ModeloAnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,
    val descricao: String = "",

    // PADRAO ou PERSONALIZADO
    val categoria: String = "PERSONALIZADO",

    val ativo: Boolean = true,
    val dataCriacao: String = "",
    val dataAtualizacao: String = ""
)