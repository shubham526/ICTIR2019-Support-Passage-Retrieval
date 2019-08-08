package EntitySalience;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;
import others.CheckHeapSize;
import support_passage.PseudoDocument;
import support_passage.Utilities;

import java.io.IOException;
import java.util.*;

public class EntitySaliency {
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String PARA_RUN_FILE ;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String, LinkedHashMap<String, Double>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,LinkedHashMap<String, Double>> entityRankings;
    // ArrayList of run strings
    private ArrayList<String> runStrings;

    public EntitySaliency(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String PARA_RUN_FILE, String ENTITY_RUN_FILE, String OUT_FILE) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.PARA_RUN_FILE = PARA_RUN_FILE;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();
        this.entityRankings = new HashMap<>();
        this.paraRankings = new HashMap<>();

        System.out.println("Setting up index for use...");
        searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
        System.out.println("Done");

        System.out.println("Getting entity rankings from run file...");
        Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.ENTITY_RUN_FILE, entityRankings);
        System.out.println("Done");

        System.out.println("Getting paragraph rankings from run file...");
        Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.PARA_RUN_FILE, paraRankings);
        System.out.println("Done");

        System.out.println("Heap Size = " + CheckHeapSize.getHeapSize());
        System.out.println("Max Heap Size = " +CheckHeapSize.getHeapMaxSize());

    }
    /**
     * Method to calculate the first feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    public   void feature(){
        System.out.println("Calulating...");
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);
        /*for (String query : querySet) {
            doTask(query);
        }*/


        // Create the run file
        System.out.println("Writing to run file");
        String filePath = TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE;
        Utilities.writeFile(runStrings, filePath);
        System.out.println("Run file written at: " + filePath);
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


    private void doTask(String queryId)  {
        System.out.println("========================================================================================");
        System.out.println("Query: " + queryId);
        System.out.println("-------------------------------------------------------------");
        // Get the ranking of entities for the query
        LinkedHashMap<String, Double> entityMap = entityRankings.get(queryId);
        // Get the list of paragraphs for the query
        ArrayList<String> paraList = new ArrayList<>(paraRankings.get(queryId).keySet());
        // Get the list of entities for the query
        ArrayList<String> entityList = new ArrayList<>(entityMap.keySet());
        ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
        HashMap<String, HashMap<String, Double>> entityParaMap = new HashMap<>();


        Map<String, Map<Document, Double>> scoreMap = new HashMap<>();

        // For top 100 entity in this list of entities do
        for (int i = 0; i < 100; i++) {
            String entityId = entityList.get(i);
            // Create a pseudo-document for the entity
            PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);
            HashMap<String, Double> paraMap = new HashMap<>();

            if (d != null) {
                // Add it to the list of pseudo-documents for this entity
                pseudoDocuments.add(d);

                // Get the list of lucene documents that make up this pseudo-document
                ArrayList<Document> documents = d.getDocumentList();

                // For every document in the pseudo-document for the entity
                for (Document document : documents) {

                    // Get the text of the document
                    String text = document
                            .getField("text")
                            .stringValue()
                            .replace("\"", "")
                            .replace("\'","");

                    // Get the salient entities in the document
                    Map<String, Double> saliencyMap = Saliency.getSalientEntities(text);

                    // If there are no salient entities, then continue
                    if (saliencyMap == null) {
                        continue;
                    }

                    // Otherwise check if the entity is salient to the document
                    if (saliencyMap.containsKey(Utilities.process(entityId))) {

                        // If it is, then the score of the document is the saliency score of the entity
                        paraMap.put(document.get("id"), saliencyMap.get(Utilities.process(entityId)));
                    } else {

                        // Otherwise it is zero
                        paraMap.put(document.get("id"), 0.0d);
                    }
                }
            }

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
    private void makeRunStrings(String queryId, String entityId, HashMap<String, Double> scoreMap) {
        LinkedHashMap<String, Double> paraScore = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 0;

        for (String paraId : paraScore.keySet()) {
            double score = paraScore.get(paraId);
            if (score > 0) {
                runFileString = queryId + "+" +entityId + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "ENTITY CONTEXT NEIGHBOURS";
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
            new EntitySaliency(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).feature();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
