package EntitySalience;

import lucene.Index;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import support_passage.PseudoDocument;
import support_passage.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Class to find how many entities are salient for a passage using SWAT.
 * @author Shubham Chatterjee
 * @version 5/13/2019
 */

public class SalienceStat {
    private IndexSearcher searcher;
    private String indexDir;

    // HashMap where Key = query and Value = List of entities retrieved for the query
    private HashMap<String, ArrayList<String>> entityRankings;

    // HashMap where Key = query and Value = List of passages retrieved for the query
    private HashMap<String, ArrayList<String>> passageRankings;

    // HashMap where Key = query and Value = number of entities marked salient by SWAT
    private HashMap<String,Integer> salienceMap;

    // Path to the entity run file
    private String entityRunFile;

    // Path to the passage run file
    private String passageRunFile;

    // Path to the file where results will be written
    private String resultFile;

    SalienceStat(String indexDir, String passageRunFile, String entityRunFile, String resultFile) {
        this.indexDir = indexDir;
        this.passageRunFile = passageRunFile;
        this.entityRunFile = entityRunFile;
        this.resultFile = resultFile;

        System.out.print("Setting up Index for use....");
        this.searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("Done.");

        System.out.print("Getting entity rankings...");
        this.entityRankings = Utilities.getRankings(entityRunFile);
        System.out.println("Done.");

        System.out.print("Getting passage rankings...");
        this.passageRankings = Utilities.getRankings(passageRunFile);
        System.out.println("Done");

        this.salienceMap = new HashMap<>();
    }

    public void getStat1() {
        ArrayList<String> querySet = new ArrayList<>(entityRankings.keySet());
        ArrayList<String> fileStrings = new ArrayList<>();
        String res;
        for (int i = 0; i < 3; i++) {
            String queryID = querySet.get(i);
            System.out.println(queryID);

            ArrayList<String> entityList = entityRankings.get(queryID);
            ArrayList<String> paraList = passageRankings.get(queryID);

            for (int j = 0; j < 5; j++) {
                String entityID = entityList.get(j);
                System.out.println(entityID);
                String processedEntityID = Utilities.process(entityID);
                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityID, paraList, searcher);

                if (d != null) {
                    ArrayList<Document> pseudoDocPsgList = d.getDocumentList();
                    System.out.println("Found: "  + pseudoDocPsgList.size() + "passages about entity.");
                    for (int k = 0; k < 5; k++) {
                        if (pseudoDocPsgList.size() > k) {
                            Document document = pseudoDocPsgList.get(k);
                            String text = document.get("text");
                            Map<String, Double> saliencyMap = Saliency.getSalientEntities(text);
                            if (saliencyMap == null) {
                                //System.out.println("text:" + text);
                                continue;
                            }
                            if (saliencyMap.containsKey(processedEntityID)) {
                                res = "Yes";
                            } else {
                                res = "No";
                            }
                            System.out.println(res);
                            String str =
                                    "QueryID: " + queryID
                                            + "\n"
                                            + "EntityID: " + entityID
                                            + "\n"
                                            + "Text:"
                                            + "\n"
                                            + text
                                            + "\n"
                                            + "SWAT result:" + res
                                            + "===================================================================\n";
                            fileStrings.add(str);
                        }
                    }
                }

            }
            System.out.println(queryID);

        }
        System.out.print("Writing results to file....");
        Utilities.writeFile(fileStrings, resultFile);
        System.out.println("Done.");
        System.out.println("Written results to: " + resultFile);
    }
    public void getStat2() {
        ArrayList<String> querySet = new ArrayList<>(entityRankings.keySet());
        ArrayList<String> fileStrings = new ArrayList<>();
        String res;
        for (int i = 0; i < 5; i++) {
            String queryID = querySet.get(i);
            System.out.println(queryID);

            ArrayList<String> entityList = entityRankings.get(queryID);
            ArrayList<String> paraList = passageRankings.get(queryID);

            for (int j = 0; j < 5; j++) {
                String entityID = entityList.get(j);
                System.out.println(entityID);
                String processedEntityID = Utilities.process(entityID);
                // Create a pseudo-document for the entity
                PseudoDocument d = Utilities.createPseudoDocument(entityID, paraList, searcher);

                if (d != null) {
                    ArrayList<Document> pseudoDocPsgList = d.getDocumentList();
                    System.out.println("Found: "  + pseudoDocPsgList.size() + "passages about entity.");
                    for (int k = 0; k < 5; k++) {
                        if (pseudoDocPsgList.size() > k) {
                            Document document = pseudoDocPsgList.get(k);
                            String text = document.get("text");
                            Map<String, Double> saliencyMap = Saliency.getSalientEntities(text);
                            if (saliencyMap == null) {
                                //System.out.println("text:" + text);
                                continue;
                            }
                            if (saliencyMap.containsKey(processedEntityID)) {
                                res = "Yes";
                            } else {
                                res = "No";
                            }
                            System.out.println(res);
                            String str =
                                    "QueryID: " + queryID
                                            + "\n"
                                            + "EntityID: " + entityID
                                            + "\n"
                                            + "Text:"
                                            + "\n"
                                            + text
                                            + "\n"
                                            + "SWAT result:" + res
                                            + "===================================================================\n";
                            fileStrings.add(str);
                        }
                    }
                }

            }
            System.out.println(queryID);

        }
    }

    public static void main(String[] args) {
        String indexDir = args[0];
        String psgFile = args[1];
        String entFile = args[2];
        String resFile = args[3];
        new SalienceStat(indexDir,psgFile,entFile,resFile).getStat1();
    }

}
