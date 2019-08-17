package features;

import help.PseudoDocument;
import help.Utilities;
import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
        ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
        HashMap<String, Integer> freqMap = new HashMap<>();

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

                // Get the list of entities that co-occur with this entity in the pseudo-document
                if (d != null) {
                    // Add it to the list of pseudo-documents for this entity
                    pseudoDocuments.add(d);
                    // Get the list of co-occurring entities
                    pseudoDocEntityList = d.getEntityList();
                    // For every co-occurring entity do
                    for (String e : pseudoDocEntityList) {
                        // If the entity also occurs in the list of entities relevant for the query then
                        if (processedEntityList.contains(e)) {

                            // Find the frequency of this entity in the pseudo-document and store it
                            freqMap.put(e, Utilities.frequency(e, pseudoDocEntityList));
                        }
                    }
                }
            }
            // Now score the passages in the pseudo-documents
            scorePassage(queryId, pseudoDocuments, freqMap);
            System.out.println("Done query: " + queryId);
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
    private int getParaScore(Document doc, HashMap<String, Integer> freqMap) {

        int entityScore, paraScore = 0;
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
     * Method to score the passages in a pseudo-document corresponding to an entity.
     * For every pseudo-document, get the entity corresponding to this document and the
     * list of documents making up this pseudo-document. For every such document, get a score.
     *
     * @param query   Query ID
     * @param pseudoDocuments List of pseudo-documents
     * @param freqMap HashMap where Key = Paragraph ID and Value = Score
     */

    private void scorePassage(String query, @NotNull ArrayList<PseudoDocument> pseudoDocuments, HashMap<String, Integer> freqMap) {


        // For every pseudo-document do
        for (PseudoDocument d : pseudoDocuments) {

            // Get the entity corresponding to the pseudo-document
            String entityId = d.getEntity();
            HashMap<String, Integer> scoreMap = new HashMap<>();

            // Get the list of documents in the pseudo-document corresponding to the entity
            ArrayList<Document> documents = d.getDocumentList();

            // For every document do
            for (Document doc : documents) {

                // Get the paragraph id of the document
                String paraId = doc.getField("id").stringValue();

                // Get the score of the document
                int score = getParaScore(doc, freqMap);

                // Store the paragraph id and score in a HashMap
                scoreMap.put(paraId, score);
            }
            // Make the run file strings for query-entity and document
            makeRunStrings(query, entityId, scoreMap);
        }
    }

    /**
     * Method to make the run file strings.
     *
     * @param queryId  Query ID
     * @param scoreMap HashMap of the scores for each paragraph
     */

    private void makeRunStrings(String queryId, String entityId, HashMap<String, Integer> scoreMap) {
        LinkedHashMap<String, Integer> paraScore = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 0;

        for (String paraId : paraScore.keySet()) {
            int score = paraScore.get(paraId);
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

