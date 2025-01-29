package webScraping.servisi;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class KPRobot {
    public static WebDriver driver;
    public static WebDriverWait wait;
    public static Properties config = new Properties();

    static {
        try { // Loading configuration file
            File configFile = new File("//home/nemanja/IdeaProjects/RPA-task/config.Properties");
            if (!configFile.exists()) {
                System.err.println("Configuration file not found at: " + configFile.getAbsolutePath());
                System.exit(1);
            }

            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);
            } catch (IOException e) {
                System.err.println("Error reading configuration file: " + e.getMessage());
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
            System.exit(1);
        }
    }

    @Test
    void setup() throws InterruptedException {
        // ChromeDriver initialisation
        String chromeDriverPath = config.getProperty("chromedriver_path");
        if (chromeDriverPath == null || chromeDriverPath.isEmpty()) {
            throw new RuntimeException("ERR: 'chromedriver_path' not specified in the config file!");
        }
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);

        try {
            driver = new ChromeDriver(); // ChromeDriver instance which will be used to automate website use
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("ERR: ChromeDriver initialization failed: " + e.getMessage());
        }

        try {
            driver.get("https://www.kupujemprodajem.com/");
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("ERR: Unable to navigate to URL!");
        }

        // Maximizing the window
        driver.manage().window().maximize();
        Thread.sleep(1000);

        try {
            WebElement minimizeButton = driver.findElement(By.xpath("//*[@id=\"kp-portal\"]/div/div/aside/div/div/button/span"));
            minimizeButton.click(); // Closing Sign Up form
        } catch (NoSuchElementException e) {
            System.err.println("ERR: Minimize button not found!: " + e.getMessage());
        }
    }

    public void scraping() throws IOException, InterruptedException {
        // Scraper instance && and runner
        String URL = driver.getCurrentUrl();
        final Scraper scraper = new Scraper(URL, driver);
        System.out.println("Starting to scrape on: " + URL);

        scraper.startScraping();
    }

    public String sendMostFrequentLocation() throws InterruptedException, IOException {
        String URL = driver.getCurrentUrl();
        final Scraper scraper = new Scraper(URL, driver);
        System.out.println("Calculating most frequent location among listings scraped...");

        return scraper.mostFrequentLocation();
    }

    @Test
    void testSteps() throws InterruptedException {
        Thread.sleep(1000);

        // Entering search text from config file
        WebElement textBox = driver.findElement(By.xpath("//*[@id=\"keywords\"]"));
        textBox.click();
        String itemsForSearch = config.getProperty("items_for_search", "default search item");
        textBox.sendKeys(itemsForSearch);
        Thread.sleep(3000);

        // Proceeding with search
        WebElement searchButton = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[1]/div/div/div/div/div/div[2]/form/section/div/div[1]/div/section/section/div/span[2]"));
        searchButton.click();
        Thread.sleep(2000);

        applyFilters();

        // Start scraping
        try {
            scraping();
        } catch (Exception e) {
            throw new RuntimeException("ERR: Couldn't run 'scraping' function!" + e.getMessage());
        }

        // Send most frequent location to Website Form
        try{
            sendToWebsiteForm();
        } catch (IOException e) {
            System.err.println("ERROR: Couldn't run 'sendToWebsiteForm' function!" + e.getMessage());
        }

        // Close the driver
        CloseDriver();
    }

    private void applyFilters() throws InterruptedException {
        try {
            // Open filters and set prices from config file
            WebElement filtersButton = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[3]/div/div/div[2]/div/section[1]/div[1]/ul/li[5]/button/span"));
            filtersButton.click();
            Thread.sleep(2000);

            // Price range specification
            WebElement lowestPrice = driver.findElement(By.xpath("//*[@id=\"priceFrom\"]"));
            lowestPrice.click();
            lowestPrice.sendKeys(config.getProperty("bottom_price", "600"));

            WebElement highestPrice = driver.findElement(By.xpath("//*[@id=\"priceTo\"]"));
            highestPrice.click();
            highestPrice.sendKeys(config.getProperty("top_price", "800"));
            Thread.sleep(2000);

            WebElement onlyWithPrice = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[1]/div/div/div/div/div/div[2]/form/section/" +
                    "div/div[2]/div/div/div[1]/div[2]/span/div[2]/div[1]/span/label/div"));
            onlyWithPrice.click();
            Thread.sleep(2000);

            WebElement onlyWithPic = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[1]/div/div/div/div/div/div[2]/form/section/" +
                    "div/div[2]/div/div/div[1]/div[4]/span/div[2]/div/span/label/div"));
            onlyWithPic.click();
            Thread.sleep(2000);

            WebElement applyButton = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[1]/div/div/div/div/div/div[2]/form/section/" +
                    "div/div[2]/div/div/div[2]/button[2]"));
            applyButton.click(); // Search for listings with custom filtters
            Thread.sleep(3000);
        } catch (NoSuchElementException e) {
            throw new RuntimeException("ERR: Couldn't apply filters: " + e.getMessage());
        }
    }

    private void sendToWebsiteForm() throws IOException, InterruptedException {
        try {
            driver.get("https://docs.google.com/forms/d/1uSkQclzgYimoODTeHpvt2MO7QTAEanQahYGDmqsCkFs/viewform?edit_requested=true");

            Thread.sleep(1000);
            WebElement placeholder = driver.findElement(By.xpath("//*[@id=\"mG61Hd\"]/div[2]/div/div[2]/div/div/div/div[2]/div/div[1]/div/div[1]/input"));
            placeholder.click();

            String locationToSend = sendMostFrequentLocation();
            placeholder.sendKeys(locationToSend);
            Thread.sleep(1000);

            WebElement sendAnswerButton = driver.findElement(By.xpath("//*[@id=\"mG61Hd\"]/div[2]/div/div[3]/div[1]/div[1]/div/span/span"));
            sendAnswerButton.click();
        } catch (NoSuchElementException e) {
            System.err.println("ERR: Couldn't fill Website Form: " + e.getMessage());
        }
    }

    public void CloseDriver() {
        driver.quit();
    }
}
