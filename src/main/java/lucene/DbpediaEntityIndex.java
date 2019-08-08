package lucene;

import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class to create a lucene index.
 * For every paragraph, its id and entities are stored.
 * Entities taken from DBpedia.
 * @author Shubham Chatterjee
 * @version 02/24/2019
 */

public class DbpediaEntityIndex {
    // Location to store the index
    private String INDEX_DIR;
    //Path to the DBPedia links file
    private String DBPEDIA_FILE;
   // To see progress
    private ProgressBar pb;

    /**
     * Constructor.
     * @param INDEX_DIR Location where index to be stored
     * @param DBPEDIA_FILE Path to the DBPedia links file
     */

    public DbpediaEntityIndex(String INDEX_DIR, String DBPEDIA_FILE) {
        try {
            pb = new ProgressBar("Progress", Files.lines(Paths.get(DBPEDIA_FILE)).count());
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.INDEX_DIR = INDEX_DIR;
        this.DBPEDIA_FILE = DBPEDIA_FILE;
    }

    /**
     * Create an IndexWriter object with the specified Analyzer.
     * @param analyzer Analyzer Type of analyzer to use for building the index
     * @return IndexWriter An instance of the index writer to build the index
     * @throws IOException
     */
    private  IndexWriter createWriter(Analyzer analyzer)throws IOException {
        Directory indexdir = FSDirectory.open((new File(INDEX_DIR)).toPath());
        IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iw = new IndexWriter(indexdir, conf);
        return iw;
    }

    /**
     * Create a document of the specified paragraph with the text, id and entities.
     * @param arr  String array. First element is the paragraph id and the remaining are entities in the paragraph.
     * @return Document A document containing the text, id and entities as fields
     */
    private Document createDocument(String[] arr) {
        Document doc = new Document();
        String paraID = arr[0];
        StringBuilder entityString = new StringBuilder();
        for(int i = 1; i < arr.length; i++) {
            String s = arr[i];
            s = s.replaceAll("\\s+", "_").toLowerCase();
            entityString.append(" ").append(s);
        }
        doc.add(new StringField("id", paraID, Field.Store.YES));
        doc.add(new StringField("entity", entityString.toString(), Field.Store.YES));
        //System.out.println(para.getParaId());

        return doc;
    }


    /**
     * Create a Lucene index for each paragraph.
     * @param analyzer Analyzer Analyzer to use to create the index
     * @throws IOException
     */
    public void createIndex(Analyzer analyzer)throws IOException {
        System.out.println("Creating paragraph index at:" +INDEX_DIR);
        System.out.println("Creating index writer");
        IndexWriter writer = createWriter(analyzer);
        System.out.println("Done creating index writer.");

        System.out.println("Starting to index now.....");
        int i = 1;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader((new FileReader(DBPEDIA_FILE)));
            String s;
            while((s = reader.readLine()) != null) {
                String[] arr = s.split("\\s+");
                if (arr.length > 1) {
                    writer.addDocument(createDocument(arr));
                    pb.step();
                    i++;
                    if (i % 10000 == 0) {
                        writer.commit();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                    pb.close();
                } else {
                    System.out.println("Input Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n Done indexing.");
        writer.commit();
        writer.close();

    }

    public static void main(String[] args) {
        String indexDir = args[0];
        String dbpediaFile = args[1];
        String analyzer = args[2];
        System.out.println("Using " + analyzer + " analyzer.");

        Analyzer a = analyzer.equalsIgnoreCase("std") ? new StandardAnalyzer() : new EnglishAnalyzer();


        try {
            new DbpediaEntityIndex(indexDir, dbpediaFile).createIndex(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Index written to:" +indexDir);
    }




}
