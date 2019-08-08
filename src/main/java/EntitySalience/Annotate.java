package EntitySalience;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.jetbrains.annotations.Contract;
import org.json.JSONObject;
import support_passage.Utilities;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class Annotate {
    private String cbor;
    private String file;
    private HashMap<String, Map<String, Double>> salientEntityMap;
    private HashMap<String, ArrayList<String>> allEntityMap;

    @Contract(pure = true)
    public Annotate(String cbor, String file) {
        this.cbor = cbor;
        this.file = file;
        this.salientEntityMap = new HashMap<>();
        this.allEntityMap = new HashMap<>();
    }

    public void annotateSalientEntities() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(cbor)));
        Iterable<Data.Paragraph> ip = DeserializeData.iterableParagraphs(bis);
        System.out.println("Annotating file: " + cbor);
        System.out.println("Annotating salient entities");

        StreamSupport.stream(ip.spliterator(), true)
                .forEach(paragraph ->
                {
                   String text = paragraph.getTextOnly();
                   Map<String, Double> salMap = Saliency.getSalientEntities(text);
                    salientEntityMap.put(paragraph.getParaId(), salMap);
                    System.out.println(paragraph.getParaId());
                });
        System.out.println("Writing data to file: " + file);
         Utilities.writeMap(salientEntityMap,file);
        System.out.println("Done");
    }

    public void annotateAllEntities() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(cbor)));
        Iterable<Data.Paragraph> ip = DeserializeData.iterableParagraphs(bis);
        System.out.println("Annotating file: " + cbor);
        System.out.println("Annotating all entities");

        StreamSupport.stream(ip.spliterator(), true)
                .forEach(paragraph ->
                {
                    String text = paragraph.getTextOnly();
                    ArrayList<String> allEntityList = Saliency.getAllEntities(text);
                    allEntityMap.put(paragraph.getParaId(), allEntityList);
                    System.out.println(paragraph.getParaId());
                });
        System.out.println("Writing data to file: " + file);
        Utilities.writeMap(allEntityMap,file);
        System.out.println("Done");
    }

    public static void main(String[] args) {
        String cbor = args[0];
        String file = args[1];
        try {
            new Annotate(cbor,  file).annotateSalientEntities();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
