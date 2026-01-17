package com.finuts.test.fakes.dao

import com.finuts.data.local.dao.AccountDao
import com.finuts.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of AccountDao for unit testing.
 * Stores entities in memory with full CRUD support.
 */
class FakeAccountDao : AccountDao {

    private val accounts = MutableStateFlow<List<AccountEntity>>(emptyList())

    override fun getAllAccounts(): Flow<List<AccountEntity>> =
        accounts.map { it.sortedBy { a -> a.name } }

    override suspend fun getAccountById(id: String): AccountEntity? =
        accounts.value.find { it.id == id }

    override fun getActiveAccounts(): Flow<List<AccountEntity>> =
        accounts.map { list ->
            list.filter { !it.isArchived }.sortedBy { it.name }
        }

    override fun getAccountsByType(type: String): Flow<List<AccountEntity>> =
        accounts.map { list ->
            list.filter { it.type == type }.sortedBy { it.name }
        }

    override suspend fun insert(account: AccountEntity) {
        accounts.update { list ->
            list.filterNot { it.id == account.id } + account
        }
    }

    override suspend fun insertAll(accounts: List<AccountEntity>) {
        accounts.forEach { insert(it) }
    }

    override suspend fun update(account: AccountEntity) {
        accounts.update { list ->
            list.map { if (it.id == account.id) account else it }
        }
    }

    override suspend fun deleteById(id: String) {
        accounts.update { list -> list.filter { it.id != id } }
    }

    override suspend fun archive(id: String, timestamp: Long) {
        accounts.update { list ->
            list.map {
                if (it.id == id) it.copy(isArchived = true, updatedAt = timestamp)
                else it
            }
        }
    }

    override suspend fun getTotalBalance(currencyCode: String): Long? =
        accounts.value
            .filter { it.currencyCode == currencyCode && !it.isArchived }
            .sumOf { it.balance }
            .takeIf { accounts.value.any { a -> a.currencyCode == currencyCode } }

    // Test helpers
    fun setAccounts(newAccounts: List<AccountEntity>) {
        accounts.value = newAccounts
    }

    fun clear() {
        accounts.value = emptyList()
    }

    fun getAll(): List<AccountEntity> = accounts.value
}
