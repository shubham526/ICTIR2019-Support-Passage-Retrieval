package support_passage;

import java.io.*;
import java.util.*;
import lucene.Index;
import lucene.RAMIndex;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import others.CheckHeapSize;


/**
 * Class for Support Passage Ranking.
 * @author Shubham Chatterjee
 * @version 02/23/2019
 */
public class SupportPassageFeature {

    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String PARA_RUN_FILE ;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    private HashMap<String,ArrayList<String>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private HashMap<String,ArrayList<String>> entityRankings;
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
    public SupportPassageFeature(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String PARA_RUN_FILE, String ENTITY_RUN_FILE, String OUT_FILE) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.PARA_RUN_FILE = PARA_RUN_FILE;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();

        System.out.println("Getting entity rankings from run file...");
        entityRankings = Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.ENTITY_RUN_FILE);
        System.out.println("Done");

        System.out.println("Getting paragraph rankings from run file...");
        paraRankings = Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.PARA_RUN_FILE);
        System.out.println("Done");

        System.out.println("Heap Size = " + CheckHeapSize.getHeapSize());
        System.out.println("Max Heap Size = " +CheckHeapSize.getHeapMaxSize());

    }

    public void getFeature(int num) throws IOException {
        Analyzer analyzer = null;
        Similarity similarity = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int a, s, takeKTerms, takeKDocs, takeKEntities;
        String c;
        boolean choice;
        switch(num) {
            case 1 :
                System.out.println("Calculating feature1...");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
                System.out.println("Done");
                new Feature1().feature();
                System.out.println("Done calculating feature1");
                break;

            case 2 :
                System.out.println("Calculating feature2...");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
                System.out.println("Done");
                new Feature2().feature();
                System.out.println("Done calculating feature2");
                break;
            case 3:
                System.out.println("Calculating feature3...");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
                System.out.println("Done");
                new Feature3().feature();
                System.out.println("Done calculating feature3");
                break;
            case 4:
                System.out.println("Calculating feature4...");
                System.out.println("How many terms for query expansion?");
                takeKTerms = Integer.parseInt(br.readLine());
                System.out.println("How many documents for query expansion?");
                takeKDocs = Integer.parseInt(br.readLine());
                System.out.println("Which similarity? Enter the appropriate number from the list: ");
                System.out.println("1. BM25");
                System.out.println("2. Language Model with Jelinek-Mercer Smoothing");
                System.out.println("3. Language Model with Dirichlet Smoothing");
                s = Integer.parseInt(br.readLine());
                switch (s) {
                    case 1 :
                        similarity = new BM25Similarity();
                        break;
                    case 2:
                        System.out.println("Enter lambda: ");
                        float lambda = Float.parseFloat(br.readLine());
                        similarity = new LMJelinekMercerSimilarity(lambda);
                        break;
                    case 3:
                        similarity = new LMDirichletSimilarity();
                        break;
                        default:
                            System.out.println("Wrong choice!");
                            System.exit(1);
                }
                System.out.println("Which analyzer? Enter the appropriate number from the list: ");
                System.out.println("1. Standard");
                System.out.println("2. English");
                a = Integer.parseInt(br.readLine());

                switch (a) {
                    case 1 :
                        analyzer = new StandardAnalyzer();
                        break;
                    case 2:
                        analyzer = new EnglishAnalyzer();

                        break;
                    default:
                        System.out.println("Wrong choice!");
                        System.exit(1);
                }
                System.out.println("Omit query terms when calculating expansion terms (Y/N)?");
                c = br.readLine();
                choice = c.equalsIgnoreCase("Y");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(INDEX_DIR, "text", analyzer, similarity).getSearcher();
                System.out.println("Done");
                new Feature4(takeKTerms, takeKDocs, similarity, analyzer, choice).feature();
                System.out.println("Done calculating feature4");
                break;
            case 5:
                System.out.println("Calculating feature 5");
                System.out.println("How many entities for query expansion?");
                takeKEntities = Integer.parseInt(br.readLine());
                System.out.println("Which analyzer? Enter the appropriate number from the list: ");
                System.out.println("1. Standard");
                System.out.println("2. English");
                a = Integer.parseInt(br.readLine());

                switch (a) {
                    case 1 :
                        analyzer = new StandardAnalyzer();
                        break;
                    case 2:
                        analyzer = new EnglishAnalyzer();

                        break;
                    default:
                        System.out.println("Wrong choice!");
                        System.exit(1);
                }
                System.out.println("Which similarity? Enter the appropriate number from the list: ");
                System.out.println("1. BM25");
                System.out.println("2. Language Model with Jelinek-Mercer Smoothing");
                System.out.println("3. Language Model with Dirichlet Smoothing");
                s = Integer.parseInt(br.readLine());
                switch (s) {
                    case 1 :
                        similarity = new BM25Similarity();
                        break;
                    case 2:
                        System.out.println("Enter lambda: ");
                        float lambda = Float.parseFloat(br.readLine());
                        similarity = new LMJelinekMercerSimilarity(lambda);
                        break;
                    case 3:
                        similarity = new LMDirichletSimilarity();
                        break;
                    default:
                        System.out.println("Wrong choice!");
                        System.exit(1);
                }
                System.out.println("Omit query terms when calculating expansion terms (Y/N)?");
                c = br.readLine();
                choice = c.equalsIgnoreCase("Y");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(INDEX_DIR, "text", analyzer, similarity).getSearcher();
                System.out.println("Done");
                new Feature5(takeKEntities, choice, analyzer).feature();
                break;
            case 6:
                System.out.println("Calculating feature6...");
                System.out.println("Setting up index for use...");
                searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
                System.out.println("Done");
                System.out.println("How many entities for query expansion?");
                takeKEntities = Integer.parseInt(br.readLine());
                System.out.println("Which similarity? Enter the appropriate number from the list: ");
                System.out.println("1. BM25");
                System.out.println("2. Language Model with Jelinek-Mercer Smoothing");
                System.out.println("3. Language Model with Dirichlet Smoothing");
                s = Integer.parseInt(br.readLine());
                switch (s) {
                    case 1 :
                        similarity = new BM25Similarity();
                        break;
                    case 2:
                        System.out.println("Enter lambda: ");
                        float lambda = Float.parseFloat(br.readLine());
                        similarity = new LMJelinekMercerSimilarity(lambda);
                        break;
                    case 3:
                        similarity = new LMDirichletSimilarity();
                        break;
                    default:
                        System.out.println("Wrong choice!");
                        System.exit(1);
                }
                System.out.println("Which analyzer? Enter the appropriate number from the list: ");
                System.out.println("1. Standard");
                System.out.println("2. English");
                a = Integer.parseInt(br.readLine());

                switch (a) {
                    case 1 :
                        analyzer = new StandardAnalyzer();
                        break;
                    case 2:
                        analyzer = new EnglishAnalyzer();

                        break;
                    default:
                        System.out.println("Wrong choice!");
                        System.exit(1);
                }
                System.out.println("Omit query terms when calculating expansion terms (Y/N)?");
                c = br.readLine();
                choice = c.equalsIgnoreCase("Y");
                new Feature6(takeKEntities, choice, analyzer, similarity).feature();
                System.out.println("Done calculating feature6");
                break;
        }

    }

    /**
     * Inner class to calculate the feature 1.
     */

    private class Feature1 {

        /**
         * Method to calculate the first feature.
         * Works in parallel using Java 8 parallelStreams.
         * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
         * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
         */

        private  void feature() {
            //Get the set of queries
            Set<String> querySet = entityRankings.keySet();
            System.out.println(querySet.size());

            // Do in parallel
            querySet.parallelStream().forEach(this::doTask);


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

        private void doTask(String queryId) {
            ArrayList<String> pseudoDocEntityList;
            ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
            HashMap<String, Integer> freqMap = new HashMap<>();
            // Get the list of entities relevant for the query
            ArrayList<String> entityList = entityRankings.get(queryId);
            if (entityList == null) {
                return;
            }
            ArrayList<String> processedEntityList = Utilities.process(entityList);
            // Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = paraRankings.get(queryId);
            if (paraList == null) {
                return;
            }

            // For every entity in this list of relevant entities do
            for (String entityId : entityList) {

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
                            + " " + score + " " + "ENTITY CONTEXT NEIGHBOURS";
                    runStrings.add(runFileString);
                    rank++;
                }

            }
        }
    }

    /**
     * Inner class to calculate feature2.
     */

    private class Feature2 {
        /**
         * Method to calculate the second feature.
         * Works in parallel using Java 8 parallelStreams.
         * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
         * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N")
         * to set the thread pool size
         */

        private  void feature() {
            //Get the set of queries
            Set<String> querySet = entityRankings.keySet();

            // Do in parallel
            querySet.parallelStream().forEach(this::doTask);

            // Create the run file
            System.out.println("Writing to run file");
            Utilities.writeFile(runStrings, TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
            System.out.println("Run file written at: " + TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
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
            // Get the list of entities relevant for the query
            ArrayList<String> entityList = Utilities.process(entityRankings.get(queryId));
            //Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = Utilities.process(paraRankings.get(queryId));
            HashMap<String, HashMap<String, Integer>> scoreMap = new HashMap<>();
            Document doc = null;
            int score;

            // For every paragraph relevant to the query do
            for (String paraId : paraList) {
                // Get the lucene document corresponding to the paragraph
                try {
                    doc = Index.Search.searchIndex("id", paraId, searcher);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                // Get the entities in the paragraph
                // Make an ArrayList from the String array
                List<String> pEntList = Arrays.asList(
                        Utilities.clean(doc.getField("entity").stringValue().split(" ")));
                /*
                 * The feature value for a query-entity pair and the paragraph is the number of links
                 * a paragraph has to an entity in the list entityList. This is basically an intersection
                 * operation on the two lists :
                 * list1 = list of entities relevant to the query
                 * list2 = list of entities in the paragraph
                 */
                score = Utilities.intersection(entityList, pEntList).size();
                ArrayList<String> paraEntities = Utilities.unprocess(pEntList,entityRankings.get(queryId));
                HashMap<String, Integer> map = new HashMap<>();
                for (String e : paraEntities) {
                    String query = queryId + "+" + e;
                    if (scoreMap.containsKey(query)) {
                        map = scoreMap.get(query);
                    }
                    map.put(paraId, score);
                    scoreMap.put(query ,map);
                }
            }
            makeRunStrings(scoreMap);
            System.out.println("Done query: " + queryId);
        }

        private void makeRunStrings(@NotNull HashMap<String, HashMap<String, Integer>> scoreMap) {
            String runFileString;
            int rank , score;
            for (String queryId : scoreMap.keySet()) {
                rank = 0;
                LinkedHashMap<String, Integer> map = Utilities.sortByValueDescending(scoreMap.get(queryId));
                for (String paraId : map.keySet()) {
                    score = map.get(paraId);
                    if (score != 0) {
                        runFileString = queryId + " Q0 " + paraId + " " + rank
                                + " " + score + " " + "feature2";
                        //System.out.println(runFileString);
                        runStrings.add(runFileString);
                        rank++;
                    }
                }
            }
        }
    }

    /**
     * Inner class to calculate feature3.
     */
    private class Feature3 {
        /**
         * Method to calculate the feature.
         * @throws IOException
         */
        private  void feature() throws IOException {
            //Get the set of queries
            Set<String> querySet = entityRankings.keySet();

            for (String q : querySet) {
                doTask(q);
            }

            // Create the run file
            System.out.println("Writing to run file");
            Utilities.writeFile(runStrings, TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
            System.out.println("Run file written at: " + TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
        }

        /**
         * Helper method.
         * @param queryId String
         * @throws IOException
         */
        private void doTask(String queryId) throws IOException {
            // Get the list of entities relevant for the query
            ArrayList<String> entityList = entityRankings.get(queryId);

            // Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = paraRankings.get(queryId);

            ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
            ArrayList<Document> queryDocs = new ArrayList<>();
            HashMap<String, PseudoDocument>  entityToPseudoDocMap = new HashMap<>();
            Map<Document, Float> documentScore;

            // For every entity in this list of relevant entities do
            for (String entityId : entityList) {
                //System.out.println(entityId);

                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);
                if (d != null) {
                    // Add it to the list of pseudo-documents for this entity
                    pseudoDocuments.add(d);

                    // Add to HashMap where Key = entityID and Value = Pseudo-document
                    entityToPseudoDocMap.put(entityId, d);

                    // Convert Pseudo-document to lucene document
                    Document doc = Utilities.pseudoDocToDoc(d);

                    // Add it to list of documents for query
                    queryDocs.add(doc);
                }
            }

            // Build the index
            System.out.println("=====================================================================================");
            System.out.println("Building index for query: " + queryId);
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
                    .substring(queryId.indexOf(":")+1)          // remove enwiki: from query
                    .replaceAll("%20", " ")     // replace %20 with whitespace
                    .toLowerCase();                            //  convert query to lowercase
            // Now search the query
            System.out.println("Searching index for query:" + query);
            LinkedHashMap<Document, Float> results = Utilities.sortByValueDescending(RAMIndex.searchIndex(query, 100, is, qp));
            System.out.println("Got " + results.size() + " results.");
            if (!results.isEmpty()) {
                System.out.println("Scoring paragraphs");
                documentScore = Utilities.sortByValueDescending(scoreParas(results, entityToPseudoDocMap));
                makeRunStrings(queryId, documentScore, entityToPseudoDocMap);
            } else {
                System.out.println("No results found. Cannot score documents.");
            }
            System.out.println("Done query: " + queryId);
            RAMIndex.close(iw);
        }

        /**
         * Make the run file strings.
         * @param queryId String
         * @param documentScore HashMap where Key = Document and Value = Score
         * @param entityToPseudoDocMap HashMap where Key = entity and Value = Pseudo-document for this entity
         */

        private void makeRunStrings(String queryId, Map<Document, Float> documentScore, @NotNull Map<String, PseudoDocument> entityToPseudoDocMap) {
            String runFileString, paraId;
            int rank;
            float score;

            // For every entity do
            for (String entityId : entityToPseudoDocMap.keySet()) {
                rank = 0;
                // Get the pseudo-document for the entity
                PseudoDocument doc = entityToPseudoDocMap.get(entityId);
                // Get the documents in the pseudo-document
                ArrayList<Document> docList = doc.getDocumentList();
                // For every document do
                for (Document d : docList) {
                    paraId = d.getField("id").stringValue();
                    if (documentScore.containsKey(d)) {
                        score = documentScore.get(d);
                        runFileString = queryId + "+" + entityId + " Q0 " + paraId + " " + rank
                                + " " + score + " " + "feature3";
                        runStrings.add(runFileString);
                        rank++;
                    }
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


        private Map<Document, Float> scoreParas(@NotNull Map<Document, Float> results, HashMap<String, PseudoDocument>  entityToPseudoDocMap) {
            Map<Document, Float> documentScore = new HashMap<>();
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
                        // If the document is already has a score get that score and add it to the new score
                        // Else add it to the score map
                        if (documentScore.containsKey(document)) {
                            s = documentScore.get(document);
                        }
                        s += score;
                        documentScore.put(document, s);
                    }
                }
            }
            System.out.println("Scored " + documentScore.keySet().size() + " documents");
            return documentScore;
        }

    }

    /**
     * Inner class to computer feature4.
     */

    private class Feature4 {
        private int takeKTerms; // Number of query expansion terms
        private int takeKDocs; // Number of documents for query expansion
        private boolean omitQueryTerms; // Omit query terms or not when calculating expansion terms
        private Similarity similarity;
        private Analyzer analyzer;
        HashMap<String, ArrayList<String>> entityQrels;

        /**
         * Constructor.
         * @param takeKTerms Number of query expansion terms
         * @param takeKDocs Number of documents for query expansion
         */
        @Contract(pure = true)
        public Feature4(int takeKTerms, int takeKDocs, Similarity similarity, Analyzer analyzer, boolean omitQueryTerms) {

            this.takeKTerms = takeKTerms;
            this.takeKDocs = takeKDocs;
            this.similarity = similarity;
            this.analyzer = analyzer;
            this.omitQueryTerms = omitQueryTerms;
            String entityQrelsFilePath = "/home/shubham/Desktop/research/TREC_CAR/data/benchmarks/benchmarkY1/train/train.pages.cbor-article.entity.qrels";
            entityQrels = Utilities.getRankings(entityQrelsFilePath);
        }
        /**
         * Method to calculate the second feature.
         * Works in parallel using Java 8 parallelStreams.
         * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
         * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
         * @throws IOException
         */

        private  void feature() throws IOException {
            /*System.out.println("takeKTerms = " + takeKTerms);
            System.out.println("takeKDocs = " + takeKDocs);
            System.out.println("Similarity = " + similarity.toString());
            System.out.println("Analyzer = " + analyzer.toString());
            System.out.println("omitQueryTerms = " + omitQueryTerms);*/
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
            System.out.println("Writing to run file");
            Utilities.writeFile(runStrings, TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
            System.out.println("Run file written at: " + TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
        }

        /**
         * Helper method.
         * @param queryId String Query
         * @throws IOException
         */
        private void doTask(String queryId) throws IOException {
            // Get the list of entities relevant for the query
            ArrayList<String> entityList = entityRankings.get(queryId);
            Set<String> retEnitySet = new HashSet<>(entityList);
            Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryId));
            retEnitySet.retainAll(relEntitySet);

            // Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = Utilities.process(paraRankings.get(queryId));

            ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
            ArrayList<Document> queryDocs = new ArrayList<>();
            HashMap<String, PseudoDocument>  entityToPseudoDocMap = new HashMap<>();
            // For every entity in this list of relevant entities do
            for (String entityId : retEnitySet) {
            //for (int i = 0; i < 50; i++) {
                //String entityId = entityList.get(i);
                //System.out.println(entityId);

                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);
                if (d != null) {
                    // Add it to the list of pseudo-documents for this entity
                    pseudoDocuments.add(d);

                    // Add to HashMap where Key = entityID and Value = Pseudo-document
                    entityToPseudoDocMap.put(entityId, d);

                    // Convert Pseudo-document to lucene document
                    Document doc = Utilities.pseudoDocToDoc(d);

                    // Add it to list of documents for query
                    queryDocs.add(doc);
                }
            }
            // Building the index
            //System.out.println("=====================================================================================");
            //System.out.println("Building index for query: " + queryId);
            // First create the IndexWriter
            IndexWriter iw = RAMIndex.createWriter(new EnglishAnalyzer());
            // Now create the index
            RAMIndex.createIndex(queryDocs, iw);
            // Create the IndexSearcher
            IndexSearcher is = RAMIndex.createSearcher(similarity, iw);
            String queryStr = queryId
                    .substring(queryId.indexOf(":")+1)          // remove enwiki: from query
                    .replaceAll("%20", " ")     // replace %20 with whitespace
                    .toLowerCase();                            //  convert query to lowercase
            // Get the RM3 expansion terms
            List<Map.Entry<String, Float>> relevanceModel = RM3Expand.getExpansionTerms(is, takeKTerms,takeKDocs,
                    queryStr,omitQueryTerms, analyzer);
            // Convert the query to a BooleanQuery expanded using the RM3 terms
            BooleanQuery booleanQuery = RM3Expand.toRm3Query(queryStr, relevanceModel, analyzer);
            // Search the index with this expanded query
            LinkedHashMap<Document, Float> results = Utilities.sortByValueDescending(RAMIndex.searchIndex(booleanQuery, 100, is));
            //System.out.println("Found " + results.size() + " RM3 results.");
            if (!results.isEmpty()) {
                //System.out.println("Scoring paragraphs");
                LinkedHashMap<String, Float> documentScore = Utilities.sortByValueDescending(scoreParas(results, entityToPseudoDocMap));
                makeRunStrings(queryId, documentScore, entityToPseudoDocMap);
            } else {
                //System.out.println("No results found. Cannot score documents.");
            }
            //System.out.println("Done query: " + queryId);
            RAMIndex.close(iw);

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



        private Map<String, Float> scoreParas(@NotNull Map<Document, Float> results, HashMap<String, PseudoDocument>  entityToPseudoDocMap) {
            Map<String, Float> documentScore = new HashMap<>();
            List<String> myList = new ArrayList<>();
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
                        String pid = document.getField("id").stringValue();
//                        if (myList.contains(pid)) {
//                            System.out.println(pid + " "  + "present");
//                        } else {
//                            myList.add(pid);
//                        }
                        float s = 0;
                        // If the document is already has a score get that score and add it to the new score
                        // Else add it to the score map
                        if (documentScore.containsKey(pid)) {
                            //System.out.println("here");
                            s = documentScore.get(pid);
                        }
                        s += score;
                        documentScore.put(pid, s);
                    }
                }
            }
            //System.out.println("Scored " + documentScore.keySet().size() + " documents");
            return documentScore;
        }
        /**
         * Make the run file strings.
         * @param queryId String
         * @param documentScore HashMap where Key = Document and Value = Score
         * @param entityToPseudoDocMap HashMap where Key = entity and Value = Pseudo-document for this entity
         */

        private void makeRunStrings(String queryId, Map<String, Float> documentScore, @NotNull Map<String, PseudoDocument> entityToPseudoDocMap) {
            String runFileString, paraId;
            int rank;
            double score;

            // For every entity do
            for (String entityId : entityToPseudoDocMap.keySet()) {
                rank = 0;
                // Get the pseudo-document for the entity
                PseudoDocument doc = entityToPseudoDocMap.get(entityId);
                // Get the documents in the pseudo-document
                ArrayList<Document> docList = doc.getDocumentList();
                HashMap<String, Double> scoreMap = new HashMap<>();
                // For every document do
                for (Document d : docList) {
                    paraId = d.getField("id").stringValue();
                    if (documentScore.containsKey(paraId)) {
                        score = documentScore.get(paraId);
                        scoreMap.put(paraId,score);
//                        runFileString = queryId + "+" + entityId + " Q0 " + paraId + " " + rank
//                                + " " + score + " " + "QEW";
//                        //System.out.println(runFileString);
//                        runStrings.add(runFileString);
//                        rank++;
                    }
                }
                LinkedHashMap<String, Double> sortedScoreMap = Utilities.sortByValueDescending(scoreMap);
                for (String pid : sortedScoreMap.keySet()) {
                    runFileString = queryId + "+" + entityId + " Q0 " + pid + " " + rank
                                + " " + sortedScoreMap.get(pid) + " " + "QEW";
                        //System.out.println(runFileString);
                        runStrings.add(runFileString);
                        rank++;
                }

            }
        }
    }

    /**
     * Inner class to calculate feature5.
     * A variation of Feature1.
     */
    private class Feature5 {
        private int takeKEntities;
        private boolean omitQueryTerms;
        private Analyzer analyzer;


        @Contract(pure = true)
        public Feature5(int takeKEntities, boolean omitQueryTerms, Analyzer analyzer) {
            this.takeKEntities = takeKEntities;
            this.omitQueryTerms = omitQueryTerms;
            this.analyzer = analyzer;
        }
        /**
         * Method to calculate the first feature.
         * Works in parallel using Java 8 parallelStreams.
         * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
         * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
         */

        private  void feature() {
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

        private void doTask(String queryId) throws IOException {
            List<Map.Entry<String, Integer>> contextEntityList;
            List<Map.Entry<String, Integer>> expansionEntities;

            // Get the list of entities relevant for the query
            ArrayList<String> entityList = entityRankings.get(queryId);

            // Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = Utilities.process(paraRankings.get(queryId));

            // For every entity in this list of relevant entities do
            for (String entityId : entityList) {
            //for (int i = 0; i < 50; i++){
                //String entityId = entityList.get(i);
                // Get the list of all entities which co-occur with this entity in a given context
                // Context here is the same as a PseudoDocument for the entity
                // So we are actually looking at all entities that occur in the PseudoDocument
                // sorted in descending order of frequency
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
        private List<Map.Entry<String, Integer>> getContextEntities(String entityId, ArrayList<String> entityList, ArrayList<String> paraList ) {
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
        private void makeRunStrings(String queryId, String entityId, @NotNull TopDocs topDocs) throws IOException {
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
    }

    /**
     * Inner class to calculate feature6.
     */
    private class Feature6 {
        private int takeKEntities;
        private boolean omitQueryTerms;
        private Analyzer analyzer;
        private Similarity similarity;


        @Contract(pure = true)
        public Feature6(int takeKEntities, boolean omitQueryTerms, Analyzer analyzer, Similarity similarity) {
            this.takeKEntities = takeKEntities;
            this.omitQueryTerms = omitQueryTerms;
            this.analyzer = analyzer;
            this.similarity = similarity;
        }
        /**
         * Method to calculate the feature.
         * @throws IOException
         */
        private  void feature() throws IOException {
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
            System.out.println("Writing to run file");
            Utilities.writeFile(runStrings, TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
            System.out.println("Run file written at: " + TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE);
        }

        /**
         * Helper method.
         * @param queryId String
         * @throws IOException
         */
        private void doTask(String queryId) throws IOException {
            // Get the list of entities relevant for the query
            ArrayList<String> entityList = entityRankings.get(queryId);

            // Get the list of paragraphs relevant for the query
            ArrayList<String> paraList = paraRankings.get(queryId);

            List<PseudoDocument> pseudoDocuments = new ArrayList<>(); // List of pseudo-documents
            List<Document> queryDocs = new ArrayList<>(); // List of lucene documents corresponding to the pseudo-documents
            Map<String, PseudoDocument>  entityToPseudoDocMap = new HashMap<>(); // Map where Key = entity and Value = PseudoDocument
            Map<Document, Float> documentScore = new LinkedHashMap<>(); // Map where Key = Document and Value = Score
            List<Map.Entry<String, Integer>> contextEntityList = new ArrayList<>(); // List of entities in context
            List<Map.Entry<String, Integer>> expansionEntities; // List of expansion entities

            // Get the list of pseudo-documents for the query
            getPseudoDocumentList(entityList, paraList, pseudoDocuments, entityToPseudoDocMap, queryDocs);

            ///////////////////////////////////////
            // Build the index of pseudo-documents
            ///////////////////////////////////////

            // First create the IndexWriter
            IndexWriter iw = RAMIndex.createWriter(analyzer);
            // Now create the index
            RAMIndex.createIndex(queryDocs, iw);
            // Create the IndexSearcher
            IndexSearcher is = RAMIndex.createSearcher(similarity, iw);

            // Process the query
            String queryStr = queryId
                    .substring(queryId.indexOf(":") + 1)          // remove enwiki: from query
                    .replaceAll("%20", " ")     // replace %20 with whitespace
                    .toLowerCase();                            //  convert query to lowercase


            // For every entity in this list of relevant entities do
            //for (String entityId : entityList) {
            for (int i = 0; i < 50; i++) {
                String entityId = entityList.get(i);

                // Get the PseudoDocument corresponding to the entity
                PseudoDocument pseudoDocument = entityToPseudoDocMap.get(entityId);
                // If the map contains no PseudoDocument for entityId then continue
                if (pseudoDocument == null) {
                    continue;
                }
                // Get the entities co-occurring with this entity
                getContextEntities(pseudoDocument, contextEntityList, entityList);

                // Use the top K entities for expansion
                expansionEntities = contextEntityList.subList(0, Math.min(takeKEntities, contextEntityList.size()));

                // If no expansion entities were found then continue
                if (expansionEntities.size() == 0) {
                    continue;
                }
                // Convert the query to an expanded BooleanQuery
                BooleanQuery booleanQuery = EntityRMExpand.toEntityRmQuery(queryStr, expansionEntities, omitQueryTerms,
                        analyzer);

               // Search the index
                //System.out.println("Searching index for query:" + queryStr);
                LinkedHashMap<Document, Float> results = Utilities.sortByValueDescending(RAMIndex.searchIndex(booleanQuery, 100, is));
                //System.out.println("Got " + results.size() + " results.");
                if (!results.isEmpty()) {
                    scoreParas(results, entityToPseudoDocMap, documentScore);
                    makeRunStrings(queryId, documentScore, entityToPseudoDocMap);
                }
                pseudoDocuments.clear();
                contextEntityList.clear();
                documentScore.clear();
            }
            // Close the index
            RAMIndex.close(iw);
        }
        private void getPseudoDocumentList(@NotNull ArrayList<String> entityList,
                                           ArrayList<String> paraList,
                                           List<PseudoDocument> pseudoDocuments,
                                           Map<String, PseudoDocument>  entityToPseudoDocMap,
                                           List<Document> queryDocs) {
            // For every entity in this list of relevant entities do
            for (String entityId : entityList) {

                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);
                if (d != null) {
                    // Add it to the list of pseudo-documents for this entity
                    pseudoDocuments.add(d);

                    // Add to HashMap where Key = entityID and Value = Pseudo-document
                    entityToPseudoDocMap.put(entityId, d);

                    // Convert Pseudo-document to lucene document
                    Document doc = Utilities.pseudoDocToDoc(d);

                    // Add it to list of documents for query
                    queryDocs.add(doc);
                }
            }
        }
        private void getContextEntities(@NotNull PseudoDocument pseudoDocument,
                                        List<Map.Entry<String, Integer>> contextEntityList,
                                        ArrayList<String> entityList) {
            HashMap<String, Integer> freqMap = new HashMap<>();
            ArrayList<String> processedEntityList = Utilities.process(entityList);
            ArrayList<String> pseudoDocEntityList;

            // Get the list of co-occurring entities
            pseudoDocEntityList = pseudoDocument.getEntityList();
            // For every co-occurring entity do
            for (String e : pseudoDocEntityList) {
                // If the entity also occurs in the list of entities relevant for the query then
                if (processedEntityList.contains(e)) {
                    // Find the frequency of this entity in the pseudo-document and store it
                    freqMap.put(e, Utilities.frequency(e, pseudoDocEntityList));
                }
            }

            // Sort the entities in decreasing order of frequency
            // Add all the entities to the list
            contextEntityList.addAll(Utilities.sortByValueDescending(freqMap).entrySet());
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



        private void scoreParas(@NotNull Map<Document, Float> results,
                                                Map<String, PseudoDocument>  entityToPseudoDocMap,
                                                Map<Document, Float> documentScore) {
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
                        // If the document is already has a score get that score and add it to the new score
                        // Else add it to the score map
                        if (documentScore.containsKey(document)) {
                            s = documentScore.get(document);
                        }
                        s += score;
                        documentScore.put(document, s);
                    }
                }
            }
            documentScore = Utilities.sortByValueDescending(documentScore);
            //System.out.println("Scored " + documentScore.keySet().size() + " documents");
        }
        /**
         * Make the run file strings.
         * @param queryId String
         * @param documentScore HashMap where Key = Document and Value = Score
         * @param entityToPseudoDocMap HashMap where Key = entity and Value = Pseudo-document for this entity
         */

        private void makeRunStrings(String queryId, Map<Document, Float> documentScore, @NotNull Map<String, PseudoDocument> entityToPseudoDocMap) {
            String runFileString, paraId;
            int rank;
            float score;

            // For every entity do
            for (String entityId : entityToPseudoDocMap.keySet()) {
                rank = 0;
                // Get the pseudo-document for the entity
                PseudoDocument doc = entityToPseudoDocMap.get(entityId);
                // Get the documents in the pseudo-document
                ArrayList<Document> docList = doc.getDocumentList();
                // For every document do
                for (Document d : docList) {
                    paraId = d.getField("id").stringValue();
                    if (documentScore.containsKey(d)) {
                        score = documentScore.get(d);
                        runFileString = queryId + "+" + entityId + " Q0 " + paraId + " " + rank
                                + " " + score + " " + "feature6";
                        //System.out.println(runFileString);
                        runStrings.add(runFileString);
                        rank++;
                    }
                }

            }
        }
    }
    /**
     * Main method to run the code.
     * @param args Command line parameters.
     */

    public static void main(@NotNull String[] args) {

        String indexDir = args[0];
        String trecCarDir = args[1];
        String outputDir = args[2];
        String dataDir = args[3];
        String paraRunFile = args[4];
        String entityRunFile = args[5];
        String outFile = args[6];

        try {
            new SupportPassageFeature(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).getFeature(1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
