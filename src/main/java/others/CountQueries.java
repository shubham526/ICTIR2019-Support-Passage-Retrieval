package others;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CountQueries {
    private String file;
    CountQueries(String file) {
        this.file = file;
    }
    private void count() throws IOException {
        ArrayList<String> list = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        while ((line = in.readLine()) != null) {
            String queryID = line.split(" ")[0].split("\\+")[0];
            if (!list.contains(queryID)) {
                list.add(queryID);
            }
        }
        System.out.println("Number of queries: " + list.size());
    }

    public static void main(String[] args) throws IOException {
        new CountQueries(args[0]).count();
    }
}
