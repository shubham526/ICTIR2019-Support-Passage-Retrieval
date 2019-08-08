package EntitySalience;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import support_passage.PseudoDocument;
import support_passage.Utilities;

import java.io.IOException;
import java.util.*;

/**
 * Analyze why salience does not work.
 * This class reads in the input entity run and the candidate passage run.
 * It makes to HashMaps, one for entities with salient passages and other for entities with no salient passages.
 * It then writes the to HashMaps to disk.
 * @author Shubham Chatterjee
 * @version 7/16/2019
 */

public class SalienceAnalysis {
    private IndexSearcher searcher;
    // HashMap where Key = query and Value = List of entities retrieved for the query
    private HashMap<String, ArrayList<String>> entityRankings;

    // HashMap where Key = query and Value = List of passages retrieved for the query
    private HashMap<String, ArrayList<String>> passageRankings;

    // HashMap where Key = paraID and Value = Map of (entity, salience_score)
    private HashMap<String, Map<String, Double>> salientEntityMap;

    private HashMap<String, Set<String>> entWithSalPsgMap = new HashMap<>();
    private HashMap<String, Set<String>> entWithNoSalPsgMap = new HashMap<>();


    /**
     * Constructor.
     * @param entityFile String Path to the entity run file.
     * @param passageFile String Path to the passage run file.
     * @param swatFile String Path to the SWAT serialized file.
     */
    public SalienceAnalysis(String entityFile,  String passageFile, String swatFile, String entityQrelFile, String indexDir, String outDir) {

        System.out.print("Setting up index for use...");
        searcher = new Index.Setup(indexDir).getSearcher();
        System.out.println("[Done].");

        System.out.print("Reading entity rankings...");
        this.entityRankings = Utilities.getRankings(entityFile);
        System.out.println("[Done].");

        System.out.print("Reading passage rankings...");
        this.passageRankings = Utilities.getRankings(passageFile);
        System.out.println("[Done]");

        System.out.print("Reading the SWAT annotations...");
        try {
            this.salientEntityMap = Utilities.readMap(swatFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");

        analyze(outDir);
    }

    /**
     * Method to make the analysis.
     */
    private void analyze(String outDir) {
        //Get the set of queries
        Set<String> querySet = entityRankings.keySet();

        // Do in parallel
        querySet.parallelStream().forEach(this::doTask);
        System.out.print("Writing to disk....");
        try {
            Utilities.writeMap(entWithSalPsgMap, outDir + "/entity-with-sal-psg-map.ser");
            Utilities.writeMap(entWithNoSalPsgMap, outDir + "/entity-with-no-sal-psg-map.ser");
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");
        System.out.println("Files written to directory: " + outDir);



    }

    /**
     * Method to do the actual work.
     * @param queryID String The query ID
     */

    private void doTask(String queryID) {

        Set<String> entWithSalPsg = new HashSet<>();
        Set<String> entWithNoSalPsg = new HashSet<>();
        // Get the list of entities retrieved for the query
        ArrayList<String> entityList = entityRankings.get(queryID);
        ArrayList<String> processedEntityList = Utilities.process(entityList);

        // Get the list of paragraphs retrieved for the query
        ArrayList<String> paraList = passageRankings.get(queryID);

        // For every entity in this list of retrieved entities do
        for (String entityId : processedEntityList) {

            // Create a pseudo-document for the entity
            PseudoDocument d = Utilities.createPseudoDocument(entityId, paraList, searcher);

            if (d != null) {
                // If the PseudoDocument is not null (that is, contains at least one document) then
                // Get the list of documents in the pseudo-document about the entity
                ArrayList<Document> documentList = d.getDocumentList();
                for (Document document : documentList) {
                    // For every such pseudo-document
                    // Get the ID of the paragraph
                    String paraID = document.getField("id").stringValue();
                    // If there is an entry for this paragraph in the saliency map
                    if (salientEntityMap.containsKey(paraID)) {
                        // Get the set of entities salient in the paragraph
                        if (salientEntityMap.get(paraID) != null) {
                            Set<String> salEnt = salientEntityMap.get(paraID).keySet();
                            if (salEnt.contains(entityId)) {
                                // If the set of salient entities contains the entity then
                                // It means the entity has a passage in the candidate set
                                // And the entity is salient in the passage
                                // Add the entity to the list of entities with a passage in the candidate set
                                // and salient in the passage
                                entWithSalPsg.add(entityId);
                                // If we already found a passage for the entity in which it is salient, then we may stop
                                // so break out of this loop
                                break;
                            }
                        }

                    }
                }
                // If we reach here (outside the for loop) and the "entityID" is not present in entWithSalPsg
                // Then it means that we iterated over all the documents mentioning the entity
                // But did not find one where the entity was salient
                // So add this entity to the list entWithNoSalPsg
                if (! entWithSalPsg.contains(entityId)) {
                    entWithNoSalPsg.add(entityId);
                }
            } else {
                // Otherwise if the pseudo-document is null (that is, no document about the entity exists) then
                // This means this entity has no passage in the candidate set
                // Add it to the list of entities with no passages in the candidate set
                entWithNoSalPsg.add(entityId);
            }

        }
        entWithSalPsgMap.put(queryID, entWithSalPsg);
        entWithNoSalPsgMap.put(queryID, entWithNoSalPsg);
        System.out.println("Done: " + queryID);
    }

    public static void main(String[] args) {
        String entityFile = args[0];
        String passageFile = args[1];
        String swatFile = args[2];
        String entityQrelFile = args[3];
        String indexDir = args[4];
        String outDir = args[5];

        new SalienceAnalysis(entityFile, passageFile, swatFile, entityQrelFile, indexDir, outDir);
    }
}
