package EntitySalience;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import support_passage.PseudoDocument;
import support_passage.Utilities;

import java.io.IOException;
import java.util.*;

/**
 * Experiment-1: Filter all entities with salient passage. Calculate MAP.
 * Compare with another result on same query-entity set.
 * @author Shubham Chatterjee
 * @version 7/18/2019
 */
public class Experiment1 {
    private IndexSearcher searcher;
    private HashMap<String, Set<String>> entWithSalPsgMap = new HashMap<>();
    private HashMap<String, Map<String, Double>> salientEntityMap;
    private HashMap<String, ArrayList<String>> passageRankings;
    private HashMap<String, ArrayList<String>> entityRankings;


    Experiment1(String indexDir, String entWithSalPsgMapFile, String swatFile, String passageFile, String entityFile, String runFile) {

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("[Done].");

        System.out.print("Reading entities with salient passage file...");
        try {
            this.entWithSalPsgMap = Utilities.readMap(entWithSalPsgMapFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");

        System.out.print("Reading passage rankings...");
        this.passageRankings = Utilities.getRankings(passageFile);
        System.out.println("[Done]");

        System.out.print("Reading entity rankings...");
        this.entityRankings = Utilities.getRankings(entityFile);
        System.out.println("[Done].");

        System.out.print("Reading the SWAT annotations...");
        try {
            this.salientEntityMap = Utilities.readMap(swatFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");
        score(runFile);
    }

    private void getEntityToIDMap(ArrayList<String> entityList, HashMap<String, String> entityToIDMap) {
        for (String entityID : entityList) {
            String entity = Utilities.process(entityID);
            entityToIDMap.put(entity, entityID);
        }
    }

    private void score(String runFile) {
        ArrayList<String> runFileStrings = new ArrayList<>();
        Set<String> queryList = passageRankings.keySet();
        for (String queryID : queryList) {
            Map<String, Double> scoreMap = new HashMap<>();
            ArrayList<String> retEntityList = entityRankings.get(queryID);
            ArrayList<String> salEntityList = new ArrayList<>(entWithSalPsgMap.get(queryID));
            HashMap<String, String> entityToIDMap = new HashMap<>();
            getEntityToIDMap(retEntityList, entityToIDMap);
            ArrayList<String> paraList = passageRankings.get(queryID);
            for (String entity : salEntityList) {

                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entity, paraList, searcher);

                if (d != null) {
                    // If the PseudoDocument is not null (that is, contains at least one document) then
                    // Get the list of documents in the pseudo-document about the entity
                    ArrayList<Document> documentList = d.getDocumentList();
                    for (Document document : documentList) {
                        // For every such document
                        // Get the ID of the paragraph
                        String paraID = document.getField("id").stringValue();
                        Map<String, Double> salEntForParaMap = salientEntityMap.get(paraID);
                        if (salEntForParaMap != null && salEntForParaMap.containsKey(entity)) {
                            double score = salEntForParaMap.get(entity);
                            scoreMap.put(paraID, score);
                        }

                    }
                }
                makeRunFileString(queryID, entityToIDMap.get(entity), scoreMap, runFileStrings);
            }
            System.out.println("Done: " + queryID);
        }
        System.out.print("Writing to file....");
        Utilities.writeFile(runFileStrings, runFile);
        System.out.println("[Done].");

    }
    private void makeRunFileString(String queryID,
                                   String entityID,
                                   Map<String, Double> scoreMap,
                                   List<String> runStrings) {
        LinkedHashMap<String, Double> paraScore = Utilities.sortByValueDescending(scoreMap);
        String runFileString;
        int rank = 0;

        for (String paraId : paraScore.keySet()) {
            double score = paraScore.get(paraId);
            if (score > 0) {
                runFileString = queryID + "+" +entityID + " Q0 " + paraId + " " + rank
                        + " " + score + " " + "Salience";
                runStrings.add(runFileString);
                rank++;
            }

        }
    }

    public static void main(String[] args) {
        String indexDir = args[0];
        String entWithSalPsgMapFile = args[1];
        String swatFile = args[2];
        String passageRunFile = args[3];
        String entityRunFile = args[4];
        String runFile = args[5];
        new Experiment1(indexDir, entWithSalPsgMapFile, swatFile, passageRunFile, entityRunFile, runFile);
    }




}
