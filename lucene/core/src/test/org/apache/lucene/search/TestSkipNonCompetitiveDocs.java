package org.apache.lucene.search;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.tests.util.TestUtil;

import java.io.IOException;

/**
 * TestSortOptimization.
 */
public class TestSkipNonCompetitiveDocs extends LuceneTestCase {
	/**
	 * optimize numeric sort by using bkd-tree.
	 */
    public void testNumericSort() throws IOException {
        final Directory dir = newDirectory();
        IndexWriterConfig config =
                new IndexWriterConfig()
                        // Make sure to use the default codec, otherwise some random points formats that have
                        // large values for maxPointsPerLeaf might not enable skipping with only 10k docs
                        .setCodec(TestUtil.getDefaultCodec());
        final IndexWriter writer = new IndexWriter(dir, config);
        final int numDocs = atLeast(10000);
        for (int i = 0; i < numDocs; i++) {
            final Document doc = new Document();
            // doc values for sort, agg.
            doc.add(new NumericDocValuesField("my_field", i));
            // index(bkd-tree) for search(range).
            doc.add(new LongPoint("my_field", i));
            writer.addDocument(doc);
            if (i == 7000) writer.flush();
        }
        final IndexReader reader = DirectoryReader.open(writer);
        writer.close();

        IndexSearcher searcher = newSearcher(reader, true, true, false);
        final SortField sortField = new SortField("my_field", SortField.Type.LONG);
        Sort sort = new Sort(sortField);
        final int numHits = 3;
        final int totalHitsThreshold = 3;

        { // simple sort
					CollectorManager<TopFieldCollector, TopFieldDocs> manager =
							TopFieldCollector.createSharedManager(sort, numHits, null, totalHitsThreshold);
					TopFieldDocs topDocs = searcher.search(new MatchAllDocsQuery(), manager);
					assertEquals(topDocs.scoreDocs.length, numHits);
					for (int i = 0; i < numHits; i ++) {
						FieldDoc fieldDoc = (FieldDoc)topDocs.scoreDocs[i];
						assertEquals(i, ((Long) fieldDoc.fields[0]).intValue());
					}
					assertEquals(TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO, topDocs.totalHits.relation);
					assertNonCompetitiveHitsAreSkipped(topDocs.totalHits.value, numDocs);
				}

		reader.close();
		dir.close();
    }

	/**
	 * optimize sort set sort by using postings.
	 */
	public void testStringSortSort() {

	}

	private void assertNonCompetitiveHitsAreSkipped(long collectedHits, long numDocs) {
		if (collectedHits >= numDocs) {
			fail(
					"Expected some non-competitive hits are skipped; got collected_hits="
							+ collectedHits
							+ " num_docs="
							+ numDocs);
		}
	}
}
