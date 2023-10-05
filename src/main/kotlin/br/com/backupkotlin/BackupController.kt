package br.com.backupkotlin

import BackupManager
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.util.concurrent.atomic.AtomicLong

data class DirectoryItem(val name: String, val isDirectory: Boolean)

@RestController
class BackupController(private val backupManager: BackupManager) {
    val counter: AtomicLong = AtomicLong()

    @GetMapping("/backup")
    fun backup(@RequestParam sourcePath: String) {
        backupManager.copyToBackup(sourcePath)
    }

    @GetMapping("/download")
    fun restore(@RequestParam sourcePath: String, @RequestParam destinationPath: String) {
        backupManager.downloadFilesFromBackup(sourcePath, destinationPath)
    }

    @GetMapping("/downloadAll")
    fun downloadAll(@RequestParam destinationPath: String) {
        backupManager.downloadAllFilesFromBackup(destinationPath)
    }

    @GetMapping("/listBackup")
    fun listBackup(): List<DirectoryItem> {
        val items = backupManager.getDirectoryItems()
        return items
    }

    @GetMapping("/deleteBackup")
    fun deleteBackup(@RequestParam filePath: String) {
        backupManager.deleteFileOrFolderFromBackup("backup-folder/$filePath")
    }

    @GetMapping("/clearBackup")
    fun clearBackup() {
        backupManager.clearBackupFolder()
    }
}
