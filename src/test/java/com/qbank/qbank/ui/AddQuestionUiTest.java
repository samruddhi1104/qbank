package com.qbank.qbank.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddQuestionUiTest extends BaseUiTest {

    @Test
    @DisplayName("TC01 - Valid question is saved and listed as DRAFT")
    void addValidQuestion() {
        String text = uniqueText("What is Jenkins");

        addQuestion(text, "DevOps", "CI/CD", "MEDIUM");

        WebElement row = rowWithText(text);
        assertEquals("DevOps", cell(row, 3), "Subject should match");
        assertEquals("CI/CD", cell(row, 4), "Topic should match");
        assertEquals("MEDIUM", cell(row, 5), "Difficulty should match");
        assertEquals("DRAFT", cell(row, 6), "New questions start as DRAFT");
    }

    @Test
    @DisplayName("TC02 - Empty form does not add a question")
    void emptyFormAddsNothing() {
        int before = rowCount();

        addQuestion("", "", "", null);

        assertEquals(before, rowCount(), "No new row should be added for an empty form");
        assertTrue(driver.getCurrentUrl().contains("/questions"), "User stays on the questions page");
    }
}
