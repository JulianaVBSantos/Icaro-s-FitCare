package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "respostas_anamnese",
    foreignKeys = [
        ForeignKey(
            entity = PerguntaAnamneseAtribuidaEntity::class,
            parentColumns = ["id"],
            childColumns = ["perguntaAtribuidaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["perguntaAtribuidaId"], unique = true)
    ]
)
data class RespostaAnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val perguntaAtribuidaId: Long,
    val resposta: String = "",
    val dataAtualizacao: String = ""
)