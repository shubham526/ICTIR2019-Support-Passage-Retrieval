package help;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.TermQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EntityRMExpand {
    @Nullable
    public static BooleanQuery toEntityRmQuery(String queryStr,
                                               List<Map.Entry<String, Integer>> expansionEntities,
                                               boolean omitQueryTerms,
                                               Analyzer analyzer) throws IOException {
        BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
        List<String> tokens = new ArrayList<>(64);
        if (!omitQueryTerms) {
            tokens = tokenizeQuery(queryStr, "text", analyzer);
            for (String token : tokens) {
                booleanQuery.add(new BoostQuery(new TermQuery(new Term("text", token)), 1.0f),
                        BooleanClause.Occur.SHOULD);
            }
        }




        // add Entity RM terms
        for (Map.Entry<String, Integer> stringIntegerEntry : expansionEntities.subList(0, Math.min(expansionEntities.size(), (64 - tokens.size())))) {
            String e = stringIntegerEntry.getKey().replaceAll("_", " ");
            List<String> entityToks = tokenizeQuery(e, "text", analyzer);
            for (String entity : entityToks) {
                float weight = stringIntegerEntry.getValue();
                booleanQuery.add(new BoostQuery(new TermQuery(new Term("text", entity)), weight),
                        BooleanClause.Occur.SHOULD);
            }
        }

        return booleanQuery.build();
    }
    @NotNull
    private static List<String> tokenizeQuery(String queryStr, String searchField,
                                              @NotNull Analyzer analyzer) throws IOException {
        TokenStream tokenStream = analyzer.tokenStream(searchField, new StringReader(queryStr));
        List<String> tokens = new ArrayList<>();
        tokenStream.reset();
        while (tokenStream.incrementToken() && tokens.size() < 64) {
            final String token = tokenStream.getAttribute(CharTermAttribute.class).toString();
            tokens.add(token);
        }
        tokenStream.end();
        tokenStream.close();
        return tokens;
    }
}



