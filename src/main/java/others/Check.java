package others;

import support_passage.Utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Check {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String file = "/home/shubham/Desktop/research/TREC_CAR/data/train.pages.cbor-paragraphs.cbor.entity-links.ser";
        /*HashMap<String, HashMap<String, Double>> myMap = Utilities.readMap(file);


        for (String s : myMap.keySet()) {
            System.out.println(s);
            System.out.println("============================================");
            HashMap<String, Double> myList = myMap.get(s);
            for (String s1 : myList.keySet()) {
                System.out.println(s1 + " : " + myList.get(s1));
                System.out.println("---------------------");
            }
        }*/
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        HashMap<String, ArrayList<String>> myMap = Utilities.readMap(file);
        for (String s : myMap.keySet()) {
            System.out.println(s);
            System.out.println("==========================================");
            ArrayList<String> myList = myMap.get(s);
            for (String s1 : myList) {
                System.out.println(s1);
                System.out.println("------------------------");

            }
            br.readLine();

        }
    }
}
