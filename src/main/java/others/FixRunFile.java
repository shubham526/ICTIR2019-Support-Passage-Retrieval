package others;

import org.jetbrains.annotations.Contract;
import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class FixRunFile {
    private String runFile;
    private String newFile;

    @Contract(pure = true)
    public FixRunFile(String s1, String s2) {
        runFile = s1;
        newFile = s2;
        fix();
    }
    public void fix() {
        BufferedReader br = null;
        String line , queryID ,paraID, score, runFileString;
        ArrayList<String> runStrings = new ArrayList<>();
        int rank;
        System.out.print("Reading file.....");

        try {
            br = new BufferedReader(new FileReader(new File(runFile)));
            while((line = br.readLine()) != null) {
                //line = line.replace("\n", "").replace("\r", "");
                String[] fields = line.split(" ");
                queryID = fields[0];
                paraID = fields[2];
                rank = Integer.parseInt(fields[3]);
                score = fields[4];
                runFileString = queryID + " Q0 " + paraID + " " + (rank + 1)
                        + " " + score + " " + fields[5];
                runStrings.add(runFileString);

            }
            System.out.println("[Done].");
            System.out.print("Writing file....");
            Utilities.writeFile(runStrings, newFile);
            System.out.println("[Done].");
            System.out.println("New file written at: " + newFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String runFile = "/home/shubham/Desktop/research/TREC_CAR/trec-car-2019/output/section/benchmarkY3/test/set-2/trec-car-2019-support-passage-ecn.run";
        String newFile = "/home/shubham/Desktop/research/TREC_CAR/trec-car-2019/output/section/benchmarkY3/test/set-2/trec-car-2019-support-passage-ecn-fixed.run";
        new FixRunFile(runFile, newFile);
    }
}
