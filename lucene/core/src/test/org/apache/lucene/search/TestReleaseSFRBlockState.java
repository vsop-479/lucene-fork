package org.apache.lucene.search;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.tests.util.TestUtil;

import java.io.IOException;

/**
 * since 8.7: only merge use blockState's bytes.
 * https://issues.apache.org/jira/browse/LUCENE-9483
 * 1. fetch: only decompress one doc.
 * 2. merge: decompress the whole chunk.
 */
public class TestReleaseSFRBlockState extends LuceneTestCase {
    /**
     * check whether BlockState release after segment read.
     */
    public void testBlockStateRelease() throws IOException {
        final Directory dir = newDirectory();
        IndexWriterConfig config =
                new IndexWriterConfig()
                        // Make sure to use the default codec, otherwise some random points formats that have
                        // large values for maxPointsPerLeaf might not enable skipping with only 10k docs
                        .setCodec(TestUtil.getDefaultCodec());
        final IndexWriter writer = new IndexWriter(dir, config);
        // seg1.
        writer.addDocument(generateDoc("ai", 5));
        writer.addDocument(generateDoc("mi", 6));
        writer.addDocument(generateDoc("fi", 2));
        writer.addDocument(generateDoc("di", 3));
        writer.flush();
        // seg2.
        writer.addDocument(generateDoc("ai", 2));
        writer.addDocument(generateDoc("mi", 4));
        writer.addDocument(generateDoc("fi", 3));
        writer.addDocument(generateDoc("di", 1));
        writer.flush();
        // seg3.
        writer.addDocument(generateDoc("ai", 8));
        writer.addDocument(generateDoc("mi", 4));
        writer.addDocument(generateDoc("fi", 6));
        writer.addDocument(generateDoc("di", 3));
        writer.flush();

        DirectoryReader reader = DirectoryReader.open(writer);
        IndexSearcher searcher = newSearcher(reader, false, true, false);
        TermQuery termQuery = new TermQuery(new Term("name", "fi"));
        TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), 10);
        System.out.println("totalHits: " + topDocs.totalHits.value);

        DocumentStoredFieldVisitor visitor = new DocumentStoredFieldVisitor("name", "age");
        // avoid sfr cloned by SegmentReader.getFieldsReader.
        StoredFields storedFields = reader.storedFields();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            System.out.println("get doc: " + scoreDoc.doc);
            storedFields.document(scoreDoc.doc, visitor);
            String name = visitor.getDocument().get("name");
            System.out.println("get doc name: " + name);
        }

        // delete doc，使merge时，采用MergeStrategy.DOC策略，触发reader读取doc。
        // 否则，使用MergeStrategy.BULK策略，不用解压，直接copy chunk。
        writer.deleteDocuments(termQuery);
        writer.forceMerge(1);

        writer.close();
        reader.close();
        dir.close();
    }

    public Document generateDoc(String name, int age){
        Document doc = new Document();
        doc.add(new StringField("name", name, Field.Store.YES));
        // for indexing(bkd)
        doc.add(new IntPoint("age", age));
        // for store
        doc.add(new StoredField("age", age));
        // for sort, retrieval.
        doc.add(new IntField("age", age));
        return doc;
    }
}
