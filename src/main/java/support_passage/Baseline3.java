package support_passage;

import lucene.Index;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * Baseline 3 for support passage task.
 * Retrieving passages using compound query.
 * Filter passages based on entities.
 * @author Shubham Chatterjee
 * @version 5/18/2019
 */

public class Baseline3 {
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String PARA_RUN_FILE ;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;
    //  HashMap where Key = queryID, Value = HashMap of (entityID, score)
    private HashMap<String, LinkedHashMap<String,Double>> entityRankings;
    // HashMap where Key = queryID, Value = HashMap of (paragraphID, score)
    private HashMap<String,LinkedHashMap<String,Double>> paraRankings;
    // ArrayList of run strings
    private ArrayList<String> runStrings;

    /**
     * Constructor.
     * @param INDEX_DIR Path to the index directory
     * @param TREC_CAR_DIR Path to TREC-CAR directory
     * @param OUTPUT_DIR Path to the output directory within TREC-CAR directory
     * @param DATA_DIR Path to data directory within TREC-CAR directory
     * @param PARA_RUN_FILE Name of the paragraph run file within data directory
     * @param ENTITY_RUN_FILE Name of the entity run file within data directory
     * @param OUT_FILE Name of the new output file
     * @throws IOException
     */
    public Baseline3(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String PARA_RUN_FILE, String ENTITY_RUN_FILE, String OUT_FILE) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.PARA_RUN_FILE = PARA_RUN_FILE;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();
        this.paraRankings = new HashMap<>();
        this.entityRankings = new HashMap<>();

        System.out.print("Setting up index....");
        searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
        System.out.println("Done");

        System.out.print("Getting entity rankings from run file...");
        Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.ENTITY_RUN_FILE, entityRankings);
        System.out.println("Done");

        System.out.print("Getting paragraph rankings from run file...");
        Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.PARA_RUN_FILE, paraRankings);
        System.out.println("Done");

    }
    /**
     * Method to calculate the first feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    public void baseline() throws IOException, ParseException {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);

        // Do in serial

//        ProgressBar pb = new ProgressBar("Progress", querySet.size());
//
//        for (String query : querySet) {
//            doTask(query);
//            pb.step();
//        }
//        pb.close();

        // Create the run file
        System.out.println("Writing to run file");
        String filePath = TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE;
        Utilities.writeFile(runStrings, filePath);
        System.out.println("Run file written at: " + filePath);
    }

    /**
     * Helper method.
     * @param queryID String
     */
    private void doTask(String queryID)  {
        // Get the list of paragraphs relevant to the query
        ArrayList<String> paraList = new ArrayList<>(paraRankings.get(queryID).keySet());

        // Get the list of entities relevant to the query
        ArrayList<String> entityList = new ArrayList<>(entityRankings.get(queryID).keySet());


        // For every entity relevant to the query do
        for (String entityID : entityList) {
            HashMap<String, Double> paraScoreMap = new HashMap<>();

            // Create a Pseudo-Document for the entity
            PseudoDocument pseudoDocument = Utilities.createPseudoDocument(entityID, paraList,searcher);

            // Get the lucene documents in the pseudo-document

            if (pseudoDocument != null) {

                ArrayList<Document> documents = pseudoDocument.getDocumentList();

                // For every document do
                for (Document document : documents) {

                    // Get the id of the document
                    String paraID = document.get("id");

                    // Get the score of the document for the query
                    double score = getScore(queryID, paraID);

                    paraScoreMap.put(paraID, score);

                }
            }
            // Make the run file strings for the query-entity pair
            makeRunStrings(queryID, entityID, paraScoreMap);
        }
        System.out.println("Done: " + queryID);

    }

    /**
     * Get the score of a paragraph for the query.
     * @param queryID String
     * @param paraID String
     * @return Double score of the paragraph for the query
     */
    private double getScore(String queryID, String paraID) {
        return paraRankings.get(queryID).get(paraID);
    }

    /**
     * Method to make the run file strings.
     *
     * @param queryId  Query ID
     * @param scoreMap HashMap of the scores for each paragraph
     */

    private void makeRunStrings(String queryId, String entityId, HashMap<String, Double> scoreMap) {
        LinkedHashMap<String, Double> paraScore = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 1;

        for (String paraId : paraScore.keySet()) {
            double score = paraScore.get(paraId);
            if (score > 0) {
                runFileString = queryId + "+" +entityId + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "baseline3";
                runStrings.add(runFileString);
                rank++;
            }

        }
    }

    public static void main(String[] args) {
        String indexDir = args[0];
        String trecCarDir = args[1];
        String outputDir = args[2];
        String dataDir = args[3];
        String paraRunFile = args[4];
        String entityRunFile = args[5];
        String outFile = args[6];

        try {
            new Baseline3(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).baseline();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
}
