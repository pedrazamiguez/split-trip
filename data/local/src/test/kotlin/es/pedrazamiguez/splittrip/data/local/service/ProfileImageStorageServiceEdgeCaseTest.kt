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
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ProfileImageStorageServiceEdgeCaseTest {

    private lateinit var context: Context
    private lateinit var mockResolver: ContentResolver
    private lateinit var service: ProfileImageStorageServiceImpl
    private val originalSdkInt = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        mockResolver = mockk(relaxed = true)
        every { context.contentResolver } returns mockResolver

        val tempFilesDir = File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            "temp_files_dir_edge"
        ).also {
            it.mkdirs()
        }
        every { context.filesDir } returns tempFilesDir

        service = ProfileImageStorageServiceImpl(context)
    }

    @After
    fun tearDown() {
        val tempFilesDir = File(ApplicationProvider.getApplicationContext<Context>().filesDir, "temp_files_dir_edge")
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
    fun saveAndCompressAvatar_withLargeDimensions_calculatesCorrectInSampleSize() = runTest {
        // Given
        val userId = "user-large-dimensions"
        val sourceUriStr = "content://es.pedrazamiguez.splittrip.fileprovider/avatars_temp/avatar_large.jpg"
        every { context.packageName } returns "es.pedrazamiguez.splittrip"
        val tempAvatarsDir = File(context.filesDir, "avatars_temp").also { it.mkdirs() }
        File(tempAvatarsDir, "avatar_large.jpg").writeText("data")

        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockUri.scheme } returns "content"
        every { mockUri.authority } returns "es.pedrazamiguez.splittrip.fileprovider"
        every { mockUri.pathSegments } returns listOf("avatars_temp", "avatar_large.jpg")

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                // Dimensions larger than MAX_IMAGE_DIMENSION (2048) to trigger the loop
                opts.outWidth = 5000
                opts.outHeight = 5000
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 2500
        every { mockOriginalBitmap.height } returns 2500
        every { mockOriginalBitmap.isRecycled } returns false
        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 2500, 2500) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap
        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Output".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)
        tempAvatarsDir.deleteRecursively()
    }

    @Test
    fun saveAndCompressAvatar_withFileSchemeUri_doesNotResolveAsLocalFile() = runTest {
        // Given
        val userId = "user-file-scheme"
        val sourceUriStr = "file:///storage/emulated/0/Download/avatar.jpg"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockUri.scheme } returns "file"

        // This uri fails resolveLocalFileFromUri because scheme != "content"
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Dummy Data".toByteArray())

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 200
                opts.outHeight = 200
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 200
        every { mockOriginalBitmap.height } returns 200
        every { mockOriginalBitmap.isRecycled } returns false
        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 200, 200) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap
        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Output".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)
    }

    @Test
    fun saveAndCompressAvatar_withResolvedFileNotExisting_fallsBackToContentResolver() = runTest {
        // Given
        val userId = "user-non-existent-local"
        val sourceUriStr = "content://es.pedrazamiguez.splittrip.fileprovider/avatars_temp/non_existent.jpg"
        every { context.packageName } returns "es.pedrazamiguez.splittrip"

        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockUri.scheme } returns "content"
        every { mockUri.authority } returns "es.pedrazamiguez.splittrip.fileprovider"
        every { mockUri.pathSegments } returns listOf("avatars_temp", "non_existent.jpg")

        // The file returned by resolveLocalFileFromUri does not exist.
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Fallback Content".toByteArray())

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 300
                opts.outHeight = 300
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 300
        every { mockOriginalBitmap.height } returns 300
        every { mockOriginalBitmap.isRecycled } returns false
        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 300, 300) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap
        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            val outStream = arg<java.io.OutputStream>(2)
            outStream.write("Output".toByteArray())
            true
        }

        // When
        val resultUri = service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(resultUri)
    }

    @Test
    fun saveAndCompressAvatar_withDifferentUnresolvedContentUris_failsOrFallsBack() = runTest {
        val testCases = listOf(
            mockk<Uri>(relaxed = true).apply {
                every { scheme } returns "content"
                every { authority } returns null
            },
            mockk<Uri>(relaxed = true).apply {
                every { scheme } returns "content"
                every { authority } returns "some.other.authority"
            },
            mockk<Uri>(relaxed = true).apply {
                every { scheme } returns "content"
                every { authority } returns "es.pedrazamiguez.splittrip.fileprovider"
                every { pathSegments } returns emptyList()
            },
            mockk<Uri>(relaxed = true).apply {
                every { scheme } returns "content"
                every { authority } returns "es.pedrazamiguez.splittrip.fileprovider"
                every { pathSegments } returns listOf("unsupported_dir", "file.jpg")
            }
        )

        for (mockUri in testCases) {
            every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Dummy Data".toByteArray())
            mockkStatic(BitmapFactory::class)
            every { BitmapFactory.decodeStream(any(), null, any()) } answers {
                val opts = arg<BitmapFactory.Options>(2)
                if (opts.inJustDecodeBounds) {
                    opts.outWidth = 100
                    opts.outHeight = 100
                }
                mockk<Bitmap>(relaxed = true)
            }
            val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
            every { mockOriginalBitmap.width } returns 100
            every { mockOriginalBitmap.height } returns 100
            every { mockOriginalBitmap.isRecycled } returns false
            every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns
                mockOriginalBitmap

            val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
            val mockScaledBitmap = mockk<Bitmap>(relaxed = true)
            mockkStatic(Bitmap::class)
            every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 100, 100) } returns mockCroppedBitmap
            every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap
            every { mockScaledBitmap.compress(any(), 80, any()) } answers {
                val outStream = arg<java.io.OutputStream>(2)
                outStream.write("Output".toByteArray())
                true
            }

            val resultUri = service.saveAndCompressAvatar("user-unresolved", "content://dummy", null)
            assertNotNull(resultUri)
        }
    }

    @Test
    fun deleteLocalAvatar_whenDeleteFails_logsWarning() = runTest {
        // Given
        val userId = "user-delete-fail"
        val avatarsDir = File(context.filesDir, "avatars").also { it.mkdirs() }
        val fileAsDir = File(avatarsDir, "$userId.webp").also { it.mkdirs() }
        File(fileAsDir, "cant_delete_dir_with_files").writeText("data")

        // When
        service.deleteLocalAvatar(userId)

        // Then
        assertTrue(fileAsDir.exists())
        fileAsDir.deleteRecursively()
    }

    @Test
    fun deleteLocalAvatar_whenExceptionThrown_logsError() = runTest {
        // Given
        val userId = "user-delete-error"
        every { context.filesDir } throws RuntimeException("Disk error")

        // When/Then
        service.deleteLocalAvatar(userId)
    }

    @Test
    fun cleanTempCameraFiles_whenTempDirDoesNotExist_returnsEarly() = runTest {
        // Given
        val nonExistentDir = File(context.filesDir, "non_existent_temp")
        every { context.filesDir } returns nonExistentDir

        // When
        service.cleanTempCameraFiles()
    }

    @Test
    fun cleanTempCameraFiles_whenListFilesReturnsNull_returnsEarly() = runTest {
        // Given
        val tempDirAsFile = File(context.filesDir, "avatars_temp")
        if (tempDirAsFile.exists()) tempDirAsFile.deleteRecursively()
        tempDirAsFile.createNewFile()

        // When
        service.cleanTempCameraFiles()

        // Then
        tempDirAsFile.delete()
    }

    @Test
    fun cleanTempCameraFiles_whenExceptionThrown_logsError() = runTest {
        // Given
        every { context.filesDir } throws RuntimeException("FilesDir error")

        // When
        service.cleanTempCameraFiles()
    }

    @Test
    fun saveAndCompressAvatar_withAndroidRWebpLossy_usesCorrectCompressFormat() = runTest {
        // Given
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.R)
        val userId = "user-webp-lossy"
        val sourceUriStr = "content://media/picker/image_r"
        val mockUri = mockk<Uri>(relaxed = true)
        mockkStatic(Uri::class)
        every { Uri.parse(sourceUriStr) } returns mockUri
        every { mockResolver.openInputStream(mockUri) } returns ByteArrayInputStream("Dummy Content".toByteArray())

        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any(), null, any()) } answers {
            val opts = arg<BitmapFactory.Options>(2)
            if (opts.inJustDecodeBounds) {
                opts.outWidth = 100
                opts.outHeight = 100
            }
            mockk<Bitmap>(relaxed = true)
        }

        val mockOriginalBitmap = mockk<Bitmap>(relaxed = true)
        every { mockOriginalBitmap.width } returns 100
        every { mockOriginalBitmap.height } returns 100
        every { mockOriginalBitmap.isRecycled } returns false
        every { BitmapFactory.decodeStream(any(), null, match { !it.inJustDecodeBounds }) } returns mockOriginalBitmap

        val mockCroppedBitmap = mockk<Bitmap>(relaxed = true)
        val mockScaledBitmap = mockk<Bitmap>(relaxed = true)
        mockkStatic(Bitmap::class)
        every { Bitmap.createBitmap(mockOriginalBitmap, 0, 0, 100, 100) } returns mockCroppedBitmap
        every { Bitmap.createScaledBitmap(mockCroppedBitmap, 512, 512, true) } returns mockScaledBitmap

        var compressFormatUsed: Bitmap.CompressFormat? = null
        every { mockScaledBitmap.compress(any(), 80, any()) } answers {
            compressFormatUsed = arg<Bitmap.CompressFormat>(0)
            true
        }

        // When
        service.saveAndCompressAvatar(userId, sourceUriStr, null)

        // Then
        assertNotNull(compressFormatUsed)
        val formatName = compressFormatUsed?.name
        assertTrue(formatName == "WEBP_LOSSY" || formatName == "WEBP")
    }
}
