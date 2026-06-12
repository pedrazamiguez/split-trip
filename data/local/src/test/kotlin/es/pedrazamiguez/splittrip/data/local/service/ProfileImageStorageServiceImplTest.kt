package es.pedrazamiguez.splittrip.data.local.service

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.domain.model.CropRect
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
class ProfileImageStorageServiceImplTest {

    private lateinit var context: Context
    private lateinit var mockResolver: ContentResolver
    private lateinit var service: ProfileImageStorageServiceImpl
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

        service = ProfileImageStorageServiceImpl(context)
    }

    @After
    fun tearDown() {
        val tempFilesDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "temp_files_dir")
        if (tempFilesDir.exists()) {
            tempFilesDir.deleteRecursively()
        }
        val avatarsDir = File(tempFilesDir, "avatars")
        if (avatarsDir.exists()) {
            avatarsDir.deleteRecursively()
        }
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", originalSdkInt)
        clearAllMocks()
        unmockkAll()
    }

    @Test
    fun saveAndCompressAvatar_withCropRect_cropsDownscalesAndCompresses() = runTest {
        // Given
        val userId = "user-1"
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
            }
            // First pass returns dummy bitmap; decodeStream returns null if inJustDecodeBounds = true.
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 1000
        every { mockOriginalBitmap.height } returns 800
        every { mockOriginalBitmap.isRecycled } returns false

        // On the second call to decodeStream, return the mockOriginalBitmap
        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 100, 80, 500, 400) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap

        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Compressed Avatar Bytes".toByteArray())
            true
        }

        val cropRect = CropRect(left = 0.1f, top = 0.1f, right = 0.6f, bottom = 0.6f)

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, cropRect)

        // Then
        assertNotNull(resultUri)
        assertTrue(resultUri.endsWith("user-1.webp"))

        val savedFile = File(context.filesDir, "avatars/$userId.webp")
        assertTrue(savedFile.exists())
        assertEquals("Compressed Avatar Bytes", savedFile.readText())

        verify(exactly = 1) { mockOriginalBitmap.recycle() }
        verify(exactly = 1) { mockCroppedBitmap.recycle() }
        verify(exactly = 1) { mockScaledBitmap.recycle() }
    }

    @Test
    fun saveAndCompressAvatar_withoutCropRect_cropsAsSquareFromCenter() = runTest {
        // Given
        val userId = "user-2"
        val sourceUriStr = "content://media/picker/image2"
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
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 1000
        every { mockOriginalBitmap.height } returns 800
        every { mockOriginalBitmap.isRecycled } returns false

        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)

        mockkStatic(Bitmap::class)
        // Without cropRect, it should crop a square from the center of size min(1000, 800) = 800.
        // left = (1000 - 800) / 2 = 100. top = (800 - 800) / 2 = 0. width = 800, height = 800.
        every { Bitmap.createBitmap(mockOriginalBitmap, 100, 0, 800, 800) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap

        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Compressed Center Square".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)

        val savedFile = File(context.filesDir, "avatars/$userId.webp")
        assertTrue(savedFile.exists())
        assertEquals("Compressed Center Square", savedFile.readText())
    }

    @Test
    fun saveAndCompressAvatar_withLocalFileProviderUri_resolvesAndReadsDirectly() = runTest {
        // Given
        val userId = "user-local"
        val sourceUriStr = "content://es.pedrazamiguez.splittrip.fileprovider/avatars_temp/avatar_camera_test.jpg"

        every { context.packageName } returns "es.pedrazamiguez.splittrip"

        val tempCacheDir = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "temp_cache_dir").also {
            it.mkdirs()
        }
        val tempAvatarsDir = File(tempCacheDir, "avatars_temp").also { it.mkdirs() }
        File(tempAvatarsDir, "avatar_camera_test.jpg").writeText("Dummy Image Data")
        every { context.cacheDir } returns tempCacheDir

        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockUri.scheme } returns "content"
        every { mockUri.authority } returns "es.pedrazamiguez.splittrip.fileprovider"
        every { mockUri.pathSegments } returns listOf("avatars_temp", "avatar_camera_test.jpg")

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 500
                opts.outHeight = 500
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 500
        every { mockOriginalBitmap.height } returns 500
        every { mockOriginalBitmap.isRecycled } returns false

        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 500, 500) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap

        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Local File WebP Output".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)
        val savedFile = File(context.filesDir, "avatars/$userId.webp")
        assertTrue(savedFile.exists())
        assertEquals("Local File WebP Output", savedFile.readText())

        // Clean up
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun saveAndCompressAvatar_withLocalReceiptFileProviderUri_resolvesAndReadsDirectly() = runTest {
        // Given
        val userId = "user-receipt"
        val sourceUriStr = "content://es.pedrazamiguez.splittrip.fileprovider/receipts/receipt_test.jpg"

        every { context.packageName } returns "es.pedrazamiguez.splittrip"

        val tempFilesDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "temp_files_dir").also {
            it.mkdirs()
        }
        val tempReceiptsDir = File(tempFilesDir, "receipts").also { it.mkdirs() }
        File(tempReceiptsDir, "receipt_test.jpg").writeText("Dummy Receipt Data")
        every { context.filesDir } returns tempFilesDir

        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockUri.scheme } returns "content"
        every { mockUri.authority } returns "es.pedrazamiguez.splittrip.fileprovider"
        every { mockUri.pathSegments } returns listOf("receipts", "receipt_test.jpg")

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 500
                opts.outHeight = 500
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 500
        every { mockOriginalBitmap.height } returns 500
        every { mockOriginalBitmap.isRecycled } returns false

        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)

        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 500, 500) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap

        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Local Receipt WebP Output".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)
        val savedFile = File(context.filesDir, "avatars/$userId.webp")
        assertTrue(savedFile.exists())
        assertEquals("Local Receipt WebP Output", savedFile.readText())

        // Clean up
        tempFilesDir.deleteRecursively()
    }

    @Test
    fun deleteLocalAvatar_existingFile_deletesFile() = runTest {
        // Given
        val userId = "user-3"
        val avatarsDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val testFile = File(avatarsDir, "$userId.webp").also { it.writeText("some bytes") }

        // When
        service.deleteLocalAvatar(userId)

        // Then
        assertTrue("Avatar file should be deleted", !testFile.exists())
    }
}
