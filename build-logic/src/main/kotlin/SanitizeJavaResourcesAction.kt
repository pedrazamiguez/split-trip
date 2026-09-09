import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.DirectoryProperty
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

open class SanitizeJavaResourcesAction @Inject constructor(
    private val buildDirectory: DirectoryProperty
) : Action<Task> {

    override fun execute(task: Task) {
        if (!buildDirectory.isPresent) return
        val mergedJavaResDir = buildDirectory.dir("intermediates/merged_java_res").get().asFile
        if (!mergedJavaResDir.exists()) return

        mergedJavaResDir.walkTopDown().filter { it.isFile && it.name == "base.jar" }.forEach { jarFile ->
            val tempFile = File.createTempFile("sanitized", ".jar", jarFile.parentFile)
            var modified = false
            ZipInputStream(jarFile.inputStream().buffered()).use { zipIn ->
                ZipOutputStream(tempFile.outputStream().buffered()).use { zipOut ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name.contains(":") || entry.name.endsWith(".kotlin_module")) {
                            modified = true
                        } else {
                            val newEntry = ZipEntry(entry.name)
                            newEntry.time = entry.time
                            zipOut.putNextEntry(newEntry)
                            zipIn.copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                        entry = zipIn.nextEntry
                    }
                }
            }
            if (modified) {
                tempFile.copyTo(jarFile, overwrite = true)
            }
            tempFile.delete()
        }
    }
}
