package features;

import help.PseudoDocument;
import help.RM3Expand;
import help.Utilities;
import lucene.Index;
import lucene.RAMIndex;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * QE with words from the pseudo-document.
 * @author Shubham Chatterjee
 * @version 7/22/2019
 */

public class QEWords {
    private IndexSearcher searcher;

    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String, ArrayList<String>> paraRankings;

    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,ArrayList<String>> entityRankings;

    private HashMap<String, ArrayList<String>> entityQrels;

    // ArrayList of run strings
    private ArrayList<String> runStrings = new ArrayList<>();
    private int takeKTerms; // Number of query expansion terms
    private int takeKDocs; // Number of documents for query expansion
    private boolean omitQueryTerms; // Omit query terms or not when calculating expansion terms
    private Similarity similarity; // Similarity to use
    private Analyzer analyzer; // Analyzer to use

    /**
     * Constructor.
     * @param indexDir String Path to the index directory.
     * @param trecCarDir String Path to the TREC-CAR directory.
     * @param outputDir String Path to the output directory within the TREC-CAR directory.
     * @param dataDir String Path to the data directory within the TREC-CAR directory.
     * @param paraRunFile String Name of the passage run file within the data directory.
     * @param entityRunFile String Name of the entity run file within the data directory.
     * @param entityQrelPath String Path to the entity ground truth file.
     * @param outFile String Name of the output file.
     * @param takeKTerms Integer Top K terms for query expansion.
     * @param takeKDocs Integer Top K documents for feedback set.
     * @param similarity Similarity Type of similarity to use.
     * @param analyzer Analyzer Type of analyzer to use.
     * @param omitQueryTerms Boolean Whether or not to omit query terms during expansion.
     */

    public QEWords(String indexDir,
                   String trecCarDir,
                   String outputDir,
                   String dataDir,
                   String paraRunFile,
                   String entityRunFile,
                   String outFile,
                   String entityQrelPath,
                   int takeKTerms,
                   int takeKDocs,
                   boolean omitQueryTerms,
                   Analyzer analyzer,
                   Similarity similarity) {


        this.takeKTerms = takeKTerms;
        this.takeKDocs = takeKDocs;
        this.similarity = similarity;
        this.analyzer = analyzer;
        this.omitQueryTerms = omitQueryTerms;

        String entityFilePath = trecCarDir + "/" + dataDir + "/" + entityRunFile;
        String paraFilePath = trecCarDir + "/" + dataDir + "/" + paraRunFile;
        String outputFilePath = trecCarDir + "/" + outputDir + "/" + outFile;

        System.out.print("Reading entity rankings...");
        entityRankings = Utilities.getRankings(entityFilePath);
        System.out.println("[Done].");

        System.out.print("Reading paragraph rankings...");
        paraRankings = Utilities.getRankings(paraFilePath);
        System.out.println("[Done].");

        System.out.print("Reading entity ground truth...");
        entityQrels = Utilities.getRankings(entityQrelPath);
        System.out.println("[Done].");

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir, "text", analyzer, similarity).getSearcher();
        System.out.println("[Done].");

        try {
            feature(outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to calculate the feature.
     * @param outputFilePath String Path to the output file.
     * @throws IOException IOException
     */

    private  void feature(String outputFilePath) throws IOException {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();
        ProgressBar pb = new ProgressBar("Progress", querySet.size());

        // Do in serial
        for (String q : querySet) {
            doTask(q);
            pb.step();
        }
        pb.close();

        // Create the run file
        System.out.print("Writing to run file.....");
        Utilities.writeFile(runStrings, outputFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outputFilePath);
    }

    /**
     * Helper method.
     * @param queryId String Query
     * @throws IOException IOException
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

            // For every entity in this set of relevant (retrieved) entities do
            for (String entityId : retEntitySet) {

                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);

                // If there exists a pseudo-document about the entity
                if (d != null) {

                    // Get the list of lucene documents in the pseudo-document
                    ArrayList<Document> documents = d.getDocumentList();

                    // Get the top documents for this query-entity pair
                    // This is obtained after expanding the query with contextual words
                    // And retrieving with the expanded query from the index
                    TopDocs topDocs = getTopDocsForEntity(queryId, documents);

                    // Make the run file strings for the query-entity pair

                    makeRunStrings(queryId, entityId, topDocs);

                }
            }
        }

    }

    /**
     * Get the top documents for the query-entity pair.
     * This is done by first creating an in-memory index of the passages mentioning the entity.
     * Then documents are retrieved from this index.
     * Top documents retrieved are used to derive expansion terms.
     * The query is expanded with these expansion terms and the original index (not the in-memory index) is searched
     * with this expanded query.
     * Finally a new set of passages are retrieved using this expanded query.
     * The effect is that we expanded the query using terms from the passages which mention the entity (contextual words).
     * @param queryID Sting QueryID
     * @param documents List List of passages mentioning the entity.
     * @return TopDocs The top 100 (or less) documents retrieved using query expansion with contextual words.
     * @throws IOException IOException
     */

    private TopDocs getTopDocsForEntity(@NotNull String queryID,
                                        ArrayList<Document> documents) throws IOException {
        ///////////////////////////////////////////
        // Building the index of documents
        //////////////////////////////////////////

        // First create the IndexWriter
        IndexWriter iw = RAMIndex.createWriter(analyzer);
        // Now create the index
        RAMIndex.createIndex(documents, iw);
        // Create the IndexSearcher
        IndexSearcher is = RAMIndex.createSearcher(similarity, iw);


        String queryStr = queryID
                .substring(queryID.indexOf(":")+1)          // remove enwiki: from query
                .replaceAll("%20", " ")     // replace %20 with whitespace
                .toLowerCase();                            //  convert query to lowercase

        // Get the RM3 expansion terms
        List<Map.Entry<String, Float>> relevanceModel = RM3Expand.getExpansionTerms(is, takeKTerms,takeKDocs,
                queryStr,omitQueryTerms, analyzer);
        // Convert the query to a BooleanQuery expanded using the RM3 terms
        BooleanQuery booleanQuery = RM3Expand.toRm3Query(queryStr, relevanceModel, analyzer);

        // Search the index with this expanded query
        TopDocs topDocs = Index.Search.searchIndex(booleanQuery, 100, searcher);
        RAMIndex.close(iw);
        return topDocs;
    }

    /**
     * Make the run file strings for the query.
     * @param queryId String QueryID
     * @param entityId String EntityID
     * @param topDocs TopDocs The top documents retrieved for the query-entity pair.
     * @throws IOException IOException
     */

    private void makeRunStrings(String queryId, String entityId,
                                @NotNull TopDocs topDocs) throws IOException {
        String query = queryId + "+" + entityId;
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Document d;
        String runFileString;

        for (int i = 0; i < scoreDocs.length; i++) {
            d = searcher.doc(scoreDocs[i].doc);
            String pID = d.getField("id").stringValue();
            runFileString = query + " Q0 " + pID + " " + (i + 1) + " " + topDocs.scoreDocs[i].score + " " + "QEW";
            //System.out.println(runFileString);
            runStrings.add(runFileString);
        }
    }

    /**
     * Main method to run the code.
     * @param args Command line parameters.
     */

    public static void main(@NotNull String[] args) {

        Similarity similarity = null;
        Analyzer analyzer = null;
        boolean omit;
        String s1 = null, s2;

        String indexDir = args[0];
        String trecCarDir = args[1];
        String outputDir = args[2];
        String dataDir = args[3];
        String paraRunFile = args[4];
        String entityRunFile = args[5];
        String entityQrel = args[6];
        int takeKTerms = Integer.parseInt(args[7]);
        int takeKDocs = Integer.parseInt(args[8]);
        String o = args[9];
        omit = o.equalsIgnoreCase("y") || o.equalsIgnoreCase("yes");
        String a = args[10];
        String sim = args[11];

        System.out.printf("Using %d terms for query expansion\n", takeKTerms);
        System.out.printf("Using %d documents as feedback set for query expansion\n", takeKDocs);
        if (omit) {
            System.out.println("Using RM1");
            s2 = "rm1";
        } else {
            System.out.println("Using RM3");
            s2 = "rm3";
        }


        switch (a) {
            case "std" :
                analyzer = new StandardAnalyzer();
                System.out.println("Analyzer: Standard");
                break;
            case "eng":
                analyzer = new EnglishAnalyzer();
                System.out.println("Analyzer: English");
                break;
            default:
                System.out.println("Wrong choice of analyzer! Exiting.");
                System.exit(1);
        }
        switch (sim) {
            case "BM25" :
            case "bm25":
                System.out.println("Similarity: BM25");
                similarity = new BM25Similarity();
                s1 = "bm25";
                break;
            case "LMJM":
            case "lmjm":
                System.out.println("Similarity: LMJM");
                try {
                    float lambda = Float.parseFloat(args[12]);
                    System.out.println("Lambda = " + lambda);
                    similarity = new LMJelinekMercerSimilarity(lambda);
                    s1 = "lmjm";
                } catch (IndexOutOfBoundsException e) {
                    System.out.println("No lambda value for similarity LM-JM.");
                    System.exit(1);
                }
                break;
            case "LMDS":
            case "lmds":
                System.out.println("Similarity: LMDS");
                similarity = new LMDirichletSimilarity();
                s1 = "lmds";
                break;

            default:
                System.out.println("Wrong choice of similarity! Exiting.");
                System.exit(1);
        }
        String outFile = "qew" + "-" + s1 + "-" + s2 + ".run";

        new QEWords(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile, entityQrel,
                takeKTerms, takeKDocs, omit, analyzer, similarity);

    }
}

