package com.example.ministoreapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ministoreapp.data.local.dao.AuditLogDao
import com.example.ministoreapp.data.local.dao.BanRecordDao
import com.example.ministoreapp.data.local.dao.CashierDao
import com.example.ministoreapp.data.local.dao.CreditTransactionDao
import com.example.ministoreapp.data.local.dao.CustomerDao
import com.example.ministoreapp.data.local.dao.EventDao
import com.example.ministoreapp.data.local.dao.MasterDataDao
import com.example.ministoreapp.data.local.dao.RestockItemDao
import com.example.ministoreapp.data.local.dao.PriceHistoryDao
import com.example.ministoreapp.data.local.dao.ProductDao
import com.example.ministoreapp.data.local.dao.ProductGroupDao
import com.example.ministoreapp.data.local.dao.SecuritySettingsDao
import com.example.ministoreapp.data.local.entities.AuditLogEntity
import com.example.ministoreapp.data.local.entities.BanRecordEntity
import com.example.ministoreapp.data.local.entities.BrandEntity
import com.example.ministoreapp.data.local.entities.CashierEntity
import com.example.ministoreapp.data.local.entities.CategoryEntity
import com.example.ministoreapp.data.local.entities.ContainerEntity
import com.example.ministoreapp.data.local.entities.CreditTransactionEntity
import com.example.ministoreapp.data.local.entities.CustomerEntity
import com.example.ministoreapp.data.local.entities.EventEntity
import com.example.ministoreapp.data.local.entities.RestockItemEntity
import com.example.ministoreapp.data.local.entities.PriceHistoryEntity
import com.example.ministoreapp.data.local.entities.ProductEntity
import com.example.ministoreapp.data.local.entities.ProductGroupEntity
import com.example.ministoreapp.data.local.entities.SecuritySettingsEntity
import com.example.ministoreapp.data.local.entities.UnitEntity
import com.example.ministoreapp.data.local.entities.VendorEntity
import com.example.ministoreapp.data.local.entities.WholesaleUnitEntity
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

@Database(
    entities = [
        ProductGroupEntity::class,
        ProductEntity::class,
        CashierEntity::class,
        SecuritySettingsEntity::class,
        CustomerEntity::class,
        CreditTransactionEntity::class,
        BanRecordEntity::class,
        EventEntity::class,
        RestockItemEntity::class,
        PriceHistoryEntity::class,
        AuditLogEntity::class,
        CategoryEntity::class,
        BrandEntity::class,
        UnitEntity::class,
        ContainerEntity::class,
        WholesaleUnitEntity::class,
        VendorEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productGroupDao(): ProductGroupDao
    abstract fun productDao(): ProductDao
    abstract fun cashierDao(): CashierDao
    abstract fun securitySettingsDao(): SecuritySettingsDao
    abstract fun customerDao(): CustomerDao
    abstract fun creditTransactionDao(): CreditTransactionDao
    abstract fun banRecordDao(): BanRecordDao
    abstract fun eventDao(): EventDao
    abstract fun restockItemDao(): RestockItemDao
    fun orderItemDao(): RestockItemDao = restockItemDao()
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun masterDataDao(): MasterDataDao

    companion object {
        const val DB_NAME = "gj_store.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Executors.newSingleThreadExecutor().execute {
                                val database = getInstance(context)
                                runBlocking {
                                    database.securitySettingsDao().insertOrUpdate(
                                        SecuritySettingsEntity(
                                            id = "default",
                                            storeName = "StoreName Sari-Sari and Wholesale",
                                            securityPin = "526375",
                                            pinOnAppLaunchEnabled = true,
                                            pinOnSettingsOpeningEnabled = true
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun createPreSyncBackup(context: Context): Boolean {
            return try {
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    val backupFile = File(dbFile.parent, "pre_sync_backup.db")
                    FileInputStream(dbFile).use { input ->
                        FileOutputStream(backupFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
