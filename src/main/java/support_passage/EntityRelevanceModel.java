package support_passage;


import static java.util.stream.Collectors.toCollection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

//import me.tongfei.progressbar.ProgressBar;

import org.apache.lucene.search.BooleanClause;

/**
 * Class to expand queries using retrieved entities and obtain a passage ranking
 * @author shubham
 *
 */
public class EntityRelevanceModel
{
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private String INDEX_DIR;
    private String OUT_DIR;
    private String OUT_FILE;
    private String RUN_FILE_PATH;
    private int NUM_RESULTS;
    private String RET_MODEL;
    private String ANALYZER_TYPE;
    private HashMap<String,HashMap<String,String>> entityRankings = new HashMap<String,HashMap<String,String>>();

    public EntityRelevanceModel(String id, String od, String of, String rfp,int n,String rm, String at) throws IOException
    {
        INDEX_DIR = id;
        OUT_DIR = od;
        OUT_FILE = of;
        RUN_FILE_PATH = rfp;
        NUM_RESULTS = n;
        RET_MODEL = rm;
        ANALYZER_TYPE = at;
        Similarity s = RET_MODEL.equalsIgnoreCase("LM-DS") ? new LMDirichletSimilarity(): new BM25Similarity();

        System.out.println("Creating searcher with"+" "+RET_MODEL+" "+"similiarity");
        searcher = createSearcher(s);
        System.out.println("Done");

        analyzer = ANALYZER_TYPE.equalsIgnoreCase("english") ? new EnglishAnalyzer() : new StandardAnalyzer();
        System.out.println("Using"+" "+ANALYZER_TYPE+" "+"analyzer");

        System.out.println("Getting entity rankings from run file...");
        entityRankings = getRankings(RUN_FILE_PATH);
        System.out.println("Done");

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "20");
        System.out.println("ForkJoin pool size=20");

        expandQuery();
    }
    private HashMap<String,HashMap<String,String>> getRankings(String path)
    {
        HashMap<String,HashMap<String,String>> map = new HashMap<String,HashMap<String,String>> ();
        BufferedReader br = null;
        String line = "",queryID = "",field2 = "", info = "";

        try
        {
            br = new BufferedReader(new FileReader(path));
            while((line = br.readLine()) != null)
            {
                String[] fields = line.split(" ");
                queryID = fields[0];
                field2 = fields[2];
                info = fields[3] + " " + fields[4] + " " + fields[5];
                HashMap<String,String> scoreMap = new HashMap<String,String>();
                if(map.containsKey(queryID))
                    scoreMap = map.get(queryID);
                scoreMap.put(field2, info);
                map.put(queryID, scoreMap);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return map;
    }
    private void expandQuery() throws IOException
    {
        Set<String> querySet = entityRankings.keySet();
        String outFilePath = OUT_DIR+"/"+OUT_FILE;
        ArrayList<String> runStrings = new ArrayList<String>();

        querySet
                .parallelStream()
                .forEach(queryID ->
                {
                    try
                    {
                        doTask(queryID,runStrings);
                        System.out.println("Done:"+queryID);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
        System.out.println("Writing to file:"+outFilePath);
        if(runStrings.size() == 0)
            System.out.println("empty");
        else
            writeFile(runStrings,outFilePath);
        System.out.println("Done");

		/*ProgressBar pb = new ProgressBar("Progress", querySet.size());

		for(String queryStr : querySet)
		{
			doTask(queryStr);
			pb.step();
		}
		pb.close();*/
    }
    private  void doTask(String queryID,ArrayList<String> runStrings) throws IOException
    {
        Set<String> entityList = entityRankings.get(queryID).keySet().stream().limit(100).collect(toCollection(LinkedHashSet::new));
        for(String entityID: entityList)
        {
            BooleanQuery booleanQuery = toEntityRmQuery(process(queryID),process(entityID));
            String paraID = getTopDoc(booleanQuery);
            String info = getInfo(queryID,entityID);
            String rank = info.split(" ")[0];
            String score = info.split(" ")[1];
            String runFileString = paraID != null ? queryID+" Q0 "+paraID+"/"+entityID+" "+rank+" "+score+" "+RET_MODEL+"-"+ANALYZER_TYPE+"-EntityRM" : queryID+" Q0 "+entityID+" "+rank+" "+score+" "+RET_MODEL+"-"+ANALYZER_TYPE+"-EntityRM";
            runStrings.add(runFileString);
        }
    }
    private String getInfo(String query, String entity)
    {
        String info = "";
        if(entityRankings.containsKey(query))
        {
            HashMap<String, String> infoMap = entityRankings.get(query);
            if(infoMap.containsKey(entity))
                info = infoMap.get(entity);
        }
        return info;
    }
    private String getTopDoc(BooleanQuery query) throws IOException
    {
        TopDocs tops = searcher.search(query, NUM_RESULTS);
        ScoreDoc[] scoreDoc = tops.scoreDocs;
        if(scoreDoc.length == 0)
            return null;
        Document d = searcher.doc(scoreDoc[0].doc);
        String paraID = d.getField("Id").stringValue();
        return paraID;
    }
    private void tokenizeQuery(String queryStr, String searchField, List<String> tokens) throws IOException
    {
        TokenStream tokenStream = analyzer.tokenStream(searchField, new StringReader(queryStr));
        tokenStream.reset();
        tokens.clear();
        while (tokenStream.incrementToken() && tokens.size() < 64)
        {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            tokens.add(token);
        }
        tokenStream.end();
        tokenStream.close();
    }
    private BooleanQuery toEntityRmQuery(String queryStr, String entity) throws IOException
    {
        List<String> queryToks = new ArrayList<>();
        tokenizeQuery(queryStr, "Text", queryToks);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        for (String token : queryToks)
            booleanQuery.add(new BoostQuery(new TermQuery(new Term("Text", token)),1.0f), BooleanClause.Occur.SHOULD);

        List<String> entityToks = new ArrayList<>();
        tokenizeQuery(entity, "EntityLinks", entityToks);
        for(String e: entityToks)
            booleanQuery.add(new BoostQuery(new TermQuery(new Term("EntityLinks", e)),1.0f), BooleanClause.Occur.MUST);

        return booleanQuery.build();
    }
    private void writeFile(ArrayList<String> runStrings, String filePath)
    {
        BufferedWriter out = null;
        try
        {
            out = new BufferedWriter(new FileWriter(filePath,true));

            for(String s : runStrings)
            {
                if(s!=null) {
                    out.write(s);
                    out.newLine();}
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(out != null)
                {
                    out.close();
                }
                else
                {
                    System.out.println("Output Buffer has not been initialized!");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    private HashMap<String,ArrayList<String>> getRunFileMap(String path)
    {
        HashMap<String,ArrayList<String>> map = new HashMap<String,ArrayList<String>>();

        BufferedReader in = null;
        String line = "";
        ArrayList<String> list;
        try
        {
            in = new BufferedReader(new FileReader(path));
            while((line = in.readLine()) != null)
            {
                String query = line.split(" ")[0];
                String entity = line.split(" ")[2];
                list = new ArrayList<String>();
                if(map.containsKey(query))
                    list = map.get(query);
                list.add(entity);
                map.put(query, list);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(in != null)
                {
                    in.close();
                }
                else
                {
                    System.out.println("Input Buffer has not been initialized!");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return map;
    }
    private String process(String str)
    {
        return str.substring(str.indexOf(":")+1).replaceAll("%20", " ");
    }
    private IndexSearcher createSearcher(Similarity s)throws IOException
    {
        Directory dir = FSDirectory.open((new File(INDEX_DIR).toPath()));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(s);
        return searcher;
    }
    public static void main(String[] args) throws IOException
    {
        String index_dir = args[0];
        String out_dir = args[1];
        String out_file = args[2];
        String run_file_path = args[3];
        int numResults = Integer.parseInt(args[4]);
        String retMod = args[5];
        String ana = args[6];
        new EntityRelevanceModel(index_dir,out_dir,out_file,run_file_path,numResults,retMod,ana);

    }
}
