package EntitySalience;

import lucene.Index;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import others.CheckHeapSize;
import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This class checks the effect of introducing saliency in the ranking of feature 1.
 */

public class SaliencyEffect {
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String PARA_RUN_FILE ;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;
    //HashMap where Key = queryID and Value = list of paragraphs relevant for the queryID
    //private HashMap<String, ArrayList<String>> paraRankings;
    private LinkedHashMap<String,LinkedHashMap<String, Double>> paraRankings;
    //HashMap where Key = queryID and Value = list of entities relevant for the queryID
    private LinkedHashMap<String,LinkedHashMap<String, Double>> entityRankings;
    private HashMap<String, HashMap<String, HashMap<String, Double>>> runFileMap = new HashMap<>();
    // ArrayList of run strings
    private ArrayList<String> runStrings;

    public SaliencyEffect(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String PARA_RUN_FILE, String ENTITY_RUN_FILE, String OUT_FILE) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.PARA_RUN_FILE = PARA_RUN_FILE;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();
        this.entityRankings = new LinkedHashMap<>();
        this.paraRankings = new LinkedHashMap<>();

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
    public   void feature() throws IOException, ParseException {
        System.out.println("Calulating...");
        //Get the set of queries
        Set<String> querySet = paraRankings.keySet();
        ProgressBar pb = new ProgressBar("Progress", querySet.size());

        for (String query : querySet) {
            doTask(query);
            pb.step();
        }
        pb.close();


        // Create the run file
        System.out.println("Writing to run file");
        String filePath = TREC_CAR_DIR + "/" + OUTPUT_DIR + "/" + OUT_FILE;
        Utilities.writeFile(runStrings, filePath);
        System.out.println("Run file written at: " + filePath);
    }

    private void doTask(String query) throws IOException, ParseException {
        ArrayList<String> paraList = new ArrayList<>(paraRankings.get(query).keySet());
        Map<Document, Double> paraMap = new HashMap<>();
        Map<String, HashMap<String, Double>> scoreMap = new HashMap<>();
        String queryID = query.split("\\+")[0];
        String entityID = query.split("\\+")[1];
        String processedEntityID = Utilities.process(entityID);

        for (String paraID : paraList) {
            Document document = Index.Search.searchIndex("id", paraID, searcher);
            String text = document.getField("text").stringValue();
            Map<String, Double> saliencyMap = Saliency.getSalientEntities(text);
            if (saliencyMap == null) {
                //System.out.println("text:" + text);
                continue;
            }
            //System.out.println(saliencyMap);
            if (saliencyMap.containsKey(processedEntityID)) {
                paraMap.put(document, saliencyMap.get(processedEntityID));
            }
        }
        if (paraMap.size() != 0) {
            // Convert this paraMap to a distribution
            double sum = 0.0d;
            for (Double score : paraMap.values()) {
                sum += score;
            }
            Map<Document, Double> paraMap2 = new HashMap<>();
            for (Document document : paraMap.keySet()) {
                double score = paraMap.get(document) / sum;
                paraMap2.put(document, score);
            }
            // Score the paragraphs
            HashMap<String, Double> scores = new HashMap<>();
            for (Document document : paraMap2.keySet()) {
                double prob_entity_given_query = entityRankings.get(queryID).get(entityID);
                double prob_para_given_entity = paraMap2.get(document);
                double score = prob_entity_given_query * prob_para_given_entity;
                scores.put(document.get("id"), score);
                //System.out.println(scores);
            }
            scoreMap.put(query, scores);
            makeRunStrings(scoreMap);
        }
        //System.out.println("Done query: " + query);
    }
   /* private void getRunFileMap(HashMap<String, HashMap<String, HashMap<String, Double>>> queryMap) {
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
    }*/
    private void makeRunStrings(Map<String, HashMap<String, Double>> scoreMap) {
        String runFileString;
        for (String queryId : scoreMap.keySet()) {
            int rank = 0;
            HashMap<String, Double> sortedScoreMap = Utilities.sortByValueDescending(scoreMap.get(queryId));
            for (String paraId : sortedScoreMap.keySet()) {
                double score = sortedScoreMap.get(paraId);
                runFileString = queryId + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "Saliency";
                runStrings.add(runFileString);
                //System.out.println(runFileString);
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
            new SaliencyEffect(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).feature();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

    }





}
