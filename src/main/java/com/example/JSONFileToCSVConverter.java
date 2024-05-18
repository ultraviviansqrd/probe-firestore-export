package com.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVWriter;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JSONFileToCSVConverter {

    public static void main(String[] args) {
        String jsonFilePath = "input.json"; // Path to the JSON file
        String csvFilePath = "output.csv"; // Path to the CSV file

        try {

            // Read JSON data from the file
            FileReader reader = new FileReader(jsonFilePath);
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();

            // Extract Members object from collections
            JsonObject collections = jsonObject.getAsJsonObject("__collections__");
            JsonObject members = collections.getAsJsonObject("Members");

            // Create headers for CSV
            Set<Map.Entry<String, JsonElement>> membersEntrySet = members.entrySet();
            List<String> headers = new ArrayList<>();
            headers.add("Document ID");

            for (Map.Entry<String, JsonElement> entry : membersEntrySet) {
                JsonObject user = entry.getValue().getAsJsonObject();
                headers.addAll(user.keySet());
                break;
            }

            // Remove Firestore's sub-collection element from list of headers
            headers.remove("__collections__");

            System.out.println("Generated CSV headers (" + headers.size() + "):\n" + headers + "\n\n");

            // Write data to CSV
            FileWriter writer = new FileWriter(csvFilePath);
            CSVWriter csvWriter = new CSVWriter(writer);
            csvWriter.writeNext(headers.toArray(new String[0]));

            //reset membersEntrySet
            membersEntrySet = members.entrySet();

            int csvRowCounter = 0;

            for (Map.Entry<String, JsonElement> entry : membersEntrySet) {
                String documentId = entry.getKey();
                JsonObject userObject = entry.getValue().getAsJsonObject();
                List<String> rowData = new ArrayList<>();
                rowData.add(documentId);

                //Skip first header item (Document ID)
                for (int fieldCounter = 1; fieldCounter < headers.size(); ++fieldCounter) {
                    String fieldName = headers.get(fieldCounter);
                    JsonElement valueElement = userObject.get(fieldName);

                    if (valueElement == null) {
                        System.out.println("Missing field value (" + documentId + "): " + fieldName);
                        
                        //add blank csv value
                        rowData.add("");

                    } else {
                        if (valueElement.isJsonPrimitive()) {
                            rowData.add(valueElement.getAsString());
                        
                        } else if (valueElement.isJsonObject() && valueElement.getAsJsonObject().has("__datatype__")) {
                            JsonObject datatypeObject = valueElement.getAsJsonObject();
                            JsonObject valueObject = datatypeObject.getAsJsonObject("value");
                            long seconds = valueObject.getAsJsonObject().get("_seconds").getAsLong();
                            Date date = new Date(seconds * 1000L);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            rowData.add(sdf.format(date));

                        } else {
                            System.out.println("Unexpected field value for User" + documentId + ":  fieldName:  " + fieldName 
                                + "  :" + valueElement.toString());
                        }
                    }
                }

                csvWriter.writeNext(rowData.toArray(new String[0]));
                ++csvRowCounter;
            }

            csvWriter.close();
            writer.close();

            System.out.println("\n\nNumber of members in JSON file: " + membersEntrySet.size());
            System.out.println("Number of members written to CSV file: " + csvRowCounter);

            System.out.println("CSV file has been created successfully!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}