package others;

import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 *Class to filter the feature files using the qrels.
 */

public class FilterRankings {
    private String qrelPath;
    private String filePath;
    private String newFilePath;

    FilterRankings(String qp, String fp, String nfp) {
        qrelPath = qp;
        filePath = fp;
        newFilePath = nfp;
        filter();
    }

    void filter() {
        HashMap<String, ArrayList<String>> qrelMap = new HashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, Double>> fileMap = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, Double>> filterMap = new LinkedHashMap<>();
        ArrayList<String> runStrings = new ArrayList<>();

        System.out.print("Reading qrels....");
        readQrel(qrelPath, qrelMap);
        System.out.println("Done.");

        System.out.print("Reading file....");
        readFile(filePath, fileMap);
        System.out.println("Done.");

        System.out.print("Filtering rankings based on qrels...");
        filter(qrelMap, fileMap, filterMap);
        System.out.println("Done.");

        System.out.print("Making run strings...");
        makeRunStrings(filterMap, runStrings);
        System.out.println("Done.");

        System.out.print("Writing to file...");
        Utilities.writeFile(runStrings, newFilePath);
        System.out.println("Done.");

    }
    void filter(HashMap<String, ArrayList<String>> qrelMap,
                LinkedHashMap<String, LinkedHashMap<String, Double>> fileMap,
                LinkedHashMap<String, LinkedHashMap<String, Double>> filterMap) {

        for (String queryID : fileMap.keySet()) {
            if (qrelMap.containsKey(queryID)) {
                filterMap.put(queryID, fileMap.get(queryID));
            }
        }
    }
    private void makeRunStrings(@NotNull LinkedHashMap<String,
                                LinkedHashMap<String, Double>> scoreMap,
                                ArrayList<String> runStrings) {
        String runFileString;
        int rank;
        double score;
        for (String queryId : scoreMap.keySet()) {
            rank = 0;
            LinkedHashMap<String, Double> map = Utilities.sortByValueDescending(scoreMap.get(queryId));
            for (String paraId : map.keySet()) {
                score = map.get(paraId);
                if (score != 0) {
                    runFileString = queryId + " Q0 " + paraId + " " + rank
                            + " " + score + " " + "combined";
                    //System.out.println(runFileString);
                    runStrings.add(runFileString);
                    rank++;
                }
            }
        }
    }
    void readQrel(String path, HashMap<String,ArrayList<String>> map) {
        BufferedReader in = null;
        ArrayList<String> paraList;
        try {
            in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                String queryID = line.split(" ")[0];
                String paraID = line.split(" ")[2];
                if (map.containsKey(queryID)) {
                    paraList = map.get(queryID);
                } else {
                    paraList = new ArrayList<>();
                }
                paraList.add(paraID);
                map.put(queryID, paraList);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    void readFile(String path, LinkedHashMap<String, LinkedHashMap<String, Double>> fileMap) {
        BufferedReader in = null;
        LinkedHashMap<String, Double> paraScore;
        try {
            in = new BufferedReader(new FileReader(path));
            String line;
            while ((line = in.readLine()) != null) {
                String queryID = line.split(" ")[0];
                String paraID = line.split(" ")[2];
                double score = Double.parseDouble(line.split(" ")[4]);
                if (fileMap.containsKey(queryID)) {
                    paraScore = fileMap.get(queryID);
                } else {
                    paraScore = new LinkedHashMap<>();
                }
                paraScore.put(paraID, score);
                fileMap.put(queryID, paraScore);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String qfp = args[0];
        String rfp = args[1];
        String nfp = args[2];
        new FilterRankings(qfp, rfp, nfp);
    }
}
