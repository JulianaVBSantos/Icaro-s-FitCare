package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "perguntas_anamnese_atribuidas",
    foreignKeys = [
        ForeignKey(
            entity = AnamneseAtribuidaEntity::class,
            parentColumns = ["id"],
            childColumns = ["anamneseAtribuidaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["anamneseAtribuidaId"])
    ]
)
data class PerguntaAnamneseAtribuidaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val anamneseAtribuidaId: Long,
    val perguntaModeloId: Long? = null,
    val texto: String,
    val tipoResposta: String = "TEXTO",
    val opcoes: String = "",
    val obrigatoria: Boolean = false,
    val ordem: Int = 0
)
