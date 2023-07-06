package com.PDFfileWokingTest.MyController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

@Controller
public class HomeController {

    @GetMapping("/")
    public String homepage() {
        return "Homepage.html";
    }

    @GetMapping("/convertjson")
    public String convertjson() {
        return "convertjson.html";
    }
    
    @GetMapping("/convertjsonwith")
    public String convertjsonwithoutid() {
        return "convertjsonwithid.html";
    }

    @GetMapping("/insertdatabase")
    public String insertdatabase() {
        return "insertdatabase.html";
    }
    
    
     

    @PostMapping("/conversion")
    public String pdftojson(@RequestParam("pdfFile") MultipartFile pdfFile) {
        if (pdfFile.isEmpty()) {
            // Handle empty file error
            return "redirect:/convertjson?error=emptyfile";
        }

        try {
            // Create a temporary file to store the uploaded PDF
            File tempFile = File.createTempFile("temp", ".pdf");
            pdfFile.transferTo(tempFile);

            PDDocument pd = PDDocument.load(tempFile);

            int totalPages = pd.getNumberOfPages();
            System.out.println("Total Pages in Document: " + totalPages);

            JsonObject jsonTables = new JsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Enable pretty printing

            int tableCount = 1;

            for (int pagecount = 1; pagecount <= totalPages; pagecount++) {
                ObjectExtractor oe = new ObjectExtractor(pd);
                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                //full page data 
                Page page = oe.extract(pagecount);
                //only table data here
                List<Table> tables = sea.extract(page);                

                for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                    Table table = tables.get(tableIndex);

                    JsonObject jsonTable = new JsonObject();

                    List<String> header = new ArrayList<>();
                    for (RectangularTextContainer cell : table.getRows().get(0)) {
                        header.add(cell.getText());
                    }
                    jsonTable.add("header", gson.toJsonTree(header));

                    JsonArray rowsArray = new JsonArray();
                    for (int i = 1; i < table.getRows().size(); i++) {
                        List<String> rowData = new ArrayList<>();
                        for (RectangularTextContainer cell : table.getRows().get(i)) {
                            rowData.add(cell.getText());
                        }
                        rowsArray.add(gson.toJsonTree(rowData));
                    }
                    jsonTable.add("rows", rowsArray);

                    String tableKey = "Table" + tableCount + "_page" + pagecount;
                    jsonTables.add(tableKey, jsonTable);

                    // Print the table information
                    System.out.println("Table " + tableCount + " found on Page: " + pagecount);
                    System.out.println("Table Information:");
                    System.out.println(jsonTable);
                    System.out.println();

                    tableCount++;
                }
            }

            // Generate the output file path dynamically
            String outputFileName = pdfFile.getOriginalFilename().replace(".pdf", ".json");
            String OUTPUT_FILE = "C:\\Users\\kabir\\OneDrive\\Desktop\\output\\" + outputFileName;

            try (FileWriter fileWriter = new FileWriter(OUTPUT_FILE)) {
                String jsonString = gson.toJson(jsonTables);
               // System.out.println("prety formated "+jsonString);
                fileWriter.write(jsonString);
                System.out.println("JSON data saved to " + OUTPUT_FILE);
            } catch (IOException e) {
                System.err.println("Error writing JSON data to file: " + e.getMessage());
                // Handle file writing error
                return "redirect:/convertjson?error=writeerror";
            }

            pd.close();
        } catch (IOException e) {
            System.err.println("Error processing the PDF file: " + e.getMessage());
            // Handle PDF processing error
            return "redirect:/convertjson?error=pdferror";
        }

        return "redirect:/Homepage.html";
    }
    

    
   // auto generation of id and add new column name Idno for each table
    
    @PostMapping("/convertingwithid")
    public String pdftojsonwithid(@RequestParam("pdfFile") MultipartFile pdfFile) {
        if (pdfFile.isEmpty()) {
            // Handle empty file error
            return "redirect:/convertjson?error=emptyfile";
        }

        try {
            // Create a temporary file to store the uploaded PDF
            File tempFile = File.createTempFile("temp", ".pdf");
            pdfFile.transferTo(tempFile);

            PDDocument pd = PDDocument.load(tempFile);

            int totalPages = pd.getNumberOfPages();
            System.out.println("Total Pages in Document: " + totalPages);

            JsonObject jsonTables = new JsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Enable pretty printing

            int tableCount = 1;

            for (int pagecount = 1; pagecount <= totalPages; pagecount++) {
                ObjectExtractor oe = new ObjectExtractor(pd);
                SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                //full page data 
                Page page = oe.extract(pagecount);
                //only table data here
                List<Table> tables = sea.extract(page);

                for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
                    Table table = tables.get(tableIndex);

                    JsonObject jsonTable = new JsonObject();

                    List<String> header = new ArrayList<>();
                    header.add("Idno"); // Add "Idno" as the first element in the header
                    for (RectangularTextContainer cell : table.getRows().get(0)) {
                        header.add(cell.getText());
                    }
                    jsonTable.add("header", gson.toJsonTree(header));

                    JsonArray rowsArray = new JsonArray();
                    for (int i = 1; i < table.getRows().size(); i++) {
                        List<String> rowData = new ArrayList<>();
                        rowData.add(Integer.toString(i)); // Auto-generated Idno value
                        for (RectangularTextContainer cell : table.getRows().get(i)) {
                            rowData.add(cell.getText());
                        }
                        rowsArray.add(gson.toJsonTree(rowData));
                    }
                    jsonTable.add("rows", rowsArray);

                    String tableKey = "Table" + tableCount + "_page" + pagecount;
                    jsonTables.add(tableKey, jsonTable);

                    // Print the table information
                    System.out.println("Table " + tableCount + " found on Page: " + pagecount);
                    System.out.println("Table Information:");
                    System.out.println(jsonTable);
                    System.out.println();

                    tableCount++;
                }
            }

            // Generate the output file path dynamically
            String outputFileName = pdfFile.getOriginalFilename().replace(".pdf", ".json");
            String OUTPUT_FILE = "C:\\Users\\kabir\\OneDrive\\Desktop\\output\\" + outputFileName;

            try (FileWriter fileWriter = new FileWriter(OUTPUT_FILE)) {
                String jsonString = gson.toJson(jsonTables);
                // System.out.println("prety formated "+jsonString);
                fileWriter.write(jsonString);
                System.out.println("JSON data saved to " + OUTPUT_FILE);
            } catch (IOException e) {
                System.err.println("Error writing JSON data to file: " + e.getMessage());
                // Handle file writing error
                return "redirect:/convertjson?error=writeerror";
            }

            pd.close();
        } catch (IOException e) {
            System.err.println("Error processing the PDF file: " + e.getMessage());
            // Handle PDF processing error
            return "redirect:/convertjson?error=pdferror";
        }

        return "redirect:/Homepage.html";
    }

    
    

    }