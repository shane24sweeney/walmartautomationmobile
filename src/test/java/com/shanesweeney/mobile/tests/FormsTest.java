package com.shanesweeney.mobile.tests;

import com.shanesweeney.mobile.core.Config;
import com.shanesweeney.mobile.pages.AlertDialog;
import com.shanesweeney.mobile.pages.FormsScreen;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("regression")
class FormsTest extends BaseTest {

    @Test
    @Tag("smoke")
    @DisplayName("echoes typed text")
    void echoesTypedText() {
        FormsScreen forms = tabBar.openForms().typeText("Hello Appium");
        assertEquals("Hello Appium", forms.typedText());
    }

    @Test
    @DisplayName("toggles the switch on and off")
    void togglesSwitch() {
        FormsScreen forms = tabBar.openForms().toggleSwitch();
        assertEquals("Click to turn the switch OFF", forms.switchLabel());
        assertEquals("Click to turn the switch ON", forms.toggleSwitch().switchLabel());
    }

    @Test
    @DisplayName("selects a dropdown option")
    void selectsDropdownOption() {
        assumeTrue(Config.PLATFORM == Config.Platform.ANDROID, "The iOS build's picker does not open under XCUITest");
        FormsScreen forms = tabBar.openForms().selectDropdownOption("Appium is awesome");
        assertEquals("Appium is awesome", forms.dropdownValue());
    }

    @Test
    @DisplayName("active button shows an alert")
    void activeButtonShowsAlert() {
        AlertDialog alert = tabBar.openForms().tapActiveButton();
        assertAll(
                () -> assertEquals("This button is", alert.title()),
                () -> assertEquals("This button is active", alert.message()));
        alert.dismiss();
    }
}
