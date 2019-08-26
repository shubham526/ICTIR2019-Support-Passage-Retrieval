import baselines.Baseline1;
import baselines.Baseline2;
import features.EntityContextNeighbors;
import features.PseudoDocRetScore;
import features.QEEntities;
import features.QEWords;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.jetbrains.annotations.NotNull;
import salience.Experiment1;
import salience.Experiment2;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Project runner.
 * @author Shubham Chatterjee
 * @version 8/26/2019
 */
public class ProjectMain {
    public static void main(@NotNull String[] args) {

        if (args.length == 0) {
            help();
            System.exit(-1);
        }
        String command = args[0];
        String indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile, entityQrel, a, s, o,
                s1 = null, s2, swatFile, supportPsgRunFile;
        int takeKEntities, takeKDocs, takeKTerms;
        Similarity similarity;
        Analyzer analyzer;
        boolean omit;


        switch(command) {
            case "baseline1":
                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                outFile = args[7];
                entityQrel = args[8];
                new Baseline1(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                        outFile, entityQrel);
                break;

            case "baseline2":
                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                entityRunFile = args[5];
                entityQrel = args[6];
                outFile = args[7];
                a = args[8];
                s = args[9];
                List<String> searchFields = Arrays.asList(Arrays.copyOfRange(args, 10, args.length));


                System.out.print("Searching fields: ");
                for (String field : searchFields) {
                    System.out.print(field + " ");
                }
                System.out.println();
                analyzer = null;
                similarity = null;

                switch (a) {
                    case "eng":
                        System.out.println("Using English analyzer.");
                        analyzer = new EnglishAnalyzer();
                        break;
                    case "std":
                        System.out.println("Using Standard analyzer.");
                        analyzer = new StandardAnalyzer();
                        break;
                    default:
                        System.out.println("Wrong analyzer choice! Can be either English(eng) or Standard(std)");
                }

                switch (s) {
                    case "bm25":
                        System.out.println("Using BM25.");
                        similarity = new BM25Similarity();
                        break;
                    case "lmds":
                        System.out.println("Using LM-DS.");
                        similarity = new LMDirichletSimilarity();
                        break;
                    case "lmjm":
                        System.out.println("Using LM-JM.");
                        similarity = new LMJelinekMercerSimilarity(0.5f);
                    default:
                        System.out.println("Wrong choice! Can be either BM25(bm25), LM-DS(lmds) or LM-JM(lmjm)");
                }


                try {
                    new Baseline2(indexDir, trecCarDir, outputDir, dataDir, entityRunFile, entityQrel,
                            outFile, analyzer, similarity, searchFields);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "ecn":
                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                outFile = args[7];
                entityQrel = args[8];
                try {
                    new EntityContextNeighbors(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                            outFile, entityQrel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "pdrs":
                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                outFile = args[7];
                entityQrel = args[8];
                try {
                    new PseudoDocRetScore(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile,
                            outFile, entityQrel);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case "qee":

                similarity = null;
                analyzer = null;

                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                entityQrel = args[7];
                takeKEntities = Integer.parseInt(args[8]);
                o = args[9];
                omit = o.equalsIgnoreCase("y") || o.equalsIgnoreCase("yes");
                a = args[10];
                s = args[11];

                System.out.printf("Using %d entities for query expansion\n", takeKEntities);

                if (omit) {
                    System.out.println("Using RM1");
                    s2 = "rm1";
                } else {
                    System.out.println("Using RM3");
                    s2 = "rm3";
                }

                System.out.println("Similarity: " + s);
                System.out.println("Analyzer: " + a);

                switch (a) {
                    case "std" :
                        analyzer = new StandardAnalyzer();
                        break;
                    case "eng":
                        analyzer = new EnglishAnalyzer();

                        break;
                    default:
                        System.out.println("Wrong choice of analyzer! Exiting.");
                        System.exit(1);
                }
                switch (s) {
                    case "BM25" :
                        similarity = new BM25Similarity();
                        s1 = "bm25";
                        break;
                    case "LMJM":
                        float lambda = Float.parseFloat(args[12]);
                        System.out.println("Lambda = " + lambda);
                        similarity = new LMJelinekMercerSimilarity(lambda);
                        s1 = "lmjm";
                        break;
                    case "LMDS":
                        similarity = new LMDirichletSimilarity();
                        s1 = "lmds";
                        break;

                    default:
                        System.out.println("Wrong choice of similarity! Exiting.");
                        System.exit(1);
                }
                outFile = "qee" + "-" + s1 + "-" + s2 + ".run";

                new QEEntities(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile, entityQrel,
                        takeKEntities, omit, analyzer, similarity);
                break;

            case "qew":
                similarity = null;
                analyzer = null;

                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                entityQrel = args[7];
                takeKTerms = Integer.parseInt(args[8]);
                takeKDocs = Integer.parseInt(args[9]);
                o = args[10];
                omit = o.equalsIgnoreCase("y") || o.equalsIgnoreCase("yes");
                a = args[11];
                s = args[12];

                System.out.printf("Using %d terms for query expansion\n", takeKTerms);
                System.out.printf("Using %d documents as feedback set for query expansion\n", takeKDocs);

                if (omit) {
                    System.out.println("Using RM1");
                    s2 = "rm1";
                } else {
                    System.out.println("Using RM3");
                    s2 = "rm3";
                }

                System.out.println("Similarity: " + s);
                System.out.println("Analyzer: " + a);

                switch (a) {
                    case "std" :
                        analyzer = new StandardAnalyzer();
                        break;
                    case "eng":
                        analyzer = new EnglishAnalyzer();

                        break;
                    default:
                        System.out.println("Wrong choice of analyzer! Exiting.");
                        System.exit(1);
                }
                switch (s) {
                    case "BM25" :
                        similarity = new BM25Similarity();
                        s1 = "bm25";
                        break;
                    case "LMJM":
                        float lambda = Float.parseFloat(args[13]);
                        System.out.println("Lambda = " + lambda);
                        similarity = new LMJelinekMercerSimilarity(lambda);
                        s1 = "lmjm";
                        break;
                    case "LMDS":
                        similarity = new LMDirichletSimilarity();
                        s1 = "lmds";
                        break;

                    default:
                        System.out.println("Wrong choice of similarity! Exiting.");
                        System.exit(1);
                }
                outFile = "qew" + "-" + s1 + "-" + s2 + ".run";
                new QEWords(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile, entityQrel,
                        takeKTerms, takeKDocs, omit, analyzer, similarity);

                break;

            case "sal-exp-1":
                indexDir = args[1];
                trecCarDir = args[2];
                outputDir = args[3];
                dataDir = args[4];
                paraRunFile = args[5];
                entityRunFile = args[6];
                outFile = args[7];
                entityQrel = args[8];
                swatFile = args[9];

                new Experiment1(indexDir, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile,
                        entityQrel, swatFile);
                break;

            case "sal-exp-2":
                trecCarDir = args[1];
                outputDir = args[2];
                dataDir = args[3];
                supportPsgRunFile = args[4];
                entityRunFile = args[5];
                outFile = args[6];
                swatFile = args[7];

                new Experiment2(trecCarDir, outputDir, dataDir, supportPsgRunFile, entityRunFile, outFile, swatFile);
                break;

            default: help();

        }
    }
    private static void help() {
        System.out.println("================================================================================");
        System.out.println("This code produces the results from the ICTIR 2019 Support Passage Short Paper");
        System.out.println("================================================================================");

        System.out.println("The following options are available:");
        System.out.println("baseline1: Produces the first baseline run.");
        System.out.println("baseline2: Produces the second baseline run.");
        System.out.println("ecn      : Produces the run using method \"ECN\".");
        System.out.println("pdrs     : Produces the run using method \"Retrieval score of ECD\".");
        System.out.println("qee      : Produces the run using method \"Query Expansion with Entities\".");
        System.out.println("qew      : Produces the run using method \"Query Expansion with Words\".");
        System.out.println("sal-exp-1: Produces the run using first experiment for salience.");
        System.out.println("sal-exp-2: Produces the run using second experiment for salience.");
        System.out.println();
        System.out.println("For description of above methods, see paper.");
        System.out.println("For additional information on how to run the code, see the online appendix or Github.");
        System.exit(-1);




    }
}
