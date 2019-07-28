package com.stonesoupprogramming.automotivecms.offers.offerscraper.functions

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao

fun PublishedContentDao.exists(title: String, link: String): Boolean =
        this.countByTitleAndLink(title = title, link = link) == 0

fun PublishedContentDao.exists(title: String, summary: String, link: String): Boolean =
        this.countByTitleAndSummaryAndLink(title = title, summary = summary, link = link) == 0
