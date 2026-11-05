
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "anamneses",
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
        Index(value = ["grupoId"]),
        Index(
            value = ["alunoId", "grupoId", "versao"],
            unique = true
        )
    ]
)
data class AnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val alunoId: Long,
    val grupoId: String,
    val versao: Int = 1,

    val dataAvaliacao: String = "",
    val historicoSaude: String = "",
    val medicamentos: String = "",
    val cirurgias: String = "",
    val acompanhamentoMedico: String = "",

    val condicoesSelecionadas: String = "",
    val restricoesSelecionadas: String = "",
    val riscosSelecionados: String = "",

    val observacoes: String = "",
    val recomendacoes: String = "",
    val cuidados: String = "",
    val objetivos: String = "",

    val profissionalResponsavel: String = "",
    val dataCriacao: String = "",
    val dataAtualizacao: String = ""
)
