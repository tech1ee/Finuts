package com.finuts.test.fakes

import app.cash.turbine.test
import com.finuts.test.BaseTest
import com.finuts.test.TestData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for FakeAccountRepository to ensure our test fakes work correctly.
 */
class FakeAccountRepositoryTest : BaseTest() {

    private val repository = FakeAccountRepository()

    @Test
    fun `getAllAccounts returns empty list initially`() = runTest {
        repository.getAllAccounts().test {
            assertEquals(emptyList(), awaitItem())
        }
    }

    @Test
    fun `createAccount adds account to list`() = runTest {
        val account = TestData.account(id = "1", name = "Test Account")

        repository.getAllAccounts().test {
            assertEquals(emptyList(), awaitItem())

            repository.createAccount(account)

            val accounts = awaitItem()
            assertEquals(1, accounts.size)
            assertEquals("Test Account", accounts.first().name)
        }
    }

    @Test
    fun `getAccountById returns account when exists`() = runTest {
        val account = TestData.account(id = "acc-1", name = "My Account")
        repository.createAccount(account)

        repository.getAccountById("acc-1").test {
            val result = awaitItem()
            assertEquals("My Account", result?.name)
        }
    }

    @Test
    fun `getAccountById returns null when not exists`() = runTest {
        repository.getAccountById("non-existent").test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `getActiveAccounts excludes archived accounts`() = runTest {
        val activeAccount = TestData.account(id = "1", name = "Active", isArchived = false)
        val archivedAccount = TestData.account(id = "2", name = "Archived", isArchived = true)

        repository.setAccounts(listOf(activeAccount, archivedAccount))

        repository.getActiveAccounts().test {
            val active = awaitItem()
            assertEquals(1, active.size)
            assertEquals("Active", active.first().name)
        }
    }

    @Test
    fun `updateAccount modifies existing account`() = runTest {
        val account = TestData.account(id = "1", name = "Original")
        repository.createAccount(account)

        val updatedAccount = account.copy(name = "Updated")
        repository.updateAccount(updatedAccount)

        repository.getAccountById("1").test {
            assertEquals("Updated", awaitItem()?.name)
        }
    }

    @Test
    fun `deleteAccount removes account from list`() = runTest {
        val account = TestData.account(id = "1")
        repository.createAccount(account)

        repository.deleteAccount("1")

        repository.getAllAccounts().test {
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `archiveAccount sets isArchived to true`() = runTest {
        val account = TestData.account(id = "1", isArchived = false)
        repository.createAccount(account)

        repository.archiveAccount("1")

        repository.getAccountById("1").test {
            assertEquals(true, awaitItem()?.isArchived)
        }
    }

    @Test
    fun `clear removes all accounts`() = runTest {
        repository.setAccounts(
            listOf(
                TestData.account(id = "1"),
                TestData.account(id = "2"),
                TestData.account(id = "3")
            )
        )

        repository.clear()

        repository.getAllAccounts().test {
            assertTrue(awaitItem().isEmpty())
        }
    }
}
