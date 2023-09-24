import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

fun backupDirectory(sourceDir: String, destinationDir: String) {
    val sourcePath = Paths.get(sourceDir)
    val destinationPath = Paths.get(destinationDir)

    if (!Files.exists(destinationPath)) {
        try {
            Files.createDirectories(destinationPath)
            println("Pasta de destino criada em $destinationDir")
        } catch (e: IOException) {
            println("Erro ao criar pasta de destino: ${e.message}")
            return
        }
    }
}

fun main() {
    val sourceDirectory = "files-to-backup"
    val destinationDirectory = "backup-folder"

    backupDirectory(sourceDirectory, destinationDirectory)
}