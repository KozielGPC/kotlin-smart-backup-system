package br.com.backupkotlin

import BackupManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Application {

	@Bean
	fun backupManager() = BackupManager()

	@Bean
	fun backupController(backupManager: BackupManager) = BackupController(backupManager)
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
