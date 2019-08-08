package lucene;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import me.tongfei.progressbar.ProgressBar;

/**
 * This class is used to index the documents into a Lucene index
 * @author Shubham Chatterjee
 *
 */

public class Index
{
    /**
     * Directory where index is stored
     */
    private  static String INDEX_DIR ;
    /**
     * Path to the paragraph corpus file
     */
    private  static String CBOR_FILE ;
    /**
     * Number of documents indexed
     */
    private static int COUNT;

    //private static String ENTITY_FILE;
    private  static IndexSearcher is = null;
    private  static QueryParser qp = null;
    private static ProgressBar pb;
    /**
     * Inner class to build a luecene index
     * @author Shubham Chatterjee
     *
     */
    public  final static class Build
    {
        /**
         * Builds a Lucene index of paragraphs in CBOR_FILE in the directory pointed to by INDEX_DIR
         * @param INDEX_DIR String Directory to store index
         * @param CBOR_FILE String Paragraph corpus file
         * @throws IOException
         */
        public Build(String INDEX_DIR,String CBOR_FILE) throws IOException
        {
            Index.INDEX_DIR = INDEX_DIR;
            Index.CBOR_FILE = CBOR_FILE;
            //Index.ENTITY_FILE = ENTITY_FILE;
            COUNT = 0;
            pb = new ProgressBar("Progress",29794697 );
        }
        /**
         * Create an IndexWriter object with the specified Analyzer
         * @param analyzer Analyzer Type of analyzer to use for building the index
         * @return IndexWriter An instance of the index writer to build the index
         * @throws IOException
         */
        private static IndexWriter createWriter(Analyzer analyzer)throws IOException
        {
            Directory indexdir = FSDirectory.open((new File(INDEX_DIR)).toPath());
            IndexWriterConfig conf = new IndexWriterConfig(analyzer);
            conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter iw = new IndexWriter(indexdir, conf);
            return iw;
        }
        /**
         * Create a document with the given entity, text and id
         * @param entity String
         * @param text String
         * @param id String
         * @return Document An document containing the given fields
         */
        private static Document createDocument(String entity, String text, String id)
        {

            COUNT++;
            Document doc = new Document();
            doc.add(new StringField("paraentity", entity, Field.Store.YES));
            doc.add(new TextField("parabody", text, Field.Store.YES));
            doc.add(new StringField("paraid", id, Field.Store.YES));
            System.out.println(id);
            return doc;
        }
        /**
         * Create a document of the specified paragraph with the text and id
         * @param para Data.Patagraph A paragraph object
         * @return Document A document containing the text and id as fields
         */
        private static Document createDocument(Data.Paragraph para)
        {
            COUNT++;
            Document paradoc = new Document();
            List<String> entity = para.getEntitiesOnly();
            String entityString = "";
			/*try {
				BufferedReader reader = new BufferedReader((new FileReader(ENTITY_FILE)));
				String s;
				while((s = reader.readLine()) != null){


				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}*/
            for(String s : entity)
            {
                s = s.replaceAll("\\s+", "_").toLowerCase();
                entityString += " "+s;
            }
            entity = null;
            paradoc.add(new StringField("paraid", para.getParaId(), Field.Store.YES));
            paradoc.add(new StringField("paraentity", entityString, Field.Store.YES));
            paradoc.add(new TextField("parabody", para.getTextOnly(), Field.Store.YES));
            System.out.println(para.getParaId());

            return paradoc;
        }
        /**
         * Create a Lucene index for each paragraph with text and id using the given analyzer
         * @param analyzer Analyzer Analyzer to use to create the index
         * @throws IOException
         */
        public static void createIndex(Analyzer analyzer)throws IOException
        {
            IndexWriter writer = createWriter(analyzer);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(CBOR_FILE)));
			
			/*for(Data.Paragraph paragraph : DeserializeData.iterableParagraphs(bis))
			{
				try 
				{
					writer.addDocument(createDocument(paragraph));
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}*/
            Iterable<Data.Paragraph> ip = DeserializeData.iterableParagraphs(bis);

            StreamSupport.stream(ip.spliterator(), true)
                    .forEach(paragraph ->
                    {
                        try {
                            writer.addDocument(createDocument(paragraph));
                            pb.step();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //System.out.println(paragraph.getParaId());
                        //pb.step();
                    });
		/*for(Data.Paragraph paragraph : DeserializeData.iterableParagraphs(bis))
		{
			indexParagraph(paragraph, writer);
			System.out.println(paragraph.getParaId());
		}*/
            writer.commit();
            writer.close();
            pb.close();
        }
        /**
         * Create a Lucene index for each entity in each paragraph using the given analyzer
         * @param analyzer  Analyzer Analyzer to use to create the index
         * @throws IOException
         */
        public static void createIndex2(Analyzer analyzer)throws IOException
        {
            IndexWriter writer = createWriter(analyzer);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(CBOR_FILE)));

            for(Data.Paragraph paragraph : DeserializeData.iterableParagraphs(bis))
            {
                String t = paragraph.getTextOnly();
                String id = paragraph.getParaId();
                for(Data.ParaBody body: paragraph.getBodies())
                {
                    if(body instanceof Data.ParaLink)
                    {
                        String s = ((Data.ParaLink) body).getPageId();
                        try
                        {
                            writer.addDocument(createDocument(s,t,id));
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
            writer.close();
        }
    }
    /**
     * Inner class to setup a Lucene index for search
     * Use this class for setting up the searcher in a specified way for searching the index
     * @author Shubham Chatterjee
     *
     */
    public final static class Setup
    {
        /**
         * Set up the searcher with default BM25 similarity and StandardAnalyzer to search in the INDEX_DIR
         * @param INDEX_DIR
         */
        public Setup(String INDEX_DIR)
        {
            Index.INDEX_DIR = INDEX_DIR;
            try
            {
                is = createSearcher();
                qp = createParser();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        /**
         * Set up the searcher to search a specific field in the index directory provided using the analyzer and similarity given
         * @param INDEX_DIR String Directory for index
         * @param field String Field to search within the index
         * @param analyzer Analyzer Analyzer to use to search the index
         * @param sim Similarity Similarity metric to use to score the documents
         */
        public Setup(String INDEX_DIR, String field, Analyzer analyzer, Similarity sim)
        {
            Index.INDEX_DIR = INDEX_DIR;
            try
            {
                is = createSearcher(sim);
                qp = createParser(field, analyzer);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        /**
         * Create a searcher which scores documents using the provided similarity metric
         * @param sim Similarity Similarity metric to use to score the documents
         * @return IndexSearcher Searcher to search the lucene index
         * @throws IOException
         */
        private IndexSearcher createSearcher(Similarity sim)throws IOException
        {
            Directory dir = FSDirectory.open((new File(INDEX_DIR).toPath()));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(sim);
            return searcher;
        }
        /**
         * Create a searcher which scores documents using the default BM25 similarity
         * @return IndexSearcher Searcher to search the lucene index
         * @throws IOException
         */
        private IndexSearcher createSearcher()throws IOException
        {
            Directory dir = FSDirectory.open((new File(INDEX_DIR).toPath()));
            IndexReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());
            return searcher;
        }
        /**
         * Create a query parser which parses queries using the standard analyzer and searcher in the parabody
         * @return QueryParser Parser to parse the query
         * @throws IOException
         */
        private QueryParser createParser()throws IOException
        {
            QueryParser parser = new QueryParser("parabody", new StandardAnalyzer());
            return parser;
        }
        /**
         * Create a query parser which parses queries using the analyzer provided and searches the given field
         * @param field String Field to search the query
         * @param analyzer Analyzer to use to parse the query
         * @return QueryParser Parser to parse the query
         * @throws IOException
         */
        private QueryParser createParser(String field, Analyzer analyzer)throws IOException
        {
            QueryParser parser = new QueryParser(field, analyzer);
            return parser;
        }
        /**
         * Get the IndexSearcher instance
         * @return IndexSearcher
         */
        public IndexSearcher getSearcher()
        {
            return is;
        }
        /**
         * Get the QueryParser instance
         * @return QueryParser
         */
        public QueryParser getParser()
        {
            return qp;
        }
    }
    /**
     * Inner class to search a Lucene index
     * @author Shubham Chatterjee
     *
     */
    public final static class Search
    {
        /**
         * Search the index for the given query and return top n hits
         * @param query String Query to search
         * @param n Integer Top hits for the query
         * @return TopDocs Top documents matching the query
         * @throws IOException
         * @throws ParseException
         */
        public static TopDocs searchIndex(String query,int n)throws IOException,ParseException
        {
            Query q = qp.parse(query);
            TopDocs tds = is.search(q, n);
            return tds;
        }
        /**
         * Search the index for the given query and return top n hits
         * The query is a Boolean Query which may consist of one or more terms queries
         * @param query BooleanQuery Query to search
         * @param n Integer Top hits for the query
         * @return TopDocs Top documents matching the query
         * @throws IOException
         * @throws ParseException
         */
        public static TopDocs searchIndex(BooleanQuery query,int n)throws IOException
        {
            TopDocs tds = is.search(query, n);
            return tds;
        }
        public static TopDocs searchIndex(BooleanQuery booleanQuery,
                                          int n,
                                          IndexSearcher searcher)throws IOException
        {
            TopDocs search = searcher.search(booleanQuery, n);
            return search;
        }
        /**
         * Search the index for the given query in given field and return topmost hit
         * Use this to search an id or a phone number or another query which is not tokenized by lucene
         * @param field String Field to search
         * @param query String Query to search
         * @return Document The top document matching the query
         * @throws IOException
         * @throws ParseException
         */
        public static Document searchIndex(String field,String query)throws IOException,ParseException
        {
            Term term = new Term(field,query);
            Query q = new TermQuery(term);
            TopDocs tds = is.search(q,1);
            ScoreDoc[] retDocs = tds.scoreDocs;
            Document d = is.doc(retDocs[0].doc);
            return d;
        }
        /**
         * Search the index for the given query in given field and return topmost hit using the given searcher instance
         * Use this to search an id or a phone number or another query which is not tokenized by lucene
         * @param field String Field to search
         * @param query String Query to search
         * @return Document The top document matching the query
         * @throws IOException
         * @throws ParseException
         */
        public static Document searchIndex(String field,String query,IndexSearcher searcher)throws IOException,ParseException
        {
            Document d;
            Term term = new Term(field,query);
            Query q = new TermQuery(term);
            TopDocs tds = searcher.search(q,1);
            ScoreDoc[] retDocs = tds.scoreDocs;
            if(retDocs.length != 0)
            {
                d = searcher.doc(retDocs[0].doc);
                return d;
            }
            return null;
        }
        /**
         * Search the index for the given query in given field and return topmost n hits
         * @param field String Field to search
         * @param query String Query to search
         * @param n Integer Top hits for the query
         * @return HashMap<Document,Float> A Hash map of (document,score)
         * @throws IOException
         * @throws ParseException
         */
        public static HashMap<Document, Float> searchIndex(String field,String query,int n)throws IOException
        {
            HashMap<Document,Float> results = new HashMap<Document,Float>();
            Term term = new Term(field,query);
            Query q = new TermQuery(term);
            TopDocs tds = is.search(q,n);
            ScoreDoc[] retDocs = tds.scoreDocs;
            for (int i = 0; i < retDocs.length; i++)
                results.put(is.doc(retDocs[i].doc),tds.scoreDocs[i].score);
            return results;
        }
    }
    /**
     * Get the index size
     * @return Integer Size of the index (number of documents)
     */
    public static int getIndexSize()
    {
        return COUNT;
    }
	/*public static void main(String[] args) throws IOException
	{
		String indexDir = args[0];
		String cborDir = args[1];
		System.out.println("Building index at location:"+indexDir);
		new Index.Build(indexDir,cborDir);
		Index.Build.createIndex(new StandardAnalyzer());
		System.out.println("Number of paragraphs indexed = "+Index.getIndexSize());
	}*/
}
