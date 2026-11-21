package es.pedrazamiguez.splittrip.domain.usecase.group

import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveCurrencyException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.UpdateGroupUseCaseImpl
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UpdateGroupUseCaseTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var getGroupCurrenciesInUseUseCase: GetGroupCurrenciesInUseUseCase
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private lateinit var useCase: UpdateGroupUseCase

    private val fixedMonth = YearMonth.of(2026, 11)
    private val testGroup = Group(id = "group-123", name = "Trip to Japan", currency = "JPY")

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        getGroupCurrenciesInUseUseCase = mockk()
        userPreferenceRepository = mockk()
        coEvery { getGroupCurrenciesInUseUseCase(any()) } returns emptySet()
        coEvery { userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(any()) } just Runs
        useCase = UpdateGroupUseCaseImpl(
            groupRepository = groupRepository,
            getGroupCurrenciesInUseUseCase = getGroupCurrenciesInUseUseCase,
            userPreferenceRepository = userPreferenceRepository,
            yearMonthProvider = { fixedMonth }
        )
    }

    @Nested
    inner class Invocation {

        @Test
        fun `returns Result success on successful update`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroup.id) } returns testGroup
            coEvery { groupRepository.updateGroup(testGroup) } just Runs

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isSuccess)
        }

        @Test
        fun `wraps repository exception in Result failure`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroup.id) } returns testGroup
            coEvery { groupRepository.updateGroup(testGroup) } throws RuntimeException("Network error")

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
        }

        @Test
        fun `delegates to repository with the provided group`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroup.id) } returns testGroup
            coEvery { groupRepository.updateGroup(any()) } just Runs

            // When
            useCase(testGroup)

            // Then
            coVerify(exactly = 1) { groupRepository.updateGroup(testGroup) }
        }

        @Test
        fun `does not increment monthly counter when mainImagePath is unchanged`() = runTest {
            val existing = testGroup.copy(mainImagePath = "https://cdn.example/cover.jpg")
            val updated = existing.copy(name = "Renamed Trip")
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) {
                userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(any())
            }
        }

        @Test
        fun `does not increment monthly counter when removing cover photo`() = runTest {
            val existing = testGroup.copy(mainImagePath = "https://cdn.example/cover.jpg")
            val updated = existing.copy(mainImagePath = null)
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) {
                userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(any())
            }
        }

        @Test
        fun `increments monthly counter once when adding cover photo where previously null`() = runTest {
            val existing = testGroup.copy(mainImagePath = null)
            val updated = existing.copy(mainImagePath = "/tmp/new-cover.jpg")
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(fixedMonth)
            }
        }

        @Test
        fun `increments monthly counter once when replacing existing cover photo`() = runTest {
            val existing = testGroup.copy(mainImagePath = "https://cdn.example/old.jpg")
            val updated = existing.copy(mainImagePath = "/tmp/new-cover.jpg")
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) {
                userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(fixedMonth)
            }
        }

        @Test
        fun `does not increment monthly counter when validation fails`() = runTest {
            val archived = testGroup.copy(status = GroupStatus.ARCHIVED, mainImagePath = null)
            val updated = archived.copy(mainImagePath = "/tmp/new-cover.jpg")
            coEvery { groupRepository.getGroupById(testGroup.id) } returns archived

            val result = useCase(updated)

            assertTrue(result.isFailure)
            coVerify(exactly = 0) {
                userPreferenceRepository.incrementMonthlyManualReceiptAttachmentCount(any())
            }
        }
    }

    @Nested
    inner class ArchiveGuard {

        @Test
        fun `returns failure when group is archived`() = runTest {
            // Given
            val archivedGroup = testGroup.copy(status = GroupStatus.ARCHIVED)
            coEvery { groupRepository.getGroupById(testGroup.id) } returns archivedGroup

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is GroupArchivedException)
        }

        @Test
        fun `returns failure when group does not exist locally`() = runTest {
            // Given
            coEvery { groupRepository.getGroupById(testGroup.id) } returns null

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        }

        @Test
        fun `does not call updateGroup when group is archived`() = runTest {
            // Given
            val archivedGroup = testGroup.copy(status = GroupStatus.ARCHIVED)
            coEvery { groupRepository.getGroupById(testGroup.id) } returns archivedGroup

            // When
            val result = useCase(testGroup)

            // Then
            assertTrue(result.isFailure)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }
    }

    @Nested
    inner class CurrencyGuard {

        @Test
        fun `fails with CannotRemoveCurrencyException when removing extra currency in use`() = runTest {
            val existing = testGroup.copy(currency = "EUR", extraCurrencies = listOf("JPY"))
            val updated = existing.copy(extraCurrencies = emptyList())
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { getGroupCurrenciesInUseUseCase(testGroup.id) } returns setOf("JPY")

            val result = useCase(updated)

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull() as CannotRemoveCurrencyException
            assertEquals("JPY", exception.currencyCode)
            assertEquals(CannotRemoveCurrencyException.Reason.CURRENCY_IN_USE, exception.reason)
            coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
        }

        @Test
        fun `fails when changing default currency without keeping it in extraCurrencies`() =
            runTest {
                val existing = testGroup.copy(currency = "EUR", extraCurrencies = emptyList())
                val updated = existing.copy(currency = "USD", extraCurrencies = emptyList())
                coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
                coEvery { getGroupCurrenciesInUseUseCase(testGroup.id) } returns setOf("EUR")

                val result = useCase(updated)

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull() as CannotRemoveCurrencyException
                assertEquals("EUR", exception.currencyCode)
                coVerify(exactly = 0) { groupRepository.updateGroup(any()) }
            }

        @Test
        fun `succeeds when changing default currency while keeping previous default in extraCurrencies`() = runTest {
            val existing = testGroup.copy(currency = "EUR", extraCurrencies = emptyList())
            val updated = existing.copy(currency = "USD", extraCurrencies = listOf("EUR"))
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { getGroupCurrenciesInUseUseCase(testGroup.id) } returns setOf("EUR")
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { groupRepository.updateGroup(updated) }
        }

        @Test
        fun `succeeds when removing unused extra currency`() = runTest {
            val existing = testGroup.copy(currency = "EUR", extraCurrencies = listOf("JPY", "USD"))
            val updated = existing.copy(extraCurrencies = listOf("JPY"))
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { getGroupCurrenciesInUseUseCase(testGroup.id) } returns setOf("JPY")
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { groupRepository.updateGroup(updated) }
        }

        @Test
        fun `skips currency usage query when no currencies were removed`() = runTest {
            val existing = testGroup.copy(currency = "JPY", extraCurrencies = emptyList())
            val updated = existing.copy(name = "New Name", currency = "JPY")
            coEvery { groupRepository.getGroupById(testGroup.id) } returns existing
            coEvery { groupRepository.updateGroup(updated) } just Runs

            val result = useCase(updated)

            assertTrue(result.isSuccess)
            coVerify(exactly = 0) { getGroupCurrenciesInUseUseCase(any()) }
        }
    }
}
