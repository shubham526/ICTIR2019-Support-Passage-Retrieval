package EntitySalience;

import me.tongfei.progressbar.ProgressBar;
import support_passage.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Class to score documents with log P(D|Q) where P(D|Q) = retrieval score of document D for query Q.
 * @author Shubham Chatterjee
 * @version 5/13/2019
 */

public class DocumentProb {
    private String paraRunFile;
    private HashMap<String, LinkedHashMap<String, Double>> paraRankings;
    String runFilePath;
    ArrayList<String> runStrings;

    public DocumentProb(String paraRunFile, String runFilePath) {
        this.paraRunFile = paraRunFile;
        this.runFilePath = runFilePath;
        Utilities.getRankings(paraRunFile, paraRankings);
        this.runStrings = new ArrayList<>();
    }

    public void feature() {
        Set<String> querySet = paraRankings.keySet();
        ProgressBar pb = new ProgressBar("Progress", querySet.size());
        for (String queryID : querySet) {
            pb.step();
        }
        pb.close();
        // Create the run file
        System.out.println("Writing to run file");
        Utilities.writeFile(runStrings, runFilePath);
        System.out.println("Run file written at: " + runFilePath);
    }
    private void doTask(String queryID) {
        LinkedHashMap<String, Double> paraMap = paraRankings.get(queryID);
        for (String paraID : paraMap.keySet()) {
            double score = paraMap.get(paraID);
            double logScore = Math.log(score);
            paraMap.put(paraID,logScore);

        }
        paraRankings.put(queryID,paraMap);
        makeRunFileStrings(queryID);
    }
    private void makeRunFileStrings(String queryID) {
        LinkedHashMap<String, Double> paraMap = paraRankings.get(queryID);
        LinkedHashMap<String, Double> sortedParaMap = Utilities.sortByValueDescending(paraMap);

    }
}
