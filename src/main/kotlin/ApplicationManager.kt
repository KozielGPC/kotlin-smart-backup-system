import java.util.*

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

        backupManager.copyDirectoryToBackup(filePath)
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

