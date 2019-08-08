package others;

import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Modify the support passage run for only entities with/without salient passage.
 * @author Shubham Chatterjee
 * @version 7/19/2019
 */
public class Modify {
    private HashMap<String, Set<String>> entWithSalPsgMap;
    private HashMap<String, ArrayList<String>> qrelMap;

    /**
     * Constructor.
     * @param runFile String The file to modify. This can be either a run file or a qrel file.
     * @param outFile String The modified file.
     * @param type String The type of modification. Whether based on salience or relevance of entity.
     * @param typeFile String The file corresponding to type. If type is salience,
     *                 then this file is the entWithSalPsgFile
     *                 else this file is the qrel file.
     */

    Modify(String runFile, String outFile, String type, String typeFile) {

        System.out.println("Modifying file based on entity " + type);

        if (type.equalsIgnoreCase("salience")) {

            System.out.print("Reading entities with salient passage file...");
            try {
                this.entWithSalPsgMap = Utilities.readMap(typeFile);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("[Done].");
        } else {
            System.out.print("Reading entity qrels...");
            this.qrelMap = Utilities.getRankings(typeFile);
            System.out.println("[Done].");
        }

        System.out.print("Modifying run file.....");
        modify(runFile, outFile, type);

    }

    private void modify(String runFile, String outFile, String type) {
        BufferedReader br;
        String line , query, queryID ,entityID;
        ArrayList<String> runStrings = new ArrayList<>();

        try {
            br = new BufferedReader(new FileReader(runFile));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                query = fields[0];
                queryID = query.split("\\+")[0];
                entityID = query.split("\\+")[1];
                ArrayList<String> entityList;

                if (type.equalsIgnoreCase("salience")) {
                    entityList = new ArrayList<>(entWithSalPsgMap.get(queryID));
                } else {
                    entityList = Utilities.process(qrelMap.get(queryID));
                }
                if (entityList.contains(Utilities.process(entityID))) {
                    runStrings.add(line);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.print("Writing modified run to disk.....");
        Utilities.writeFile(runStrings, outFile);
        System.out.println("[Done].");
        System.out.println("Modified run written to: " + outFile);
    }

    public static void main(String[] args) {
        String runFile = args[0];
        String outFile = args[1];
        String type = args[2];
        if (type.equalsIgnoreCase("salience") || type.equalsIgnoreCase("relevance")) {
            String typeFile = args[3];
            new Modify(runFile, outFile, type, typeFile);
        } else {
            System.out.println("Wrong type. Type can be either: salience or entity. Program exits.");
        }

    }

}
