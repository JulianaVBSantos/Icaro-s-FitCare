
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "treinos",
    foreignKeys = [
        ForeignKey(
            entity = AlunoEntity::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["alunoId"])]
)
data class TreinoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alunoId: Long,
    val nome: String,
    val descricao: String = "",
    val dataInicio: String,
    val dataFim: String = "",
    val observacoes: String = ""
)
