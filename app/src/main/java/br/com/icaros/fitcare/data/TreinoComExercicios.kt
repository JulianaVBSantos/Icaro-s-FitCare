
package br.com.icaros.fitcare.data

import androidx.room.Embedded
import androidx.room.Relation

data class TreinoComExercicios(
    @Embedded
    val treino: TreinoEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "treinoId"
    )
    val exercicios: List<ExercicioEntity>
)