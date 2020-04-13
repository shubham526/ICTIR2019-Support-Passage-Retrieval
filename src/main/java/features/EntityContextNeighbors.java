package features;

import help.PseudoDocument;
import help.Utilities;
import lucene.Index;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * This class scores support passages for a query-entity pair using other frequently co-occurring entities.
 * For every entity, we first build a pseudo-document (of passages mentioning the entity).
 * Then we find the most frequently co-occurring entities with the given entity.
 * We score a passage in the pseudo-document about an entity by summing over the frequency of the co-occurring entities.
 * For more details, refer to the paper and the online appendix.
 * @author Shubham Chatterjee
 * @version 02/25/2019
 */

public class EntityContextNeighbors {
    private final IndexSearcher searcher;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private final HashMap<String, ArrayList<String>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private final HashMap<String,ArrayList<String>> entityRankings;
    private final HashMap<String, ArrayList<String>> entityQrels;
    // ArrayList of run strings
    private final ArrayList<String> runStrings;

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
     * @throws IOException IOException
     */

    public EntityContextNeighbors(String indexDir,
                                  String trecCarDir,
                                  String outputDir,
                                  String dataDir,
                                  String passageRunFile,
                                  String entityRunFile,
                                  String outFile,
                                  String entityQrelFilePath) throws IOException {


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

        feature(outFilePath);

    }

    /**
     * Method to calculate the feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    private  void feature(String outFilePath) {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);
        //ProgressBar pb = new ProgressBar("Progress", querySet.size());

        // Do in serial
//        for (String q : querySet) {
//              doTask(q);
//            //pb.step();
//        }
//        pb.close();


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
        ArrayList<String> pseudoDocEntityList;
        Map<String, Double> freqDist = new HashMap<>();

        if (entityRankings.containsKey(queryId) && entityQrels.containsKey(queryId)) {
            ArrayList<String> processedEntityList = Utilities.process(entityRankings.get(queryId));

            // Get the set of entities retrieved for the query
            Set<String> retEntitySet = new HashSet<>(entityRankings.get(queryId));

            // Get the set of entities relevant for the query
            Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryId));

            // Get the number of retrieved entities which are also relevant
            // Finding support passage for non-relevant entities makes no sense!!

            retEntitySet.retainAll(relEntitySet);

            // Get the list of passages retrieved for the query
            ArrayList<String> paraList = paraRankings.get(queryId);
            // For every entity in this list of relevant entities do
            for (String entityId : retEntitySet) {


                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);

                if (d != null) {

                    // Get the list of entities that co-occur with this entity in the pseudo-document
                    pseudoDocEntityList = d.getEntityList();

                    // Find the frequency distribution over the co-occurring entities
                    freqDist = getDistribution(pseudoDocEntityList, processedEntityList);

                    // Score the passages in the pseudo-document for this entity using the frequency distribution of
                    // co-occurring entities
                    scoreDoc(queryId, d, freqDist);
                }
            }
            System.out.println("Done query: " + queryId);
        }
    }

    @NotNull
    private Map<String, Double> getDistribution(@NotNull ArrayList<String> pseudoDocEntityList,
                                                ArrayList<String> processedEntityList) {

        HashMap<String, Integer> freqMap = new HashMap<>();


        // For every co-occurring entity do
        for (String e : pseudoDocEntityList) {
            // If the entity also occurs in the list of entities relevant for the query then
            if (processedEntityList.contains(e)) {

                // Find the frequency of this entity in the pseudo-document and store it
                freqMap.put(e, Utilities.frequency(e, pseudoDocEntityList));
            }
        }
        return  toDistribution(freqMap);
    }

    @NotNull
    private Map<String, Double> toDistribution (@NotNull Map<String, Integer> rankings) {
        Map<String, Double> normRankings = new HashMap<>();
        double sum = 0.0d;
        for (double score : rankings.values()) {
            sum += score;
        }

        for (String s : rankings.keySet()) {
            double normScore = rankings.get(s) / sum;
            normRankings.put(s,normScore);
        }

        return normRankings;
    }

    private void scoreDoc(String queryId, @NotNull PseudoDocument d, Map<String, Double> freqMap) {
        // Get the entity corresponding to the pseudo-document
        String entityId = d.getEntity();
        //freqMap = entFreqMap.get(entityId);
        HashMap<String, Double> scoreMap = new HashMap<>();

        // Get the list of documents in the pseudo-document corresponding to the entity
        ArrayList<Document> documents = d.getDocumentList();
        // For every document do
        for (Document doc : documents) {

            // Get the paragraph id of the document
            String paraId = doc.getField("id").stringValue();

            // Get the score of the document
            double score = getParaScore(doc, freqMap);

            // Store the paragraph id and score in a HashMap
            scoreMap.put(paraId, score);
        }
        display(entityId, freqMap, scoreMap);
        makeRunStrings(queryId, entityId, scoreMap);

    }
    private void display(String entity,
                         Map<String, Double> freqMap,
                         HashMap<String, Double> scoreMap) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedHashMap<String, Double> sortedFreqMap = Utilities.sortByValueDescending(freqMap);
        LinkedHashMap<String, Double> sortedScoreMap = Utilities.sortByValueDescending(scoreMap);
        System.out.println("Top 10 frequently co-occurring entities with " + entity);
        int i = 1;
        for (String e : sortedFreqMap.keySet()) {
            System.out.println("Entity:" + e + " " + "P(" + e + "|" + entity + ") = " + sortedFreqMap.get(e));
            i++;
            if (i == 10) break;
        }
        System.out.println("Press any key to display a passage--->");
        try {
            br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        i = 1;
        for (String p : sortedScoreMap.keySet()) {
            Document doc = null;
            try {
                doc = Index.Search.searchIndex("id", p, searcher);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            assert doc != null;
            List<String> pEntList = Utilities.getEntities(doc);

            System.out.println(doc.get("text"));
            System.out.println("Entities in the passage which frequently co-occur:");
            for (String e : pEntList) {
                if (freqMap.containsKey(e)) {
                    System.out.println(e + " " + freqMap.get(e));
                }
            }
            System.out.println("Score = " + sortedScoreMap.get(p));
            try {
                br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            i++;
            if (i == 10) break;
        }

    }

    /**
     * Method to find the score of a paragraph.
     * This method looks at all the entities in the paragraph and calculates the score from them.
     * For every entity in the paragraph, if the entity has a score from the entity context pseudo-document,
     * then sum over the entity scores and store the score in a HashMap.
     *
     * @param doc  Document
     * @param freqMap HashMap where Key = entity id and Value = score
     * @return Integer
     */

    @Contract("null, _ -> fail")
    private double getParaScore(Document doc, Map<String, Double> freqMap) {

        double entityScore, paraScore = 0;
        // Get the entities in the paragraph
        // Make an ArrayList from the String array
        assert doc != null;
        ArrayList<String> pEntList = Utilities.getEntities(doc);
        /* For every entity in the paragraph do */
        for (String e : pEntList) {
            // Lookup this entity in the HashMap of frequencies for the entities
            // Sum over the scores of the entities to get the score for the passage
            // Store the passage score in the HashMap
            if (freqMap.containsKey(e)) {
                entityScore = freqMap.get(e);
                paraScore += entityScore;
            }

        }
        return paraScore;
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
                        + " " + score + " " + "ECN";
                runStrings.add(runFileString);
                rank++;
            }

        }
    }

    /**
     * Main method.
     * @param args Command Line arguments
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

        try {
            new EntityContextNeighbors(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                    outFile, entityQrel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

