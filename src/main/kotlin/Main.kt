import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class BackupManager(private val sourceDir: String, private val destinationDir: String) {
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

    fun copyFileToBackup(sourceFilePath: String) {
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

    fun copyDirectoryContentsToBackup() {
        val sourcePath = Paths.get(sourceDir)
        val destinationPath = Paths.get(destinationDir)

        try {
            Files.walkFileTree(
                sourcePath,
                EnumSet.noneOf(FileVisitOption::class.java),
                Int.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourcePath.relativize(file)
                        val destinationFile = destinationPath.resolve(relativePath)
                        try {
                            Files.createDirectories(destinationFile.parent)
                            Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                            println("Arquivo ${file.fileName} copiado para o diretório de backup")
                        } catch (e: IOException) {
                            println("Erro ao copiar o arquivo para o diretório de backup: ${e.message}")
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                        println("Erro ao visitar o arquivo: $exc")
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourcePath.relativize(dir)
                        val destinationDir = destinationPath.resolve(relativePath)
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

                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc != null) {
                            println("Erro ao visitar o diretório: $exc")
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

        Files.walkFileTree(path, setOf(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = path.relativize(file)
                println("Arquivo encontrado: $relativePath")
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val relativePath = path.relativize(dir)
                println("Pasta encontrada: $relativePath")
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                println("Erro ao visitar o arquivo: ${exc.message}")
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun downloadFilesFromBackup(destinationDir: String) {
        val sourcePath = Paths.get(this.destinationDir) // Usamos a pasta de origem definida na classe

        if (!Files.exists(sourcePath)) {
            println("A pasta de origem (backup-folder) não existe: ${sourcePath.toAbsolutePath()}")
            return
        }

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

        try {
            Files.walkFileTree(
                sourcePath,
                EnumSet.noneOf(FileVisitOption::class.java),
                Int.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourcePath.relativize(file)
                        val destinationFile = destinationPath.resolve(relativePath)
                        try {
                            Files.createDirectories(destinationFile.parent)
                            Files.copy(file, destinationFile, StandardCopyOption.REPLACE_EXISTING)
                            println("Arquivo ${file.fileName} copiado para a pasta de destino")
                        } catch (e: IOException) {
                            println("Erro ao copiar o arquivo para a pasta de destino: ${e.message}")
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path?, exc: IOException?): FileVisitResult {
                        println("Erro ao visitar o arquivo: $exc")
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val relativePath = sourcePath.relativize(dir)
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
            println("Erro ao copiar a pasta de origem para a pasta de destino: ${e.message}")
        }
    }

}

class ApplicationManager(private val backupManager: BackupManager) {

    fun start() {
        val scanner = Scanner(System.`in`)

        while (true) {
            println("Escolha uma opção:")
            println("1. Escolher arquivo para backup")
            println("2. Listar arquivos de backup")
            println("3. Baixar arquivo de backup")
            println("4. Sair")

            val choice = scanner.nextInt()

            when (choice) {
                1 -> chooseFileForBackup()
                2 -> listBackupFiles()
                3 -> downloadBackupFile()
                4 -> {
                    println("Saindo do programa.")
                    return
                }
                else -> println("Opção inválida. Tente novamente.")
            }
        }
    }

    private fun chooseFileForBackup() {
        val scanner = Scanner(System.`in`)
        println("Digite o caminho do arquivo que deseja fazer backup:")
        val filePath = scanner.next()

        backupManager.copyFileToBackup(filePath)
    }

    private fun listBackupFiles() {
        println("Lista de arquivos de backup:")
        backupManager.printAllFilesAndFolders()
    }

    private fun downloadBackupFile() {
        val scanner = Scanner(System.`in`)
        println("Digite o nome do arquivo que deseja baixar:")
        val fileName = scanner.next()
        val destinationDir = "destination-folder"

        val sourceFilePath = "backup-folder/$fileName"
        val destinationFilePath = "$destinationDir/$fileName"

        if (backupManager.isFileModified(sourceFilePath, destinationFilePath)) {
            println("Baixando arquivo...")
            backupManager.downloadFilesFromBackup(destinationDir)
        } else {
            println("O arquivo já existe na pasta de destino ou não foi modificado.")
        }
    }
}


fun main(args: Array<String>) {
    // Inicializa backup
    val sourceDirectory = "files-to-backup"
    val destinationDirectory = "backup-folder"

    val backupManager = BackupManager(sourceDirectory, destinationDirectory)
    val applicationManager = ApplicationManager(backupManager)

    // Salva arquivos escolhidos para backup
    backupManager.copyDirectoryContentsToBackup()

    // Inicializa o gerenciador de aplicação
    applicationManager.start()
}
