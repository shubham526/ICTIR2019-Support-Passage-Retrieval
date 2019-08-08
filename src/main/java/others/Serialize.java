package others;
import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Class to store the entity links provided in the training data as a HashMap to disk.
 */
public class Serialize {
    private String paragraphCbor;
    private String serFile;
    private HashMap<String, List<String>> map;

    Serialize(String paragraphCbor, String serFile) {
        this.paragraphCbor = paragraphCbor;
        this.serFile = serFile;
        this.map = new HashMap<>();
        readCbor();
    }

    private void readCbor() {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(new File(paragraphCbor)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Iterable<Data.Paragraph> ip = DeserializeData.iterableParagraphs(bis);
        System.out.println("Annotating file: " + paragraphCbor);

        StreamSupport.stream(ip.spliterator(), true)
                .forEach(paragraph ->
                {
                    String paraID = paragraph.getParaId();
                    List<String> entityList = getEntityIdsOnly(paragraph);
                    System.out.println(entityList);
                    map.put(paraID, entityList);
                    System.out.println(paraID);
                    System.out.println("===================================================");
                });
        System.out.println("Writing data to file: " + serFile);
        try {
            Utilities.writeMap(map,serFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }
    @NotNull
    private List<String> getEntityIdsOnly(@NotNull Data.Paragraph p) {
        List<String> result = new ArrayList<>();
        for(Data.ParaBody body: p.getBodies()){
            if(body instanceof Data.ParaLink){
                result.add(((Data.ParaLink) body).getPageId());
            }
        }
        return result;
    }

    public static void main(@NotNull String[] args) {
        String cbor = args[0];
        String file = args[1];
        new Serialize(cbor, file);
    }
}
