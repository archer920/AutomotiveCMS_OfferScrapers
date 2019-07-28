package com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader

private val logger = LoggerFactory.getLogger("com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.WebDriverExtensions")

val jquery: String by lazy {
    BufferedReader(
            InputStreamReader(Any::class.java.getResourceAsStream("/jquery-3.3.1.js"))
    ).use { it.readText() }
}

fun createChromeDriver(headless: Boolean = true): RemoteWebDriver {
    return if (headless){
        val options = ChromeOptions()
        options.addArguments(listOf("--headless", "--no-sandbox", "--disable-gpu"))
        ChromeDriver(options)
    } else {
        ChromeDriver()
    }
}

fun RemoteWebDriver.navigate(url: String, maxAttempts: Int = 10, attemptNumber: Int = 0) {
    try {
        this.get(url)
    } catch (e: Exception){
        logger.error("Error while navigating to $url", e)

        if (attemptNumber < maxAttempts){
            Thread.sleep(60 * 1000)
            navigate(url, maxAttempts, attemptNumber + 1)
        }
    }
}

fun RemoteWebDriver.runJq() {
    this.executeScript(jquery)
}

fun RemoteWebDriver.runScript(script: String): Any? {
    return this.executeScript(script)
}

fun <R> RemoteWebDriver.use(block: (RemoteWebDriver) -> R): R {
    return try {
        block.invoke(this)
    } catch (e: Exception) {
        throw e
    } finally {
        this.close()
    }
}
