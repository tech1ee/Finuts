package com.finuts.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.finuts.data.local.dao.AccountDao
import com.finuts.data.local.dao.BudgetDao
import com.finuts.data.local.dao.CategoryCorrectionDao
import com.finuts.data.local.dao.CategoryDao
import com.finuts.data.local.dao.LearnedMerchantDao
import com.finuts.data.local.dao.TransactionDao
import com.finuts.data.local.entity.AccountEntity
import com.finuts.data.local.entity.BudgetEntity
import com.finuts.data.local.entity.CategoryCorrectionEntity
import com.finuts.data.local.entity.CategoryEntity
import com.finuts.data.local.entity.LearnedMerchantEntity
import com.finuts.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        CategoryCorrectionEntity::class,
        LearnedMerchantEntity::class
    ],
    version = 7,
    exportSchema = true
)
@ConstructedBy(FinutsDatabaseConstructor::class)
abstract class FinutsDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryCorrectionDao(): CategoryCorrectionDao
    abstract fun learnedMerchantDao(): LearnedMerchantDao

    companion object {
        const val DATABASE_NAME = "finuts.db"
    }
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object FinutsDatabaseConstructor : RoomDatabaseConstructor<FinutsDatabase> {
    override fun initialize(): FinutsDatabase
}
