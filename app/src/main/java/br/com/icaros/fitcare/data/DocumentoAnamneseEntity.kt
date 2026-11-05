package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documentos_anamnese",
    foreignKeys = [
        ForeignKey(
            entity = AlunoEntity::class,
            parentColumns = ["id"],
            childColumns = ["alunoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["alunoId"])
    ]
)
data class DocumentoAnamneseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val alunoId: Long,
    val nomeArquivo: String,
    val caminhoArquivo: String,
    val tipoArquivo: String,
    val tamanhoArquivo: Long = 0,
    val dataImportacao: String
)