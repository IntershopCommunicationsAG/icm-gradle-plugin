package com.intershop.gradle.icm.utils

import org.gradle.api.GradleException
import org.gradle.api.file.RegularFile
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * Encapsulates a file containing a library list. The actual format depends on the used entries but may look like
 * ```
 * org.glassfish.jersey.media:jersey-media-multipart:3.1.7
 * org.glassfish.main.external:ldapbp-repackaged:5.0
 * org.hibernate.validator:hibernate-validator:8.0.1.Final
 * org.javassist:javassist:3.30.2-GA
 * org.jboss.logging:jboss-logging:3.6.0.Final
 * ```
 */
class LibraryListFile(val entries : Set<String>) {

    companion object {
        /**
         * Reads a [LibraryListFile] from a [RegularFile]
         */
        fun read(sourceFile : RegularFile) : LibraryListFile {
            val entries = mutableSetOf<String>()
            try {
                BufferedReader(FileReader(sourceFile.asFile)).use { br ->
                    br.lines().forEach { if (it.isNotEmpty()) entries.add(it) }
                }
            } catch (e: IOException) {
                throw GradleException("It was not possible to read ${sourceFile.asFile}")
            }
            return LibraryListFile(entries)
        }
    }

    /**
     * Writes this [LibraryListFile]'s entries into a [RegularFile]
     */
    fun write(targetFile : RegularFile) {
        val sortedDeps = entries.toList().sorted()
        val listFile = targetFile.asFile

        if (listFile.exists()) {
            listFile.delete()
        }

        listFile.printWriter().use { out ->
            sortedDeps.forEach {
                out.println(it)
            }
        }
    }

    /**
     * Creates a new [LibraryListFile] containing the entries of [this.entries.plus(other.entries)]
     */
    fun plus(other : LibraryListFile) : LibraryListFile = LibraryListFile(entries.plus(other.entries))

    /**
     * Creates a new [LibraryListFile] containing the entries of [this.entries.minus(other.entries)]
     */
    fun minus(other : LibraryListFile) : LibraryListFile = LibraryListFile(entries.minus(other.entries))

    override fun toString(): String {
        return "LibraryListFile(entries=$entries)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LibraryListFile

        return entries == other.entries
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }


}