package baselines;

import help.Utilities;
import lucene.Index;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * This class creates a baseline for the support passage task.
 * Method: Score of a passage is the number of entities in common with the list of entities retrieved for the query.
 * @author Shubham Chatterjee
 * @version 02/25/2019
 */

public class Baseline1 {
    private IndexSearcher searcher;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String, ArrayList<String>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,ArrayList<String>> entityRankings;
    private HashMap<String, ArrayList<String>> entityQrels;
    // ArrayList of run strings
    private ArrayList<String> runStrings;

    /**
     * Constructor.
     * @param indexDir String Path to the index directory.
     * @param trecCarDir String Path to the TREC-CAR directory.
     * @param outputDir String Path to output directory within TREC-CAR directory.
     * @param dataDir String Path to data directory within TREC-CAR directory.
     * @param passageRunFile String Name of the passage run file within data directory.
     * @param entityRunFile String Name of the entity run file within data directory.
     * @param outFile String Name of the output run file. This will be stored in the output directory mentioned above.
     * @param entityQrelFilePath String Path to the entity ground truth file.
     */

    public Baseline1(String indexDir,
                     String trecCarDir,
                     String outputDir,
                     String dataDir,
                     String passageRunFile,
                     String entityRunFile,
                     String outFile,
                     String entityQrelFilePath){

        String entityRunFilePath = trecCarDir + "/" + dataDir + "/" + entityRunFile;
        String passageRunFilePath = trecCarDir + "/" + dataDir + "/" + passageRunFile;
        String outFilePath = trecCarDir + "/" + outputDir + "/" + outFile;
        this.runStrings = new ArrayList<>();

        System.out.print("Reading entity rankings...");
        entityRankings = Utilities.getRankings(entityRunFilePath);
        System.out.println("[Done].");

        System.out.print("Reading passage rankings...");
        paraRankings = Utilities.getRankings(passageRunFilePath);
        System.out.println("[Done].");

        System.out.print("Reading entity ground truth...");
        entityQrels = Utilities.getRankings(entityQrelFilePath);
        System.out.println("[Done].");

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("[Done].");

        makeBaseline(outFilePath);

    }
    /**
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N")
     * to set the thread pool size
     */

    private  void makeBaseline(String outFilePath) {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);

        // Create the run file
        System.out.print("Writing to run file.....");
        Utilities.writeFile(runStrings, outFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outFilePath);
    }

    /**
     * Helper method.
     * For every query, look at all the entities relevant for the query.
     * For every such entity, create a pseudo-document consisting of passages which contain this entity.
     * For every co-occurring entity in the pseudo-document, if the entity is also relevant for the query,
     * then find the frequency of this entity in the pseudo-document and score the passages using this frequency information.
     *
     * @param queryId String
     */

    private void doTask(String queryId) {

        if (entityRankings.containsKey(queryId) && entityQrels.containsKey(queryId)) {
            // Get the list of entities retrieved for the query
            ArrayList<String> retEntityList = entityRankings.get(queryId);
            ArrayList<String> processedRetEntityList = Utilities.process(retEntityList);

            // Get the list of entities relevant for the query
            ArrayList<String> relEntityList = entityQrels.get(queryId);

            //Get the list of paragraphs retrieved for the query
            ArrayList<String> paraList = paraRankings.get(queryId);


            HashMap<String, HashMap<String, Integer>> scoreMap = new HashMap<>();
            Document doc = null;
            int score;

            // For every paragraph retrieved for the query do
            for (String paraId : paraList) {

                // Get the lucene document corresponding to the paragraph
                try {
                    doc = Index.Search.searchIndex("id", paraId, searcher);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }

                // Get the entities in the paragraph
                // Make an ArrayList from the String array
                assert doc != null;
                List<String> pEntList = Arrays.asList(
                        Utilities.clean(doc.getField("entity").stringValue().split(" ")));
                /*
                 * The feature value for a query-entity pair and the paragraph is the number of links
                 * a paragraph has to an entity in the list entityList. This is basically an intersection
                 * operation on the two lists :
                 * list1 = list of entities retrieved for the query
                 * list2 = list of entities in the paragraph
                 */
                score = Utilities.intersection(processedRetEntityList, pEntList).size();
                ArrayList<String> paraEntities = Utilities.unprocess(pEntList, retEntityList);

                // Only do for relevant entities
                paraEntities.retainAll(relEntityList);

                HashMap<String, Integer> map = new HashMap<>();
                for (String e : paraEntities) {
                    String query = queryId + "+" + e;
                    if (scoreMap.containsKey(query)) {
                        map = scoreMap.get(query);
                    }
                    map.put(paraId, score);
                    scoreMap.put(query, map);
                }
            }
            makeRunStrings(scoreMap);
            System.out.println("Done query: " + queryId);
        }
    }

    /**
     * Make run file strings to write to the run file.
     * @param scoreMap Map Map where Key = queryd Value = Map of (paraID, paraScore)
     */

    private void makeRunStrings(@NotNull HashMap<String,
                                HashMap<String, Integer>> scoreMap) {
        String runFileString;
        int rank , score;
        for (String queryId : scoreMap.keySet()) {
            rank = 1;
            LinkedHashMap<String, Integer> map = Utilities.sortByValueDescending(scoreMap.get(queryId));
            for (String paraId : map.keySet()) {
                score = map.get(paraId);
                if (score != 0) {
                    runFileString = queryId + " Q0 " + paraId + " " + rank
                            + " " + score + " " + "Baseline1";
                    //System.out.println(runFileString);
                    runStrings.add(runFileString);
                    rank++;
                }
            }
        }
    }

    /**
     * Main method.
     * @param args Command line arguments.
     */
    public static void main(@NotNull String[] args) {
        String indexDir = args[0];
        String trecCarDir = args[1];
        String outputDir = args[2];
        String dataDir = args[3];
        String paraRunFile = args[4];
        String entityRunFile = args[5];
        String outFile = args[6];
        String entityQrel = args[7];

        new Baseline1(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                outFile, entityQrel);
    }
}
