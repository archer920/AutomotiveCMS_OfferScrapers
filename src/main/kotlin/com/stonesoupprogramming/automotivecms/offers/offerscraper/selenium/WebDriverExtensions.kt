package com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader

private val logger = LoggerFactory.getLogger("com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium.WebDriverExtensions")

val jquery: String by lazy {
    logger.info("Loading jquery-3.3.1.js")
    try {
        BufferedReader(
                InputStreamReader(Any::class.java.getResourceAsStream("/jquery-3.3.1.js"))
        ).use {
            it.readText()
        }
    } catch (e: Exception){
        try {
            val f = File(System.getProperty("user.dir") + "/src/main/resources/jquery-3.3.1.js")
            if (!f.exists()){
                logger.info("FIXME: $f does not exist")
            }
            BufferedReader(
                    FileReader(f)
            ).use {
                it.readText()
            }
        } catch (e: Exception){
            logger.error("Failed to load jquery", e)
            throw e
        }
    }
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

fun waitUntilClickable(driver: RemoteWebDriver, webElement: WebElement, timeout: Long = 10){
    WebDriverWait(driver, timeout).until(ExpectedConditions.elementToBeClickable(webElement))
}

fun waitUntilAllPresent(driver: RemoteWebDriver, by: By, timeout: Long = 10){
    WebDriverWait(driver, timeout).until(ExpectedConditions.presenceOfAllElementsLocatedBy(by))
}
