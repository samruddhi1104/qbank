package com.qbank.qbank.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Saves a PNG screenshot to target/screenshots whenever a UI test fails. */
public class ScreenshotExtension implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        try {
            if (BaseUiTest.driver instanceof TakesScreenshot shooter) {
                Path dir = Paths.get("target", "screenshots");
                Files.createDirectories(dir);
                String fileName = context.getRequiredTestClass().getSimpleName() + "_"
                        + context.getRequiredTestMethod().getName() + "_"
                        + System.currentTimeMillis() + ".png";
                Path file = dir.resolve(fileName);
                Files.write(file, shooter.getScreenshotAs(OutputType.BYTES));
                System.out.println("Failure screenshot saved: " + file.toAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Could not save screenshot: " + e.getMessage());
        }
    }
}
