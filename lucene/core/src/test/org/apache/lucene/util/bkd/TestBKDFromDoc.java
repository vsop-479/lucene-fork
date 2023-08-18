package org.apache.lucene.util.bkd;

import org.apache.lucene.codecs.lucene95.Lucene95Codec;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PointRangeQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import java.io.IOException;
import java.util.Random;

public class TestBKDFromDoc extends LuceneTestCase {

	public void testAddBKDDoc() throws IOException {
		final Directory dir = newDirectory();
		IndexWriterConfig iwc = new IndexWriterConfig();
		iwc.setCodec(new Lucene95Codec());
		final IndexWriter writer = new IndexWriter(dir, iwc);
		int[] val;
		val = new int[500];
//		val = new int[]{5, 3, 8, 8, 3, 2, 1, 9, 0, 4};
		Random random = new Random();
		for (int i = 0; i < val.length; ++i) {
			final Document doc = new Document();
			doc.add(new IntPoint("height", random.nextInt(10000)));
			writer.addDocument(doc);
		}
		Document doc = new Document();
		doc.add(new IntPoint("height", 4));
		writer.addDocument(doc);
		doc = new Document();
		doc.add(new IntPoint("height", 5));
		writer.addDocument(doc);
		doc = new Document();
		doc.add(new IntPoint("height", 6));
		writer.addDocument(doc);
		writer.flush();
		final DirectoryReader reader = DirectoryReader.open(writer);
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		TopDocs topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 3, 7), 10);
		writer.close();
		reader.close();
		dir.close();

	}

	public void testDirectWrite() throws IOException {
		final Directory dir = newDirectory();
		IndexWriterConfig iwc = new IndexWriterConfig();
		iwc.setCodec(new Lucene95Codec());
		final IndexWriter writer = new IndexWriter(dir, iwc);
		int[] val;
		val = new int[200];
		Random random = new Random();
		for (int i = 0; i < val.length; ++i) {
			final Document doc = new Document();
			doc.add(new IntPoint("height", random.nextInt(255)));
			writer.addDocument(doc);
		}
		writer.flush();
		final DirectoryReader reader = DirectoryReader.open(writer);
		writer.close();
		reader.close();
		dir.close();

	}
}
