package support_passage;

import lucene.Index;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * Class to make baseline for support passage retrieval.
 * Retrieve passages with query = queryID + entityID.
 * @author Shubham Chatterjee
 * @version 05/09/2019
 */

public class Baseline2 {
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;

    // List of queries
    private ArrayList<String> queryList;

    // HashMap where Key = queryID, Value = ArrayList of EntityID
    private HashMap<String,ArrayList<String>> entityRankings;
    private HashMap<String,ArrayList<String>> entityQrels;

    // ArrayList of run strings
    private ArrayList<String> runStrings;

    // List of tokens in a query
    //private List<String> tokens;

    // Fields to search
    private List<String> searchFields;

    private Analyzer analyzer;
    private Similarity similarity;

    // List of paragraphs already retrieved.
    // Only paragraphs not in this list are added to the runfile.
    private ArrayList<String> paraID;

    /**
     * Constructor.
     * @param INDEX_DIR Path to the index directory
     * @param TREC_CAR_DIR Path to TREC-CAR directory
     * @param OUTPUT_DIR Path to the output directory within TREC-CAR directory
     * @param DATA_DIR Path to data directory within TREC-CAR directory
     * @param ENTITY_RUN_FILE Name of the entity run file within data directory
     * @param OUT_FILE Name of the new output file
     * @throws IOException Exception
     */
    public Baseline2(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String ENTITY_RUN_FILE,
                     String entity_qrel_file, String OUT_FILE, Analyzer analyzer, Similarity similarity, List<String> searchFields) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();
        this.analyzer = analyzer;
        this.similarity = similarity;
        //this.tokens = new ArrayList<>(128);
        this.paraID = new ArrayList<>();
        this.searchFields = searchFields;

        System.out.print("Setting up Index for use....");
        this.searcher = new Index.Setup(INDEX_DIR, "text", analyzer, similarity).getSearcher();
        System.out.println("Done.");

        System.out.print("Getting entity rankings from run file...");
        entityRankings = Utilities.getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.ENTITY_RUN_FILE);
        this.queryList = new ArrayList<>(entityRankings.keySet());
        System.out.println("Done");

        System.out.print("Getting entity qrels...");
        entityQrels = Utilities.getRankings(entity_qrel_file);
        System.out.println("Done");

    }

    /**
     * Do the baseline.
     * @throws IOException Exception
     */

    public void baseline() throws IOException {
//        ProgressBar pb = new ProgressBar("Progress", queryList.size());
//        //int n = 0;
//
//        for (String queryID : queryList) {
//            ArrayList<String> entityList = entityRankings.get(queryID);
//            for (String entityID : entityList) {
//                search(queryID, entityID);
//            }
//            //System.out.print(queryID + " ");
//            //n++;
//            //System.out.println("n = " + n);
//            pb.step();
//        }
//        //System.out.println("n="+n);
//        pb.close();

         //Do in parallel
        queryList.parallelStream().forEach(this::doTask);

        // Create the run file
        System.out.println("Writing to run file");
        String filePath = TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE;
        Utilities.writeFile(runStrings, filePath);
        System.out.println("Run file written at: " + filePath);
    }
    private void doTask(String queryID) {
        //ArrayList<String> entityList = entityRankings.get(queryID);
        // Get the set of entities retrieved for the query
        Set<String> retEntitySet = new HashSet<>(entityRankings.get(queryID));

        // Get the set of entities relevant for the query
        Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryID));

        // Get the number of retrieved entities which are also relevant
        // Finding support passage for non-relevant entities makes no sense!!

        retEntitySet.retainAll(relEntitySet);

        //for (String entityID : entityList) {
        for (String entityID : retEntitySet) {
            try {
                search(queryID, entityID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(queryID);
    }

    /**
     * Search the Lucene index for Query = QueryID + EntityID
     * @param queryID String
     * @param entityID String
     * @throws IOException Exception
     */
    private void search(String queryID, String entityID) throws IOException {
        String query = queryID.substring(queryID.indexOf(":")+1).replaceAll("%20"," ");
        String entity = entityID.substring(entityID.indexOf(":")+1).replaceAll("%20"," ");
//        System.out.println("queryID:" + queryID);
//        System.out.println("query:" + query);
//        System.out.println("entityID:" + entityID);
//        System.out.println("entity:" + entity);
        BooleanQuery booleanQuery = toQuery(query, entity);
        TopDocs topDocs = Index.Search.searchIndex(booleanQuery,100);

        //System.out.println(topDocs.totalHits);
        createRunFile(queryID + "+" + entityID, topDocs);
    }
    /**
     * Create a run file
     * Run file string format: $queryId Q0 $paragraphId $rank $score $name
     * @param queryID String ID of the query
     * @param topDocs TopDocs Top hits for the query
     * @throws IOException Exception
     */
    private void createRunFile(String queryID, @NotNull TopDocs topDocs) throws IOException {
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        if (scoreDocs.length == 0) {
            System.out.println("Got nothing for: " + queryID);
            return;
        }
        Document d;
        String runFileString;

        for (int i = 0; i < scoreDocs.length; i++) {
            d = searcher.doc(scoreDocs[i].doc);
            String pID = d.getField("Id").stringValue();

            runFileString = queryID + " Q0 " + pID + " " + (i + 1) + " " + topDocs.scoreDocs[i].score + " " + "Baseline2";
            if(!paraID.contains(pID)) {
                paraID.add(pID);
                runStrings.add(runFileString);
                //System.out.println(runFileString);
            }
        }
    }
    /**
     * Convert a query along  to a boolean query
     * @param query String  query
     * @param entity String entity
     * @return BooleanQuery A boolean query representing the terms in the original query
     * @throws IOException Exception
     */
    private BooleanQuery toQuery(String query, String entity) throws IOException {

        List<String> tokens = new ArrayList<>(128);
        List<String> entTokens = new ArrayList<>(128);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        tokenizeQuery(query,"Text",tokens, new EnglishAnalyzer());
        for (String searchField : searchFields) {
            for (String token : tokens) {
                booleanQuery.add(new TermQuery(new Term(searchField, token)), BooleanClause.Occur.SHOULD);
            }
        }
        tokenizeQuery(entity, "EntityLinks",entTokens, new EnglishAnalyzer());
        for (String token : entTokens) {
            booleanQuery.add(new TermQuery(new Term("EntityLinks", token)), BooleanClause.Occur.SHOULD);
        }
        return booleanQuery.build();
    }

    /**
     * Tokenizes the query and stores results in (the passed in) `tokens` -- Not thread safe!
     * @param stringToTokenize String
     * @param field String
     * @param tokens List
     * @throws IOException
     */
    private void tokenizeQuery(String stringToTokenize, String field, List<String> tokens, Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream(field, new StringReader(stringToTokenize));
        tokenStream.reset();
        tokens.clear();
        while (tokenStream.incrementToken() && tokens.size() < 64) {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            tokens.add(token);
        }
        tokenStream.end();
        tokenStream.close();
    }
    private static class Filter {
        private String runFile;
        private String outFile;
        private IndexSearcher searcher;
        private String index;
        private HashMap<String, HashMap<String, HashMap<String, Double>>> runFileMap = new HashMap<>();

        public Filter(String index, String runFile, String outFile) {
            this.index = index;
            this.runFile = runFile;
            this.outFile = outFile;

            System.out.print("Setting up index....");
            searcher = new Index.Setup(this.index).getSearcher();
            System.out.println("Done");

            System.out.print("Reading run file....");
            getRunFileMap(this.runFileMap);
            System.out.println("Done");
        }
        private void getRunFileMap(HashMap<String, HashMap<String, HashMap<String, Double>>> queryMap) {
            BufferedReader in = null;
            String line;
            HashMap<String, HashMap<String, Double>> entityMap;
            HashMap<String, Double> paraMap;
            int n =0;
            try {
                in = new BufferedReader(new FileReader(runFile));
                while((line = in.readLine()) != null) {
                    String[] fields = line.split(" ");
                    String queryID = fields[0].split("\\+")[0];
                    String entityID = fields[0].split("\\+")[1];
                    String paraID = fields[2];
                    double paraScore = Double.parseDouble(fields[4]);
                    if (queryMap.containsKey(queryID)) {
                        entityMap = queryMap.get(queryID);
                    } else {
                        entityMap = new HashMap<>();
                        n++;
                        System.out.println(queryID);
                    }
                    if (entityMap.containsKey(entityID)) {
                        paraMap = entityMap.get(entityID);
                    } else {
                        paraMap = new HashMap<>();
                    }
                    paraMap.put(paraID,paraScore);
                    entityMap.put(entityID,paraMap);
                    queryMap.put(queryID,entityMap);

                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if(in != null) {
                        in.close();
                    } else {
                        System.out.println("Input Buffer has not been initialized!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("n="+n);
        }
        public void filter() throws IOException, ParseException {
            ArrayList<String> runStrings = new ArrayList<>();
            ArrayList<String> queryList = new ArrayList<>(runFileMap.keySet());
            ProgressBar pb = new ProgressBar("Progress", queryList.size());
            for (String queryID : queryList) {
                HashMap<String, HashMap<String, Double>> entityMap = runFileMap.get(queryID);
                ArrayList<String> entityList = new ArrayList<>(entityMap.keySet());
                for (String entityID : entityList) {
                    HashMap<String, Double> paraMap = entityMap.get(entityID);
                    ArrayList<String> paraList = new ArrayList<>(paraMap.keySet());
                    int rank = 1;
                    for (String paraID : paraList) {
                        // Get the document corresponding to the paragraph from the lucene index
                        Document doc = Index.Search.searchIndex("id", paraID, searcher);
                        // Get the entities in the paragraph
                        assert doc != null;
                        ArrayList<String> pEntList = Utilities.getEntities(doc);
                        // If the document does not have any entities then ignore
                        if (pEntList.isEmpty()) {
                            continue;
                        }
                        // If the entity is present in the paragraph
                        if (pEntList.contains(Utilities.process(entityID))) {
                            String runFileString = queryID + "+" + entityID + " Q0 " + paraID + " " + rank
                                    + " " + paraMap.get(paraID) + " " + "baseline2-filtered";
                            //System.out.println(runFileString);
                            runStrings.add(runFileString);
                            rank++;
                        }

                    }
                }
                pb.step();
            }
            pb.close();
            // Create the run file
            System.out.println("Writing to run file");
            Utilities.writeFile(runStrings, outFile);
            System.out.println("Run file written at: " + outFile);
        }
    }

    /**
     * Main methid.
     * @param args command line arguments
     */

    public static void main(@NotNull String[] args) throws ParseException, IOException {
        String mode = args[0];

        if (mode.equalsIgnoreCase("baseline")) {
            System.out.println("Making baseline2....");

            String indexDir = args[1];
            String trecCarDir = args[2];
            String outputDir = args[3];
            String dataDir = args[4];
            String entityRunFile = args[5];
            String outFile = args[6];
            String a = args[7];
            String s = args[8];
            List<String> searchFields = Arrays.asList(Arrays.copyOfRange(args, 9, args.length));


            System.out.print("Searching fields: ");
            for (String field : searchFields) {
                System.out.print(field + " ");
            }
            System.out.println();
            Analyzer analyzer = null;
            Similarity similarity = null;

            switch (a) {
                case "eng":
                    System.out.println("Using English analyzer.");
                    analyzer = new EnglishAnalyzer();
                    break;
                case "std":
                    System.out.println("Using Standard analyzer.");
                    analyzer = new StandardAnalyzer();
                    break;
                default:
                    System.out.println("Wrong analyzer choice! Can be either English(eng) or Standard(std)");
            }

            switch (s) {
                case "bm25":
                    System.out.println("Using BM25.");
                    similarity = new BM25Similarity();
                    break;
                case "lmds":
                    System.out.println("Using LM-DS.");
                    similarity = new LMDirichletSimilarity();
                    break;
                case "lmjm":
                    System.out.println("Using LM-JM.");
                    similarity = new LMJelinekMercerSimilarity(0.5f);
                default:
                    System.out.println("Wrong choice! Can be either BM25(bm25), LM-DS(lmds) or LM-JM(lmjm)");
            }


            try {
                new Baseline2(indexDir, trecCarDir, outputDir, dataDir, entityRunFile,
                        "/home/shubham/Desktop/research/TREC_CAR/data/benchmarks/benchmarkY1/train/train.pages.cbor-article.entity.qrels",
                        outFile, analyzer, similarity, searchFields).baseline();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (mode.equalsIgnoreCase("filter")) {
            System.out.println("Filtering.......");
            String index = args[1];
            String runFile = args[2];
            String outFile = args[3];
            new Baseline2.Filter(index, runFile, outFile).filter();
            System.out.println("Done");
        } else {
            System.out.println("Wrong mode");
            System.exit(1);
        }
    }
}
