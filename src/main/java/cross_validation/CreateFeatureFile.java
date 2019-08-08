package cross_validation;

import org.jetbrains.annotations.NotNull;
import others.CheckHeapSize;
import support_passage.Utilities;

import java.io.*;
import java.util.*;

/**
 * This class reads in feature files from a directory and makes a RankLib compatible feature file.
 * @author Shubham Chatterjee
 * @version 4/16/19
 */

public class CreateFeatureFile {

    // Path to the qrels file
    private String qrelPath;

    // Path to the directory where feature file will be stored
    private String featureFileDir;

    // Name of the feature file
    private String featureFileName;

    // Directory where all the run files are stored
    private File featureDir;


    /**
     * Constructor.
     * @param directoryPath Path to the directory containing the run files to be used for creating the RankLib feature file
     * @param qrelPath Path to the ground truth file
     * @param featureFileDir Path to the directory where the feature file will be stored
     * @param featureFileName Name of the feature file
     */

    public CreateFeatureFile(String directoryPath, String qrelPath, String featureFileDir, String featureFileName) {
        this.featureDir = new File(directoryPath);
        this.qrelPath = qrelPath;
        this.featureFileDir = featureFileDir;
        this.featureFileName = featureFileName;
        System.out.println("Heap size = " + CheckHeapSize.getHeapSize());
        System.out.println("Max Heap Size = " + CheckHeapSize.getHeapMaxSize());
        createFeatureFile();
    }

    /**
     * Create the feature file.
     */
    private void createFeatureFile()  {

        // Map to store the ground truth. Key = Query and Value = List of relevant paragraphs
        LinkedHashMap<String, ArrayList<String>> relevanceMap = new LinkedHashMap<>();

        // List of Maps for every run file used to create the feature map.
        // Each Map has Key = QueryID. Value = Map  of (paraID, score)
        ArrayList<LinkedHashMap<String, LinkedHashMap<String, Double>>> featureMapList = new ArrayList<>();

        // List of queries
        ArrayList<String> queryList = new ArrayList<>();

        //List of feature file strings
        ArrayList<String> featureStrings = new ArrayList<>();

        LinkedHashMap<Integer, LinkedList<Double>> featureFileMap = new LinkedHashMap<>();



        // Keep track of the queries to generate the feature file
        int qid = 0;

        // Name of the temporary un-normalized feature file
        String tempFileName = "temp1.txt";

        System.out.print("Reading the ground truth file....");
        readQrels(relevanceMap);
        System.out.println("Done.");

        // Get the list of files in the directory
        File[] listOfFeatureFiles = featureDir.listFiles();
        assert listOfFeatureFiles != null;

        // Sort the files is important to maintain order while iterating over the files
        Arrays.sort(listOfFeatureFiles);

        System.out.println("The following files will be used to create the RankLib feature file: ");
        System.out.println("============================================================");
        display(listOfFeatureFiles);
        System.out.println("============================================================");

        for (File file : listOfFeatureFiles) {
            if (file.isFile()) {
                System.out.print("Reading feature file: " + file.getName() + "....");
                featureMapList.add(readFile(file));
                System.out.println("Done.");
            }
        }

        // Get the list of all queries from all run files
        System.out.print("Getting list of queries....");
        getQueryList(queryList, featureMapList);
        System.out.println("Done.");


        // For every query do
        System.out.print("Making feature file strings....");
        for (String query : queryList) {
            qid++;
            // If there is relevance judgement for the query then
            if (relevanceMap.containsKey(query)) {
                // Get the list of retrieved paragraphs for the query from all the run files
                HashSet<String> paraList = new HashSet<>();
                getParaList(query, paraList, featureMapList);

                // Make the feature file strings
                makeFeatureFileStrings(query, qid, paraList, relevanceMap.get(query), featureMapList, featureStrings);
            }

        }
        System.out.println("Done.");
        System.out.print("Writing feature file...");
        Utilities.writeFile(featureStrings, featureFileDir + "/" + featureFileName);
        System.out.println("Done");
//        System.out.print("Reading temporary un-normalized feature file...");
//        readFeatureFile(featureFileMap);
//        System.out.println("Done.");
//        System.out.print("Normalizing features...");
//        normalizeFeatures(featureFileMap);
//        System.out.println("Done");
//        System.out.print("Making new feature file strings...");
//        ArrayList<String> fStrings = makeFeatureFileStrings(featureFileMap, featureStrings);
//        System.out.println("Done");
//
//        // Write to the feature file
//        System.out.print("Writing feature file....");
//        Utilities.writeFile(fStrings, featureFileDir + "/" + featureFileName);
//        System.out.println("Done.");
//        System.out.println("Feature file written to: " + featureFileDir + "/" + featureFileName);
    }

    private void readFeatureFile(LinkedHashMap<Integer, LinkedList<Double>> featureFileMap) {
        BufferedReader in = null;
        File tempFile = new File(featureFileDir + "/temp1.txt");
        LinkedList<Double> featureValues;
        try {
            in = new BufferedReader(new FileReader(tempFile));
            String line;
            while ((line = in.readLine()) != null) {
                String[] lineParts = line.split(" ");
                for (int i = 2; i < lineParts.length - 2; i++) {
                    String featureValuePair = lineParts[i];
                    int fNum = Integer.parseInt(featureValuePair.split(":")[0]);
                    double fVal = Double.parseDouble(featureValuePair.split(":")[1]);
                    if (featureFileMap.containsKey(fNum)) {
                        featureValues = featureFileMap.get(fNum);
                    } else {
                        featureValues = new LinkedList<>();
                    }
                    featureValues.add(fVal);
                    featureFileMap.put(fNum, featureValues);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void normalizeFeatures(LinkedHashMap<Integer, LinkedList<Double>> featureFileMap) {
        for (int fNum : featureFileMap.keySet()) {
            LinkedList<Double> featureValues = featureFileMap.get(fNum);
            LinkedList<Double> normalizedFeatureValues = normalizeFeatures(featureValues);
            featureFileMap.put(fNum,normalizedFeatureValues);
        }
    }
    private LinkedList<Double> normalizeFeatures( LinkedList<Double> featureValues) {
        double mean = mean(featureValues);
        double sd = sd(featureValues);
        LinkedList<Double> normalizedFeatureValues = new LinkedList<>();
        for (double fVal : featureValues) {
            double normalizedVal = (fVal - mean) / sd;
            normalizedFeatureValues.add(normalizedVal);
        }
        return normalizedFeatureValues;
    }

    private double mean(LinkedList<Double> featureValues) {
        double sum = 0.0d;
        for (double fVal : featureValues) {
            sum += fVal;
        }

        return sum / featureValues.size();
    }
    private double sd(LinkedList<Double> featureValues) {
        double sum = 0.0d;
        double mean = mean(featureValues);
        for(double fVal: featureValues) {
            sum += Math.pow(fVal - mean, 2);
        }

        return Math.sqrt(sum / featureValues.size());
    }

    /**
     * Make the feature file strings.
     * @param query The query
     * @param paraList The list of paragraphs retrieved for the query
     * @param relevantParas The list of paragraphs relevant for the query
     * @param featureMapList The Map of each run file
     * @param featureStrings The feature string
     */
    private void makeFeatureFileStrings(String query,
                                        int qid,
                                        @NotNull HashSet<String> paraList,
                                        ArrayList<String> relevantParas,
                                        ArrayList<LinkedHashMap<String, LinkedHashMap<String, Double>>> featureMapList,
                                        ArrayList<String> featureStrings) {
        for (String paraID : paraList) {
            StringBuilder featureFileString = new StringBuilder();
            int i = 1;
            if (relevantParas.contains(paraID)) {
                featureFileString.append("1 qid:").append(qid);
            } else {
                featureFileString.append("0 qid:").append(qid);
            }
            for (LinkedHashMap<String, LinkedHashMap<String, Double>> runFile : featureMapList) {
                if (!runFile.containsKey(query)) {
                    featureFileString.append(" ").append(i).append(":0");
                } else {
                    if (runFile.get(query).containsKey(paraID)) {
                        featureFileString.append(" ").append(i).append(":").append(runFile.get(query).get(paraID));
                    } else {
                        featureFileString.append(" ").append(i).append(":0");
                    }
                }
                i++;
            }
            featureFileString.append(" #").append(query).append("_").append(paraID);
            featureStrings.add(featureFileString.toString());
        }
    }

    private ArrayList<String> makeFeatureFileStrings(LinkedHashMap<Integer, LinkedList<Double>> featureFileMap,
                                        ArrayList<String> featureStrings) {
        //featureStrings.clear();
        ArrayList<String> fStrings = new ArrayList<>();
        //BufferedReader in = null;
        //File tempFile = new File(featureFileDir + "/temp.txt");
        StringBuilder featureString;
        int count = 0;

        for (String line : featureStrings) {
            featureString = new StringBuilder();
            String[] lineParts = line.split(" ");
            String rel = lineParts[0];
            String qid = lineParts[1];
            String info = lineParts[lineParts.length - 1];
            featureString.append(rel).append(" ").append(qid).append(" ");
            for (int i = 2; i < lineParts.length - 2; i++) {
                String featureValuePair = lineParts[i];
                int fNum = Integer.parseInt(featureValuePair.split(":")[0]);
                double fVal = featureFileMap.get(fNum).get(count);
                featureString.append(fNum).append(":").append(fVal).append(" ");
            }
            count++;
            featureString.append("#").append(" ").append(info);
            fStrings.add(featureString.toString());
        }
        return fStrings;
    }





        /*try {
            in = new BufferedReader(new FileReader(tempFile));
            String line;
            while ((line = in.readLine()) != null) {
                featureString = new StringBuilder();
                String[] lineParts = line.split(" ");
                String rel = lineParts[0];
                String qid = lineParts[1];
                String info = lineParts[lineParts.length - 1];
                featureString.append(rel).append(" ").append(qid).append(" ");
                for (int i = 2; i < lineParts.length - 2; i++) {
                    String featureValuePair = lineParts[i];
                    int fNum = Integer.parseInt(featureValuePair.split(":")[0]);
                    double fVal = featureFileMap.get(fNum).get(count);
                    featureString.append(fNum).append(":").append(fVal).append(" ");
                }
                count++;
                featureString.append("#").append(" ").append(info);
                featureStrings.add(featureString.toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                    tempFile.delete();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Display the name of each file.
     * @param list List of files
     */

    private void display(@NotNull File[] list) {
        for (File file : list) {
            System.out.println(file.getName());
        }
    }

    /**
     * Get the list of queries.
     * @param queryList List if queries
     * @param featureMapList The List of Maps for each run file
     */

    private void getQueryList(@NotNull ArrayList<String> queryList,
                              @NotNull ArrayList<LinkedHashMap<String, LinkedHashMap<String, Double>>> featureMapList) {

        queryList.addAll(featureMapList.get(0).keySet());
        for (int i = 1; i < featureMapList.size(); i++) {
            for (String query : featureMapList.get(i).keySet()) {
                if (!queryList.contains(query)) {
                    queryList.add(query);
                }
            }
        }

    }

    /**
     * Get the list of paragraphs.
     * @param query The query
     * @param paraList The list of paragraphs retrieved for the query
     * @param featureMapList The List of Maps for each run file
     */
    private void getParaList(String query,
                             HashSet<String> paraList,
                             @NotNull ArrayList<LinkedHashMap<String, LinkedHashMap<String, Double>>> featureMapList) {

        for (int i = 0; i < featureMapList.size(); i++) {
            if (featureMapList.get(i).containsKey(query)) {
                paraList.addAll(featureMapList.get(i).get(query).keySet());
            }
        }

    }

    /**
     * Read the ground truth file.
     * @param relevanceMap The Map of relevance judgements
     */
    private void readQrels(LinkedHashMap<String, ArrayList<String>> relevanceMap) {
        BufferedReader in = null;
        ArrayList<String> paras;
        try {
            in = new BufferedReader(new FileReader(qrelPath));
            String line;
            while ((line = in.readLine()) != null) {
                String queryID = line.split(" ")[0];
                String paraID = line.split(" ")[2];
                if (relevanceMap.containsKey(queryID)) {
                    paras = relevanceMap.get(queryID);
                } else {
                    paras = new ArrayList<>();
                }
                paras.add(paraID);
                relevanceMap.put(queryID, paras);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Read the run file.
     * @param file Run file to read
     * @return Map corresponding to the run file read
     */
    private LinkedHashMap<String, LinkedHashMap<String, Double>> readFile(File file) {
        BufferedReader in = null;
        LinkedHashMap<String, LinkedHashMap<String, Double>> featureMap = new LinkedHashMap<>();
        LinkedHashMap<String, Double> featureValues;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                String[] parts = line.split(" ");
                String queryID = parts[0];
                String paraID = parts[2];
                double score = Double.parseDouble(parts[4]);
                if (featureMap.containsKey(queryID)) {
                    featureValues = featureMap.get(queryID);
                } else {
                    featureValues = new LinkedHashMap<>();
                }
                featureValues.put(paraID, score);
                featureMap.put(queryID, featureValues);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(in != null) {
                    in.close();
                } else {
                    System.out.println("Buffer has not been initialized!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return featureMap;

    }

    /**
     * Main method.
     * @param args Command line arguments
     */

    public static void main(String[] args)  {
        String directoryPath = args[0];
        String qrelPath = args[1];
        String featureFileDir = args[2];
        String featureFileName = args[3];
        new CreateFeatureFile(directoryPath, qrelPath, featureFileDir, featureFileName);
    }
}
