package cross_validation;

import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;

import java.io.*;
import java.util.*;

/**
 * This class reads a RankLib feature file and a RankLib model file.
 * It creates a trec_eval compatible run file from these two RankLib files.
 * @author Shubham Chatterjee
 * @version 4/12/2019
 */

public class CombineRuns {
    // Path to the RankLib feature file
    private String featureFile;

    // Path to the RankLib model file
    private String modelFile;

    // Path to the combined run file
    private String combinedFile;

    public CombineRuns(String featureFile, String modelFile, String combinedFile) {
        this.featureFile = featureFile;
        this.modelFile = modelFile;
        this.combinedFile = combinedFile;
        writeFile();
    }

    private void getFeatureWeights(Map<String, Double> weightMap) {
        BufferedReader in = null;
        String line;
        int count = 0;
        String[] linePart;

        try {
            in = new BufferedReader(new FileReader(modelFile));
            while ((line = in.readLine()) != null) {
                count++;
                // The line with the weights is the line-9 in the model file
                if (count == 9) {
                    // Split the line on whitespace
                    linePart = line.split(" ");
                    // For every (feature, weight) pair in the line do
                    for (String part : linePart) {
                        // Split the pair on ":" to get the feature number and the weight
                        String feature = part.split(":")[0];
                        double weight = Double.parseDouble(part.split(":")[1]);
                        // Add to the weight map
                        weightMap.put(feature, weight);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getScores(LinkedHashMap<String, LinkedHashMap<String, Double>> scoreMap,
                          LinkedHashMap<String, Double> weightMap) {
        BufferedReader in = null;
        String line;
        String[] linePart;
        // double sum = 0.0d;
        LinkedHashMap<String, Double> map;
        try {
            in = new BufferedReader(new FileReader(featureFile));
            while ((line = in.readLine()) != null) {
                double sum = 0.0d;
                linePart = line.split(" ");
                String queryID = linePart[linePart.length - 1].split("_")[0].substring(1);
                String paraID = linePart[linePart.length - 1].split("_")[1];

                // For every (feature, value) pair do
                // Note that: linePart[0] --> target, linePart[1] -- > qid,
                // linePart[2]..linePart[linePart.length - 1] -- > (feature : score) pairs
                // This is according to the RankLib feature file format
                for (int i = 2; i < linePart.length - 1; i++) {
                    String feature = linePart[i].split(":")[0];
                    double weight = weightMap.get(feature);
                    double score = Double.parseDouble(linePart[i].split(":")[1]);
                    double newScore = weight * score;
                    sum += newScore;

                }
                // If the scoreMap already contains the query, then get the map of (paraID, score) for the query
                // Otherwise make a new map of map of (paraID, score) for the query
                if (scoreMap.containsKey(queryID)) {
                    map = scoreMap.get(queryID);
                } else {
                    map = new LinkedHashMap<>();
                }
                // Put the (paraID,score) values in the map
                if (! map.containsKey(paraID)) {
                    map.put(paraID, sum);
                    scoreMap.put(queryID,map);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    private void makeRunStrings(@NotNull LinkedHashMap<String, LinkedHashMap<String, Double>> scoreMap,
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

    private void writeFile() {
        LinkedHashMap<String, LinkedHashMap<String, Double>> scoreMap = new LinkedHashMap<>();
        LinkedHashMap<String, Double> weightMap = new LinkedHashMap<>();
        ArrayList<String> runStrings = new ArrayList<>();
        // First get the weights for the feature from the model file
        System.out.print("Reading model file. Getting feature weights...");
        getFeatureWeights(weightMap);
        System.out.println("Done.");
        System.out.println("The following weights are read from the model file:");
        System.out.println(weightMap);
        // Now get the scores for the combined run file
        System.out.print("Reading feature file. Getting the scores for each feature...");
        getScores(scoreMap, weightMap);
        System.out.println("Done.");
        // Get the run file strings
        System.out.print("Writing to file....");
        makeRunStrings(scoreMap, runStrings);
        Utilities.writeFile(runStrings, combinedFile);
        System.out.println("Done.");
        System.out.println("Combined run file written to: " + combinedFile);

    }

    public static void main(String[] args) {
        String featureFile = args[0];
        String modelFile = args[1];
        String combinedFile = args[2];
        new CombineRuns(featureFile, modelFile, combinedFile);
    }
}





