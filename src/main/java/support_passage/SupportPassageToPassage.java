package support_passage;

import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class to convert a support passage run file to a passage run file.
 * @author Shubham Chatterjee
 * @version 03/04/2019
 */

public class SupportPassageToPassage {
    // Path to the support passage run file
    private String SUPPORT_PASSAGE_FILE;

    // Path to the new paragraph run file made from the support passage run file
    private String OUT_RUN_FILE;


    // HashMap where Key = QueryID and Value = HashMap where Key = ParaID, Value = ParaScore
    private HashMap<String, HashMap<String,Float>> scoreMap;

    /**
     * Constructor.
     * @param str1 Path to the support passage run file
     * @param str2 ath to the new paragraph run file made from the support passage run file
     */

    public SupportPassageToPassage(String str1, String str2) {
        SUPPORT_PASSAGE_FILE = str1;
        OUT_RUN_FILE = str2;

        /*Generate the scores for passages to be written to new run file*/
        System.out.println("Generating new scores for the passage runfile...");
        scoreMap = getNewScores();
        System.out.println("Done");

        /*Create the run file*/
        System.out.println("Creating run file...");
        createFile();
        System.out.println("Done");
    }

    /**
     * Get the scores of the passages from the support passage file.
     * @return HashMap where Key = QueryID and Value = HashMap where Key = ParaID and Value = ParaScore
     */
    private HashMap<String,HashMap<String,Float>> getNewScores() {
        BufferedReader in = null;
        String line, qID, pID;
        float score;
        HashMap<String,Float> paraScore;
        HashMap<String,HashMap<String,Float>> map = new HashMap<> ();
        try {
            // Read a line from the file
            in = new BufferedReader(new FileReader(SUPPORT_PASSAGE_FILE));
            while((line = in.readLine()) != null) {
                // Get the queryID
                qID = line.split(" ")[0].split("\\+")[0];
                //Get the paraID
                pID = line.split(" ")[2].split("/")[0];
                //Get the score
                score = Float.parseFloat(line.split(" ")[4]);
                paraScore = new HashMap<>();
                // If the map already contains the queryID then
                if(map.containsKey(qID)) {
                    // Get the corresponding HashMap of (paraID, Score)
                    paraScore = map.get(qID);
                    // If this HashMap already contains the paraID then
                    if(paraScore.containsKey(pID)) {
                        // Get the score of the paraID
                        float s = paraScore.get(pID);
                        // Add the new score  just read from the file corresponding to this paraID to the old score
                        s = s + score;
                        // Update the score of the paraID in the HashMap of (paraID, score)
                        paraScore.put(pID, s);
                    } else {
                        // Otherwise the HashMap of (paraID, score) does not contain the paraID
                        // So add this paraId and its score to the HashMap
                        paraScore.put(pID, score);
                    }
                } else {
                    // Otherwise the HashMap of (queryID, (paraID, score)) does not contain the queryID
                    // So first make an entry into the HashMap of (paraID, score)
                    paraScore.put(pID, score);
                }
                // Then add an entry into the HashMap of (queryID, (paraID, score))
                map.put(qID, paraScore);
            }
            // Once everything is done, sort the HashMap of (queryID, (paraID, score))
            map = getSortedMap(map);
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
        return map;
    }

    /**
     * Get the sorted HashMap.
     * @param map HashMap
     * @return HashMap
     */
    @NotNull
    private HashMap<String,HashMap<String,Float>> getSortedMap(@NotNull HashMap<String,HashMap<String,Float>> map) {
        HashMap<String,HashMap<String,Float>> sortedMap = new HashMap<>();
        for(String s : map.keySet())
            sortedMap.put(s, Utilities.sortByValueDescending(map.get(s)));
        return sortedMap;
    }

    /**
     * Create a passage run file.
     */

    private void createFile() {
        ArrayList<String> runStrings = new ArrayList<>();
        String runFileString;
        for(String qID : scoreMap.keySet()) {
            HashMap<String,Float> paraScore = scoreMap.get(qID);
            int rank = 1;
            for(String pID : paraScore.keySet()) {
                runFileString = qID + " Q0 " + pID + " " + rank + " " + paraScore.get(pID) + " " + "COMBINED";
                runStrings.add(runFileString);
                rank++;
            }
        }
        writeFile(runStrings,OUT_RUN_FILE);
        System.out.println("Creating run file done at location: " + OUT_RUN_FILE);
    }

    /**
     * Write to a file.
     * @param runStrings ArrayList of run file strings
     * @param filePath Path to the output file
     */
    private void writeFile(@NotNull ArrayList<String> runStrings, String filePath) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filePath,true));

            for(String s : runStrings) {
                out.write(s);
                out.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null) {
                    out.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main method to drive the program.
     * @param args Command line arguments
     */
    public static void main(@NotNull String[] args) {
        String supportPassageRunFile = args[0];
        String passageRunFile = args[1];

        new SupportPassageToPassage(supportPassageRunFile, passageRunFile);
    }
}
