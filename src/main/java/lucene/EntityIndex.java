package lucene;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import me.tongfei.progressbar.ProgressBar;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;
import others.CheckHeapSize;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

/**
 * Class to create a lucene index.
 * For every paragraph, its id, text and entities are stored.
 * Entities taken from both paragraphs as well as DBpedia.
 * @author Shubham Chatterjee
 * @version 02/24/2019
 */

public class EntityIndex {
    // Location to store the index
    private String INDEX_DIR;
    // Path to the paragraph corpus
    private String CBOR_FILE;
    //Path to the DBPedia links file
    private String DBPEDIA_FILE;
    // HashMap of debpedia entities
    private HashMap<String,String> entityMap;
    private ProgressBar pb1;

    /**
     * Constructor.
     * @param INDEX_DIR Location where index to be stored
     * @param CBOR_FILE Path to the paragraph corpus
     * @param DBPEDIA_FILE Path to the DBPedia links file
     */

    public EntityIndex(String INDEX_DIR, String CBOR_FILE, String DBPEDIA_FILE) {
        try {
            pb1 = new ProgressBar("Progress", Files.lines(Paths.get(DBPEDIA_FILE)).count() );
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.INDEX_DIR = INDEX_DIR;
        this.CBOR_FILE = CBOR_FILE;
        this.DBPEDIA_FILE = DBPEDIA_FILE;
        System.out.println("Loading DBPedia file in memory...");
        this.entityMap = getDBPediaEntities();
        System.out.println("Done");
    }

    /**
     * Get the entities from the dbpedia file.
     * @return HashMap where Key = paragraph id and Value = entities in the form of whitespace separated string.
     */

    private HashMap<String, String> getDBPediaEntities() {
        HashMap<String, String> map = new HashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader((new FileReader(DBPEDIA_FILE)));
            String s;
            while((s = reader.readLine()) != null) {
                String[] arr = s.split("\\s+");
                pb1.step();
                if (arr.length > 1) {
                    map.put(arr[0],arr[1]);
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
                } else {
                    System.out.println("Input Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        pb1.close();
        System.out.println("Final memory allocations:");
        System.out.println("Heap Size = " + CheckHeapSize.getHeapSize());
        System.out.println("Max Heap Size = " + CheckHeapSize.getHeapMaxSize());
        System.out.println("Free Heap Size = " + CheckHeapSize.getHeapFreeSize());
        return map;

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
     * @param para  A Data.Paragraph object
     * @return Document A document containing the text, id and entities as fields
     */
    private Document createDocument(@NotNull Data.Paragraph para) {
        Document doc = new Document();
        List<String> entity = getParaEntities(para);
        StringBuilder entityString = new StringBuilder();
        for(String s : entity) {
            s = s.replaceAll("\\s+", "_").toLowerCase();
            entityString.append(" ").append(s);
        }
        doc.add(new StringField("id", para.getParaId(), Field.Store.YES));
        doc.add(new StringField("entity", entityString.toString(), Field.Store.YES));
        doc.add(new TextField("text", para.getTextOnly(), Field.Store.YES));
        //System.out.println(para.getParaId());

        return doc;
    }

    private List<String> getParaEntities(@NotNull Data.Paragraph para) {
        List<String> entityList = para.getEntitiesOnly();
        String pid = para.getParaId();
        if (entityMap.containsKey(pid)) {
            String pEnt = entityMap.get(pid);
            String[] arr = pEnt.split("\\s+");
            entityList.addAll(Arrays.asList(arr));
        }
        return entityList;
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
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(CBOR_FILE)));
        Iterable<Data.Paragraph> ip = DeserializeData.iterableParagraphs(bis);
        AtomicInteger i = new AtomicInteger(1);
        System.out.println("Starting to index now.....");

        StreamSupport.stream(ip.spliterator(), true)
                .forEach(paragraph ->
                {
                    try {
                        writer.addDocument(createDocument(paragraph));
                        i.getAndIncrement();
                        if (i.get() % 10000 == 0) {
                            System.out.print('.');
                            writer.commit();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        System.out.println("\n Done indexing.");
        writer.commit();
        writer.close();

    }

    public static void main(@NotNull String[] args) {
        String indexDir = args[0];
        String cborFile = args[1];
        String dbpediaFile = args[2];
        String analyzer = args[3];
        System.out.println("Using " + analyzer + " analyzer.");

        Analyzer a = analyzer.equalsIgnoreCase("std") ? new StandardAnalyzer() : new EnglishAnalyzer();
        System.out.println("Initial memory allocations:");
        System.out.println("Heap Size = " + CheckHeapSize.getHeapSize());
        System.out.println("Max Heap Size = " + CheckHeapSize.getHeapMaxSize());
        System.out.println("Free Heap Size = " + CheckHeapSize.getHeapFreeSize());


        try {
            new EntityIndex(indexDir, cborFile, dbpediaFile).createIndex(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Index written to:" +indexDir);
    }




}
