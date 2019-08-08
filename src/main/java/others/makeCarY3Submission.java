package others;

import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;

import java.util.*;

public class makeCarY3Submission {

    public makeCarY3Submission(String generatedRunFile,
                               String runFile,
                               String outFile) {


        System.out.print("Reading passage generated from support passage run....");
        HashMap<String, LinkedHashMap<String, Double>> generatedRunFileMap = new HashMap<>();
        Utilities.getRankings(generatedRunFile, generatedRunFileMap);
        System.out.println("[Done].");

        System.out.print("Reading candidate passage run file....");
        HashMap<String, LinkedHashMap<String, Double>> runFileMap = new HashMap<>();
        Utilities.getRankings(runFile, runFileMap);
        System.out.println("[Done].");

        System.out.println("Fixing...");
        HashMap<String, LinkedHashMap<String, Double>> newRunFileMap = new HashMap<>();
        makeNewRunFile(generatedRunFileMap, runFileMap, newRunFileMap);
        System.out.println("[Done].");

        ArrayList<String> runFileStrings = new ArrayList<>();
        makeRunFileStrings(newRunFileMap, runFileStrings);


        System.out.print("Writing new run file to file...");
        Utilities.writeFile(runFileStrings, outFile);
        System.out.println("[Done].");

        System.out.println("New run file written at: " + outFile);


    }

    private void makeNewRunFile(HashMap<String, LinkedHashMap<String, Double>> generatedRunFileMap,
                                @NotNull HashMap<String, LinkedHashMap<String, Double>> runFileMap,
                                HashMap<String, LinkedHashMap<String, Double>> newRunFileMap) {
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

    private void makeRunFileStrings(@NotNull HashMap<String, LinkedHashMap<String, Double>> newRunFileMap,
                                    ArrayList<String> runFileStrings) {
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

    public static void main(@NotNull String[] args) {
        String generatedRunFile = args[0];
        String runFile = args[1];
        String outFile = args[2];
        new makeCarY3Submission(generatedRunFile, runFile, outFile);
    }

}
