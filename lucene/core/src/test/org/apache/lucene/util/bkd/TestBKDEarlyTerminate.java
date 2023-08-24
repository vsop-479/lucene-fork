package org.apache.lucene.util.bkd;

import org.apache.lucene.codecs.lucene95.Lucene95Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;

import java.io.IOException;
import java.util.Random;

public class TestBKDEarlyTerminate extends LuceneTestCase {
  public void testAddBKDDoc() throws IOException {
    final Directory dir = newDirectory();
    IndexWriterConfig iwc = new IndexWriterConfig();
    iwc.setCodec(new Lucene95Codec());
    final IndexWriter writer = new IndexWriter(dir, iwc);
    int[] val;
    val = new int[1000];
    val = new int[]{5, 3, 8, 8, 3, 2, 1, 9, 0, 4};

    Random random = new Random(123);
    for (int i = 0; i < val.length; ++i) {
      val[i] = random.nextInt(100);
      final Document doc = new Document();
      doc.add(new IntPoint("height", val[i]));
      writer.addDocument(doc);
    }
    writer.flush();
    final DirectoryReader reader = DirectoryReader.open(writer);
    IndexSearcher indexSearcher = new IndexSearcher(reader);
    indexSearcher.search(IntPoint.newRangeQuery("height", 3, 7), 10);
    long t0 = System.nanoTime();
    TopDocs topDocs;
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 3, 7), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 14, 19), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 21, 27), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 34, 37), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 41, 49), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 54, 57), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 61, 68), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 73, 79), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 84, 89), 10);
    topDocs = indexSearcher.search(IntPoint.newRangeQuery("height", 90, 99), 10);
    long t1 = System.nanoTime();
    System.out.println(topDocs.totalHits.value);
    System.out.println("took: " + (t1 - t0));
    writer.close();
    reader.close();
    dir.close();
  }

  public void testDirectWrite() throws IOException {
    final Directory dir = LuceneTestCase.newDirectory();
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
