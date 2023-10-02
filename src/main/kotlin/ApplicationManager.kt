import java.util.*

class ApplicationManager(private val backupManager: BackupManager) {

    fun start() {
        val scanner = Scanner(System.`in`)

        while (true) {
            println("Escolha uma opção:")
            println("1. Escolher arquivo para backup")
            println("2. Listar arquivos de backup")
            println("3. Baixar arquivo de backup")
            println("4. Baixar todos os arquivos de backup")
            println("5. Deletar arquivo de backup")
            println("6. Limpar pasta de backup")
            println("7. Sair")

            val choice = scanner.nextInt()

            when (choice) {
                1 -> chooseFileForBackup()
                2 -> listBackupFiles()
                3 -> downloadBackupFile()
                4 -> downloadAllFilesFromBackup()
                5 -> deleteBackupFile()
                6 -> clearBackupFolder()
                7 -> {
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
        println("Digite o caminho do arquivo que deseja baixar:")
        val fileName = scanner.next()
        val destinationDir = "destination-folder"

        val sourceFilePath = "backup-folder/$fileName"
        val destinationFilePath = "$destinationDir/$fileName"

        if (backupManager.isFileModified(sourceFilePath, destinationFilePath)) {
            println("Baixando arquivo...")
            backupManager.downloadFilesFromBackup(sourceFilePath, destinationDir)
        } else {
            println("O arquivo já existe na pasta de destino ou não foi modificado.")
        }
    }

    private fun downloadAllFilesFromBackup() {
        val scanner = Scanner(System.`in`)
        println("Digite o caminho para destino de backup:")
        val fileName = scanner.next()

        if (backupManager.isFileModified("backup-folder/", fileName)) {
            println("Baixando arquivo...")
            backupManager.downloadAllFilesFromBackup("backup-folder/", fileName)
        } else {
            println("O arquivo já existe na pasta de destino ou não foi modificado.")
        }
    }

    private fun clearBackupFolder() {
        println("Tem certeza de que deseja limpar totalmente a pasta de backup? (S/N)")
        val scanner = Scanner(System.`in`)
        val confirmation = scanner.next().toLowerCase()

        if (confirmation == "s") {
            if (backupManager.clearBackupFolder()) {
                println("Pasta de backup foi limpa com sucesso.")
            } else {
                println("Não foi possível limpar a pasta de backup.")
            }
        } else {
            println("Operação cancelada.")
        }
    }

    private fun deleteBackupFile() {
        val scanner = Scanner(System.`in`)
        println("Digite o nome do arquivo que deseja excluir:")
        val fileName = scanner.next()

        val filePath = "backup-folder/$fileName"

        if (backupManager.deleteFileFromBackup(filePath)) {
            println("Arquivo $fileName foi excluído da pasta de backup.")
        } else {
            println("Não foi possível excluir o arquivo $fileName da pasta de backup.")
        }
    }
}

