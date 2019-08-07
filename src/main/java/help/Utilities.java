package help;
import lucene.Index;
import lucene.RAMIndex;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for various utility operations for the Support Passage Task.
 * @author Shubham Chatterjee
 * @version 03/01/2019
 */
public class Utilities {
    /**
     * Method to get rankings from the paragraph run file and entity run file.
     * @param inFilePath Path to file from which rankings must be taken
     * @return HashMap where Key = queryID and Value = List of rankings of paragraphs or entities
     */
    public static HashMap<String, ArrayList<String>> getRankings(String inFilePath) {
        HashMap<String,ArrayList<String>> rankings = new HashMap<> ();
        BufferedReader br;
        String line , queryID ,field2;

        try {
            br = new BufferedReader(new FileReader(inFilePath));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                queryID = fields[0];
                field2 = fields[2];
                ArrayList<String> list = new ArrayList<>();
                if(rankings.containsKey(queryID))
                    list = rankings.get(queryID);
                list.add(field2);
                rankings.put(queryID, list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rankings;
    }

    public static void getRankings(String inFilePath,  HashMap<String, LinkedHashMap<String, Double>> rankings) {
        BufferedReader br = null;
        String line , queryID ,field2;
        double score;

        try {
            br = new BufferedReader(new FileReader(inFilePath));
            while((line = br.readLine()) != null) {
                String[] fields = line.split(" ");
                queryID = fields[0];
                field2 = fields[2];
                score = Double.parseDouble(fields[4]);
                LinkedHashMap<String, Double> map = new LinkedHashMap<>();
                if(rankings.containsKey(queryID)) {
                    map = rankings.get(queryID);
                }
                map.put(field2, score);
                rankings.put(queryID, map);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    br.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Method to write run strings to a run file.
     * @param runStrings List of strings to be written to file
     * @param filePath Path to the output file
     */
    public static void writeFile(@NotNull ArrayList<String> runStrings, String filePath) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(filePath,true));

            for(String s : runStrings) {
                if (s != null) {
                    out.write(s);
                    out.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null) {
                    out.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to get the entities from a Lucene document.
     * @param d Lucene document returned by searching Lucene index
     * @return List of entities in the document
     */
    @NotNull
    public static ArrayList<String> getEntities(@NotNull Document d) {

        //return (ArrayList<String>) Arrays.asList(clean(d.getField("entity").stringValue().split(" ")));
        return new ArrayList<>(Arrays.asList(clean(d.getField("entity").stringValue().split(" "))));

    }

    /**
     * Method to remove all null values from an array.
     * @param v Array which must be cleaned
     * @return Cleaned array
     */
    @NotNull
    public static String[] clean(final String[] v) {
        List<String> list = new ArrayList<>(Arrays.asList(v));
        list.removeAll(Collections.singleton(null));
        list.removeAll(Collections.singleton(""));
        return list.toArray(new String[list.size()]);
    }

    /**
     * Convert an entity id to lowercase after removing the %20 and enwiki:
     * @param entityID String
     * @return String
     */

    public static String process(String entityID) {
        entityID = entityID.substring(entityID.indexOf(":")+1);
        entityID = entityID.replaceAll("%20", "_").toLowerCase();
        return entityID;
    }

    /**
     * Convert a list of entity ids to lowercase after removing %20 and enwiki:
     * @param entity String
     * @return String
     */
    public static ArrayList<String> process(@NotNull List<String> entity)
    {
        ArrayList<String> list = new ArrayList<>();
        for(String s : entity)
        {
            s = s.substring(s.indexOf(":")+1);
            s = s.replaceAll("%20", "_").toLowerCase();
            list.add(s);
        }
        return list;
    }

    @Contract(pure = true)
    public static ArrayList<String> unprocess(List<String> pEntList, @NotNull List<String> entityList) {
        ArrayList<String> list = new ArrayList<>();
        for (String e : entityList) {
            if (pEntList.contains(process(e))) {
                list.add(e);
            }
        }
        return list;
    }

    /**
     * Method to find the frequency of an entity in the list of entities.
     * @param e Entity to search
     * @param list List in which to search
     * @return Integer
     */

    @Contract(pure = true)
    public static int frequency(String e, @NotNull ArrayList<String> list) {
        int count = 0;
        for (String s : list) {
            if (s.equalsIgnoreCase(e)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Sort a Map in descending order by value.
     * @param map The map to be sorted
     * @return A Sorted map
     */

    public static <K, V>LinkedHashMap<K, V> sortByValueDescending(@NotNull Map<K, V> map) {
        LinkedHashMap<K, V> reverseSortedMap = new LinkedHashMap<>();
        map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue((Comparator<? super V>) Comparator.reverseOrder()))
                .forEachOrdered(x -> reverseSortedMap.put(x.getKey(), x.getValue()));
        return reverseSortedMap;
    }

    /**
     * Method to create a Pseudo-Document for an entity.
     * @param entity String EntityID
     * @param paraList ArrayList List of paragraphs relevant for query
     * @param searcher IndexSearcher
     * @return A Pseudo-Document for the (query, entity) pair
     */

    @Nullable
    public static PseudoDocument createPseudoDocument(String entity, @NotNull ArrayList<String> paraList, IndexSearcher searcher) {
        ArrayList<Document> documentList = new ArrayList<>();
        ArrayList<String> pseudoDocEntityList = new ArrayList<>();
        // Get the list of paragraphs relevant for the query
        // For every paragraph in the list of paragraphs relevant for the query do
        for (String paraId : paraList) {
            try {
                // Get the document corresponding to the paragraph from the lucene index
                Document doc = Index.Search.searchIndex("id", paraId, searcher);
                // Get the entities in the paragraph
                String[] entityList = Utilities.clean(doc.getField("entity").stringValue().split(" "));
                // Make an ArrayList from the String array
                ArrayList<String> pEntList = new ArrayList<>(Arrays.asList(entityList));
                // If the document does not have any entities then ignore
                if (pEntList.isEmpty()) {
                    continue;
                }
                // If the entity is present in the paragraph
                if (pEntList.contains(Utilities.process(entity))) {
                    // Add it to the pseudo document
                    documentList.add(doc);
                    // Add all the entities to the pseudo document entity list
                    pseudoDocEntityList.addAll(pEntList);
                }

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }

        }

        // If there are no documents in the pseudo-document
        if (documentList.size() == 0) {
            return null;
        }
        return new PseudoDocument(documentList, entity, pseudoDocEntityList);
    }

    /**
     * Find the intersection of two lists.
     * @param list1 The first list
     * @param list2 The second list
     * @param <T> Any parameter
     * @return List
     */

    public static <T> List<T> intersection(@NotNull List<T> list1, @NotNull List<T> list2) {
        return list1.stream().filter(list2::contains).collect(Collectors.toList());

    }
    public static ArrayList<PseudoDocument> getPseudoDocs(@NotNull ArrayList<String> entityList, ArrayList<String> paraList, IndexSearcher searcher) {
        ArrayList<PseudoDocument> pseudoDocuments = new ArrayList<>();
        // For every entity in this list of relevant entities do
        for (String entityId : entityList) {

            // Create a pseudo-document for the entity
            PseudoDocument d = createPseudoDocument(entityId, paraList, searcher);
            if (d != null) {
                pseudoDocuments.add(d);
            }
        }
        return pseudoDocuments;
    }
    /**
     * Converts a PseudoDocument to a Lucene Document.
     * We concatenate all the passages from all the documents contained in a pseudo-document and index it as a
     * TextField. The entity is indexed as a StringField.
     * @param d PseudoDocument
     * @return Document
     */
    public static Document pseudoDocToDoc(@NotNull PseudoDocument d) {
        Document doc = new Document();
        StringBuilder text = new StringBuilder(); // To store the text of the pseudo-document
        ArrayList<Document> documents = d.getDocumentList(); // Get the list of documents in the pseudo-document
        // For every document do
        for (Document document : documents) {
            // Concatenate the texts of all the documents into a single text
            text.append(document.getField("text").stringValue()).append(" ");
        }
        doc.add(new TextField("text", text.toString(), Field.Store.YES)); // Add the text as a field
        doc.add(new StringField("entity", d.getEntity(), Field.Store.YES)); // Add the entity as a field
        return doc; // return the document
    }

    /**
     * Read a serialized HashMap from the disk.
     * @param file String file to read
     * @param <K> Key
     * @param <V> Value
     * @return HashMap
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")

    public static <K, V>HashMap<K, V> readMap(String file) throws IOException, ClassNotFoundException {
        HashMap<K, V> mapInFile;
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(file)));
        mapInFile = (HashMap<K,V>)ois.readObject();

        ois.close();
        return mapInFile;
    }

    /**
     * Read a serialized List from the disk.
     * @param file String file to read
     * @param <K> Key
     * @return HashMap
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")

    public static <K>List<K> readList(String file) throws IOException, ClassNotFoundException {
        List<K> listInFile;
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(file)));
        listInFile = (List<K>)ois.readObject();

        ois.close();
        return listInFile;
    }

    /**
     * Write a HashMap to disk.
     * @param map HashMap to write.
     * @param file Name of file.
     * @param <K> Key
     * @param <V> Value
     * @throws IOException
     */
    public static <K,V> void writeMap(HashMap<K,V> map, String file) throws IOException {

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(file)));
        oos.writeObject(map);
        oos.flush();
        oos.close();
    }

    /**
     * Write a List to disk.
     * @param list List to write.
     * @param file Name of file.
     * @param <K> Key
     * @throws IOException
     */
    public static <K> void writeList(List<K> list, String file) throws IOException {

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(file)));
        oos.writeObject(list);
        oos.flush();
        oos.close();
    }

    public static double Prec_at_1(String runFile, String qrelFile) {
        //BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // Map of paragraphs retrieved
        HashMap<String, ArrayList<String>> runFileMap = getRankings(runFile);

        // Map of paragraphs relevant
        HashMap<String, ArrayList<String>> qrelFileMap = getRankings(qrelFile);

        // Map to store P@1 for every query
        HashMap<String, Integer> precMap = new HashMap<>();

        double avgPrecAtOne;
        int sum = 0;

        // For every query
        for (String queryID : runFileMap.keySet()) {
            //System.out.println("Query: " + queryID);

            //Get the list of paragraphs retrieved
            ArrayList<String> retParaList = runFileMap.get(queryID);
//            System.out.println("Retrieved--->");
//            System.out.println(retParaList);
//            try {
//                br.readLine();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


            // Get the list of paragraphs relevant
            ArrayList<String> relParaList = qrelFileMap.get(queryID);
//            System.out.println("Relevant--->");
//            System.out.println(relParaList);
//            try {
//                br.readLine();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            // If the first paragraph retrieved is also relevant
            if (relParaList != null) {
                if (relParaList.contains(retParaList.get(0))) {

                    // P@1 is 1 for the query
                    precMap.put(queryID, 1);
                    //System.out.println("1");
                } else {

                    // P@1 is 0 for the query
                    precMap.put(queryID, 0);
                    //System.out.println("0");
                }
            } else {
                //System.out.println("Did not find relevant information for: " + queryID);
            }
            //System.out.println(precMap.get(queryID));
//            try {
//                br.readLine();
//            } catch (IOException e) {
//
//            }
        }

        // Average the P@1 across queries
        for (double i : precMap.values()) {
            sum += i;
        }
        System.out.println("sum = " + sum);
        System.out.println("precMap.size() = " + precMap.size());
        //System.out.println(runFileMap.size());
        System.out.println(qrelFileMap.size());
        avgPrecAtOne = (double)sum / qrelFileMap.size();
        return avgPrecAtOne;

    }
    public static double sd(ArrayList<Double> values) {
        double mean = mean(values);
        double n = values.size();
        double dv = 0;
        for (double d : values) {
            double dm = d - mean;
            dv += dm * dm;
        }
        return Math.sqrt(dv / n);
    }
    /**
     * Calculate the mean of an array of values
     *
     * @param values The values to calculate
     * @return The mean of the values
     */
    public static double mean(ArrayList<Double> values) {
        return sum(values) / values.size();
    }
    /**
     * Sum up all the values in an array
     *
     * @param values an array of values
     * @return The sum of all values in the Array
     */
    public static double sum(ArrayList<Double> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("The data array either is null or does not contain any data.");
        }
        else {
            double sum = 0;
            for (double i : values) {
                sum += i;
            }
            return sum;
        }
    }
    public static HashMap<Document, Float> getDocumentToScoreMap(TopDocs tds, IndexSearcher is) {
        HashMap<Document,Float> results = new HashMap<>();
        ScoreDoc[] retDocs = tds.scoreDocs;
        for (int i = 0; i < retDocs.length; i++) {
            try {
                results.put(is.doc(retDocs[i].doc),tds.scoreDocs[i].score);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return results;
    }
    public static void main(String[] args) {
        String runFile = "/home/shubham/Desktop/research/Support_Passage/runs/all/15_sal_psg_in_pseudo_doc.run";
        String qrelFile = "/home/shubham/Desktop/research/Support_Passage/data/train.pages.cbor-article.support.qrels";
        System.out.println("P@1 = " + Prec_at_1(runFile, qrelFile));
    }

}
