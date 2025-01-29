package webScraping.servisi;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.time.Duration;
import java.util.*;


public class Scraper {
    private final WebDriver driver;
    private final String baseUrl;
    private final Properties config = new Properties();

    public Scraper(String baseUrl, WebDriver driver) {
        this.baseUrl = baseUrl;
        this.driver = driver;
        dataConfiguration();
    }

    private void dataConfiguration() {
        String configFile = "/home/nemanja/IdeaProjects/RPA-task/config.Properties";

        try (FileReader reader = new FileReader(configFile)) {
            config.load(reader);
            System.out.println("Configuration loaded successfully!");
        } catch (IOException e) {
            System.err.println("Unable to read configuration file: " + e.getMessage());
        }
    }

    public String createFileInWorkingDirectory() {
        String workingDir = config.getProperty("working_dir", "working");
        File workingFolder = new File(workingDir);

        if (!workingFolder.exists()) {
            workingFolder.mkdirs();
        }
        String fileName = config.getProperty("input_file", "DataScraped.csv");
        String filePath = workingDir + "/" + fileName;

        try {
            File file = new File(filePath);
            if (file.createNewFile()) {
                System.out.println("File created successfully at: " + filePath);
            } else {
                System.out.println("File already exists.");
            }
            return filePath;
        } catch (IOException e) {
            System.err.println("An error occurred while creating the file: " + e.getMessage());
            return null;
        }
    }

    public void startScraping() {
        String filePath = createFileInWorkingDirectory(); // Get CSV file path
        if (filePath != null) {
            try (FileWriter fileWriter = new FileWriter(filePath)) { // Preparing CSV file && setting headers
                fileWriter.append("Naslov; Cena; Lokacija; Broj pregleda");
                System.out.println("FileWriter successfully created!");

                scrapeAllPages(fileWriter);
                System.out.println("Scraping completed!");
            } catch (IOException e) {
                System.err.println("ERR: Failed to write to file: " + e.getMessage());
            }
        } else {
            System.err.println("Failed to create file. Please check working directory configuration.");
        }
    }

    public int scrapeAllPages(FileWriter fileWriter) throws IOException {
        int listingCounter = 0;
        int pageNumber = 1;
        int pageNumberLimit;

        try {
            pageNumberLimit = Integer.parseInt(config.getProperty("page_number_limit", "2"));
        } catch (NumberFormatException e) {
            System.err.println("ERR: Invalid page_number_limit in config file. Defaulting to 2.");
            pageNumberLimit = 2;
        }

        while (pageNumber <= pageNumberLimit) { // Scraping specified number of pages
            String pageURL = generatePageURL(pageNumber);
            driver.get(pageURL);

            if (!waitForPageToLoad()) {
                System.err.println("ERR: Failed to load page " + pageNumber);
                break;
            }

            System.out.println("Scraping page: " + pageNumber);

            List<WebElement> listings = getListings();
            if (listings != null) {
                for (WebElement listing : listings) { // Scraping data form each listing div one by one and appending the data in CSV file
                    if (scrapeListingData(listing, fileWriter) > 0) {
                        listingCounter++;
                    } else {
                        System.out.println("ERR: Listing that caused an issue located at index: " + listingCounter + " on page: " + pageNumber);
                    }
                }
            }
            pageNumber++;
        }
        return listingCounter;
    }

    private String generatePageURL(int pageNumber) {
        // Creating increment-like URL for going through website pages
        return baseUrl + "&page=" + pageNumber;
    }

    private boolean waitForPageToLoad() {
        // Driver wait used for ensuring whole page has been loaded before doing any work on it
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"__next\"]/div/div[3]/div/div/div[2]/div/div[2]")));
            return true;
        } catch (Exception e) {
            System.err.println("ERR: Page loading error: " + e.getMessage());
            return false;
        }
    }

    private List<WebElement> getListings() {
        try {
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver; // Added for safety reasons to prevent lazy loading
            List<WebElement> listings = null;
            int previousCount = 0;

            while (true) {
                // Ensuring all listings have been loaded by scrolling to the botttom of the page
                jsExecutor.executeScript("window.scrollTo(0, document.body.scrollHeight);");

                WebElement listingsWrapper = driver.findElement(By.xpath("//*[@id=\"__next\"]/div/div[3]/div/div/div[2]/div/div[2]"));
                listings = listingsWrapper.findElements(By.xpath("./div[not(contains(@class, 'Banner'))]")); // Filtering 2 invisible banner divs

                if (listings.size() > previousCount) { // Checking if all listings loaded succcessfuly
                    previousCount = listings.size();
                } else {
                    break;
                }
                System.out.println("Number of listings to scrape on this page: " + listings.size());
            }
            return listings;
        } catch (Exception e) {
            System.err.println("ERR: Failed to get listings from listings wrapper: " + e.getMessage());
            return null;
        }
    }

    private int scrapeListingData(WebElement listing, FileWriter fileWriter) throws IOException {
        try {
            String title = listing.findElement(By.className("AdItem_name__Knlo6")).getText();
            String price = listing.findElement(By.className("AdItem_price__SkT1P")).getText();
            String viewsNum = listing.findElement(By.className("AdItem_count__pO9LE")).getText();
            String location = listing.findElement(By.className("AdItem_originAndPromoLocation__PmiaP")).getText();

            fileWriter.append(String.format("%n%s;%s;%s;%s", title, price, location, viewsNum));
            return 1;
        } catch (Exception e) {
            System.err.println("ERR: Couldn't extract specific item data: " + e.getMessage());
            return 0;
        }
    }

    public String mostFrequentLocation() throws IOException {
        BufferedReader reader = null;
        String line = null;
        List<String> locations = new ArrayList<>();
        String location = null;

        String workingDir = config.getProperty("working_dir", "working");
        String inputFile = config.getProperty("input_file", "DataScraped.csv");
        String filePath = workingDir + "/" + inputFile;

        try {
            reader = new BufferedReader(new FileReader(filePath));
            line = reader.readLine();
            if (line == null) {
                System.err.println("ERR: Trying to read from an empty file!");
            }

            // Splitting data by headers (Title, price, views and location)
            String[] headers = line.split(";");
            int locationHeaderIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("Lokacija")) {
                    locationHeaderIndex = i; // Getting Lokacija header position
                    break;
                }
            }

            while ((line = reader.readLine()) != null) {
                String[] values = line.split(";");
                locations.add(values[locationHeaderIndex].trim()); // Adding the Locations in a List for finding the max
            }

            location = findMostFrequentLocation(locations);
        } catch (IOException e) {
            System.err.println("ERR: Failed to read file: " + e.getMessage());
        }
        return location;
    }

    private String findMostFrequentLocation(List<String> locatons) {
        /*Structuring the Locations in a HashMap for efficiency purposes
        * by using HashMaps most frequently appeared Location will be calculated
        *  while the Locations are being added in a HashMap, thanks to Value incrment*/
        HashMap<String, Integer> locationMap = new HashMap<>();
        try {
            for(String location : locatons) {
                // Putting the Location in the HashMap && incrmenting the Value part if that Location is already in the Map
                locationMap.put(location, locationMap.getOrDefault(location, 0) + 1);
            }
        }catch (Exception e) {
            System.err.println("ERR: Couldn't put elements into a hashMap: " + e.getMessage());
        }

        int maxValue = 0;
        String mostFrequentLocation = null;

        // Comparing the Values for each Key/Value pair to find the Location with the highest value
        for (String key : locationMap.keySet()) {
            int value = locationMap.get(key);
            if (value > maxValue) {
                mostFrequentLocation = key;
                maxValue = value;
            }
        }

        System.out.println("Most frequent location: " + mostFrequentLocation);
        System.out.println("Number of listings from this location: " + maxValue);
        return mostFrequentLocation;
    }
}