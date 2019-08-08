package support_passage;

import lucene.Index;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.jetbrains.annotations.NotNull;


import java.io.*;
import java.util.*;

/**
 * Class to create the baselines for support passage task.
 * @author Shubham Chatterjee
 * @version 03/22/2019
 */

public class Baseline1 {
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private String TREC_CAR_DIR;
    private String OUTPUT_DIR ;
    private String DATA_DIR;
    private String PARA_RUN_FILE ;
    private String ENTITY_RUN_FILE;
    private String OUT_FILE ;
    //  HashMap where Key = queryID, Value = HashMap of (entityID, score)
    private HashMap<String,LinkedHashMap<String,Double>> entityRankings;
    // HashMap where Key = queryID, Value = HashMap of (paragraphID, score)
    private HashMap<String,LinkedHashMap<String,Double>> paraRankings;
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
    public Baseline1(String INDEX_DIR, String TREC_CAR_DIR, String OUTPUT_DIR, String DATA_DIR, String PARA_RUN_FILE, String ENTITY_RUN_FILE, String OUT_FILE) throws IOException {
        this.INDEX_DIR = INDEX_DIR;
        this.TREC_CAR_DIR = TREC_CAR_DIR;
        this.OUTPUT_DIR = OUTPUT_DIR;
        this.DATA_DIR = DATA_DIR;
        this.PARA_RUN_FILE = PARA_RUN_FILE;
        this.ENTITY_RUN_FILE = ENTITY_RUN_FILE;
        this.OUT_FILE = OUT_FILE;
        this.runStrings = new ArrayList<>();
        searcher = new Index.Setup(this.INDEX_DIR).getSearcher();

        System.out.println("Getting entity rankings from run file...");
        entityRankings = getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.ENTITY_RUN_FILE);
        System.out.println("Done");

        System.out.println("Getting paragraph rankings from run file...");
        paraRankings = getRankings(this.TREC_CAR_DIR + "/" + this.DATA_DIR + "/" + this.PARA_RUN_FILE);
        System.out.println("Done");

    }
    /**
     * Method to calculate the first feature.
     * Works in parallel using Java 8 parallelStreams.
     * DEFAULT THREAD POOL SIE = NUMBER OF PROCESSORS
     * USE : System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "N") to set the thread pool size
     */

    private  void baseline() {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();
        ProgressBar pb = new ProgressBar("Progress", querySet.size());

        // Do in parallel
        //querySet.parallelStream().forEach(this::doTask);

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

    /**
     * Get the rankings from the run file.
     * @param path String Path to the entity or paragraph run file
     * @return HashMap where Key = queryID and Value = HashMap where Key = entity/paragraph, Value = score
     */
    private HashMap<String,LinkedHashMap<String,Double>> getRankings(String path)
    {
        HashMap<String,LinkedHashMap<String,Double>> map = new HashMap<> ();
        BufferedReader br;
        String line, queryID, field2;
        double score;

        try {
            br = new BufferedReader(new FileReader(path));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                queryID = fields[0];
                field2 = fields[2];
                score = Double.parseDouble(fields[4]);
                LinkedHashMap<String,Double> scoreMap = new LinkedHashMap<>();
                if(map.containsKey(queryID))
                    scoreMap = map.get(queryID);
                scoreMap.put(field2, score);
                map.put(queryID, scoreMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * Helper method.
     * @param queryId String
     */
    private void doTask(String queryId) {
        // Get the list of entities relevant for the query
        LinkedHashSet<String> entitySet = new LinkedHashSet<>(entityRankings.get(queryId).keySet());

        LinkedHashMap<String, Double> scoreMap = new LinkedHashMap<>();
        String query = null;

        // For every entity relevant to the query do
       for (String entityId : entitySet) {
           // Get the first paragraph in the paragraph ranking containing the entity
           String paraId = null;
           query = queryId + "+" +entityId;
           try {
               paraId = getParaForEntity(queryId, entityId);
           } catch (IOException | ParseException e) {
               e.printStackTrace();
           }

           // If such a paragraph is found then

           if (paraId != null) {
               // Get the score of the paragraph
               // The score can be either from the entity ranking or the paragraph ranking
               double score = getScore(queryId, entityId, "entity");
               if (score != -1) {
                   scoreMap.put(paraId, score);
               } else {
                   System.out.println("No info found about:" + paraId);
               }
           }
       }
       makeRunStrings(query, scoreMap);
       //System.out.println("Done: " + queryId);

    }
    private void makeRunStrings(String query, LinkedHashMap<String, Double> scoreMap) {
        LinkedHashMap<String, Double> map = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 0;

        for (String paraId : map.keySet()) {
            double score = map.get(paraId);
            if (score > 0) {
                runFileString = query + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "filtering";
                runStrings.add(runFileString);
                rank++;
            }

        }
    }

    /**
     * Get the first paragraph in the raking containing this entity.
     * @param queryID String QueryID
     * @param entityID String Entity
     * @return String ParaID of the first paragraph in the ranking which contains this entity
     * @throws IOException
     * @throws ParseException
     */
    private String getParaForEntity(String queryID, String entityID) throws IOException, ParseException {
        //System.out.println("Query:" + queryID);
        //System.out.println("Entity:" + entityID);
        //BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        LinkedHashSet<Map.Entry<String, Double>> paraSet = new LinkedHashSet<>(paraRankings.get(queryID).entrySet());
        String pid = null;
        for(Map.Entry<String, Double> stringDoubleEntry : paraSet) {
            String pID = stringDoubleEntry.getKey();
            double score = stringDoubleEntry.getValue();
            //System.out.println(pID + " " + score);
            Document doc = Index.Search.searchIndex("id", pID, searcher);
            ArrayList<String> pEntList = Utilities.getEntities(doc);
            if(pEntList.contains(Utilities.process(entityID))) {
                pid = pID;
                //System.out.println("ParaID:" + pID);
                //String text = doc.getField("text").stringValue();
                //System.out.println(text);
                //System.out.println();
                break;
            }
            //br.readLine();
        }
        return pid;
    }

    /**
     * Get the score corresponding to this entity/paragraph.
     * @param query String Query
     * @param str String entity/paragraph
     * @param about String the HashMap to use to get the score
     * @return double Score
     */
    private double getScore(String query, String str, @NotNull String about) {
        double score = -1;
        switch(about) {
            case "entity":
                if(entityRankings.containsKey(query)) {
                    LinkedHashMap<String, Double> infoMap = entityRankings.get(query);
                    if(infoMap.containsKey(str))
                        score = infoMap.get(str);
                }
                break;
            case "paragraph":
                if(paraRankings.containsKey(query)) {
                    LinkedHashMap<String, Double> infoMap = paraRankings.get(query);
                    if(infoMap.containsKey(str))
                        score = infoMap.get(str);
                }
                break;
        }
        return score;
    }

    /**
     * Main method to run the code.
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

        try {
            new Baseline1(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).baseline();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
