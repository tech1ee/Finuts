package com.finuts.app.test.fakes

import com.finuts.domain.entity.Account
import com.finuts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fake implementation of AccountRepository for ViewModel testing.
 */
class FakeAccountRepository : AccountRepository {

    private val accounts = MutableStateFlow<List<Account>>(emptyList())

    override fun getAllAccounts(): Flow<List<Account>> = accounts

    override fun getAccountById(id: String): Flow<Account?> =
        accounts.map { list -> list.find { it.id == id } }

    override fun getActiveAccounts(): Flow<List<Account>> =
        accounts.map { list -> list.filter { !it.isArchived } }

    override suspend fun createAccount(account: Account) {
        accounts.update { it + account }
    }

    override suspend fun updateAccount(account: Account) {
        accounts.update { list ->
            list.map { if (it.id == account.id) account else it }
        }
    }

    override suspend fun deleteAccount(id: String) {
        accounts.update { list -> list.filter { it.id != id } }
    }

    override suspend fun archiveAccount(id: String) {
        accounts.update { list ->
            list.map { if (it.id == id) it.copy(isArchived = true) else it }
        }
    }

    // Test helpers
    fun setAccounts(newAccounts: List<Account>) {
        accounts.value = newAccounts
    }

    fun clear() {
        accounts.value = emptyList()
    }
}
