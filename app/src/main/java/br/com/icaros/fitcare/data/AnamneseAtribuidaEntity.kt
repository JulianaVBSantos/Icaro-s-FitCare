
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anamneses_atribuidas",
    foreignKeys = [
        ForeignKey(
            entity = AlunoEntity::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ModeloAnamneseEntity::class,
            parentColumns = ["id"],
            childColumns = ["modeloId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["alunoId"]),
        Index(value = ["modeloId"]),
        Index(value = ["status"])
    ]
)
data class AnamneseAtribuidaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val alunoId: Long,
    val modeloId: Long,

    val nomeModelo: String,

    // NAO_ENVIADA, PENDENTE,
    // EM_ANDAMENTO ou RESPONDIDA
    val status: String = "NAO_ENVIADA",

    val dataCriacao: String = "",
    val dataEnvio: String = "",
    val dataInicioResposta: String = "",
    val dataResposta: String = "",

    val profissionalResponsavel: String = "",

    // Reservado para integração futura com backend.
    val identificadorExterno: String = ""
)
