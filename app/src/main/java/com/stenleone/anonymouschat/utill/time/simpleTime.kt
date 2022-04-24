package com.stenleone.anonymouschat.utill.time

import java.text.SimpleDateFormat
import java.util.*

fun getNowUtc(): Date =
    runCatching {
        val defaultFormat = SimpleDateFormat(defaultPattern)
        defaultFormat.timeZone = TimeZone.getDefault()

        val utcFormat = SimpleDateFormat(defaultPattern)
        defaultFormat.timeZone = TimeZone.getTimeZone("UTC")

        utcFormat.parse(defaultFormat.format(Date())!!)!!
    }.getOrDefault(Date())

