package com.intershop.gradle.icm.utils

import org.gradle.api.file.RegularFile
import org.gradle.api.logging.Logger
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object DependencyListUtil {

    fun getIDList(logger: Logger, envType: String, listFile: RegularFile): List<String> {
        val list = mutableListOf<String>()
        try {
            BufferedReader(FileReader(listFile.asFile)).use { br ->
                br.lines().forEach { if (it.isNotEmpty()) list.add(it) }
            }
        } catch (e: IOException) {
            logger.warn("It was not possible to read {} for {}", listFile.asFile, envType)
        }
        return list.sorted()
    }
}
