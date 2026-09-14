package com.gameplatform.e2e;

import java.util.ArrayList;
import java.util.List;

import com.gameplatform.e2e.pages.LobbyPage;
import com.gameplatform.e2e.pages.LoginPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

// Needs the app running. Env: E2E_BASE_URL, HEADLESS=false
@Tag("e2e")
public abstract class BaseE2E {

    private static final String BASE_URL = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:5173");
    private static final String PASSWORD = "password123";

    private final List<WebDriver> drivers = new ArrayList<>();

    // One browser per player
    protected WebDriver newBrowser() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1400,900");
        if (!"false".equals(System.getenv("HEADLESS"))) {
            options.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(options);
        drivers.add(driver);
        return driver;
    }

    protected LobbyPage registerAndLogin(WebDriver driver, String tag) {
        String username = "qa" + System.currentTimeMillis() + tag;
        driver.get(BASE_URL);
        return new LoginPage(driver)
                .goToRegister()
                .register(username, username + "@example.com", PASSWORD)
                .login(username, PASSWORD);
    }

    @AfterEach
    void quitBrowsers() {
        drivers.forEach(WebDriver::quit);
    }
}
