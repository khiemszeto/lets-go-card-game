package com.gameplatform.e2e.pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class GamePage extends BasePage {

    public GamePage(WebDriver driver) {
        super(driver);
        visible(qa("my-hand"));
    }

    private static By cardIn(String container, String rank, String suit) {
        return By.cssSelector("[data-testid=\"QA:" + container + "\"] [data-testid=\"QA:card-" + rank + "-" + suit + "\"]");
    }

    public List<String> handCardIds() {
        return driver.findElements(By.cssSelector("[data-testid=\"QA:my-hand\"] [data-testid^=\"QA:card-\"]"))
                .stream()
                .map(card -> card.getDomAttribute("data-testid"))
                .toList();
    }

    public GamePage waitForHandSize(int size) {
        wait.until(d -> handCardIds().size() == size);
        return this;
    }

    public void play(String rank, String suit) {
        click(cardIn("my-hand", rank, suit));
        click(qa("play"));
    }

    public void waitForLastPlay(String rank, String suit) {
        visible(cardIn("last-play", rank, suit));
    }
}
