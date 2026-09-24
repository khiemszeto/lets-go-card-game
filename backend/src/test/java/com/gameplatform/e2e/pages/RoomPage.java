package com.gameplatform.e2e.pages;

import org.openqa.selenium.WebDriver;

public class RoomPage extends BasePage {

    public RoomPage(WebDriver driver) {
        super(driver);
        visible(qa("room-title"));
    }

    public String roomId() {
        return visible(qa("room-title")).getText().replaceAll("\\D", "");
    }

    public void ready() {
        click(qa("ready"));
    }
}
