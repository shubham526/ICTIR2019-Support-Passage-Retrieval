package others;

import lucene.Index;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import support_passage.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReadSerializedFile {
    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException {
        String file = "/home/shubham/Desktop/research/TREC_CAR/data/train.pages.cbor-paragraphs.cbor.entity-links.ser";
        String index = "/home/shubham/Desktop/research/TREC_CAR/data/paragraph.entity.lucene";
        IndexSearcher searcher = new Index.Setup(index).getSearcher();
        HashMap<String, List<String>> fileMap = Utilities.readMap(file);
        for (String pid : fileMap.keySet()) {
            System.out.println(pid);
            System.out.println(fileMap.get(pid));
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++");
            Document doc = Index.Search.searchIndex("id", pid, searcher);
            assert doc != null;
            ArrayList<String> pEntList = Utilities.getEntities(doc);
            System.out.println(pEntList);
            System.out.println("=============================================");
        }
    }
}
