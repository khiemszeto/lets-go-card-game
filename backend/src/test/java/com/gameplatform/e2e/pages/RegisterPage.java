package com.gameplatform.e2e.pages;

import org.openqa.selenium.WebDriver;

public class RegisterPage extends BasePage {

    public RegisterPage(WebDriver driver) {
        super(driver);
        visible(qa("register-username"));
    }

    public LoginPage register(String username, String email, String password) {
        type(qa("register-username"), username);
        type(qa("register-email"), email);
        type(qa("register-password"), password);
        click(qa("register-submit"));
        return new LoginPage(driver);
    }
}
