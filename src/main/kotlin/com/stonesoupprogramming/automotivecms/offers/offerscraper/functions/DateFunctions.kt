package com.stonesoupprogramming.automotivecms.offers.offerscraper.functions

import java.time.LocalDate
import java.time.ZoneId
import java.util.*

fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

fun expirationDate(): Date = LocalDate.now().minusDays(System.getenv()["PUBLICATION_EXP_DAYS"]!!.toLong()).toDate()
