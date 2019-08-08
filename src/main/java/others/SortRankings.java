package others;

import me.tongfei.progressbar.ProgressBar;
import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SortRankings {
    private String oldRunFile;
    private String newRunFile;
    private HashMap<String, HashMap<String, HashMap<String, Double>>> runFileMap = new HashMap<>();

    SortRankings(String oldRunFile, String newRunFile) {
        this.oldRunFile = oldRunFile;
        this.newRunFile = newRunFile;

        System.out.print("Reading run file...");
        getRunFileMap(this.runFileMap);
        System.out.println("Done.");
    }
    private void getRunFileMap(HashMap<String, HashMap<String, HashMap<String, Double>>> queryMap) {
        BufferedReader in = null;
        String line;
        HashMap<String, HashMap<String, Double>> entityMap;
        HashMap<String, Double> paraMap;
        int n =0;
        try {
            in = new BufferedReader(new FileReader(oldRunFile));
            while((line = in.readLine()) != null) {
                String[] fields = line.split(" ");
                String queryID = fields[0].split("\\+")[0];
                String entityID = fields[0].split("\\+")[1];
                String paraID = fields[2];
                double paraScore = Double.parseDouble(fields[4]);
                if (queryMap.containsKey(queryID)) {
                    entityMap = queryMap.get(queryID);
                } else {
                    entityMap = new HashMap<>();
                    n++;
                    System.out.println(queryID);
                }
                if (entityMap.containsKey(entityID)) {
                    paraMap = entityMap.get(entityID);
                } else {
                    paraMap = new HashMap<>();
                }
                paraMap.put(paraID,paraScore);
                entityMap.put(entityID,paraMap);
                queryMap.put(queryID,entityMap);

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
        System.out.println("n="+n);
    }
    private void makeRunFile() {
        ArrayList<String> runStrings = new ArrayList<>();
        ArrayList<String> queryList = new ArrayList<>(runFileMap.keySet());
        ProgressBar pb = new ProgressBar("Progress", queryList.size());
        for (String queryID : queryList) {
            HashMap<String, HashMap<String, Double>> entityMap = runFileMap.get(queryID);
            ArrayList<String> entityList = new ArrayList<>(entityMap.keySet());
            for (String entityID : entityList) {
                HashMap<String, Double> paraMap = Utilities.sortByValueDescending(entityMap.get(entityID));
                ArrayList<String> paraList = new ArrayList<>(paraMap.keySet());
                int rank = 1;
                for (String paraID : paraList) {
                    String runFileString = queryID + "+" + entityID + " Q0 " + paraID + " " + rank
                            + " " + paraMap.get(paraID) + " " + "baseline2-filtered-sorted";
                    //System.out.println(runFileString);
                    runStrings.add(runFileString);
                    rank++;
                }
            }
            pb.step();
        }
        pb.close();
        // Create the run file
        System.out.println("Writing to run file");
        Utilities.writeFile(runStrings, newRunFile);
        System.out.println("Run file written at: " + newRunFile);
    }

    public static void main(String[] args) {
        String oldRunFile = args[0];
        String newRunFile = args[1];
        new SortRankings(oldRunFile,newRunFile).makeRunFile();
    }
}
