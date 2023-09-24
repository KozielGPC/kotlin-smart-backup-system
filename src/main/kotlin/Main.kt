import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

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

fun isFileModified(sourcePath: String, destinationPath: String): Boolean {
    val sourceFile = Paths.get(sourcePath)
    val destinationFile = Paths.get(destinationPath)

    if (!Files.exists(destinationFile)) {
        return true // Se o arquivo de destino não existir, ele foi modificado
    }

    try {
        val sourceAttributes = Files.readAttributes(sourceFile, BasicFileAttributes::class.java)
        val destinationAttributes = Files.readAttributes(destinationFile, BasicFileAttributes::class.java)

        val sourceModifiedTime = sourceAttributes.lastModifiedTime()
        val destinationModifiedTime = destinationAttributes.lastModifiedTime()

        return sourceModifiedTime > destinationModifiedTime
    } catch (e: IOException) {
        println("Erro ao verificar se o arquivo foi modificado: ${e.message}")
        return false // Assumir que houve uma falha na verificação e tratar como não modificado
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

    val sourceFilePath = "files-to-backup/texto1.txt"
    val destinationFilePath = "backup-folder/texto1.txt"

    if (isFileModified(sourceFilePath, destinationFilePath)) {
        println("O arquivo foi modificado e precisa ser copiado para o destino.")
    } else {
        println("O arquivo não foi modificado e não precisa ser copiado para o destino.")
    }
}