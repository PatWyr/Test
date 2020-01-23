package com.gitlab.mvysny.jdbiorm

import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessInitException
import org.zeroturnaround.exec.ProcessResult

object Docker {
    /**
     * Remove when https://github.com/testcontainers/testcontainers-java/issues/2110 is fixed.
     */
    val isPresent: Boolean by lazy {
        try {
            val execute: ProcessResult = ProcessExecutor().command("docker", "version")
                    .readOutput(true)
                    .execute()
            println("docker version: ${execute.outputString()}")
            execute.exitValue == 0
        } catch (e: ProcessInitException) {
            if (e.errorCode == 2) {
                println(e)
                // no such file or directory
                false
            } else {
                throw e
            }
        }
    }
}
