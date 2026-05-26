package es.pedrazamiguez.splittrip.data.local.service

import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocalDatabaseCleanerServiceImplTest {

    private val appDatabase: AppDatabase = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: LocalDatabaseCleanerServiceImpl

    @Before
    fun setUp() {
        service = LocalDatabaseCleanerServiceImpl(appDatabase, testDispatcher)
    }

    @After
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun clearAll_callsClearAllTablesOnDatabase() = runTest(testDispatcher) {
        // When
        service.clearAll()

        // Then
        coVerify(exactly = 1) { appDatabase.clearAllTables() }
    }
}
