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
    val status: String = "NAO_ENVIADA",
    val dataCriacao: String = "",
    val dataEnvio: String = "",
    val dataInicioResposta: String = "",
    val dataResposta: String = "",
    val profissionalResponsavel: String = "",
    val identificadorExterno: String = ""
)