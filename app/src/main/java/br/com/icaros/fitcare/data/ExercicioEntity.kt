
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercicios",
    foreignKeys = [
        ForeignKey(
            entity = TreinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["treinoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["treinoId"])]
)
data class ExercicioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val treinoId: Long,
    val nome: String,
    val series: Int,
    val repeticoes: String,
    val carga: String = "",
    val observacoes: String = "",
    val ordem: Int = 0
)