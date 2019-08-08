import cross_validation.CreateFeatureFile;
import lucene.EntityIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import others.CheckHeapSize;
import others.FixRunFile;
import support_passage.Baseline1;
import support_passage.Baseline2;
import support_passage.Baseline3;
import support_passage.SupportPassageFeature;
import EntitySalience.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Main class to run various other classes int he project.
 * @author Shubham Chatterjee
 * @version 03/01/2019
 */
public class ProjectMain {
    public static void main(String[] args) {
        String command = args[0];

        switch(command) {
            case "index" :
                String indexDir = args[1];
                String cborFile = args[2];
                String dbpediaFile = args[3];
                String analyzer = args[4];
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
                break;

            case "feature" :
                int n = Integer.parseInt(args[1]);
                String index = args[2];
                String trecCarDir = args[3];
                String outputDir = args[4];
                String dataDir = args[5];
                String paraRunFile = args[6];
                String entityRunFile = args[7];
                String outFile = args[8];
                System.out.println("Calculating feature " + n);

                try {
                    new SupportPassageFeature(index, trecCarDir, outputDir, dataDir, paraRunFile, entityRunFile, outFile).getFeature(n);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Done calculating feature " + n);

                break;
            case "create" :
                String directoryPath = args[1];
                String qrelPath = args[2];
                String featureFileDir = args[3];
                String featureFileName = args[4];
                new CreateFeatureFile(directoryPath, qrelPath, featureFileDir, featureFileName);
                break;

            case "saliency" :
                String indexDir1 = args[1];
                String trecCarDir1 = args[2];
                String outputDir1 = args[3];
                String dataDir1 = args[4];
                String paraRunFile1 = args[5];
                String entityRunFile1 = args[6];
                String outFile1 = args[7];

                try {
                    new EntitySaliency(indexDir1, trecCarDir1, outputDir1, dataDir1, paraRunFile1, entityRunFile1, outFile1).feature();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "fix" :
                String s1 = args[1];
                String s2 = args[2];
                new FixRunFile(s1, s2).fix();
            case "saliencyeffect" :
                String indexDir2 = args[1];
                String trecCarDir2 = args[2];
                String outputDir2 = args[3];
                String dataDir2 = args[4];
                String paraRunFile2 = args[5];
                String entityRunFile2 = args[6];
                String outFile2 = args[7];

                try {
                    new SaliencyEffect(indexDir2, trecCarDir2, outputDir2, dataDir2, paraRunFile2, entityRunFile2, outFile2).feature();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "baseline2":
                System.out.println("Making baseline2...");
                String indexDir3 = args[1];
                String trecCarDir3 = args[2];
                String outputDir3 = args[3];
                String dataDir3 = args[4];
                String entityRunFile3 = args[5];
                String outFile3 = args[6];
                String a1 = args[7];
                String s3 = args[8];
                List<String> searchFields = null;
                if (args.length  > 9) {
                    searchFields = Arrays.asList(Arrays.copyOfRange(args, 9, args.length));
                }
                System.out.print("Searching fields: ");
                for (String field : searchFields) {
                    System.out.print(field + " ");
                }

                Analyzer analyzer1 = null;
                Similarity similarity = null;

                switch(a1)  {
                    case "eng" :
                        System.out.println("Using English analyzer.");
                        analyzer1 = new EnglishAnalyzer();
                        break;
                    case "std":
                        System.out.println("Using Standard analyzer.");
                        analyzer1 = new StandardAnalyzer();
                        break;
                    default:
                        System.out.println("Wrong analyzer choice! Can be either English(eng) or Standard(std)");
                }

                switch (s3) {
                    case "bm25" :
                        similarity = new BM25Similarity();
                        System.out.println("Using BM25.");
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
                    new Baseline2(indexDir3, trecCarDir3, outputDir3, dataDir3, entityRunFile3, "/home/shubham/Desktop/research/TREC_CAR/data/benchmarks/benchmarkY1/train/train.pages.cbor-article.entity.qrels", outFile3, analyzer1, similarity,searchFields).baseline();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "baseline3" :
                String indexDir4 = args[1];
                String trecCarDir4 = args[2];
                String outputDir4 = args[3];
                String dataDir4 = args[4];
                String paraRunFile4 = args[5];
                String entityRunFile4 = args[6];
                String outFile4 = args[7];

                try {
                    new Baseline3(indexDir4, trecCarDir4, outputDir4, dataDir4, paraRunFile4, entityRunFile4, outFile4).baseline();
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
                break;
            case "annotate":
                String cbor = args[1];
                String file = args[2];
                String option = args[3];
                if (option.equalsIgnoreCase("salient")) {
                    try {
                        new Annotate(cbor, file).annotateSalientEntities();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (option.equalsIgnoreCase("all")) {
                    try {
                        new Annotate(cbor, file).annotateAllEntities();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("Wrong option! Program exits!");
                    System.exit(1);
                }
                break;
            case "page-annotate":
                String cbor1 = args[1];
                String file1 = args[2];
                try {
                    new SWATAnnotatePages(cbor1,  file1).annotate();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
