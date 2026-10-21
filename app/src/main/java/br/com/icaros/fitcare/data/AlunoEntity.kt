package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alunos",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class AlunoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,
    val email: String,
    val dataCadastro: String,
    val objetivo: String,
    val status: String = "Em dia"
)