package org.elasticsearch.classification;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.classification.ClassificationResult;
import org.apache.lucene.classification.Classifier;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.index.mapper.DocumentMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.Mapping;
import org.elasticsearch.index.mapper.ParsedDocument;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.elasticsearch.index.mapper.SourceToParse.source;

/**
 * A k-Nearest Neighbor classifier (see <code>http://en.wikipedia.org/wiki/K-nearest_neighbors</code>) based
 * on {@link MoreLikeThis}
 * <p/>
 *
 * A minimally modified copy of {@link org.apache.lucene.classification.KNearestNeighborClassifier}
 */
public class KNearestNeighborClassifier implements Classifier<BytesRef> {

    private final int k;
    private MoreLikeThis mlt;
    private String[] textFieldNames;
    private String classFieldName;
    private IndexSearcher indexSearcher;
    private Query query;

    private int minDocsFreq;
    private int minTermFreq;

    private MapperService mapperService;
    private String index;
    private String type;

    /**
     * Create a {@link Classifier} using kNN algorithm
     *
     * @param k the number of neighbors to analyze as an <code>int</code>
     */
    public KNearestNeighborClassifier(int k) {
        this.k = k;
    }

    /**
     * Create a {@link Classifier} using kNN algorithm
     *
     * @param k           the number of neighbors to analyze as an <code>int</code>
     * @param minDocsFreq the minimum number of docs frequency for MLT to be set with {@link MoreLikeThis#setMinDocFreq(int)}
     * @param minTermFreq the minimum number of term frequency for MLT to be set with {@link MoreLikeThis#setMinTermFreq(int)}
     */
    public KNearestNeighborClassifier(int k, int minDocsFreq, int minTermFreq) {
        this.k = k;
        this.minDocsFreq = minDocsFreq;
        this.minTermFreq = minTermFreq;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassificationResult<BytesRef> assignClass(String text) throws IOException {
        TopDocs topDocs = knnSearcher(text);
        List<ClassificationResult<BytesRef>> doclist = buildListFromTopDocs(topDocs);
        ClassificationResult<BytesRef> retval = null;
        double maxscore = -Double.MAX_VALUE;
        for (ClassificationResult<BytesRef> element : doclist) {
            if (element.getScore() > maxscore) {
                retval = element;
                maxscore = element.getScore();
            }
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ClassificationResult<BytesRef>> getClasses(String text) throws IOException {
        TopDocs topDocs = knnSearcher(text);
        List<ClassificationResult<BytesRef>> doclist = buildListFromTopDocs(topDocs);
        Collections.sort(doclist);
        return doclist;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ClassificationResult<BytesRef>> getClasses(String text, int max) throws IOException {
        TopDocs topDocs = knnSearcher(text);
        List<ClassificationResult<BytesRef>> doclist = buildListFromTopDocs(topDocs);
        Collections.sort(doclist);
        return doclist.subList(0, max);
    }

    private TopDocs knnSearcher(String text) throws IOException {
        if (mlt == null) {
            throw new IOException("You must first call Classifier#train");
        }
        BooleanQuery mltQuery = new BooleanQuery();
        for (String textFieldName : textFieldNames) {
            mltQuery.add(new BooleanClause(mlt.like(textFieldName, new StringReader(text)), BooleanClause.Occur.SHOULD));
        }
        Query classFieldQuery = new WildcardQuery(new Term(classFieldName, "*"));
        mltQuery.add(new BooleanClause(classFieldQuery, BooleanClause.Occur.MUST));
        if (query != null) {
            mltQuery.add(query, BooleanClause.Occur.MUST);
        }
        return indexSearcher.search(mltQuery, k);
    }

    private List<ClassificationResult<BytesRef>> buildListFromTopDocs(TopDocs topDocs) throws IOException {
        Map<BytesRef, Integer> classCounts = new HashMap<>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            BytesRef cl = getClassNameFromSource(indexSearcher.doc(scoreDoc.doc).getField("_source"));
            Integer count = classCounts.get(cl);
            if (count != null) {
                classCounts.put(cl, count + 1);
            } else {
                classCounts.put(cl, 1);
            }
        }
        List<ClassificationResult<BytesRef>> returnList = new ArrayList<>();
        int sumdoc = 0;
        for (Map.Entry<BytesRef, Integer> entry : classCounts.entrySet()) {
            Integer count = entry.getValue();
            returnList.add(new ClassificationResult<>(entry.getKey().clone(), count / (double) k));
            sumdoc += count;

        }

        //correction
        if (sumdoc < k) {
            for (ClassificationResult<BytesRef> cr : returnList) {
                cr.setScore(cr.getScore() * (double) k / (double) sumdoc);
            }
        }
        return returnList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void train(LeafReader leafReader, String textFieldName, String classFieldName, Analyzer analyzer) throws IOException {
        train(leafReader, textFieldName, classFieldName, analyzer, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void train(LeafReader leafReader, String textFieldName, String classFieldName, Analyzer analyzer, Query query) throws IOException {
        train(leafReader, new String[]{textFieldName}, classFieldName, analyzer, query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void train(LeafReader leafReader, String[] textFieldNames, String classFieldName, Analyzer analyzer, Query query) throws IOException {
        this.textFieldNames = textFieldNames;
        this.classFieldName = classFieldName;
        mlt = new MoreLikeThis(leafReader);
        mlt.setAnalyzer(analyzer);
        mlt.setFieldNames(textFieldNames);
        indexSearcher = new IndexSearcher(leafReader);
        if (minDocsFreq > 0) {
            mlt.setMinDocFreq(minDocsFreq);
        }
        if (minTermFreq > 0) {
            mlt.setMinTermFreq(minTermFreq);
        }
        this.query = query;
    }

    // the following methods were added to get the class name from source

    public void setMapperService(MapperService mapperService, String index, String type) {
        this.mapperService = mapperService;
        this.index = index;
        this.type = type;
    }

    private BytesRef getClassNameFromSource(IndexableField source) {
        ParsedDocument parsedDocument = parseDocument(index, type, new BytesArray(source.binaryValue()));
        return new BytesRef(parsedDocument.rootDoc().getField(classFieldName).stringValue());
    }

    private ParsedDocument parseDocument(String index, String type, BytesReference doc) {
        Tuple<DocumentMapper, Mapping> docMapper = mapperService.documentMapperWithAutoCreate(type);
        return docMapper.v1().parse(source(doc).index(index).type(type).flyweight(true));
    }
}
