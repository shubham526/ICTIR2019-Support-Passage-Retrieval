package others;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FindNumQueries {
    public static void find(String file) {
        ArrayList<String> list = new ArrayList<>();
        BufferedReader in = null;
        String line;
        try {
            in = new BufferedReader(new FileReader(file));
            while((line = in.readLine()) != null) {
                String[] fields = line.split(" ");
                String queryID = fields[0];
                if (!list.contains(queryID)) {
                    list.add(queryID);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Input Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Number of queries = " + list.size());
    }

    public static void main(String[] args) {
        String file = args[0];
        System.out.println("File: " + file);
        FindNumQueries.find(file);
    }

}
