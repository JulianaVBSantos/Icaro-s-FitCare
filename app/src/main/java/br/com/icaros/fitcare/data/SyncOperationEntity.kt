
package br.com.icaros.fitcare.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_operations",
    indices = [
        Index(value = ["entityType", "localEntityId"]),
        Index(value = ["createdAt"])
    ]
)
data class SyncOperationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Tipo do registro que será sincronizado (ex.: ALUNO).
    val entityType: String,

    // ID do registro correspondente no banco Room.
    val localEntityId: Long,

    // ID do documento correspondente no Firestore.
    val remoteDocumentId: String? = null,

    // Tipo da operação: INSERT, UPDATE ou DELETE.
    val operationType: String,

    // Momento em que a operação entrou na fila.
    val createdAt: Long = System.currentTimeMillis(),

    // Quantidade de tentativas de sincronização.
    val attempts: Int = 0,

    // Último erro ocorrido durante a sincronização.
    val lastError: String? = null
)