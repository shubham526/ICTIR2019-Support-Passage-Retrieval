package others;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import support_passage.Utilities;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class ShowSupportPassage {
    private IndexSearcher searcher;
    private String runFile;
    private String outFile;
    private HashMap<String, HashMap<String, ArrayList<String>>> rankings = new HashMap<>();

     public ShowSupportPassage(String indexDir, String runFile, String outFile) {
         this.runFile = runFile;
         this.outFile = outFile;

         System.out.print("Setting up Index for use....");
         this.searcher = new Index.Setup(indexDir).getSearcher();
         System.out.println("Done.");

         System.out.print("Getting rankings from run file...");
         getRunFileMap(this.rankings);
         System.out.println("Done.");
         show();
     }
    private void getRunFileMap(HashMap<String, HashMap<String, ArrayList<String>>> queryMap) {
        BufferedReader in = null;
        String line;
        HashMap<String, ArrayList<String>> entityMap;
        ArrayList<String> paraList;

        try {
            in = new BufferedReader(new FileReader(runFile));
            while((line = in.readLine()) != null) {
                String[] fields = line.split(" ");
                String queryID = fields[0].split("\\+")[0];
                String entityID = fields[0].split("\\+")[1];
                String paraID = fields[2];

                if (queryMap.containsKey(queryID)) {
                    entityMap = queryMap.get(queryID);
                } else {
                    entityMap = new HashMap<>();
                }
                if (entityMap.containsKey(entityID)) {
                    paraList = entityMap.get(entityID);
                } else {
                    paraList = new ArrayList<>();
                }
                paraList.add(paraID);
                entityMap.put(entityID,paraList);
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
    }
    private void show() {
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         ArrayList<String> queryList = new ArrayList<>(rankings.keySet());
         String myString = "";
         ArrayList<String> myList = new ArrayList<>();
         for (int i = 0; i < 5; i++) {
             String queryID = queryList.get(i);
             System.out.println("Query: " + queryID);
             HashMap<String, ArrayList<String>> entityMap = rankings.get(queryID);
             ArrayList<String> entityList = new ArrayList<>(entityMap.keySet());
             int m = entityList.size() > 5 ? 5 : entityList.size();
             for (int j = 0; j < m; j++) {
                 String entityID = entityList.get(j);
                 System.out.println("Entity: " + entityID);
                 ArrayList<String> paraList = entityMap.get(entityID);
                 int n = paraList.size() > 3 ? 3 : paraList.size();
                 for (int k = 0; k < n; k++) {
                     String paraID = paraList.get(k);
                     Document d = null;
                     try {
                         d = Index.Search.searchIndex("id", paraID, searcher);
                     } catch (IOException | ParseException e) {
                         e.printStackTrace();
                     }
                     String text = d != null ? d.getField("text").stringValue() : null;
                     System.out.println("Support Passage: ");
                     System.out.println(text);
//                     try {
//                         br.readLine();
//                     } catch (IOException e) {
//                         e.printStackTrace();
//                     }
                     myString =
                             "Query: " + queryID
                             + "\n"
                             + "Entity: " + entityID
                             + "\n"
                             + "Support Passage:"
                             + "\n"
                             + text
                             + "\n====================================================================================\n";
                     myList.add(myString);
                 }
             }
         }
         System.out.print("Writing to file....");
         Utilities.writeFile(myList,outFile);
         System.out.println("Done");
    }

    public static void main(String[] args) {
        String indexDir = args[0];
        String runFile = args[1];
        String outFile = args[2];
        new ShowSupportPassage(indexDir, runFile, outFile);
    }

}
