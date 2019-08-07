package features;

import help.EntityRMExpand;
import help.PseudoDocument;
import help.Utilities;
import lucene.Index;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
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

public class QEEntities {
    private IndexSearcher searcher;

    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String, ArrayList<String>> paraRankings;

    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,ArrayList<String>> entityRankings;

    private HashMap<String, ArrayList<String>> entityQrels;

    // ArrayList of run strings
    private ArrayList<String> runStrings = new ArrayList<>();
    private int takeKEntities; // Number of query expansion terms
    private boolean omitQueryTerms; // Omit query terms or not when calculating expansion terms
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
     * @param takeKEntities Integer Top K entities for query expansion.
     * @param similarity Similarity Type of similarity to use.
     * @param analyzer Analyzer Type of analyzer to use.
     * @param omitQueryTerms Boolean Whether or not to omit query terms during expansion.
     */

    public QEEntities(String indexDir,
                      String trecCarDir,
                      String outputDir,
                      String dataDir,
                      String paraRunFile,
                      String entityRunFile,
                      String outFile,
                      String entityQrelPath,
                      int takeKEntities,
                      boolean omitQueryTerms,
                      Analyzer analyzer,
                      Similarity similarity) {


        this.takeKEntities = takeKEntities;
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

        feature(outputFilePath);
    }
    /**
     * Method to calculate the first feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    private  void feature(String outputFilePath) {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(queryId -> {
            try {
                doTask(queryId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Create the run file
        System.out.print("Writing to run file.....");
        Utilities.writeFile(runStrings, outputFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outputFilePath);
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

    private void doTask(String queryId) throws IOException {
        List<Map.Entry<String, Integer>> contextEntityList;
        List<Map.Entry<String, Integer>> expansionEntities;

        // Get the set of entities retrieved for the query
        ArrayList<String> entityList = entityRankings.get(queryId);
        Set<String> retEntitySet = new HashSet<>(entityList);

        // Get the set of entities relevant for the query
        Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryId));

        // Get the retrieved entities which are also relevant
        // Finding support passage for non-relevant entities makes no sense!!

        retEntitySet.retainAll(relEntitySet);

        // Get the list of passages retrieved for the query
        ArrayList<String> paraList = Utilities.process(paraRankings.get(queryId));


        // For every entity in this set of relevant retrieved  entities do
        for (String entityId : retEntitySet) {

            // Get the list of all entities which co-occur with this entity in a given context
            // Context here is the same as a PseudoDocument for the entity
            // So we are actually looking at all entities that occur in the PseudoDocument
            // sorted in descending order of frequency
            // Here we are using all entities retrieved for the query to get the expansion terms
            contextEntityList = getContextEntities(entityId, entityList, paraList);

            // Use the top K entities for expansion
            expansionEntities = contextEntityList.subList(0, Math.min(takeKEntities, contextEntityList.size()));

            if (expansionEntities.size() == 0) {
                continue;
            }
            // Process the query
            String queryStr = queryId
                    .substring(queryId.indexOf(":")+1)          // remove enwiki: from query
                    .replaceAll("%20", " ")     // replace %20 with whitespace
                    .toLowerCase();                            //  convert query to lowercase
            // Convert the query to an expanded BooleanQuery
            BooleanQuery booleanQuery = EntityRMExpand.toEntityRmQuery(queryStr, expansionEntities, omitQueryTerms,
                    analyzer);

            // Search the index
            TopDocs tops = Index.Search.searchIndex(booleanQuery, 100, searcher);
            makeRunStrings(queryId, entityId, tops);

        }
        System.out.println("Done query: " + queryId);
    }
    @NotNull
    private List<Map.Entry<String, Integer>> getContextEntities(String entityId,
                                                                List<String> entityList,
                                                                ArrayList<String> paraList ) {
        List<Map.Entry<String, Integer>> contextEntityList = new ArrayList<>();
        HashMap<String, Integer> freqMap = new HashMap<>();
        ArrayList<String> processedEntityList = Utilities.process(entityList);
        ArrayList<String> pseudoDocEntityList;


        // Create a pseudo-document for the entity
        PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);

        // Get the list of entities that co-occur with this entity in the pseudo-document
        if (d != null) {
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

        // Sort the entities in decreasing order of frequency
        // Add all the entities to the list
        contextEntityList.addAll(Utilities.sortByValueDescending(freqMap).entrySet());
        // Return the list
        return contextEntityList;
    }

    /**
     * Make run file strings.
     * @param queryId String
     * @param entityId String
     * @param topDocs TopDocs
     * @throws IOException Exception
     */
    private void makeRunStrings(String queryId,
                                String entityId,
                                @NotNull TopDocs topDocs) throws IOException {
        String query = queryId + "+" + entityId;
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Document d;
        String runFileString;

        for (int i = 0; i < scoreDocs.length; i++) {
            d = searcher.doc(scoreDocs[i].doc);
            String pID = d.getField("id").stringValue();
            runFileString = query + " Q0 " + pID + " " + i + " " + topDocs.scoreDocs[i].score + " " + "QEE";
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
        int takeKEntities = Integer.parseInt(args[7]);
        String o = args[8];
        omit = o.equalsIgnoreCase("y") || o.equalsIgnoreCase("yes");
        String a = args[9];
        String sim = args[10];

        System.out.printf("Using %d entities for query expansion\n", takeKEntities);

        if (omit) {
            System.out.println("Using RM1");
            s2 = "rm1";
        } else {
            System.out.println("Using RM3");
            s2 = "rm3";
        }

        System.out.println("Similarity: " + sim);
        System.out.println("Analyzer: " + a);

        switch (a) {
            case "std" :
                analyzer = new StandardAnalyzer();
                break;
            case "eng":
                analyzer = new EnglishAnalyzer();

                break;
            default:
                System.out.println("Wrong choice of analyzer! Exiting.");
                System.exit(1);
        }
        switch (sim) {
            case "BM25" :
                similarity = new BM25Similarity();
                s1 = "bm25";
                break;
            case "LMJM":
                float lambda = Float.parseFloat(args[11]);
                System.out.println("Lambda = " + lambda);
                similarity = new LMJelinekMercerSimilarity(lambda);
                s1 = "lmjm";
                break;
            case "LMDS":
                similarity = new LMDirichletSimilarity();
                s1 = "lmds";
                break;

            default:
                System.out.println("Wrong choice of similarity! Exiting.");
                System.exit(1);
        }
        String outFile = "qe_ent" + "_" + s1 + "_" + s2 + ".run";

        new QEEntities(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile, entityQrel,
                takeKEntities, omit, analyzer, similarity);

    }

}
