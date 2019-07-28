package com.stonesoupprogramming.automotivecms.offers.offerscraper.dao

import com.stonesoupprogramming.automotivecms.offers.offerscraper.entity.PublishedContent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface PublishedContentDao: JpaRepository<PublishedContent, Long> {

    @Transactional
    fun deleteAllByDateBefore(date: Date)

    fun countByTitleAndSummaryAndLink(title: String, summary: String, link: String): Int

    fun countByTitleAndLink(title: String, link: String): Int
}
