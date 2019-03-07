package com.beust.perry

import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class MergedProperties(private vararg val filenames: String) {
    val map = hashMapOf<String, String?>()

    init {
        filenames.forEach  { fileName ->
            Properties().apply {
                Paths.get(fileName).let { path ->
                    if (path.toFile().exists()) {
                        Files.newInputStream(path).use {
                            load(it)
                        }
                    }
                }
                this.keys.map { it.toString() }. forEach { key ->
                    map[key] = this[key]?.toString()
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    println(MergedProperties("config.properties", "local.properties").map)
}