package help;

import lucene.RAMIndex;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Expand a query with words using RM3.
 * @author Shubham Chatterjee
 * @version 03/13/2019
 * The original code was due to Dr. Laura Dietz, University of New Hampshire, USA.
 * The code has been adapted by the author and modified to suit the needs of the project.
 */

public class RM3Expand {

    @NotNull
    public static List<Map.Entry<String, Float>> getExpansionTerms(IndexSearcher searcher,
                                                                   int takeKTerms,
                                                                   int takeKDocs,
                                                                   String queryStr,
                                                                   boolean omitQueryTerms,
                                                                   Analyzer analyzer) throws IOException {
        final BooleanQuery booleanQuery = toQuery(queryStr, analyzer);
        HashMap<Document, Float> results = RAMIndex.searchIndex(booleanQuery, takeKDocs, searcher);
        //System.out.println("Found " + results.size() + " initial results.");

        final Map<String, Float> wordFreqs = new HashMap<>();

        if(!omitQueryTerms) {
            addTokens(queryStr, 1.0f, wordFreqs, analyzer);
        }

        // guess if we have log scores...
        boolean useLog = false;
        for (Document d : results.keySet()) {
            if (results.get(d) < 0.0) {
                useLog = true;
                break;
            }
        }

        // compute score normalizer
        double normalizer = 0.0;
        for (Document d : results.keySet()) {
            if (useLog) {
                normalizer += Math.exp(results.get(d));
            } else {
                normalizer += results.get(d);
            }
        }
        if (useLog) {
            normalizer = Math.log(normalizer);
        }

        for (Document d : results.keySet()) {
            Double weight = useLog ? (results.get(d) - normalizer) : (results.get(d) / normalizer);
            String docContents = d.get("text");
            addTokens(docContents, weight.floatValue(), wordFreqs, analyzer);
        }

        ArrayList<Map.Entry<String, Float>> allWordFreqs = new ArrayList<>(wordFreqs.entrySet());
        allWordFreqs.sort((kv1, kv2) -> {
            return Float.compare(kv2.getValue(), kv1.getValue()); // sort descending by flipping
        });

        List<Map.Entry<String, Float>> expansionTerms = allWordFreqs.subList(0, Math.min(takeKTerms, allWordFreqs.size()));

        //System.out.println("RM3 Expansions for \""+queryStr+ "\": "+expansionTerms.toString());
        return expansionTerms;

    }
    private static BooleanQuery toQuery(String queryStr, Analyzer analyzer) throws IOException {
        List<String> tokens = new ArrayList<>();

        tokenizeQuery(queryStr, "text", tokens, analyzer);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        for (String token : tokens) {
            booleanQuery.add(new TermQuery(new Term("text", token)), BooleanClause.Occur.SHOULD);
        }
        return booleanQuery.build();
    }
    private static void tokenizeQuery(String queryStr, String searchField, @NotNull List<String> tokens, @NotNull Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream(searchField, new StringReader(queryStr));
        tokenStream.reset();
        tokens.clear();
        while (tokenStream.incrementToken() && tokens.size() < 64)
        {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            tokens.add(token);
        }
        tokenStream.end();
        tokenStream.close();
    }
    private static void addTokens(String content, Float weight, Map<String,Float> wordFreqs, @NotNull Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(content));
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            wordFreqs.compute(token, (t, oldV) ->
                    (oldV == null)? weight : oldV + weight
            );
        }
        tokenStream.end();
        tokenStream.close();
    }
    public static BooleanQuery toRm3Query(String queryStr, List<Map.Entry<String, Float>> relevanceModel, Analyzer analyzer) throws IOException {
        List<String> tokens = new ArrayList<>();
        tokenizeQuery(queryStr, "text", tokens, analyzer);
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();

        // Add original query terms
        for (String token : tokens) {
            booleanQuery.add(new BoostQuery(new TermQuery(new Term("text", token)),1.0f), BooleanClause.Occur.SHOULD);
        }


        // add RM3 terms
        for (Map.Entry<String, Float> stringFloatEntry : relevanceModel.subList(0, Math.min(relevanceModel.size(), (64 - tokens.size())))) {
            String token = stringFloatEntry.getKey();
            float weight = stringFloatEntry.getValue();
            booleanQuery.add(new BoostQuery(new TermQuery(new Term("text", token)),weight), BooleanClause.Occur.SHOULD);
        }
        return booleanQuery.build();
    }



}

