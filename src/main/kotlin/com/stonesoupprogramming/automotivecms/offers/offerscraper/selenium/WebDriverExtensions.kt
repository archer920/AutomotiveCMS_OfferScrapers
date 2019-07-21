package com.stonesoupprogramming.automotivecms.offers.offerscraper.selenium

import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.RemoteWebDriver
import java.io.BufferedReader
import java.io.InputStreamReader

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

fun RemoteWebDriver.navigate(url: String) {
    this.get(url)
}

fun RemoteWebDriver.runJq() {
    this.executeScript(jquery)
}

fun RemoteWebDriver.runScript(script: String): Any {
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
