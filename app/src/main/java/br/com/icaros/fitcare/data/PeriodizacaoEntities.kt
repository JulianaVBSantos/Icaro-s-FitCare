package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "macrociclos",
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
data class MacrocicloEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alunoId: Long,
    val nome: String,
    val objetivo: String = "",
    val dataInicio: String,
    val dataFim: String = "",
    val observacoes: String = ""
)

@Entity(
    tableName = "mesociclos",
    foreignKeys = [
        ForeignKey(
            entity = MacrocicloEntity::class,
            parentColumns = ["id"],
            childColumns = ["macrocicloId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["macrocicloId"])]
)
data class MesocicloEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val macrocicloId: Long,
    val nome: String,
    val foco: String = "",
    val dataInicio: String,
    val dataFim: String = "",
    val ordem: Int = 0
)

@Entity(
    tableName = "microciclos",
    foreignKeys = [
        ForeignKey(
            entity = MesocicloEntity::class,
            parentColumns = ["id"],
            childColumns = ["mesocicloId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mesocicloId"])]
)
data class MicrocicloEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mesocicloId: Long,
    val nome: String,
    val objetivoSemanal: String = "",
    val dataInicio: String,
    val dataFim: String = "",
    val ordem: Int = 0
)

@Entity(
    tableName = "sessoes_periodizacao",
    foreignKeys = [
        ForeignKey(
            entity = MicrocicloEntity::class,
            parentColumns = ["id"],
            childColumns = ["microcicloId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TreinoEntity::class,
            parentColumns = ["id"],
            childColumns = ["treinoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["microcicloId"]),
        Index(value = ["treinoId"])
    ]
)
data class SessaoPeriodizacaoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val microcicloId: Long,
    val treinoId: Long? = null,
    val diaSemana: String,
    val titulo: String,
    val observacoes: String = "",
    val ordem: Int = 0
)