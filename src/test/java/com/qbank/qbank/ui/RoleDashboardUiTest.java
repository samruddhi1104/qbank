package com.qbank.qbank.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleDashboardUiTest extends BaseUiTest {

    private static final By SWITCH_BUTTON = By.xpath("//button[normalize-space()='Switch']");

    @Test
    @DisplayName("TC06 - Switching role keeps the new role and the Dashboard opens")
    void switchRoleAndOpenDashboard() {
        Select role = new Select(driver.findElement(By.id("role")));
        assertEquals("TEACHER", role.getFirstSelectedOption().getAttribute("value"), "Default role");

        role.selectByValue("REVIEWER");
        clickAndWait(SWITCH_BUTTON);

        Select afterSwitch = new Select(driver.findElement(By.id("role")));
        assertEquals("REVIEWER", afterSwitch.getFirstSelectedOption().getAttribute("value"),
                "Role should now be REVIEWER");

        clickAndWait(By.linkText("Dashboard"));
        assertTrue(driver.getCurrentUrl().contains("/dashboard"), "Should be on the dashboard page");
        assertFalse(driver.getPageSource().contains("Whitelabel Error Page"), "Dashboard must load without error");

        // put the role back so later tests start as TEACHER
        driver.get(BASE_URL + "/questions");
        new Select(driver.findElement(By.id("role"))).selectByValue("TEACHER");
        clickAndWait(SWITCH_BUTTON);
    }
}
