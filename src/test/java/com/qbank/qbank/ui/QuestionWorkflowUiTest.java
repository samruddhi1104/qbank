package com.qbank.qbank.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionWorkflowUiTest extends BaseUiTest {

    @Test
    @DisplayName("TC04 - Submitting a draft changes its status to SUBMITTED")
    void submitDraftQuestion() {
        String text = uniqueText("Submit me");
        addQuestion(text, "Testing", "Selenium", "EASY");
        assertEquals("DRAFT", cell(rowWithText(text), 6));

        clickAndWait(rowWithText(text).findElement(By.xpath(".//form[contains(@action,'/status')]//button")));

        assertEquals("SUBMITTED", cell(rowWithText(text), 6), "Status should change to SUBMITTED");

        new Select(driver.findElement(By.id("statusFilter"))).selectByValue("SUBMITTED");
        clickAndWait(By.id("searchBtn"));
        assertEquals(1, rowsWithText(text).size(), "SUBMITTED filter should show the question");

        new Select(driver.findElement(By.id("statusFilter"))).selectByValue("DRAFT");
        clickAndWait(By.id("searchBtn"));
        assertEquals(0, rowsWithText(text).size(), "DRAFT filter should not show it any more");
    }

    @Test
    @DisplayName("TC05 - Deleting a question removes it from the list")
    void deleteQuestion() {
        String text = uniqueText("Delete me");
        addQuestion(text, "Testing", "Cleanup", "HARD");
        assertEquals(1, rowsWithText(text).size());

        clickAndWait(rowWithText(text).findElement(By.xpath(".//form[contains(@action,'/delete')]//button")));

        assertEquals(0, rowsWithText(text).size(), "Deleted question should disappear");
    }
}
