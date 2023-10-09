import br.com.backupkotlin.DirectoryItem
import org.springframework.web.multipart.MultipartFile
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

    fun uploadFileToBackup(file: MultipartFile): String {
        this.initBackupDirectory()
        val originalFilename = file.originalFilename ?: ""
        val destinationPath = Paths.get(destinationDir, originalFilename)

        var fis: FileInputStream? = null

        try {
            file.transferTo(destinationPath)

            // Abra o arquivo com FileInputStream
            fis = FileInputStream(destinationPath.toFile())

            val fileInfo = generateFileInfo(destinationPath, destinationPath)
            writeHashToFile(fileInfo)

            fis.close()


            return "Upload do arquivo $originalFilename realizado com sucesso."
        } catch (e: IOException) {
            println("Erro ao fazer upload do arquivo: ${e.message}")
            return "Erro ao fazer upload do arquivo."
        } finally {
            fis?.close()
        }
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
        val byteArray = ByteArray(1024)
        var bytesRead: Int

        FileInputStream(file.toFile()).use { fis ->
            while (fis.read(byteArray).also { bytesRead = it } != -1) {
                digest.update(byteArray, 0, bytesRead)
            }
        }

        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }


    private fun writeHashToFile(fileInfo: String) {
        try {
            File(hashersFile).appendText(fileInfo + "\n")
        } catch (e: IOException) {
            println("Erro ao escrever informações de hash: ${e.message}")
        }
    }

    private fun generateFileInfo(sourcePath: Path, destinationPath: Path): String {
        try {
            val sourceFile = sourcePath.toFile()
            val hash = calculateSHA256(sourcePath)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val lastModified = dateFormat.format(Date(sourceFile.lastModified()))
            return "${destinationPath.toString()}|$hash|$lastModified"
        } catch (e: IOException) {
            println("Erro ao gerar informações de arquivo: ${e.message}")
            return ""
        }
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
            println("Arquivos não foram modificados, então não precisam ser copiados")
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
                            println("Arquivos não foram modificados, então não precisam ser copiados")
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

    fun getDirectoryItems(): List<DirectoryItem> {
        this.initBackupDirectory();
        val path = Paths.get(destinationDir)

        if (!Files.exists(path)) {
            println("O diretório não existe: $destinationDir")
            return emptyList()
        }

        val items = mutableListOf<DirectoryItem>()
        Files.walkFileTree(
            path,
            EnumSet.noneOf(FileVisitOption::class.java),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = path.relativize(file).toString()
                    items.add(DirectoryItem(relativePath, isDirectory = false))
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = path.relativize(dir).toString()
                    items.add(DirectoryItem(relativePath, isDirectory = true))
                    return FileVisitResult.CONTINUE
                }
            }
        )

        return items
    }

    fun downloadFilesFromBackup(sourcePath: String, destinationPath: String) {
        this.initBackupDirectory();
        val sourceDir = Paths.get("backup-folder").resolve(sourcePath)
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
            val sourceDirName = sourceDir.fileName.toString()
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
                            if (fileName != "metadata.txt") {
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
            val fileName = sourceDir.fileName.toString()
            if (fileName != "metadata.txt") {
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

    fun downloadAllFilesFromBackup(destinationDir: String) {
        this.initBackupDirectory();
        val sourceDir = Paths.get("backup-folder")
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
                        if (fileName != "metadata.txt") {
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

    fun deleteFileOrFolderFromBackup(filePath: String): Boolean {
        this.initBackupDirectory();
        val fileOrFolderToDelete = Paths.get(filePath)

        if (!Files.exists(fileOrFolderToDelete)) {
            println("O arquivo ou pasta a ser excluído não existe na pasta de backup.")
            return false
        }

        try {
            if (Files.isDirectory(fileOrFolderToDelete)) {
                Files.walkFileTree(fileOrFolderToDelete, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        Files.delete(file)
                        println("Arquivo ${file.fileName} foi excluído da pasta de backup.")
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc == null) {
                            Files.delete(dir)
                            println("Pasta ${dir.fileName} foi excluída da pasta de backup.")
                            return FileVisitResult.CONTINUE
                        } else {
                            println("Erro ao excluir a pasta: $exc")
                            return FileVisitResult.TERMINATE
                        }
                    }
                })
                println("Pasta $filePath e seu conteúdo foram excluídos da pasta de backup.")
            } else {
                Files.delete(fileOrFolderToDelete)
                println("Arquivo $filePath foi excluído da pasta de backup.")
            }
            return true
        } catch (e: IOException) {
            println("Erro ao excluir o arquivo ou pasta da pasta de backup: ${e.message}")
            return false
        }
    }

    fun clearBackupFolder(): Boolean {
        this.initBackupDirectory();
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
