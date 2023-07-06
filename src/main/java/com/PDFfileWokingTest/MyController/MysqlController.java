package com.PDFfileWokingTest.MyController;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
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
public class MysqlController {

    private final Environment env;

    @Autowired
    public MysqlController(Environment env) {
        this.env = env;
    }

    @PostMapping("/insertingdatabaseinstant")
    public String pdftojsonwithid(@RequestParam("pdfFile") MultipartFile pdfFile) {
        if (pdfFile.isEmpty()) {
            // Handle empty file error
            return "redirect:/convertjson?error=emptyfile";
        }

        try {
            PDDocument pd = PDDocument.load(pdfFile.getInputStream());

            int totalPages = pd.getNumberOfPages();
            System.out.println("Total Pages in Document: " + totalPages);

            Gson gson = new GsonBuilder().setPrettyPrinting().create(); // Enable pretty printing

            // MySQL database connection details from application.properties
            String url = env.getProperty("spring.datasource.url");
            String username = env.getProperty("spring.datasource.username");
            String password = env.getProperty("spring.datasource.password");

            // Create a connection to the MySQL database
            try (Connection connection = DriverManager.getConnection(url, username, password)) {
                int tableCount = 1;

                for (int pagecount = 1; pagecount <= totalPages; pagecount++) {
                    ObjectExtractor oe = new ObjectExtractor(pd);
                    SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
                    Page page = oe.extract(pagecount);
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

                        String tableName = "Table" + tableCount + "_page" + pagecount;

                        // Create the table dynamically
                        createTable(connection, tableName, header);

                        // Insert the table data
                        insertTableData(connection, tableName, header, rowsArray);

                        System.out.println("Table " + tableCount + " found on Page: " + pagecount);
                        System.out.println("Table Information:");
                        System.out.println(jsonTable);
                        System.out.println();

                        tableCount++;
                    }
                }
            }

            pd.close();
        } catch (Exception e) {
            System.err.println("Error processing the PDF file: " + e.getMessage());
            // Handle PDF processing error
            return "redirect:/convertjson?error=pdferror";
        }

        return "redirect:/Homepage.html";
    }

    private void createTable(Connection connection, String tableName, List<String> header) throws SQLException {
        StringBuilder createTableQuery = new StringBuilder();
        createTableQuery.append("CREATE TABLE IF NOT EXISTS ");
        createTableQuery.append(tableName);
        createTableQuery.append(" (");
        createTableQuery.append("`IdNo` INT PRIMARY KEY AUTO_INCREMENT");

        for (String column : header) {
            createTableQuery.append(", ");
            createTableQuery.append("`");
            createTableQuery.append(column);
            createTableQuery.append("`");
            createTableQuery.append(" TEXT");
        }

        createTableQuery.append(")");

        try (PreparedStatement createTableStatement = connection.prepareStatement(createTableQuery.toString())) {
            createTableStatement.executeUpdate();
        }
    }

    private void insertTableData(Connection connection, String tableName, List<String> header, JsonArray rowsArray) throws SQLException {
        StringBuilder insertDataQuery = new StringBuilder();
        insertDataQuery.append("INSERT INTO ");
        insertDataQuery.append(tableName);
        insertDataQuery.append(" (");

        for (String column : header) {
            insertDataQuery.append("`");
            insertDataQuery.append(column);
            insertDataQuery.append("`");
            insertDataQuery.append(", ");
        }
        insertDataQuery.delete(insertDataQuery.length() - 2, insertDataQuery.length());
        insertDataQuery.append(") VALUES (");

       
                for (int i = 0; i < header.size(); i++) {
                    insertDataQuery.append("?, ");
                }
                insertDataQuery.delete(insertDataQuery.length() - 2, insertDataQuery.length());
                insertDataQuery.append(")");

                try (PreparedStatement insertDataStatement = connection.prepareStatement(insertDataQuery.toString())) {
                    for (int i = 0; i < rowsArray.size(); i++) {
                        JsonArray rowData = rowsArray.get(i).getAsJsonArray();
                        for (int j = 0; j < rowData.size(); j++) {
                            String cellData = rowData.get(j).getAsString();
                            insertDataStatement.setString(j + 1, cellData);
                        }
                        insertDataStatement.executeUpdate();
                    }
                }
            }

        }
