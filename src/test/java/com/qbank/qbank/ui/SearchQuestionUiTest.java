package com.qbank.qbank.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQuestionUiTest extends BaseUiTest {

    @Test
    @DisplayName("TC03 - Search shows only matching questions and Clear restores the list")
    void searchByKeyword() {
        String alpha = uniqueText("Alpha");
        String beta = uniqueText("Beta");
        addQuestion(alpha + " question", "Java", "Streams", "EASY");
        addQuestion(beta + " question", "Python", "Loops", "HARD");

        driver.findElement(By.id("keyword")).sendKeys(alpha);
        clickAndWait(By.id("searchBtn"));

        assertEquals(1, rowsWithText(alpha).size(), "Matching question should be shown");
        assertEquals(0, rowsWithText(beta).size(), "Non-matching question should be hidden");

        clickAndWait(By.linkText("Clear"));

        assertEquals(1, rowsWithText(alpha).size(), "Alpha is visible again after Clear");
        assertEquals(1, rowsWithText(beta).size(), "Beta is visible again after Clear");
    }
}
