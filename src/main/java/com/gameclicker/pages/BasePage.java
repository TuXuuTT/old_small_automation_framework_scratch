package com.gameclicker.pages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertEquals;

public abstract class BasePage {

    public static final int TIME_WAIT_SECONDS = 10;
    private static final int SCRIPT_TIME_OUT_WAIT_SECONDS = 60;
    private static final int PAGE_LOAD_TIME_WAIT_SECONDS = 120;
    private static Properties props = new Properties();
    protected final Logger log = LogManager.getLogger(this);
    private final WebDriver wd;
    private Wait<WebDriver> visibilityWait;
    private Wait<WebDriver> invisibilityWait;

    public BasePage(WebDriver wd) {
        this.wd = wd;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream webStream = loader.getResourceAsStream("env.properties");
        try {
            props.load(webStream);
            webStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        visibilityWait = new FluentWait<WebDriver>(getWebDriverCurrent())
                .withTimeout(TIME_WAIT_SECONDS * 10L, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        invisibilityWait
                = new FluentWait<WebDriver>(getWebDriverCurrent())
                .withTimeout(TIME_WAIT_SECONDS * 10L, TimeUnit.SECONDS)
                .pollingEvery(10, TimeUnit.MILLISECONDS)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        PageFactory.initElements(new AjaxElementLocatorFactory(wd, TIME_WAIT_SECONDS * 2), this);
    }

    protected static Properties getProps() {
        return props;
    }

    public static String getPageURL() {
        return BasePage.props.getProperty("application.url");
    }

//    protected abstract void checkUniqueElements() throws Error;

    protected WebDriver getWebDriverCurrent() {
        return wd;
    }

    protected void load() {
        log.info("Loading page: {}", getPageURL());
        wd.get(getPageURL());
    }


    protected void sendKeys(final WebElement webElement, String text) {
        waitForClickable(webElement);
        webElement.clear();
        webElement.sendKeys(text);
    }

    protected WebElement waitForVisibility(WebElement webElement) {
        try {
            visibilityWait.until(ExpectedConditions.visibilityOf(webElement));
        } catch (NoSuchElementException nse) {
            log.info("Try to wait little more (wait for visibility)");
            nse.printStackTrace();
            return null;
        }
        return webElement;
    }

    protected void waitForInvisibility(final By locator) {
        try {
            invisibilityWait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (NoSuchElementException e) {
            log.info("Try to wait little more (wait for invisibility)");
            e.printStackTrace();
        }
    }

    protected WebElement waitForClickable(WebElement webElement) {
        waitForVisibility(webElement);
        try {
            visibilityWait.until(ExpectedConditions.elementToBeClickable(webElement));
        } catch (NoSuchElementException nse) {
            log.info("Try to wait little more (wait for clickable)");
            nse.printStackTrace();
        }
        return webElement;
    }

    protected boolean click(WebElement webElement) {
        boolean result = false;
        int attempts = 0;
        while (attempts < 3) {
            try {
//                scrollToElement(waitForVisibility(webElement));
                waitForClickable(webElement).click();
                result = true;
                break;
            } catch (StaleElementReferenceException e) {
            }
            attempts++;
        }
        return result;
    }

    protected boolean isElementPresent(final WebElement we) {
        try {
            return we.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void selectCustomDropDown(WebElement buttonSelect, List<WebElement> optionsLinks, String textToSelect) {
        waitForClickable(buttonSelect).click();
        for (WebElement optionLink : optionsLinks) {
            if (waitForClickable(optionLink).getText().equalsIgnoreCase(textToSelect)) {
                optionLink.click();
                break;
            }
        }
    }

    public RemoteWebDriver switchToNewlyOpenedTab() {
        RemoteWebDriver webDriver;
        ArrayList<String> currentTabs = new ArrayList<>(getWebDriverCurrent().getWindowHandles());
        webDriver = (RemoteWebDriver) getWebDriverCurrent().switchTo().window(currentTabs.get(getWebDriverCurrent().getWindowHandles().size() - 1));
        webDriver.manage().window().maximize();
        return webDriver;
    }

    protected void moveMouseCursorToWebElement(WebElement webElement) {
//        scrollToElement(webElement);
        waitForClickable(webElement);
        Actions action = new Actions(getWebDriverCurrent());
        action.moveToElement(webElement).perform();
    }

    protected Object executeJS(final String script, final Object... params) {
        return ((JavascriptExecutor) getWebDriverCurrent()).executeScript(script, params);
    }

    protected WebElement scrollToElement(WebElement we) {
        /* this serves to avoid 2 problems:
            1. frozen navigation bar on top of page that overlay elements in case of scroll up
            2. error notifier that accidentally appears and overlay element also
        */
        executeJS("arguments[0].scrollIntoView(true);", we);
//        executeJS(String.format("javascript:window.scrollBy(%d,%d)", 0, HEADER_HEIGHT));
        return we;
    }

    protected String getValueFromElement(WebElement webElement) {
        return executeJS("return arguments[0].value", webElement).toString();
    }

    @Step
    protected void verifyFieldErrorNotifier(WebElement errorNotifierContainer, WebElement errorNotifierFlyOut, String reason) {
        scrollToElement(errorNotifierContainer);
        waitForClickable(errorNotifierContainer);
        moveMouseCursorToWebElement(errorNotifierContainer);
        waitForVisibility(errorNotifierFlyOut);
        assertEquals("Hint Text isn't expected", reason, errorNotifierFlyOut.getText());
    }

    @Step
    public void refreshPage() {
        getWebDriverCurrent().navigate().refresh();
    }

}
