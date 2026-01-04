package com.finuts.domain.repository

import com.finuts.domain.entity.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    fun getAccountById(id: String): Flow<Account?>
    fun getActiveAccounts(): Flow<List<Account>>
    suspend fun createAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(id: String)
    suspend fun archiveAccount(id: String)
}
