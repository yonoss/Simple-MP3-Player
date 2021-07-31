package com.ion.cmp.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;

public class FileUtils {

    static ObjectMapper objectMapper = new ObjectMapper();

    public static void writeObjectToFile(String file, Object content, Context context) throws JsonProcessingException {
        String contentString = objectMapper.writeValueAsString(content);
        writeToFile(file, contentString,  context);
    }

    public static <T> Object readObjectFromFile(String file,Context context, Class<T> objectClass) throws JsonProcessingException {
        String json = readFromFile(file, context);
        return objectMapper.readValue(json, objectClass);
    }

    public static <T> List<T> readListFromFile(String file, Context context, Class<T> objectClass) throws IOException {
        String json = readFromFile(file, context);
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, objectClass);
        List<T> ts = objectMapper.readValue(json, listType);
        return ts;
    }

    public static void writeToFile(String file, String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            System.out.println("Failed to write to file:" + e.toString());
        }
    }

    public static String readFromFile(String file, Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(file);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("File not found: " + e.toString());
        } catch (IOException e) {
            System.out.println("Can not read file: " + e.toString());
        }

        return ret;
    }

    public static void deleteFile(String file, Context context) {
        context.deleteFile(file);
    }
}
