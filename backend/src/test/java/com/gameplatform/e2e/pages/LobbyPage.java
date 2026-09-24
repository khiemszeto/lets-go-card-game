package com.gameplatform.e2e.pages;

import org.openqa.selenium.WebDriver;

public class LobbyPage extends BasePage {

    public LobbyPage(WebDriver driver) {
        super(driver);
        visible(qa("lobby-title"));
    }

    public RoomPage createRoom() {
        click(qa("create-room"));
        return new RoomPage(driver);
    }

    public RoomPage joinRoom(String roomId) {
        type(qa("join-room-input"), roomId);
        click(qa("join-room-submit"));
        return new RoomPage(driver);
    }
}
