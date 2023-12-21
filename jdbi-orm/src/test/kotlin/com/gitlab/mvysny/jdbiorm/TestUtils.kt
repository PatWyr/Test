package com.gitlab.mvysny.jdbiorm

import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.*

private fun GsonBuilder.registerJavaTimeAdapters(): GsonBuilder = apply {
    Converters.registerAll(this)
}

val gson: Gson = GsonBuilder()
    .registerJavaTimeAdapters()
    .create()
