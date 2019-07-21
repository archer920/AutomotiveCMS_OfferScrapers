package com.stonesoupprogramming.automotivecms.offers.offerscraper.extensions

import java.time.LocalDate
import java.time.ZoneId
import java.util.*

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
