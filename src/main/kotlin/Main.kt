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
