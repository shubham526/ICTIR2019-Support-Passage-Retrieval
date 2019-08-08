package cross_validation;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import support_passage.Utilities;

/**
 * Class to divide a support passage run file into 5 folds for 5-fold CV based on benchmarkY1-train
 * @author Shubham Chatterjee
 *
 */
public class CreateFolds {
    // Path to the output directory
    // The folds for each run file will also be stored here.
    private String OUTPUT_DIR;

    // Path to the directory containing run files which need to be divided into folds.
    private String RUN_DIR;

    // Name of file which need to be split into folds
    private String RUN_FILE;

    // Path to the benchmarkY1-train directory
    private String BENCHMARK_DIR;

    // Type of the run file. Can be: page,section
    private String TYPE;

    /**
     * Constructor.
     * @param od Path to the output directory
     * @param rd Path to the directory containing run files which need to be divided into folds
     * @param rf Name of file which need to be split into folds
     * @param bd Path to the benchmarkY1-train directory
     * @param t Type of the run file. Can be: page,section
     */

    public CreateFolds(String od, String rd, String rf, String bd, String t) {
        OUTPUT_DIR = od;
        RUN_DIR = rd;
        RUN_FILE = rf;
        BENCHMARK_DIR = bd;
        TYPE = t;

        System.out.println("Creating folds...");
        createFolds();
        System.out.println("Done");
    }

    private void createFolds(ArrayList<String> queryList, String foldRunFilePath) {
        String runFilePath = RUN_DIR+"/"+RUN_FILE;
        BufferedReader br = null;
        String line;
        ArrayList<String> runStrings = new ArrayList<>();
        try {
            br = new BufferedReader(new FileReader(runFilePath));
            while((line = br.readLine()) != null) {
                String query = line.split(" ")[0];
                String queryID = query.split("\\+")[0];
                if(queryList.contains(queryID))
                    runStrings.add(line);
            }
            //Utilities.writeFile(runStrings,RUN_DIR + "/" + foldRunFileName);
            Utilities.writeFile(runStrings,foldRunFilePath);
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
     * Create folds for the run file
     */
    private void createFolds() {
        for(int i = 0; i < 5; i++ ) {
            String foldFileName = "fold-" + i + "-train.pages.cbor-outlines.cbor";
            String foldRunFileName = "fold-" + i + "-" + RUN_FILE;
            String foldRunFilePath = OUTPUT_DIR + "/" + "fold-" + i + "/" + foldRunFileName;
            ArrayList<String> queryList = getQueryList(BENCHMARK_DIR+"/"+foldFileName);
            createFolds(queryList,foldRunFilePath);
            System.out.println("Done creating fold-" + i + " " + "for file: " + RUN_FILE + " at location: " + foldRunFilePath);
        }
    }
    /**
     * Get the list of pages from the cbor file
     */
    private ArrayList<Data.Page> getPageList(String path) {
        ArrayList<Data.Page> pageList = new ArrayList<>();
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(path)));
            for(Data.Page page: DeserializeData.iterableAnnotations(bis)) {
                pageList.add(page);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return pageList;
    }
    /**
     * Get the list of queries from the fold
     */
    private ArrayList<String> getQueryList(String path) {
        ArrayList<String> list = new ArrayList<>();
        ArrayList<Data.Page> pageList = getPageList(path);

        if(TYPE.equalsIgnoreCase("page")) {
            for(Data.Page page: pageList) {
                list.add(page.getPageId());
            }
        } else {
            for(Data.Page page: pageList) {
                for (List<Data.Section> sectionPath : page.flatSectionPaths()) {
                    list.add(Data.sectionPathId(page.getPageId(), sectionPath));
                }
            }
        }

        return list;
    }

    public static void  main(String args[]) throws IOException {
        String output_dir = args[0];
        String run_dir = args[1];
        String run_file = args[2];
        String benchmark_dir = args[3];
        String type = args [4];
        new CreateFolds(output_dir, run_dir, run_file, benchmark_dir,type);
    }

}
