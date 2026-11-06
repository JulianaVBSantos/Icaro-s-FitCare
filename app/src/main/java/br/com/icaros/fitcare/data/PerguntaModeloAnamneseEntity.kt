package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "perguntas_modelo_anamnese",
    foreignKeys = [
        ForeignKey(
            entity = ModeloAnamneseEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeloId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["modeloId"])
    ]
)
data class PerguntaModeloAnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val modeloId: Long,
    val texto: String,

    // TEXTO, NUMERO, DATA, SIM_NAO,
    // MULTIPLA_ESCOLHA ou CHECKBOX
    val tipoResposta: String = "TEXTO",

    // Opções armazenadas em formato JSON
    val opcoes: String = "",

    val obrigatoria: Boolean = false,
    val ordem: Int = 0
)