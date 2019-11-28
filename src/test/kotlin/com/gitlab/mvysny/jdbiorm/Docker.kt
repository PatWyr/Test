package com.gitlab.mvysny.jdbiorm

import org.testcontainers.containers.PostgreSQLContainer
import org.zeroturnaround.exec.ProcessExecutor
import org.zeroturnaround.exec.ProcessInitException
import org.zeroturnaround.exec.ProcessResult
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration
import kotlin.test.expect

/**
 * Provides means to start/stop various databases in Docker.
 */
object Docker {
    /**
     * Runs given [command]; if the command fails with exit code different than 0 throws an exception with the command stdout+stderr.
     * @return stdout
     */
    private fun exec(command: String): String {
        val commands = command.split(' ')
        val result = ProcessExecutor().command(commands).readOutput(true).execute()
        check(result.exitValue == 0) { "${result.outputUTF8()}\nProcess failed with ${result.exitValue}" }
        return result.outputUTF8()
    }

    /**
     * Checks whether the "docker" command-line tool is available.
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

    /**
     * @param maxDuration mysql starts sloooooowly
     */
    private fun probeJDBC(url: String, username: String, password: String, maxDuration: Duration = Duration.ofSeconds(30)) {
        // check that we have a proper driver on classpath
        expect(true) { DriverManager.getDriver(url) != null }

        val start = System.currentTimeMillis()
        var lastException: SQLException? = null
        while (Duration.ofMillis(System.currentTimeMillis() - start) < maxDuration) {
            try {
                DriverManager.getConnection(url, username, password).close()
                return
            } catch (e: SQLException) {
                lastException = e
                // back off for a bit
                Thread.sleep(500)
            }
        }
        throw lastException!!
    }

    fun isRunning(): Boolean = exec("docker ps").contains("testing_container")

    fun stopTestingContainer() {
        while (isRunning()) {
            exec("docker stop testing_container")
            // pause a bit, to avoid this failure:
            // java.lang.IllegalStateException: docker: Error response from daemon: Conflict. The container name "/testing_container" is already
            // in use by container 613ff9e5f3de3312dc8ce33f0d3d3d7b40289c04e70c551179b87f835c2ebf3a. You have to remove (or rename) that
            // container to be able to reuse that name..
            Thread.sleep(500)
        }
    }

    /**
     * Runs a new `testing_container` with MySQL 5.7.21
     */
    fun startMysql(version: String = "5.7.21", port: Int = 3306) {
        stopTestingContainer()
        exec("docker run --rm --name testing_container -e MYSQL_ROOT_PASSWORD=mysqlpassword -e MYSQL_DATABASE=db -e MYSQL_USER=testuser -e MYSQL_PASSWORD=mysqlpassword -p 127.0.0.1:$port:3306 -d mysql:$version")
        probeJDBC("jdbc:mysql://localhost:$port/db", "testuser", "mysqlpassword")
    }

    fun stopMysql() {
        stopTestingContainer()
    }

    /**
     * Runs a new `testing_container` with MariaDB 10.1.31
     */
    fun startMariaDB(version: String = "10.1.31", port: Int = 3306) {
        stopTestingContainer()
        exec("docker run --rm --name testing_container -e MYSQL_ROOT_PASSWORD=mysqlpassword -e MYSQL_DATABASE=db -e MYSQL_USER=testuser -e MYSQL_PASSWORD=mysqlpassword -p 127.0.0.1:$port:3306 -d mariadb:$version")
        probeJDBC("jdbc:mariadb://localhost:$port/db", "testuser", "mysqlpassword")
    }

    fun stopMariaDB() {
        stopTestingContainer()
    }
}
