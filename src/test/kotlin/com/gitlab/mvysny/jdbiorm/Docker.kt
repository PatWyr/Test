package com.gitlab.mvysny.jdbiorm

import org.testcontainers.DockerClientFactory

fun DockerClientFactory.isDockerAvailable(): Boolean {
    // todo Remove when https://github.com/testcontainers/testcontainers-java/issues/2110 is fixed.
    return try {
        client()
        true
    } catch (ex: Exception) {
        false
    }
}
