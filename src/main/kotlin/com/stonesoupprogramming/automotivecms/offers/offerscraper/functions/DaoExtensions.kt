package com.stonesoupprogramming.automotivecms.offers.offerscraper.functions

import com.stonesoupprogramming.automotivecms.offers.offerscraper.dao.PublishedContentDao
import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent

fun PublishedContentDao.exists(publishedContent: PublishedContent): Boolean {
    return this.countByTitleAndSummaryAndLink(
            title = publishedContent.title!!,
            summary = publishedContent.summary!!,
            link = publishedContent.link!!) == 0
}
