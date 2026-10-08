package com.shanesweeney.mobile.pages;

import com.shanesweeney.mobile.core.Config;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/** Form controls: a text input that echoes what is typed, a switch, a dropdown and two buttons. */
public class FormsScreen extends BasePage {
    private static final By TITLE = byText("Form components");
    private static final By TEXT_INPUT = AppiumBy.accessibilityId("text-input");
    private static final By TYPED_TEXT = AppiumBy.accessibilityId("input-text-result");
    private static final By SWITCH = AppiumBy.accessibilityId("switch");
    private static final By SWITCH_LABEL = AppiumBy.accessibilityId("switch-text");
    private static final By DROPDOWN = AppiumBy.accessibilityId("Dropdown");
    private static final By DROPDOWN_VALUE = byPlatform(
            AppiumBy.androidUIAutomator("new UiSelector().resourceId(\"text_input\")"),
            AppiumBy.accessibilityId("text_input"));
    private static final By ACTIVE_BUTTON = AppiumBy.accessibilityId("button-Active");

    public FormsScreen(AppiumDriver driver) {
        super(driver);
    }

    public FormsScreen typeText(String text) {
        type(TEXT_INPUT, text);
        // Otherwise the keyboard covers the buttons below the input.
        hideKeyboard(TITLE);
        return this;
    }

    public String typedText() {
        return text(TYPED_TEXT);
    }

    public FormsScreen toggleSwitch() {
        String before = text(SWITCH_LABEL);
        tap(SWITCH);
        // The label re-renders after the tap; wait for it so the next read is not stale.
        waitUntil("waiting for the switch label to change", d -> !before.equals(d.findElement(SWITCH_LABEL).getText()));
        return this;
    }

    public String switchLabel() {
        return text(SWITCH_LABEL);
    }

    /** Android opens a native list dialog. The iOS build's picker does not open under XCUITest. */
    public FormsScreen selectDropdownOption(String option) {
        if (Config.PLATFORM == Config.Platform.IOS) {
            throw new UnsupportedOperationException("The dropdown picker does not open under XCUITest");
        }
        scrollTo(DROPDOWN);
        tap(DROPDOWN);
        tap(byText(option));
        return this;
    }

    public String dropdownValue() {
        return text(DROPDOWN_VALUE);
    }

    public AlertDialog tapActiveButton() {
        scrollTo(ACTIVE_BUTTON);
        tap(ACTIVE_BUTTON);
        return new AlertDialog(driver);
    }
}
