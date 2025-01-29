package webScraping.servisi;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RefinedData {

    public static void main(String[] args) throws InterruptedException {
        testRunner();

        Properties config = new Properties();
        try {
            // Load configuration file
            File configFile = new File("/home/nemanja/IdeaProjects/RPA-task/config.Properties");
            if (!configFile.exists()) {
                System.out.println("Configuration file not found!");
                return;
            }
            try (FileReader reader = new FileReader(configFile)) {
                config.load(reader);
            } catch (IOException e) {
                throw new RuntimeException("ERR: Could not load configuration file!" + e.getMessage());
            }

            // Creating variables from config file values
            String workingDir = config.getProperty("working_dir", "working");
            String archiveDir = config.getProperty("archive_dir", "archive");
            String splitCriteria = config.getProperty("split_criteria", "semi-colon");
            int numberOfResults = Integer.parseInt(config.getProperty("number_of_results", "10"));
            String listOrder = config.getProperty("list_order", "top");

            Scraper scraper = new Scraper(null, null);
            String inputFilePath = scraper.createFileInWorkingDirectory(); // Retrieving the CSV file path
            File inputFile = new File(inputFilePath);
            File archiveFolder = new File(archiveDir);

            if (!archiveFolder.exists()) { // Creating the archive file if nonexistent
                archiveFolder.mkdirs();
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
                /* Reading headers from CSV file
                   && creating 4 separate data groups where data will be stored based on the location part
                * */
                String header = reader.readLine();
                List<String> beogradGroup = new ArrayList<>();
                List<String> noviSadGroup = new ArrayList<>();
                List<String> multiLocationGroup = new ArrayList<>();
                List<String> otherCitiesGroup = new ArrayList<>();

                // Going through each line && checking location part
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length < 4) continue; // Skiping possible invalid lines

                    String location = parts[2];

                    if (location.equalsIgnoreCase("Beograd")) {
                        beogradGroup.add(line);
                    } else if (location.equalsIgnoreCase("Novi Sad")) {
                        noviSadGroup.add(line);
                    } else if (location.contains("|")) {
                        multiLocationGroup.add(line);
                    } else {
                        otherCitiesGroup.add(line);
                    }
                }

                // Writing groups of data into separate files
                writeGroupToFile(archiveDir + File.separator + "Beograd.csv", header, beogradGroup);
                writeGroupToFile(archiveDir + File.separator + "NoviSad.csv", header, noviSadGroup);
                writeGroupToFile(archiveDir + File.separator + "MultiLocation.csv", header, multiLocationGroup);
                writeGroupToFile(archiveDir + File.separator + "OtherCities.csv", header, otherCitiesGroup);

                System.out.println("File split successfully! Check the 'archive' folder.");

                if (inputFile.delete()) { // Deletion of a file after splitting it by location criteria
                    System.out.println("File " + inputFile + " deleted successfully.");
                } else {
                    System.out.println("Failed to delete the file " + inputFile + ".");
                }
            } catch (IOException e) {
                System.err.println("ERR: Something went wrong! " + e.getMessage());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeGroupToFile (String inputFile, String header, List < String > data){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile))) {
            writer.write(header);
            writer.newLine();
            for (String line : data) { // Iterating through lines and writing it to a compatible group
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Failed to write file " + inputFile + ": " + e.getMessage());
        }
    }

    private static void testRunner() throws InterruptedException {
        KPRobot robot = new KPRobot();
        robot.setup();
        robot.testSteps();
    }
}