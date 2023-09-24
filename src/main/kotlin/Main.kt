import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

fun initBackupDirectory(destinationDir: String) {
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

fun writeFileToBackup(sourceFilePath: String, destinationDir: String) {
    val sourcePath = Paths.get(sourceFilePath)
    val fileName = sourcePath.fileName.toString()
    val destinationPath = Paths.get(destinationDir, fileName)

    try {
        val content = String(Files.readAllBytes(sourcePath))
        Files.write(destinationPath, content.toByteArray())
        println("Arquivo $fileName copiado para o diretório de backup")
    } catch (e: IOException) {
        println("Erro ao copiar o arquivo para o diretório de backup: ${e.message}")
    }
}

fun main(args: Array<String>) {
    val sourceDirectory = "files-to-backup"
    val destinationDirectory = "backup-folder"

    initBackupDirectory(destinationDirectory)

    writeFileToBackup("files-to-backup/texto1.txt", destinationDirectory)
}