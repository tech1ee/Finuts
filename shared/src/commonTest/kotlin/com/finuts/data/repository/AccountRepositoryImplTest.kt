package com.finuts.data.repository

import app.cash.turbine.test
import com.finuts.data.local.entity.AccountEntity
import com.finuts.domain.entity.Account
import com.finuts.domain.entity.AccountType
import com.finuts.domain.entity.Currency
import com.finuts.test.fakes.dao.FakeAccountDao
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountRepositoryImplTest {

    private lateinit var fakeDao: FakeAccountDao
    private lateinit var repository: AccountRepositoryImpl

    @BeforeTest
    fun setup() {
        fakeDao = FakeAccountDao()
        repository = AccountRepositoryImpl(fakeDao)
    }

    // === getAllAccounts Tests ===

    @Test
    fun `getAllAccounts returns empty list when no accounts`() = runTest {
        repository.getAllAccounts().test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAllAccounts returns all accounts mapped to domain`() = runTest {
        fakeDao.setAccounts(listOf(
            createAccountEntity("1", "Checking"),
            createAccountEntity("2", "Savings")
        ))

        repository.getAllAccounts().test {
            val accounts = awaitItem()
            assertEquals(2, accounts.size)
            assertEquals("Checking", accounts[0].name)
            assertEquals("Savings", accounts[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === getAccountById Tests ===

    @Test
    fun `getAccountById returns null when not found`() = runTest {
        repository.getAccountById("non-existent").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getAccountById returns account when found`() = runTest {
        fakeDao.setAccounts(listOf(createAccountEntity("1", "My Account")))

        repository.getAccountById("1").test {
            val account = awaitItem()
            assertEquals("1", account?.id)
            assertEquals("My Account", account?.name)
            awaitComplete()
        }
    }

    // === getActiveAccounts Tests ===

    @Test
    fun `getActiveAccounts excludes archived accounts`() = runTest {
        fakeDao.setAccounts(listOf(
            createAccountEntity("1", "Active", isArchived = false),
            createAccountEntity("2", "Archived", isArchived = true)
        ))

        repository.getActiveAccounts().test {
            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("Active", accounts[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // === createAccount Tests ===

    @Test
    fun `createAccount inserts entity into dao`() = runTest {
        val account = createDomainAccount("new-id", "New Account")

        repository.createAccount(account)

        val stored = fakeDao.getAccountById("new-id")
        assertEquals("New Account", stored?.name)
    }

    // === updateAccount Tests ===

    @Test
    fun `updateAccount modifies existing entity`() = runTest {
        fakeDao.setAccounts(listOf(createAccountEntity("1", "Old Name")))
        val updated = createDomainAccount("1", "New Name")

        repository.updateAccount(updated)

        val stored = fakeDao.getAccountById("1")
        assertEquals("New Name", stored?.name)
    }

    // === deleteAccount Tests ===

    @Test
    fun `deleteAccount removes entity from dao`() = runTest {
        fakeDao.setAccounts(listOf(createAccountEntity("1", "ToDelete")))

        repository.deleteAccount("1")

        assertNull(fakeDao.getAccountById("1"))
    }

    // === archiveAccount Tests ===

    @Test
    fun `archiveAccount sets isArchived flag`() = runTest {
        fakeDao.setAccounts(listOf(createAccountEntity("1", "ToArchive", isArchived = false)))

        repository.archiveAccount("1")

        val archived = fakeDao.getAccountById("1")
        assertTrue(archived?.isArchived == true)
    }

    // === Helper Functions ===

    private fun createAccountEntity(
        id: String,
        name: String,
        isArchived: Boolean = false
    ) = AccountEntity(
        id = id,
        name = name,
        type = "BANK_ACCOUNT",
        currencyCode = "USD",
        currencySymbol = "$",
        currencyName = "US Dollar",
        balance = 10000L,
        initialBalance = 10000L,
        icon = "account_balance",
        color = "#4CAF50",
        isArchived = isArchived,
        createdAt = 1704067200000L,
        updatedAt = 1704067200000L
    )

    private fun createDomainAccount(
        id: String,
        name: String
    ) = Account(
        id = id,
        name = name,
        type = AccountType.BANK_ACCOUNT,
        currency = Currency("USD", "$", "US Dollar"),
        balance = 10000L,
        initialBalance = 10000L,
        icon = "account_balance",
        color = "#4CAF50",
        isArchived = false,
        createdAt = Instant.fromEpochMilliseconds(1704067200000L),
        updatedAt = Instant.fromEpochMilliseconds(1704067200000L)
    )
}
