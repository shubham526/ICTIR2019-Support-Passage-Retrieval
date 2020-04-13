package features;

import help.PseudoDocument;
import help.Utilities;
import lucene.Index;
import lucene.RAMIndex;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * This class scores a support passage for a query-entity pair by summing over the retrieval scores
 * of the pseudo-document it appears in.
 * @author Shubham Chatterjee
 * @version 02/25/2019
 */
public class PseudoDocRetScore {
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

    public PseudoDocRetScore(String indexDir,
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
     * @throws IOException
     */
    private  void feature(String outFilePath) throws IOException {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        for (String q : querySet) {
            doTask(q);
        }

        // Create the run file
        System.out.print("Writing to run file.....");
        Utilities.writeFile(runStrings, outFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outFilePath);
    }

    /**
     * Helper method.
     * @param queryId String
     * @throws IOException
     */
    private void doTask(String queryId) throws IOException {

        if (entityRankings.containsKey(queryId) && entityQrels.containsKey(queryId)) {

            // Get the set of entities retrieved for the query
            Set<String> retEntitySet = new HashSet<>(entityRankings.get(queryId));

            // Get the set of entities relevant for the query
            Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryId));

            // Get the number of retrieved entities which are also relevant
            // Finding support passage for non-relevant entities makes no sense!!

            retEntitySet.retainAll(relEntitySet);

            // Get the list of passages retrieved for the query
            ArrayList<String> paraList = paraRankings.get(queryId);
            ArrayList<Document> queryDocs = new ArrayList<>();
            HashMap<String, PseudoDocument> entityToPseudoDocMap = new HashMap<>();
            Map<String, Float> documentScore = new HashMap<>();

            // Get the list of pseudo-documents and the map of entity to pseudo-documents for the query
            getPseudoDocList(retEntitySet, queryDocs, paraList, entityToPseudoDocMap);

            // Build the index
            // First create the IndexWriter
            IndexWriter iw = RAMIndex.createWriter(new EnglishAnalyzer());
            // Now create the index
            RAMIndex.createIndex(queryDocs, iw);
            // Create the IndexSearcher and QueryParser
            IndexSearcher is = RAMIndex.createSearcher(new BM25Similarity(), iw);
            QueryParser qp = RAMIndex.createParser("text", new EnglishAnalyzer());
            // Search the index for the query
            // But first process the query
            String query = queryId
                    .substring(queryId.indexOf(":") + 1)          // remove enwiki: from query
                    .replaceAll("%20", " ")     // replace %20 with whitespace
                    .toLowerCase();                            //  convert query to lowercase
            // Now search the query
            LinkedHashMap<Document, Float> results = Utilities.sortByValueDescending(RAMIndex.searchIndex(query, 100, is, qp));
            if (!results.isEmpty()) {
                documentScore = Utilities.sortByValueDescending(scoreParas(results, documentScore, entityToPseudoDocMap));
                makeRunStrings(queryId, documentScore, entityToPseudoDocMap);
            } else {
                System.out.printf("No results found for query %s. Cannot score documents.", queryId);
            }
            System.out.println("Done query: " + queryId);
            RAMIndex.close(iw);
        }
    }

    /**
     * Method to find the set of pseudo-documents for the query and the map from entity to pseudo-document.
     * @param retEntitySet Set Set of entities retrieved for the quert which are also relevant
     *                     (according to entity ground truth data)
     * @param queryDocs List List of pseudo-documents for the query.
     * @param paraList List List of passages retrieved for the query in the candidate pool.
     * @param entityToPseudoDocMap Map Map where Key = entityID and Value = PseudoDocument for the entity.
     */

    private void getPseudoDocList(@NotNull Set<String> retEntitySet,
                                  ArrayList<Document> queryDocs,
                                  ArrayList<String> paraList,
                                  HashMap<String, PseudoDocument>  entityToPseudoDocMap) {
        // For every entity in this list of relevant entities do
        for (String entityId : retEntitySet) {
            //System.out.println(entityId);

            // Create a pseudo-document for the entity
            PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);
            if (d != null) {
                // Add to HashMap where Key = entityID and Value = Pseudo-document
                entityToPseudoDocMap.put(entityId, d);

                // Convert Pseudo-document to lucene document
                Document doc = Utilities.pseudoDocToDoc(d);

                // Add it to list of documents for query
                queryDocs.add(doc);
            }
        }
    }

    /**
     * Make the run file strings.
     * @param queryId String
     *
     * @param entityToPseudoDocMap HashMap where Key = entity and Value = Pseudo-document for this entity
     */

    private void makeRunStrings(String queryId,
                                Map<String, Float> scores,
                                @NotNull Map<String, PseudoDocument> entityToPseudoDocMap) {


        // For every entity do
        for (String entityId : entityToPseudoDocMap.keySet()) {
            // Get the pseudo-document for the entity
            PseudoDocument doc = entityToPseudoDocMap.get(entityId);
            // Get the documents in the pseudo-document
            ArrayList<Document> docList = doc.getDocumentList();
            Map<String, Float> docScores = new LinkedHashMap<>();
            getPseudoDocScores(docList, docScores, scores);
            docScores = Utilities.sortByValueDescending(docScores);
            makeRunStrings(queryId, entityId, docScores);
        }
    }

    private void makeRunStrings(String queryID,
                                String entityID,
                                @NotNull Map<String, Float> docScores) {

        String query = queryID + "+" + entityID;
        String runFileString;
        Set<String> paraSet = docScores.keySet();
        int rank = 1;
        float score;
        for (String paraID : paraSet) {
            score = docScores.get(paraID);
            runFileString = query + " Q0 " + paraID + " " + rank++
                    + " " + score + " " + "pseudo-doc-ret-score";
            runStrings.add(runFileString);
            //System.out.println(runFileString);
        }
    }

    private void getPseudoDocScores(@NotNull ArrayList<Document> docList,
                                    Map<String, Float> docScores,
                                    Map<String, Float> scores) {
        String paraId;
        float score;
        for (Document d : docList) {
            paraId = d.getField("id").stringValue();
            if (scores.containsKey(paraId)) {
                score = scores.get(paraId);
                docScores.put(paraId, score);
            }
        }
    }

    /**
     * Score the paragraphs in a Pseudo-document.
     * NOTE: One of the parameters of this method is a Map of (Document, Float)
     * and this method returns another Map of (Document, Float).
     * However, the Document in the parameter comes from the pseudo-documents meaning that for every pseudo-document,
     * we convert it to a Lucene Document by indexing it's components as Fields. See {@link Utilities#pseudoDocToDoc(PseudoDocument)} method.
     * The Document in the Map returned by this method is a Document from the original index.
     * @param results HashMap where Key = Document and Value = Score
     * @param entityToPseudoDocMap HashMap where Key = entity and Value = Pseudo-document for this entity
     * @return HashMap where Key = Document and Value = Score
     */


    @Contract("_, _, _ -> param2")
    @NotNull
    private Map<String, Float> scoreParas(@NotNull Map<Document, Float> results,
                                            Map<String, Float> documentScore,
                                            HashMap<String, PseudoDocument>  entityToPseudoDocMap) {

        // For every document retrieved do
        // Each Document is actually a PseudoDocument
        for (Document doc : results.keySet()) {
            String entity = doc.getField("entity").stringValue();
            // Get the pseudo-document corresponding to this entity

            PseudoDocument d = entityToPseudoDocMap.get(entity);
            if (d != null) {
                // Get the score of this document
                float score = results.get(doc);
                // Get the list of documents contained in the pseudo-document
                // This list is the list of actual Documents
                ArrayList<Document> documentList = d.getDocumentList();
                // For every document in this list of documents do
                for (Document document : documentList) {
                    float s = 0;
                    String id = document.get("id");
                    // If the document is already has a score get that score and add it to the new score
                    // Else add it to the score map
                    if (documentScore.containsKey(id)) {
                        s = documentScore.get(id);
                    }
                    s += score;
                    documentScore.put(id, s);
                }
            }
        }
        return documentScore;
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

        try {
            new PseudoDocRetScore(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                    outFile, entityQrel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
