package es.pedrazamiguez.splittrip.data.firebase.storage

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CloudStorageDataSourceImplTest {

    private lateinit var storage: FirebaseStorage
    private lateinit var rootRef: StorageReference
    private lateinit var childRef: StorageReference
    private lateinit var dataSource: CloudStorageDataSourceImpl

    @BeforeEach
    fun setUp() {
        storage = mockk()
        rootRef = mockk()
        childRef = mockk()
        every { storage.reference } returns rootRef
        every { rootRef.child(any()) } returns childRef

        mockkStatic(Uri::class)
        val mockUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns mockUri
        every { Uri.fromFile(any()) } returns mockUri
        every { mockUri.path } returns "/path/to/file.pdf"

        // Mock tasks await extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        dataSource = CloudStorageDataSourceImpl(storage)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun `uploadReceipt uploads file and returns download URL`() = runTest {
        val mockSnapshot = mockk<UploadTask.TaskSnapshot>()
        val mockUploadTask = mockk<UploadTask>()
        every { childRef.putFile(any(), any()) } returns mockUploadTask
        coEvery { mockUploadTask.await() } returns mockSnapshot

        val mockDownloadUri = mockk<Uri>()
        every { mockDownloadUri.toString() } returns "https://firebase.storage/receipt.pdf"
        val mockDownloadUrlTask = mockk<Task<Uri>>()
        every { childRef.downloadUrl } returns mockDownloadUrlTask
        coEvery { mockDownloadUrlTask.await() } returns mockDownloadUri

        val url = dataSource.uploadReceipt("expense-123", "file:///path/to/file.pdf", "application/pdf")

        assertEquals("https://firebase.storage/receipt.pdf", url)
        verify(exactly = 1) { rootRef.child("receipts/expense-123/receipt.pdf") }
    }

    @Test
    fun `uploadReceipt normalises raw path and uploads successfully`() = runTest {
        val mockSnapshot = mockk<UploadTask.TaskSnapshot>()
        val mockUploadTask = mockk<UploadTask>()
        every { childRef.putFile(any(), any()) } returns mockUploadTask
        coEvery { mockUploadTask.await() } returns mockSnapshot

        val mockDownloadUri = mockk<Uri>()
        every { mockDownloadUri.toString() } returns "https://firebase.storage/receipt.jpg"
        val mockDownloadUrlTask = mockk<Task<Uri>>()
        every { childRef.downloadUrl } returns mockDownloadUrlTask
        coEvery { mockDownloadUrlTask.await() } returns mockDownloadUri

        val url = dataSource.uploadReceipt("expense-456", "/raw/filesystem/path/receipt.jpg", "image/jpeg")

        assertEquals("https://firebase.storage/receipt.jpg", url)
        verify(exactly = 1) { rootRef.child("receipts/expense-456/receipt.jpg") }
    }
}
