import java.io.*
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(private val destinationDir: String = "backup-folder") {
    private val hashersFile = "src/main/kotlin/hashers.txt"

    init {
        initBackupDirectory()
    }

    private fun initBackupDirectory() {
        val destinationPath = Paths.get(destinationDir)

        if (!Files.exists(destinationPath)) {
            try {
                Files.createDirectories(destinationPath)
                println("Pasta de destino criada em $destinationDir")
            } catch (e: IOException) {
                println("Erro ao criar pasta de destino: ${e.message}")
            }
        }
    }

    private fun calculateSHA256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fis = FileInputStream(file.toFile())
        val byteArray = ByteArray(1024)
        var bytesRead: Int
        while (fis.read(byteArray).also { bytesRead = it } != -1) {
            digest.update(byteArray, 0, bytesRead)
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun writeHashToFile(fileInfo: String) {
        try {
            val writer = FileWriter(hashersFile, false) // O segundo parâmetro, 'false', indica que você deseja sobrescrever o arquivo
            writer.write(fileInfo)
            writer.close()
        } catch (e: IOException) {
            println("Erro ao escrever informações de hash: ${e.message}")
        }
    }


    private fun generateFileInfo(sourcePath: Path, destinationPath: Path): String {
        val sourceFile = sourcePath.toFile()
        val hash = calculateSHA256(sourcePath)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val lastModified = dateFormat.format(Date(sourceFile.lastModified()))
        return "${destinationPath.toString()}|$hash|$lastModified"
    }

    fun isFileModified(sourcePath: String, destinationPath: String): Boolean {
        val sourceFile = Paths.get(sourcePath)
        val destinationFile = Paths.get(destinationPath)

        if (sourceFile.fileName.toString() == "metadata.txt") {
            return false // Se o arquivo de origem for "metadata.txt", não está modificado
        }

        if (!Files.exists(destinationFile)) {
            return true // Se o arquivo de destino não existir, ele foi modificado
        }

        try {
            val sourceAttributes = Files.readAttributes(sourceFile, BasicFileAttributes::class.java)
            val destinationAttributes = Files.readAttributes(destinationFile, BasicFileAttributes::class.java)

            val sourceModifiedTime = sourceAttributes.lastModifiedTime()
            val destinationModifiedTime = destinationAttributes.lastModifiedTime()

            return sourceModifiedTime.toMillis() > destinationModifiedTime.toMillis()
        } catch (e: IOException) {
            println("Erro ao verificar se o arquivo foi modificado: ${e.message}")
            return false // Assumir que houve uma falha na verificação e tratar como não modificado
        }
    }

    fun copyToBackup(sourcePath: String) {
        val sourceFile = Paths.get(sourcePath)
        val destinationPath = Paths.get(destinationDir)

        if (!Files.exists(sourceFile)) {
            println("O arquivo ou pasta de origem não existe: $sourcePath")
            return
        }

        if (Files.isRegularFile(sourceFile)) {
            copyFileToBackup(sourceFile, destinationPath)
        } else if (Files.isDirectory(sourceFile)) {
            copyDirectoryToBackup(sourceFile, destinationPath)
        } else {
            println("Tipo de arquivo não suportado: $sourcePath")
        }
    }

    fun copyFileToBackup(sourceFile: Path, destinationPath: Path) {
        val sourceFilePath = sourceFile.toFile()
        val relativePath = sourceFile.fileName
        val destinationFile = destinationPath.resolve(relativePath)

        if (isFileModified(sourceFilePath.path, destinationFile.toString())) {
            try {
                Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                println("Arquivo ${sourceFile.fileName} copiado para o diretório de backup")
                val fileInfo = generateFileInfo(sourceFile, destinationFile)
                writeHashToFile(fileInfo)
            } catch (e: IOException) {
                println("Erro ao copiar o arquivo para o diretório de backup: ${e.message}")
            }
        } else {
            println("...")
        }
    }

    fun copyDirectoryToBackup(sourceDirectory: Path, destinationPath: Path) {
        val destinationDir = destinationPath.resolve(sourceDirectory.fileName)

        if (!Files.exists(destinationDir)) {
            try {
                Files.createDirectories(destinationDir)
            } catch (e: IOException) {
                println("Erro ao criar diretório de backup: ${e.message}")
                return
            }
        }

        try {
            Files.walkFileTree(
                sourceDirectory,
                EnumSet.noneOf(FileVisitOption::class.java),
                Int.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourceDirectory.relativize(file)
                        val destinationFile = destinationDir.resolve(relativePath)

                        if (isFileModified(file.toString(), destinationFile.toString())) {
                            try {
                                Files.createDirectories(destinationFile.parent)
                                Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                                println("Arquivo ${file.fileName} copiado para o diretório de backup")
                                val fileInfo = generateFileInfo(file, destinationFile)
                                writeHashToFile(fileInfo)
                            } catch (e: IOException) {
                                println("Erro ao copiar o arquivo para o diretório de backup: ${e.message}")
                            }
                        } else {
                            println("...")
                        }

                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourceDirectory.relativize(dir)
                        val destinationDir = destinationDir.resolve(relativePath)
                        if (!Files.exists(destinationDir)) {
                            try {
                                Files.createDirectories(destinationDir)
                            } catch (e: IOException) {
                                println("Erro ao criar diretório de backup: ${e.message}")
                                return FileVisitResult.TERMINATE
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }
                }
            )
        } catch (e: IOException) {
            println("Erro ao copiar o diretório para o diretório de backup: ${e.message}")
        }
    }


    fun printAllFilesAndFolders() {
        val path = Paths.get(destinationDir)

        if (!Files.exists(path)) {
            println("O diretório não existe: $destinationDir")
            return
        }

        println("Conteúdo do diretório de backup:")
        printDirectoryContents(path, 0)
    }

    private fun printDirectoryContents(directory: Path, depth: Int) {
        val prefix = " ".repeat(depth * 4)

        Files.list(directory)
            .filter { Files.isRegularFile(it) }
            .forEach { entry ->
                val relativePath = directory.relativize(entry).toString()
                println("$prefix\uD83D\uDCC4 $relativePath")
            }

        Files.list(directory)
            .filter { Files.isDirectory(it) }
            .forEach { entry ->
                val relativePath = directory.relativize(entry).toString()
                println("$prefix\uD83D\uDCC1 $relativePath")
                printDirectoryContents(entry, depth + 1)
            }
    }


    fun downloadFilesFromBackup(sourcePath: String, destinationPath: String) {
        val sourceDir = Paths.get(sourcePath)
        val destinationDir = Paths.get(destinationPath)

        if (!Files.exists(sourceDir)) {
            println("A pasta de origem não existe: ${sourceDir.toAbsolutePath()}")
            return
        }

        if (!Files.exists(destinationDir)) {
            try {
                Files.createDirectories(destinationDir)
                println("Pasta de destino criada em $destinationPath")
            } catch (e: IOException) {
                println("Erro ao criar pasta de destino: ${e.message}")
                return
            }
        }

        if (Files.isDirectory(sourceDir)) {
            // Obtém o nome da pasta de origem
            val sourceDirName = sourceDir.fileName.toString()
            // Cria um novo diretório no destino com o mesmo nome da pasta de origem
            val destinationSubDir = destinationDir.resolve(sourceDirName)

            try {
                Files.createDirectories(destinationSubDir)
                println("Pasta de origem copiada para a pasta de destino")

                Files.walkFileTree(
                    sourceDir,
                    EnumSet.noneOf(FileVisitOption::class.java),
                    Int.MAX_VALUE,
                    object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val fileName = file.fileName.toString()
                            if (fileName != "metadata.txt") {  // Verifica se o arquivo não é "metadata.txt"
                                val relativePath = sourceDir.relativize(file)
                                val destinationFile = destinationSubDir.resolve(relativePath)
                                try {
                                    Files.createDirectories(destinationFile.parent)
                                    Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                                    println("Arquivo $fileName copiado para a pasta de destino")
                                } catch (e: IOException) {
                                    println("Erro ao copiar o arquivo para a pasta de destino: ${e.message}")
                                }
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                            println("Erro ao visitar o arquivo: $exc")
                            return FileVisitResult.CONTINUE
                        }

                        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                            val relativePath = sourceDir.relativize(dir)
                            val destinationDir = destinationSubDir.resolve(relativePath)
                            if (!Files.exists(destinationDir)) {
                                try {
                                    Files.createDirectories(destinationDir)
                                } catch (e: IOException) {
                                    println("Erro ao criar diretório de destino: ${e.message}")
                                    return FileVisitResult.TERMINATE
                                }
                            }
                            return FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                            if (exc != null) {
                                println("Erro ao visitar o diretório: $exc")
                            }
                            return FileVisitResult.CONTINUE
                        }
                    }
                )
            } catch (e: IOException) {
                println("Erro ao copiar a pasta de origem para a pasta de destino: ${e.message}")
            }
        } else {
            // Se não for um diretório, copie o arquivo diretamente
            val fileName = sourceDir.fileName.toString()
            if (fileName != "metadata.txt") {  // Verifica se o arquivo não é "metadata.txt"
                try {
                    val destinationFile = destinationDir.resolve(sourceDir.fileName)
                    Files.copy(sourceDir, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                    println("Arquivo $fileName copiado para a pasta de destino")
                } catch (e: IOException) {
                    println("Erro ao copiar o arquivo para a pasta de destino: ${e.message}")
                }
            }
        }
    }

    fun downloadAllFilesFromBackup(backupDir: String, destinationDir: String) {
        val sourceDir = Paths.get(backupDir)
        val destinationPath = Paths.get(destinationDir)

        if (!Files.exists(sourceDir)) {
            println("A pasta de backup não existe: ${sourceDir.toAbsolutePath()}")
            return
        }

        if (!Files.isDirectory(sourceDir)) {
            println("O caminho de backup não é um diretório válido.")
            return
        }

        if (!Files.exists(destinationPath)) {
            try {
                Files.createDirectories(destinationPath)
                println("Pasta de destino criada em $destinationDir")
            } catch (e: IOException) {
                println("Erro ao criar pasta de destino: ${e.message}")
                return
            }
        }

        try {
            Files.walkFileTree(
                sourceDir,
                EnumSet.noneOf(FileVisitOption::class.java),
                Int.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val fileName = file.fileName.toString()
                        if (fileName != "metadata.txt") {  // Verifica se o arquivo não é "metadata.txt"
                            val relativePath = sourceDir.relativize(file)
                            val destinationFile = destinationPath.resolve(relativePath)
                            try {
                                Files.createDirectories(destinationFile.parent)
                                Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                                println("Arquivo $fileName copiado para a pasta de destino")
                            } catch (e: IOException) {
                                println("Erro ao copiar o arquivo para a pasta de destino: ${e.message}")
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                        println("Erro ao visitar o arquivo: $exc")
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourceDir.relativize(dir)
                        val destinationDir = destinationPath.resolve(relativePath)
                        if (!Files.exists(destinationDir)) {
                            try {
                                Files.createDirectories(destinationDir)
                            } catch (e: IOException) {
                                println("Erro ao criar diretório de destino: ${e.message}")
                                return FileVisitResult.TERMINATE
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc != null) {
                            println("Erro ao visitar o diretório: $exc")
                        }
                        return FileVisitResult.CONTINUE
                    }
                }
            )
        } catch (e: IOException) {
            println("Erro ao copiar arquivos do diretório de backup: ${e.message}")
        }
    }

    fun deleteFileFromBackup(filePath: String): Boolean {
        val fileToDelete = Paths.get(filePath)

        if (!Files.exists(fileToDelete)) {
            println("O arquivo a ser excluído não existe na pasta de backup.")
            return false
        }

        try {
            Files.delete(fileToDelete)
            println("Arquivo $filePath foi excluído da pasta de backup.")
            return true
        } catch (e: IOException) {
            println("Erro ao excluir o arquivo da pasta de backup: ${e.message}")
            return false
        }
    }

    fun clearBackupFolder(): Boolean {
        val backupDir = Paths.get(destinationDir)

        if (!Files.exists(backupDir)) {
            println("A pasta de backup não existe.")
            return false
        }

        try {
            Files.walk(backupDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.delete(it) }
            return true
        } catch (e: IOException) {
            println("Erro ao limpar a pasta de backup: ${e.message}")
            return false
        }
    }
}
