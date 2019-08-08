package others;

import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;

import java.io.*;
import java.util.*;

/**
 * Class to marginalize over entities for a support passage ranking.
 * Used for the CAR Y3 submission.
 * Hacky sort of method!!
 * This class converts a support passage ranking to a passage ranking.
 * Method:  First marginalize over the entities in the support passage ranking to get Score(passage|query).
 *          The queries are matched against those in the candidate passage ranking.
 *          If some query is not found in the support passage ranking, then it is copied over from the candidate ranking.
 * @author Shubham Chatterjee
 * @version 8/1/2019
 */

public class Marginalize {

    /**
     * Constructor.
     * @param supportPassageRunFile String Path to the support passage run file.
     * @param candidatePassageRunFile String Path to the candidate passage ru  file.
     * @param newPassageRunFile String Path to the new passage run file.
     */

    public Marginalize(String supportPassageRunFile,
                       String candidatePassageRunFile,
                       String newPassageRunFile) {

        Map<String, Map<String, Map<String, Double>>> supportPassageRunFileMap = new HashMap<>();
        HashMap<String, LinkedHashMap<String, Double>> candidatePassageRunFileMap = new HashMap<>();
        HashMap<String, LinkedHashMap<String, Double>> generatedRunFileMap = new HashMap<>();
        Map<String, Map<String, Double>> newRunFileMap = new HashMap<>();
        ArrayList<String> runStrings = new ArrayList<>();

        System.out.print("Reading support passage run file...");
        readSupportPassageRunFile(supportPassageRunFile, supportPassageRunFileMap);
        System.out.println("[Done].");

        System.out.print("Reading candidate passage run file....");
        Utilities.getRankings(candidatePassageRunFile, candidatePassageRunFileMap);
        System.out.println("[Done].");


        System.out.print("Marginalizing over entities in support passage run file....");
        marginalize(supportPassageRunFileMap, generatedRunFileMap);
        System.out.println("[Done].");

        System.out.print("Making new run file....");
        makeNewRunFile(generatedRunFileMap, candidatePassageRunFileMap, newRunFileMap);
        makeRunFileStrings(newRunFileMap, runStrings);
        System.out.println("[Done].");

        System.out.print("Writing new passage run to file...");
        Utilities.writeFile(runStrings, newPassageRunFile);
        System.out.println("[Done].");

        System.out.println("New run file written to: " + newPassageRunFile);



    }

    /**
     * Read the support passage run file.
     * This method reads the ranking as a Map of Map of Map.
     * The innermost Map has Key = EntityID, Value = Score(passage|query, entity)
     * The middle Map has Key = paraID, Value = innermost Map
     * The outer map Map has Key = QueryID, Value = middle Map
     * This format helps to marginalize over the entities.
     * @param runFile String The run file to read.
     * @param queryMap Map Map of the run file in the format described above.
     */

    private void readSupportPassageRunFile(String runFile,
                             Map<String, Map<String, Map<String, Double>>> queryMap) {

        BufferedReader in = null;
        String line;
        Map<String, Map<String, Double>> paraMap;
        Map<String, Double> entityMap;

        try {
            in = new BufferedReader(new FileReader(runFile));
            while ((line = in.readLine()) != null) {
                String[] fields = line.split(" ");
                String queryID = fields[0].split("\\+")[0];
                String entityID = fields[0].split("\\+")[1];
                String paraID = fields[2];
                double paraScore = Double.parseDouble(fields[4]);

                if (queryMap.containsKey(queryID)) {
                    paraMap = queryMap.get(queryID);
                } else {
                    paraMap = new HashMap<>();
                    //System.out.println(queryID);
                }
                if (paraMap.containsKey(paraID)) {
                    entityMap = paraMap.get(paraID);
                } else {
                    entityMap = new HashMap<>();
                }
                entityMap.put(entityID, paraScore);
                paraMap.put(paraID, entityMap);
                queryMap.put(queryID, paraMap);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                } else {
                    System.out.println("Input Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to marginalize over the entities in the support passage ranking.
     * @param supportPassageRunFileMap String
     * @param generatedRunFileMap String
     */
    private void marginalize(@NotNull Map<String, Map<String, Map<String, Double>>> supportPassageRunFileMap,
                             HashMap<String, LinkedHashMap<String, Double>> generatedRunFileMap) {

        Set<String> querySet = supportPassageRunFileMap.keySet();
        for (String queryID : querySet) {
            Map<String, Map<String, Double>> paraMap = supportPassageRunFileMap.get(queryID);
            LinkedHashMap<String, Double> innnerMap = new LinkedHashMap<>();
            Set<String> paraSet = paraMap.keySet();
            for (String paraID : paraSet) {
                Map<String, Double> entityMap = paraMap.get(paraID);
                double score = sum(entityMap);
                innnerMap.put(paraID, score);
            }
            generatedRunFileMap.put(queryID, innnerMap);
        }
    }

    /**
     * Make a new run file.
     * @param generatedRunFileMap Map
     * @param runFileMap Map
     * @param newRunFileMap Map
     */
    private void makeNewRunFile(HashMap<String, LinkedHashMap<String, Double>> generatedRunFileMap,
                                @NotNull HashMap<String, LinkedHashMap<String, Double>> runFileMap,
                                Map<String, Map<String, Double>> newRunFileMap) {
        Set<String> querySet = runFileMap.keySet();

        for (String queryID : querySet) {
            if (generatedRunFileMap.containsKey(queryID)) {
                newRunFileMap.put(queryID, generatedRunFileMap.get(queryID));
            } else {
                System.out.println("Did not find query: " + queryID);
                System.out.println("Populating the query with the following passages found in the candidate passage run" +
                        "provided: ");
                System.out.println(runFileMap.get(queryID));
                System.out.println("===================================================================================");
                newRunFileMap.put(queryID, runFileMap.get(queryID));
            }
        }

    }

    /**
     * Make run file strings
     * @param newRunFileMap Map
     * @param runFileStrings List
     */
    private void makeRunFileStrings(@NotNull Map<String, Map<String, Double>> newRunFileMap,
                                    List<String> runFileStrings) {
        String runString = "";
        for (String queryID : newRunFileMap.keySet()) {
            int rank = 1;
            Map<String, Double> paraMap = newRunFileMap.get(queryID);
            Map<String, Double> sortedMap = Utilities.sortByValueDescending(paraMap);
            for (String paraID : sortedMap.keySet()) {
                double score = sortedMap.get(paraID);
                runString = queryID + " Q0 " + paraID + " " + rank++ + " " + score + " " + "ecn";
                runFileStrings.add(runString);
            }
        }
    }

    /**
     * Sum over elements in a Map.
     * Helpful for marginalization.
     * @param map Map
     * @return double
     */
    private double sum(@NotNull Map<String, Double> map) {
        double sum = 0.0d;
        for (String s : map.keySet()) {
            double d = map.get(s);
            sum += d;
        }
        return sum;
    }

    /**
     * Main method.
     * @param args Command line arguments.
     */

    public static void main(@NotNull String[] args) {
        String supportPassageRunFile = args[0];
        String candidatePassageRunFile = args[1];
        String newPassageRunFile = args[2];
        new Marginalize(supportPassageRunFile, candidatePassageRunFile, newPassageRunFile);
    }
}
