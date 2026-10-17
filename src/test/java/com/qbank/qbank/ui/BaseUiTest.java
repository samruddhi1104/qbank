package com.qbank.qbank.ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@ExtendWith(ScreenshotExtension.class)
public abstract class BaseUiTest {

    protected static WebDriver driver;
    protected static final String BASE_URL =
            System.getProperty("base.url", "http://localhost:9090");

    private static final String TABLE_ROWS = "//table[@id='questionTable']//tr[td]";
    private static final Duration WAIT = Duration.ofSeconds(10);

    @BeforeAll
    static void startBrowser() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1366,768");
        driver = new ChromeDriver(options);
    }

    @AfterAll
    static void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void openQuestionsPage() {
        driver.get(BASE_URL + "/questions");
    }

    /** Unique text so reruns never clash with data already in the app. */
    protected static String uniqueText(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Clicks an element that reloads the page (form submit or link) and waits until the
     * new page is fully loaded. Without this wait the test can read the OLD page.
     */
    protected void clickAndWait(By locator) {
        clickAndWait(driver.findElement(locator));
    }

    protected void clickAndWait(WebElement element) {
        WebElement oldPage = driver.findElement(By.tagName("html"));
        element.click();
        WebDriverWait wait = new WebDriverWait(driver, WAIT);
        wait.until(ExpectedConditions.stalenessOf(oldPage));
        wait.until(d -> "complete".equals(
                ((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    /** Fills the Add Question form and clicks Save Question. difficultyValue may be null. */
    protected void addQuestion(String text, String subject, String topic, String difficultyValue) {
        driver.findElement(By.id("text")).sendKeys(text);
        driver.findElement(By.id("subject")).sendKeys(subject);
        driver.findElement(By.id("topic")).sendKeys(topic);
        if (difficultyValue != null) {
            new Select(driver.findElement(By.id("difficulty"))).selectByValue(difficultyValue);
        }
        try {
            clickAndWait(By.id("saveBtn"));
        } catch (TimeoutException e) {
            // the page did not reload (for example the browser blocked the submit);
            // the test's own assertions decide whether that is a failure
        }
    }

    protected int rowCount() {
        return driver.findElements(By.xpath(TABLE_ROWS)).size();
    }

    protected List<WebElement> rowsWithText(String text) {
        return driver.findElements(By.xpath(rowXpath(text)));
    }

    protected WebElement rowWithText(String text) {
        return new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(By.xpath(rowXpath(text))));
    }

    protected String cell(WebElement row, int column) {
        return row.findElement(By.xpath("./td[" + column + "]")).getText().trim();
    }

    private static String rowXpath(String text) {
        return "//table[@id='questionTable']//tr[td[2][contains(normalize-space(.),'" + text + "')]]";
    }
}
