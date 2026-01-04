package com.finuts.app.feature.settings

import app.cash.turbine.test
import com.finuts.app.test.fakes.FakePreferencesRepository
import com.finuts.domain.model.AppLanguage
import com.finuts.domain.model.AppTheme
import com.finuts.domain.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for SettingsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferencesRepository: FakePreferencesRepository
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferencesRepository = FakePreferencesRepository()
        viewModel = SettingsViewModel(preferencesRepository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `preferences emits default values initially`() = runTest {
        viewModel.preferences.test {
            val initial = awaitItem()
            assertEquals(AppTheme.SYSTEM, initial.theme)
            assertEquals(AppLanguage.SYSTEM, initial.language)
            assertEquals("KZT", initial.defaultCurrency)
            assertTrue(initial.notificationsEnabled)
            assertFalse(initial.biometricEnabled)
        }
    }

    @Test
    fun `setTheme updates theme preference`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setTheme(AppTheme.DARK)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(AppTheme.DARK, updated.theme)
        }
    }

    @Test
    fun `setTheme to LIGHT updates correctly`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setTheme(AppTheme.LIGHT)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(AppTheme.LIGHT, updated.theme)
        }
    }

    @Test
    fun `setLanguage updates language preference`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setLanguage(AppLanguage.RUSSIAN)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(AppLanguage.RUSSIAN, updated.language)
        }
    }

    @Test
    fun `setLanguage to ENGLISH updates correctly`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setLanguage(AppLanguage.ENGLISH)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals(AppLanguage.ENGLISH, updated.language)
        }
    }

    @Test
    fun `setDefaultCurrency updates currency preference`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setDefaultCurrency("USD")
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals("USD", updated.defaultCurrency)
        }
    }

    @Test
    fun `setDefaultCurrency to EUR updates correctly`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setDefaultCurrency("EUR")
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertEquals("EUR", updated.defaultCurrency)
        }
    }

    @Test
    fun `setNotificationsEnabled disables notifications`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setNotificationsEnabled(false)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertFalse(updated.notificationsEnabled)
        }
    }

    @Test
    fun `setNotificationsEnabled enables notifications`() = runTest {
        viewModel.preferences.test {
            val initial = awaitItem()
            assertTrue(initial.notificationsEnabled) // Default is true

            // Disable first
            viewModel.setNotificationsEnabled(false)
            testDispatcher.scheduler.advanceUntilIdle()

            val disabled = awaitItem()
            assertFalse(disabled.notificationsEnabled)

            // Enable again
            viewModel.setNotificationsEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()

            val enabled = awaitItem()
            assertTrue(enabled.notificationsEnabled)
        }
    }

    @Test
    fun `setBiometricEnabled enables biometric`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setBiometricEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()

            val updated = awaitItem()
            assertTrue(updated.biometricEnabled)
        }
    }

    @Test
    fun `setBiometricEnabled disables biometric`() = runTest {
        viewModel.preferences.test {
            val initial = awaitItem()
            assertFalse(initial.biometricEnabled) // Default is false

            // Enable first
            viewModel.setBiometricEnabled(true)
            testDispatcher.scheduler.advanceUntilIdle()

            val enabled = awaitItem()
            assertTrue(enabled.biometricEnabled)

            // Disable again
            viewModel.setBiometricEnabled(false)
            testDispatcher.scheduler.advanceUntilIdle()

            val disabled = awaitItem()
            assertFalse(disabled.biometricEnabled)
        }
    }

    @Test
    fun `multiple preference changes accumulate correctly`() = runTest {
        viewModel.preferences.test {
            awaitItem() // Skip initial

            viewModel.setTheme(AppTheme.DARK)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.setLanguage(AppLanguage.RUSSIAN)
            testDispatcher.scheduler.advanceUntilIdle()
            awaitItem()

            viewModel.setDefaultCurrency("RUB")
            testDispatcher.scheduler.advanceUntilIdle()

            val final = awaitItem()
            assertEquals(AppTheme.DARK, final.theme)
            assertEquals(AppLanguage.RUSSIAN, final.language)
            assertEquals("RUB", final.defaultCurrency)
        }
    }

    @Test
    fun `SUPPORTED_CURRENCIES contains expected values`() {
        val currencies = SettingsViewModel.SUPPORTED_CURRENCIES
        assertTrue(currencies.contains("KZT"))
        assertTrue(currencies.contains("RUB"))
        assertTrue(currencies.contains("USD"))
        assertTrue(currencies.contains("EUR"))
        assertEquals(4, currencies.size)
    }
}
