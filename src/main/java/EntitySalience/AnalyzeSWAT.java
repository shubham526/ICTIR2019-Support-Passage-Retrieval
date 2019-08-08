package EntitySalience;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.json.JSONException;
import org.json.JSONObject;
import support_passage.Utilities;

import java.io.IOException;
import java.util.*;

/**
 * Class to analyze the SWAT annotations by writing the annotations and actual entity ids to a file.
 */
public class AnalyzeSWAT {
    private String outFile;
    private IndexSearcher searcher;
    //private HashMap<String, Map<String, Double>> swatFileMap;
    private HashMap<String, List<String>> swatFileMap;
    private HashMap<String, List<String>> linksFileMap;

    AnalyzeSWAT(String annotationFile, String entityLinksFile, String outFile, String index) throws IOException, ClassNotFoundException {
        this.outFile = outFile;
        this.swatFileMap = Utilities.readMap(annotationFile);
        this.linksFileMap = Utilities.readMap(entityLinksFile);
        searcher = new Index.Setup(index).getSearcher();
        try {
            makeFile();
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }
    }
    private void makeFile() throws JSONException, IOException, ParseException {
        //List<Map.Entry<String, Map<String, Double>>> entryList = new LinkedList<>(swatFileMap.entrySet());
        List<Map.Entry<String, List<String>>> entryList = new LinkedList<>(swatFileMap.entrySet());
        List<String> swatEntityList;
        List<String> paraEntityList;
        ArrayList<String> list = new ArrayList<>();
        StringBuilder str;
        for (int i = 0; i < 100; i++) {
            str = new StringBuilder();
            swatEntityList = new ArrayList<>();
            paraEntityList = new ArrayList<>();
            Map.Entry<String, List<String>> entry = entryList.get(i);
            String paraID = entry.getKey();
            Document doc = Index.Search.searchIndex("id", paraID, searcher);
            assert doc != null;
            String text = doc.getField("text").stringValue();
            System.out.println(paraID);
            List<String> ent = swatFileMap.get(paraID);
            if (ent != null) {
                for (String jsonString : ent) {
                    JSONObject jsonObject = new JSONObject(jsonString);
                    String s = jsonObject.getDouble("salience_class") == 1.0
                            ? jsonObject.getString("wiki_title")
                                + " X "
                                + jsonObject.getDouble("salience_score")
                            : jsonObject.getString("wiki_title");
                    swatEntityList.add(s);
                }
            }
            List<String> pent = linksFileMap.get(paraID);
            if (pent != null) {
                paraEntityList.addAll(pent);
            }
            //paraEntityList.addAll(linksFileMap.get(paraID));
            str.append("Paragraph: ").append(i + 1).append("\n");
            str.append(paraID).append("\n");
            str.append(text).append("\n");
            str.append("------------------------------------------------------------------------").append("\n");
            str.append("SWAT Entities:").append("\n");
            for (String s : swatEntityList) {
                if (s != null) {
                    System.out.println(s);
                    str.append(s).append("\n");
                }
            }
            str.append("------------------------------------------------------------------------").append("\n");
            str.append("Paragraph Entities:").append("\n");
            for (String s : paraEntityList) {
                if (s != null) {
                    s = Utilities.process(s);
                    System.out.println(s);
                    str.append(s).append("\n");
                }
            }
            list.add(str.toString());
        }
        Utilities.writeFile(list, outFile);
        System.out.println("File written: " + outFile);
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String swatFile = args[0];
        String linksFile = args[1];
        String outFile = args[2];
        String index = args[3];
        new AnalyzeSWAT(swatFile,linksFile,outFile, index);
    }


}
