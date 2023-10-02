fun main(args: Array<String>) {
    // Inicializa backup manager
    val backupManager = BackupManager()
    val applicationManager = ApplicationManager(backupManager)

    // Inicializa o gerenciador de aplicação
    applicationManager.start()
}
