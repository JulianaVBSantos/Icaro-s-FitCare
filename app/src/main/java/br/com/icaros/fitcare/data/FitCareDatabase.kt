package br.com.icaros.fitcare.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AlunoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FitCareDatabase : RoomDatabase() {

    abstract fun alunoDao(): AlunoDao

    companion object {

        @Volatile
        private var INSTANCE: FitCareDatabase? = null

        fun getDatabase(context: Context): FitCareDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitCareDatabase::class.java,
                    "fitcare_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}