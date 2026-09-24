package com.gameplatform.e2e.pages;

import org.openqa.selenium.WebDriver;

public class LoginPage extends BasePage {

    public LoginPage(WebDriver driver) {
        super(driver);
        visible(qa("login-username"));
    }

    public RegisterPage goToRegister() {
        click(qa("go-register"));
        return new RegisterPage(driver);
    }

    public LobbyPage login(String username, String password) {
        type(qa("login-username"), username);
        type(qa("login-password"), password);
        click(qa("login-submit"));
        return new LobbyPage(driver);
    }
}
