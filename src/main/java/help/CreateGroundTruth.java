package help;

import java.io.IOException;
import java.util.*;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;

/**
 * Class to create ground truth for support passage retrieval.
 * @author Shubham Chatterjee
 * @version 8/7/2019
 */

public class CreateGroundTruth {
    private IndexSearcher searcher;

    // HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String,ArrayList<String>> passageQrels;

    // HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,ArrayList<String>> entityQrels;

    private ArrayList<String> qrelStrings = new ArrayList<>();

    /**
     * Constructor.
     * @param indexDir String Path to the index directory.
     * @param passageQrelsFilePath String Path to the passage ground truth data.
     * @param entityQrelsFilePath String Path to the entity ground truth data.
     * @param supportPassageQrelsFilePath String Path to the support passage ground truth data to be saved.
     */

    public CreateGroundTruth(String indexDir,
                             String passageQrelsFilePath,
                             String entityQrelsFilePath,
                             String supportPassageQrelsFilePath) {

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("[Done].");

        System.out.print("Reading passage ground truth data....");
        passageQrels = Utilities.getRankings(passageQrelsFilePath);
        System.out.println("[Done]");

        System.out.print("Reading entity ground truth data....");
        entityQrels = Utilities.getRankings(entityQrelsFilePath);
        System.out.println("[Done]");

        System.out.println("Creating ground truth for support passage retrieval...");
        createGroundTruth();
        System.out.println("[Done].");

        System.out.print("Writing to file...");
        Utilities.writeFile(qrelStrings, supportPassageQrelsFilePath);
        System.out.println("[Done].");

        System.out.println("Ground truth file can be found at: " + supportPassageQrelsFilePath);
    }

    /**
     * Helper method.
     */

    private void createGroundTruth() {

        //Get the set of queries
        List<String> queryList = new ArrayList<>(passageQrels.keySet());
        Collections.sort(queryList);

        for (String queryID : queryList) {
            doTask(queryID);
        }
    }

    /**
     * Helper method.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     * @param queryID String query
     */

    private void doTask(String queryID) {
        ArrayList<String> entities = entityQrels.get(queryID);
        if (entities != null) {
            for (String entityID : entities) {
                createGroundTruth(queryID, entityID);
            }
            System.out.println("Done: " + queryID);
        }
    }

    /**
     * Helper method.
     * @param queryID String Query
     * @param entityID String Entity
     */
    private void createGroundTruth(String queryID, String entityID) {


        // Get the list of paragraphs relevant for the query
        ArrayList<String> paragraphs = passageQrels.get(queryID);

        ArrayList<String> paraList = new ArrayList<>();
        if(paragraphs != null) {
            // If there exists such a list
            for(String paraID : paragraphs) {
                // For every such relevant paragraph do
                // Check to see if this entity is present in the paragraph
                Document d = null;
                try {
                    d = Index.Search.searchIndex("id", paraID,searcher);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                assert d != null;
                ArrayList<String>paraEntity = Utilities.getEntities(d);
                if(paraEntity.contains(Utilities.process(entityID))) {
                    paraList.add(paraID);
                }
            }
        }
        makeQrelFileStrings(queryID, entityID, paraList);
    }

    /**
     * Make the qrel file strings from hashmap.
     * @param queryID String
     * @param entityID String
     * @param paraList List
     */

    private void makeQrelFileStrings(String queryID,
                                     String entityID,
                                     @NotNull ArrayList<String> paraList) {

        String qrelFileString;
        for (String paraID : paraList) {
            qrelFileString = queryID + "+" + entityID + " Q0 " + paraID + " " + "1";
            qrelStrings.add(qrelFileString);
        }
    }

    /**
     * Main Method.
     * @param args Command line arguments.
     */

    public static void  main(@NotNull String[] args)  {
        String indexDir = args[0];
        String passageQrelsFilePath = args[1];
        String entityQrelsFilePath = args[2];
        String supportPassageQrelsFilePath = args[3];

        new CreateGroundTruth(indexDir, passageQrelsFilePath, entityQrelsFilePath, supportPassageQrelsFilePath);
    }

}
