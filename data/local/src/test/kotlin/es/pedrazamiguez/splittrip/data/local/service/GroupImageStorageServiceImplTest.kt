package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class GroupImageStorageServiceImplTest {

    private lateinit var context: Context
    private lateinit var mockResolver: ContentResolver
    private lateinit var service: GroupImageStorageServiceImpl
    private val originalSdkInt = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockResolver = mockk(relaxed = true)
        every { context.contentResolver } returns mockResolver

        val tempFilesDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "temp_files_dir").also {
            it.mkdirs()
        }
        every { context.filesDir } returns tempFilesDir

        service = GroupImageStorageServiceImpl(context)
    }

    @After
    fun tearDown() {
        val tempFilesDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "temp_files_dir")
        if (tempFilesDir.exists()) {
            tempFilesDir.deleteRecursively()
        }
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun saveTempGroupImage_copiesDownscalesAndCompresses() = runTest {
        // Given
        val sourceUriStr = "content://media/picker/image"
        val mockUri = mockk<Uri>(relaxed = true)

        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri

        val dummyBytes = "Dummy Image Content".toByteArray()
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream(dummyBytes)

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 1000
                opts.outHeight = 800
                null
            } else {
                mockk<Bitmap>(relaxed = true)
            }
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 1000
        every { mockOriginalBitmap.height } returns 800
        every { mockOriginalBitmap.isRecycled } returns false

        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        every { mockOriginalBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Compressed Group Bytes".toByteArray())
            true
        }

        // When
        val resultUri = service.saveTempGroupImage(sourceUriStr)

        // Then
        assertNotNull(resultUri)
        val tempDir = File(context.filesDir, "groups_temp")
        val savedFiles = tempDir.listFiles() ?: emptyArray()
        assertEquals(1, savedFiles.size)
        assertEquals("Compressed Group Bytes", savedFiles[0].readText())

        verify(exactly = 1) { mockOriginalBitmap.recycle() }
    }

    @Test
    fun commitGroupImage_movesTempFileToPermanentStorage() = runTest {
        // Given
        val groupId = "group-123"
        val tempDir = File(context.filesDir, "groups_temp").also { it.mkdirs() }
        val tempFile = File(tempDir, "group_temp_test.webp").also { it.writeText("Temp Image Bytes") }
        val tempUriStr = tempFile.toURI().toString()

        // When
        val resultUri = service.commitGroupImage(groupId, tempUriStr)

        // Then
        assertNotNull(resultUri)
        val permanentDir = File(context.filesDir, "groups")
        val destFile = File(permanentDir, "$groupId.webp")
        assertTrue(destFile.exists())
        assertEquals("Temp Image Bytes", destFile.readText())
        assertTrue(!tempFile.exists())
    }

    @Test
    fun deleteLocalGroupImage_removesPermanentFile() = runTest {
        // Given
        val groupId = "group-delete"
        val permanentDir = File(context.filesDir, "groups").also { it.mkdirs() }
        val testFile = File(permanentDir, "$groupId.webp").also { it.writeText("Permanent bytes") }

        // When
        service.deleteLocalGroupImage(groupId)

        // Then
        assertTrue("Permanent file should be deleted", !testFile.exists())
    }

    @Test
    fun cleanTempGroupImages_removesOldTempFiles() = runTest {
        // Given
        val tempDir = File(context.filesDir, "groups_temp").also { it.mkdirs() }
        val oldFile = File(tempDir, "group_temp_old.webp").also { it.writeText("old") }
        val newFile = File(tempDir, "group_temp_new.webp").also { it.writeText("new") }

        // Set modification times
        val thresholdTime = System.currentTimeMillis() - (15 * 60 * 1000L) // 15 mins ago
        oldFile.setLastModified(thresholdTime)
        newFile.setLastModified(System.currentTimeMillis())

        // When
        service.cleanTempGroupImages()

        // Then
        assertTrue(!oldFile.exists())
        assertTrue(newFile.exists())
    }
}
