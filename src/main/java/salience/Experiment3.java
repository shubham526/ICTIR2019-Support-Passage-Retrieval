package salience;

import help.Utilities;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

/**
 * Class to check how many entities retrieved for every query had a salient passage in the candidate set and
 * how many of these entities were relevant.
 * @author Shubham Chaterjee
 * @version 7/17/2019
 */

public class Experiment3 {
    private HashMap<String, ArrayList<String>> entityRankings;
    private HashMap<String, ArrayList<String>> entityQrel;
    private HashMap<String, Set<String>> entWithSalPsgMap = new HashMap<>();
    private HashMap<String, Set<String>> entWithNoSalPsgMap = new HashMap<>();

    /**
     * Constructor.
     * @param entFile String Path to the entity run file.
     * @param file1 String Path to the entity with salient passage serialized file.
     * @param file2 String Path to the entity no with salient passage serialized file.
     * @param entityQrelFile String Path to the entity ground truth file.
     * @param outFile String Path to the output file.
     */


    public Experiment3(String entFile, String file1, String file2, String entityQrelFile, String outFile) {
        System.out.print("Reading entity file..");
        this.entityRankings = Utilities.getRankings(entFile);
        System.out.println("[Done].");

        System.out.print("Reading entity qrels...");
        this.entityQrel = Utilities.getRankings(entityQrelFile);
        System.out.println("[Done].");

        System.out.print("Reading entities with salient passage file...");
        try {
            this.entWithSalPsgMap = Utilities.readMap(file1);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");

        System.out.print("Reading entities with no salient passage file...");
        try {
            this.entWithNoSalPsgMap = Utilities.readMap(file2);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("[Done].");

        doTask(outFile);
    }

    /**
     * Helper method.
     * @param outFile String Path to the output file.
     */

    private void doTask(String outFile) {
        Set<String> querySet = entityRankings.keySet();
        ArrayList<String> fileStrings = new ArrayList<>();

        ArrayList<Double> entWithNoSalPsgRelStatList = new ArrayList<>();
        ArrayList<Double> relEntWithNoSalPsgStatList = new ArrayList<>();

        ArrayList<Double> entWithSalPsgRelStatList = new ArrayList<>();
        ArrayList<Double> relEntWithSalPsgStatList = new ArrayList<>();

        for (String queryID : querySet) {
            List<String> processedRelEntList = Utilities.process(entityQrel.get(queryID));
            Set<String> entWithNoSalPsg = entWithNoSalPsgMap.get(queryID);
            Set<String> entWithSalPsg = entWithSalPsgMap.get(queryID);
            List<String> retEntityList = entityRankings.get(queryID);

            int n1 = retEntityList.size(); // Number of entities retrieved for the query
            int n2 = entWithNoSalPsg.size(); // Number of entities retrieved which have no salient passage in the candidate set
            int n3 = entWithSalPsg.size(); // Number of entities retrieved which have a salient passage in the candidate set
            int n4  = processedRelEntList.size(); // Number of entities relevant for the query (according to qrels)

            // Percentage of entities retrieved which do not have a salient passage in the candidate set
            double m1 = ((double) n2 / n1) * 100;

            // Percentage of entities retrieved which do have a salient passage in the candidate set
            double m2 = ((double) n3 / n1) * 100;

            // Number of entities in common between the two sets
            int c1 = common(processedRelEntList, entWithNoSalPsg);
            // Percentage of entities with no salient passage which are also relevant
            double m3 = n2 == 0 ? 0.0 : ((double)c1 / n2) * 100;
            entWithNoSalPsgRelStatList.add(m3);
            // Percentage of relevant entities which have no salient passage
            double m4 = n4 == 0 ? 0.0 : ((double)c1 / n4) * 100;
            relEntWithNoSalPsgStatList.add(m4);

            // Number of entities in common between the two sets
            int c2 = common(processedRelEntList, entWithSalPsg);
            // Percentage of entities with salient passage which are also relevant
            double m5 = n3 == 0 ? 0.0 : ((double)c2 / n3) * 100;
            entWithSalPsgRelStatList.add(m5);
            // Percentage of relevant entities which have salient passage
            double m6 = n4 == 0 ? 0.0 : ((double)c2 / n4) * 100;
            relEntWithSalPsgStatList.add(m6);

            StringBuilder str = new StringBuilder();

            str.append("QueryID: ")
                    .append(queryID)
                    .append("\n")
                    .append("\n")
                    .append("Number of entities retrieved = ")
                    .append(n1)
                    .append("\n")
                    .append("\n")
                    .append("Percentage of entities with no salient passage = ")
                    .append((String.format("%.2f", m1)))
                    .append(" %")
                    .append("\n")
                    .append("Percentage of entities with no salient passage which are also relevant = ")
                    .append((String.format("%.2f", m3)))
                    .append(" %")
                    .append("\n")
                    .append("Percentage of relevant entities which have no salient passage = ")
                    .append((String.format("%.2f", m4)))
                    .append(" %")
                    .append("\n")
                    .append("\n")
                    .append("Percentage of entities with salient passage = ")
                    .append((String.format("%.2f", m2)))
                    .append(" %")
                    .append("\n")
                    .append("Percentage of entities with salient passage which are also relevant = ")
                    .append((String.format("%.2f", m5)))
                    .append(" %")
                    .append("\n")
                    .append("Percentage of relevant entities which have salient passage = ")
                    .append((String.format("%.2f", m6)))
                    .append(" %")
                    .append("\n")
                    .append("\n")
                    .append("=====================================================================================")
                    .append("\n");

            fileStrings.add(str.toString());
            System.out.println(str.toString());

        }
        double[] res1 = getStat(entWithSalPsgRelStatList);
        double[] res2 = getStat(entWithNoSalPsgRelStatList);
        double[] res3 = getStat(relEntWithSalPsgStatList);
        double[] res4 = getStat(relEntWithNoSalPsgStatList);

        StringBuilder str = new StringBuilder();
        str.append("Results for entities with salient passage\n")
                .append("=========================================================================\n")
                .append("Mean entities with salient passage which are also relevant = ")
                .append((String.format("%.2f", res1[0])))
                .append("\n")
                .append("Standard Deviation = ")
                .append((String.format("%.2f", res1[1])))
                .append("\n\n")
                .append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n")
                .append("Mean relevant entities with salient passage = ")
                .append((String.format("%.2f", res3[0])))
                .append("\n")
                .append("Standard Deviation = ")
                .append((String.format("%.2f", res3[1])))
                .append("\n\n")
                .append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n")
                .append("Results for entities with no salient passage\n")
                .append("=========================================================================\n")
                .append("Mean entities with no salient passage which are also relevant = ")
                .append((String.format("%.2f", res2[0])))
                .append("\n")
                .append("Standard Deviation = ")
                .append((String.format("%.2f", res2[1])))
                .append("\n\n")
                .append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n")
                .append("Mean relevant entities with no salient passage = ")
                .append((String.format("%.2f", res4[0])))
                .append("\n")
                .append("Standard Deviation = ")
                .append((String.format("%.2f", res4[1])))
                .append("\n\n")
                .append("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++\n\n");

        System.out.println(str.toString());
        fileStrings.add(str.toString());

        System.out.print("Writing to file....");
        Utilities.writeFile(fileStrings,outFile);
        System.out.println("[Done].");
        System.out.println("Results written to: " + outFile);
    }

    /**
     * Find the number of things in common between the two arguments.
     * @param relEntList List List if relevant entities.
     * @param entSet Set Set of retrieved entities.
     * @return Integer
     */

    @Contract(pure = true)
    private int common(List<String> relEntList, @NotNull Set<String> entSet) {
        int m = 0;
        for (String entity : entSet) {
            if (relEntList.contains(entity)) {
                m++;
            }
        }
        return m;
    }

    /**
     * Get the mean and standard deviation for the passed in list.
     * @param list Lisy
     * @return Array of Double
     */

    @NotNull
    private double[] getStat(List<Double> list) {
        double[] res = new double[2];
        double mean = mean(list);
        double sd = sd(list, mean);
        res[0] = mean;
        res[1] = sd;
        return res;
    }

    /**
     * Get the mean.
     * @param list List
     * @return Double
     */

    @Contract(pure = true)
    private double mean(@NotNull List<Double> list) {
        double sum = 0.0d;
        for (double num : list) {
            sum += num;
        }
        double mean = sum / list.size();
        return mean;
    }

    /**
     * Get the standard deviation.
     * @param list List
     * @param mean Double
     * @return Double
     */

    private double sd(@NotNull List<Double> list, double mean) {
        double sd = 0.0d;
        for(double num: list) {
            sd += Math.pow(num - mean, 2);
        }
        return Math.sqrt(sd/list.size());
    }

    /**
     * Main method.
     * @param args Command line arguments.
     */


    public static void main(@NotNull String[] args) {
        String entityFile = args[0];
        String entityWithSalPsgMapFile = args[1];
        String entityWithNoSalPsgMapFile = args[2];
        String entityQrelsFile = args[3];
        String outFile = args[4];
        new Experiment3(entityFile, entityWithSalPsgMapFile, entityWithNoSalPsgMapFile, entityQrelsFile, outFile);
    }
}

