package baselines;

import help.Utilities;
import lucene.Index;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

/**
 * This class makes a baseline for support passage retrieval.
 * Method: Retrieve passages with query = queryID + entityID.
 * @author Shubham Chatterjee
 * @version 05/09/2019
 */

public class Baseline2 {
    private IndexSearcher searcher;

    // List of queries
    private ArrayList<String> queryList;

    // HashMap where Key = queryID, Value = ArrayList of EntityID
    private HashMap<String,ArrayList<String>> entityRankings;

    private HashMap<String, ArrayList<String>> entityQrels;

    // ArrayList of run strings
    private ArrayList<String> runStrings;

    // Fields to search
    private List<String> searchFields;

    // List of paragraphs already retrieved.
    // Only paragraphs not in this list are added to the runfile.
    private ArrayList<String> paraID;

    /**
     * Constructor.
     * @param indexDir Path to the index directory
     * @param trecCarDir Path to TREC-CAR directory
     * @param outputDir Path to the output directory within TREC-CAR directory
     * @param dataDir Path to data directory within TREC-CAR directory
     * @param entityRunFile Name of the entity run file within data directory
     * @param entityQrelFilePath Path to the entity ground truth file
     * @param outFile Name of the new output file
     * @param analyzer Analyzer Type of analyzer to use (English or Standard)
     * @param similarity Similarity Type of similarity for the searcher (BM25, LM-DS, LM-JM)
     * @throws IOException Exception
     */
    public Baseline2(String indexDir,
                     String trecCarDir,
                     String outputDir,
                     String dataDir,
                     String entityRunFile,
                     String entityQrelFilePath,
                     String outFile,
                     Analyzer analyzer,
                     Similarity similarity,
                     List<String> searchFields) throws IOException {

        String entityRunFilePath = trecCarDir + "/" + dataDir + "/" + entityRunFile;
        String outFilePath = trecCarDir + "/" + outputDir + "/" + outFile;
        this.runStrings = new ArrayList<>();
        this.paraID = new ArrayList<>();
        this.searchFields = searchFields;

        System.out.print("Setting up Index for use....");
        this.searcher = new Index.Setup(indexDir, "Text", analyzer, similarity).getSearcher();
        System.out.println("[Done].");

        System.out.print("Reading entity ground truth...");
        entityQrels = Utilities.getRankings(entityQrelFilePath);
        System.out.println("[Done].");

        System.out.print("Reading entity rankings...");
        entityRankings = Utilities.getRankings(entityRunFilePath);
        this.queryList = new ArrayList<>(entityRankings.keySet());
        System.out.println("[Done]");

        makeBaseline(outFilePath);

    }

    /**
     * Do the baseline.
     * @param outFilePath String Path to the output file.
     * @throws IOException Exception
     */

    private void makeBaseline(String outFilePath) throws IOException {
        //Do in parallel
        queryList.parallelStream().forEach(this::doTask);

        // Create the run file
        System.out.print("Writing to run file...");
        Utilities.writeFile(runStrings, outFilePath);
        System.out.println("[Done].");
        System.out.println("Run file written at: " + outFilePath);
    }

    /**
     * Do the actual work.
     * @param queryID String Query
     */
    private void doTask(String queryID) {
        // Get the set of entities retrieved for the query
        Set<String> retEntitySet = new HashSet<>(entityRankings.get(queryID));

        // Get the set of entities relevant for the query
        Set<String> relEntitySet = new HashSet<>(entityQrels.get(queryID));

        // Get the number of retrieved entities which are also relevant
        // Finding support passage for non-relevant entities makes no sense!!

        retEntitySet.retainAll(relEntitySet);

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
    private void search(@NotNull String queryID,
                        @NotNull String entityID) throws IOException {
        String query = queryID.substring(queryID.indexOf(":")+1).replaceAll("%20"," ");
        String entity = entityID.substring(entityID.indexOf(":")+1).replaceAll("%20"," ");

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
    private void createRunFile(String queryID,
                               @NotNull TopDocs topDocs) throws IOException {
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
    private BooleanQuery toQuery(String query,
                                 String entity) throws IOException {

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
     * @throws IOException Exception
     */
    private void tokenizeQuery(String stringToTokenize,
                               String field,
                               @NotNull List<String> tokens,
                               @NotNull Analyzer analyzer) throws IOException {
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

    /**
     * Main method.
     * @param args command line arguments
     */
    public static void main(@NotNull String[] args) {
        String indexDir = args[0];
        String trecCarDir = args[1];
        String outputDir = args[2];
        String dataDir = args[3];
        String entityRunFile = args[4];
        String entityQrelFile = args[5];
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
            new Baseline2(indexDir, trecCarDir, outputDir, dataDir, entityRunFile, entityQrelFile,
                    outFile, analyzer, similarity, searchFields);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

