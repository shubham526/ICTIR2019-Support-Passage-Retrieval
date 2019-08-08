package EntitySalience;

import edu.unh.cs.treccar_v2.Data;
import edu.unh.cs.treccar_v2.read_data.DeserializeData;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import support_passage.Utilities;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

public class SWATAnnotatePages {
    private String pageCbor;
    private String file;
    private HashMap<Data.Paragraph, Map<String, Double>> map;

    @Contract(pure = true)
    public SWATAnnotatePages(String pageCbor, String file) {
        this.pageCbor = pageCbor;
        this.file = file;
        this.map = new HashMap<>();
    }

    public void annotate() throws IOException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(pageCbor)));
        Iterable<Data.Page> ip = DeserializeData.iterableAnnotations(bis);
        System.out.println("Annotating file: " + pageCbor);

        StreamSupport.stream(ip.spliterator(), true)
                .forEach(page ->
                {
                    ArrayList<Data.Para> pageParas = getPageParas(page);
                    for (Data.Para paragraph: pageParas) {
                        Data.Paragraph p = paragraph.getParagraph();
                        String text = p.getTextOnly();
                        Map<String, Double> salMap = Saliency.getSalientEntities(text);
                        map.put(p, salMap);
                        System.out.println(paragraph.getParagraph().getParaId());
                    }
                });
        System.out.println("Writing data to file: " + file);
        Utilities.writeMap(map,file);
        System.out.println("Done");
    }
    @NotNull
    private ArrayList<Data.Para> getPageParas(@NotNull Data.Page page) {
        ArrayList<Data.Para> paragraphs = new ArrayList<>();
        for(Data.PageSkeleton skeleton: page.getSkeleton()) {
            if(skeleton instanceof Data.Para) {
                paragraphs.add((Data.Para) skeleton);
            }
        }
        return paragraphs;
    }

    public static void main(@NotNull String[] args) {
        String cbor = args[0];
        String file = args[1];
        try {
            new SWATAnnotatePages(cbor,  file).annotate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
