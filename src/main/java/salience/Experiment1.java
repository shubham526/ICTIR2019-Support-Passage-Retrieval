package salience;

import help.PseudoDocument;
import help.Utilities;
import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * This class class scores passages in a pseudo-document about an entity using the salience score of the entity.
 * Method: Score(p | e, q) = Score(p | q) * Score(e | p)
 * where   Score(p | q)    = normalized retrieval score of passage 'p' for the query 'q'
 *                          (obtained from the candidate passage ranking.
 *         Score(e | p)    = normalized salience score of entity 'e' for passage 'p' (obtained from SWAT).
 * @author Shubham Chatterjee
 * @version 02/25/2019
 */

public class Experiment1 {
    private IndexSearcher searcher;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String, LinkedHashMap<String, Double>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,LinkedHashMap<String, Double>> entityRankings;
    // ArrayList of run strings
    private ArrayList<String> runStrings;
    private HashMap<String, ArrayList<String>> entityQrels;
    private HashMap<String, Map<String, Double>> salientEntityMap;

    /**
     * Constructor.
     * @param trecCarDir String Path to the support passage directory.
     * @param outputDir String Path to the output directory within the support passage directory.
     * @param dataDir String Path to the data directory within the support passage directory.
     * @param passageRunFile String Name of the passage run file (obtained from ECN) withn the data directory.
     * @param entityRunFile String Name of the entity run file.
     * @param outputRunFile String Name of the output file.
     * @param entityQrelFilePath String Path to the entity ground truth file.
     * @param swatFile String Path to the swat annotation file.
     */

    public Experiment1(String indexDir,
                       String trecCarDir,
                       String outputDir,
                       String dataDir,
                       String passageRunFile,
                       String entityRunFile,
                       String outputRunFile,
                       String entityQrelFilePath,
                       String swatFile) {

        this.runStrings = new ArrayList<>();
        this.entityRankings = new HashMap<>();
        this.paraRankings = new HashMap<>();

        String entityRunFilePath = trecCarDir + "/" + dataDir + "/" + entityRunFile;
        String passageRunFilePath = trecCarDir + "/" + dataDir + "/" + passageRunFile;
        String outputRunFilePath = trecCarDir + "/" + outputDir + "/" + outputRunFile;

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("[Done].");

        System.out.print("Reading entity rankings...");
        Utilities.getRankings(entityRunFilePath, entityRankings);
        System.out.println("[Done].");

        System.out.print("Reading paragraph rankings...");
        Utilities.getRankings(passageRunFilePath, paraRankings);
        System.out.println("[Done].");

        System.out.print("Reading entity ground truth...");
        entityQrels = Utilities.getRankings(entityQrelFilePath);
        System.out.println("[Done].");

        System.out.print("Reading the SWAT annotations...");
        try {
            this.salientEntityMap = Utilities.readMap(swatFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");

        feature(outputRunFilePath);

    }
    /**
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    private   void feature(String outputRunFilePath){

        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);

        // Create the run file
        System.out.print("Writing to run file....");
        Utilities.writeFile(runStrings, outputRunFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outputRunFilePath);
    }

    /**
     * Helper method.
     * For every relevant entity retrieved for the query, find the passages mentioning the entity.
     * For every such passage, the score of the passage is equal to the salience score of the entity if the entity is
     * salient in the passage and zero otherwise.
     * @param queryId String
     */


    private void doTask(String queryId)  {
        System.out.println("========================================================================================");
        System.out.println("Query: " + queryId);
        System.out.println("-------------------------------------------------------------");

        // Get the ranking of entities for the query
        LinkedHashMap<String, Double> entityMap = entityRankings.get(queryId);
        // Get the list of paragraphs retrieved for the query
        ArrayList<String> paraList = new ArrayList<>(paraRankings.get(queryId).keySet());

        // Get the set of entities retrieved for the query
        Set<String> retEntitySet = new HashSet<>(entityRankings.get(queryId).keySet());

        // Get the set of entities relevant for the query
        Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryId));

        // Get the number of retrieved entities which are also relevant
        // Finding support passage for non-relevant entities makes no sense!!

        retEntitySet.retainAll(relEntitySet);
        ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
        HashMap<String, HashMap<String, Double>> entityParaMap = new HashMap<>();

        // For every entity in this list of relevant entities do
        for (String entityId : retEntitySet) {

            // This map is will store the score of the entity for each passage mentioning it.
            HashMap<String, Double> paraMap = new HashMap<>();

            // For every passage mentioning the entity, get the score of the entity given the passage, i.e., P(e|p).
            getEntityToParaMap(entityId, paraList, pseudoDocuments, paraMap);

            // When we reach here, it means that we have a HashMap of paraIDs with their score for an entity
            // So now put that hashMap in the HashMap for the entity
            entityParaMap.put(entityId, paraMap);
            System.out.println("Entity: "  + entityId);
        }

        // Now score the passages in the pseudo-documents
        scorePassage(queryId, pseudoDocuments, entityParaMap);
        System.out.println("-------------------------------------------------------------");
        System.out.println("Done");

    }

    /**
     * Helper method.
     * Creates a pseudo-document for the given entity. For passsage in the pseudo-document, scores the passage.
     * @param entityID String entityID
     * @param paraList List List of passages retrieved for the query.
     * @param pseudoDocuments List List of pseudo-documents for the entity.
     * @param paraMap Map Map of (paraID, sore) where score = Salience(e|p).
     */

    private void getEntityToParaMap(String entityID,
                                    ArrayList<String> paraList,
                                    ArrayList<PseudoDocument> pseudoDocuments,
                                    HashMap<String, Double> paraMap) {
        // Create a pseudo-document for the entity
        PseudoDocument d = Utilities.createPseudoDocument(entityID, paraList, searcher);

        if (d != null) {
            // Add it to the list of pseudo-documents for this entity
            pseudoDocuments.add(d);

            // Get the list of lucene documents that make up this pseudo-document
            ArrayList<Document> documents = d.getDocumentList();

            // For every document in the pseudo-document for the entity
            for (Document document : documents) {

                //Get the id of the document
                String paraID = document.get("id");

                // Get the salient entities in the document
                Map<String, Double> saliencyMap = salientEntityMap.get(paraID);

                // If there are no salient entities, then continue
                if (saliencyMap == null) {
                    continue;
                }

                // Otherwise check if the entity is salient to the document
                // If it is, then the score of the document is the salience score of the entity
                // Otherwise it is zero
                paraMap.put(document.get("id"), saliencyMap.getOrDefault(Utilities.process(entityID), 0.0d));
            }
        }

    }

    /**
     * Score th passages in the pseudo-document.
     * @param queryId String QueryID
     * @param pseudoDocuments List List of pseudo-documents
     * @param entityParaMap Map
     */
    private void scorePassage(String queryId,
                              @NotNull ArrayList<PseudoDocument> pseudoDocuments,
                              HashMap<String, HashMap<String, Double>> entityParaMap ) {


        // Normalize the document scores to get a distribution
        LinkedHashMap<String, Double> normalizedParaRankings = normalize(paraRankings.get(queryId));
        double score;

        // For every pseudo-document do
        for (PseudoDocument pseudoDocument : pseudoDocuments) {

            // Get the entity corresponding to the pseudo-document
            String entityId = pseudoDocument.getEntity();

            HashMap<String, Double> scoreMap = new HashMap<>();

            // Normalize the saliency scores for that entity
            LinkedHashMap<String, Double> normalizedSaliencyScores = normalize(entityParaMap.get(entityId));

            // Get the documents which make up the pseudo-document for the entity
            ArrayList<Document> documents = pseudoDocument.getDocumentList();

            // For every such document in the list do
            for (Document document : documents) {

                // Get the paragraph id of the document
                String paraId = document.getField("id").stringValue();

                // Get the score of the document
                // P(p|e,q) = P(p|q) * P(p|e)
                // P(p|q) = Score(p|q) / Sum(Score(p|q))
                // P(p|e) = P(e|p) = Saliency(e|p) / Sum(Saliency(e|p))
                if (normalizedParaRankings.containsKey(paraId) && normalizedSaliencyScores.containsKey(paraId)) {
                    score = normalizedParaRankings.get(paraId) * normalizedSaliencyScores.get(paraId);
                } else {
                    score = 0.0d;
                }

                // Store the paragraph id and score in a HashMap
                scoreMap.put(paraId, score);
            }
            // Make the run file strings for query-entity and document
            makeRunStrings(queryId, entityId, scoreMap);
        }
    }

    /**
     * Normalize a map.
     * @param rankings Map
     * @return Map
     */
    @NotNull
    private LinkedHashMap<String, Double> normalize(@NotNull Map<String, Double> rankings) {
        LinkedHashMap<String, Double> normRankings = new LinkedHashMap<>();
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

    /**
     * Make run file strings.
     * @param queryId String
     * @param entityId String
     * @param scoreMap Map
     */
    private void makeRunStrings(String queryId, String entityId, HashMap<String, Double> scoreMap) {
        LinkedHashMap<String, Double> paraScore = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 0;

        for (String paraId : paraScore.keySet()) {
            double score = paraScore.get(paraId);
            if (score > 0) {
                runFileString = queryId + "+" +entityId + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "Exp-2-salience";
                runStrings.add(runFileString);
                rank++;
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
        String passageRunFile = args[4];
        String entityRunFile = args[5];
        String outputRunFile = args[6];
        String entityQrelFilePath = args[7];
        String swatFile = args[8];

        new Experiment1(indexDir, trecCarDir, outputDir, dataDir, passageRunFile, entityRunFile, outputRunFile,
                entityQrelFilePath, swatFile);

    }
}
