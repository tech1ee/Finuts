package com.finuts.data.repository

import com.finuts.data.local.dao.AccountDao
import com.finuts.data.local.mapper.toDomain
import com.finuts.data.local.mapper.toEntity
import com.finuts.domain.entity.Account
import com.finuts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.finuts.core.util.currentTimeMillis

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getAccountById(id: String): Flow<Account?> =
        kotlinx.coroutines.flow.flow {
            emit(accountDao.getAccountById(id)?.toDomain())
        }

    override fun getActiveAccounts(): Flow<List<Account>> =
        accountDao.getActiveAccounts().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun createAccount(account: Account) {
        accountDao.insert(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.update(account.toEntity())
    }

    override suspend fun deleteAccount(id: String) {
        accountDao.deleteById(id)
    }

    override suspend fun archiveAccount(id: String) {
        accountDao.archive(id, currentTimeMillis())
    }
}
