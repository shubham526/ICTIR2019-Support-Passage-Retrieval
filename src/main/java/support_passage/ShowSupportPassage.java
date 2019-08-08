package support_passage;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ShowSupportPassage {
    private String RUN_FILE;
    private IndexSearcher searcher;
    private String INDEX_DIR;
    private HashMap<String, ArrayList<String>> rankings;

    public ShowSupportPassage(String RUN_FILE, String INDEX_DIR) {
        this.RUN_FILE = RUN_FILE;
        this.INDEX_DIR = INDEX_DIR;
        System.out.println("Setting up index for use...");
        this.searcher = new Index.Setup(this.INDEX_DIR).getSearcher();
        System.out.println("Done");
        this.rankings = Utilities.getRankings(RUN_FILE);
    }
    public void show() throws IOException, ParseException {
        Set<String> querySet = rankings.keySet();
        for (String query : querySet) {
            doTask(query);
        }
    }
    private void doTask(String query) throws IOException, ParseException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String queryID = query.split("\\+")[0];
        String entityID = query.split("\\+")[1];
        ArrayList<String> paraList = rankings.get(query);
        for (int i = 0; i < 10; i++) {
            String paraID = paraList.get(i);
            Document d = Index.Search.searchIndex("id", paraID, searcher);
            String text = d.get("text");
            System.out.println("QueryID: " + queryID);
            System.out.println("EntityID: " + entityID);
            System.out.println(text);
            System.out.println("=====================================================================================");
            br.readLine();
        }
    }

    public static void main(String[] args) throws IOException, ParseException {
        String runFile = args[0];
        String indexDir = args[1];
        new ShowSupportPassage(runFile, indexDir).show();
    }

}
