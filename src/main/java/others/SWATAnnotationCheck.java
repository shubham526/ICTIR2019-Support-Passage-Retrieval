package others;

import support_passage.Utilities;

import java.io.IOException;
import java.util.*;

public class SWATAnnotationCheck {
    public static void main(String[] args) {
        String runFile = args[0];
        String writeFile = args[1];
        StringBuilder str;
        HashMap<String, Map<String, Double>> salMap = new HashMap<>();
        try {
            salMap = Utilities.readMap(runFile);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println("Read map");
        System.out.println("Map size = " + salMap.size());
        System.out.println("Printing top 10 passages--->");
        List<Map.Entry<String, Map<String, Double>>> entryList = new LinkedList<>(salMap.entrySet());
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Map.Entry<String, Map<String, Double>> entry = entryList.get(i);
            String paraID = entry.getKey();
            str = new StringBuilder();
            str.append("Paragraph: ").append(i + 1).append("\n");
            str.append("-----------------------------------------------------------------------------").append("\n");
            str.append(paraID).append("\n");
            str.append("-----------------------------------------------------------------------------").append("\n");
            System.out.println(paraID);
            Map<String, Double> ent = salMap.get(paraID);
            str.append("entities:").append("\n");
            str.append("-----------------------------------------------------------------------------").append("\n");
            if (ent != null) {
                for (String e : ent.keySet()) {
                    str.append(e).append("\n");
                    System.out.print(e + " " + ent.get(e));
                    System.out.println();
                }
                str.append("======================================================================================").append("\n");
                System.out.println("==========================================");
                list.add(str.toString());
            }
        }
        Utilities.writeFile(list, writeFile);
        System.out.println("File written");

    }
}
