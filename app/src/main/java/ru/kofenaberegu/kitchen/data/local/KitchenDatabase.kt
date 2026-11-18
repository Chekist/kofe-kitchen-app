package ru.kofenaberegu.kitchen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReportHistoryEntity::class,
        WriteOffRecordEntity::class,
        AdminRemarkEntity::class,
        ChemicalItemEntity::class,
        CustomReminderEntity::class,
        MessageQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun kitchenDao(): KitchenDao

    companion object {
        @Volatile
        private var INSTANCE: KitchenDatabase? = null

        fun getInstance(context: Context): KitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "kofe_na_beregu_kitchen.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
