package org.apache.lucene.queryparser.surround.query;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queryparser.complexPhrase.TestComplexPhraseQuery;
import org.apache.lucene.queryparser.surround.parser.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.queryparser.xml.CoreParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;

public class TestSpanNot extends LuceneTestCase {
  Directory rd;
  Analyzer analyzer = new StandardAnalyzer();

  private IndexReader reader;
  private IndexSearcher searcher;

  private void addDoc(IndexWriter writer, String value) throws IOException {
    org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
    doc.add(newTextField("content", value, Field.Store.NO));
    writer.addDocument(doc);
  }

  public void setUp() throws Exception {
    rd = newDirectory();
    IndexWriter writer = new IndexWriter(rd, newIndexWriterConfig(analyzer));
    for (int i = 0; i < 100; i++) {
      addDoc(writer, "aaa");
      addDoc(writer, "aaa bbb");
      addDoc(writer, "aaa bbb ccc");
      addDoc(writer, "aaa bbb ccc ddd");
      addDoc(writer, "aaa bbb ccc ddd eee");
    }
    writer.close();

    reader = DirectoryReader.open(rd);
    searcher = newSearcher(reader);
    super.setUp();
  }

  public void testSurround() throws Exception {
    // org.apache.lucene.queryparser.surround.query.NotQuery
    // origin: (aaa 161N ccc)
    // step1: (aaa 10w ccc)
    // step2: ((aaa 10w ccc) NOT ddd)
    String str = "content:((aaa 10W ccc) NOT ddd)";
    SrndQuery query = QueryParser.parse(str);
    Query rewritten = query.makeLuceneQueryField("content", new BasicQueryFactory(1000)).rewrite(searcher);

    // origin: ((aaa OR bbb OR ccc) 161N (eee))
    // step1: ((aaa OR bbb OR ccc) 10w (eee))
    // step2: (((aaa OR bbb OR ccc) 10w (eee)) NOT ddd)
    str = "content: (((aaa OR bbb OR ccc) 10w (eee)) NOT ddd)";
    query = QueryParser.parse(str);
    rewritten = query.makeLuceneQueryField("content", new BasicQueryFactory(1000)).rewrite(searcher);

    // origin: ((eee) 161N (aaa OR bbb OR ccc))
    // step1: ((eee) 10w (aaa OR bbb OR ccc))
    // step2: (((eee) 10w (aaa OR bbb OR ccc)) NOT ddd)
    str = "content: (((eee) 10w (aaa OR bbb OR ccc)) NOT ddd)";
    query = QueryParser.parse(str);
    rewritten = query.makeLuceneQueryField("content", new BasicQueryFactory(1000)).rewrite(searcher);

    System.out.println();
  }

//  public static void main(String[] args) throws Exception {
//    reader = DirectoryReader.open(newDirectory());
//    searcher = newSearcher(reader);
//
//    testSurround();
//  }

  public static void testComplexPhraseQP() throws Exception {
    ComplexPhraseQueryParser claim = new ComplexPhraseQueryParser("claim", new StandardAnalyzer());
    String str = "(\"john^3 smit*\"~4)^2";
    Query query = claim.parse(str);

    str = "((\"耿 我\"~4) -点)";
    query = claim.parse(str);

    System.out.println("");
  }



  public static void testXml() throws Exception {
    // xml
    CoreParser coreParser = new CoreParser("claims", new StandardAnalyzer());
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    String str = "<SpanNot fieldName=\"headline\">\n"
        + "  <Include>\n"
        + "    <SpanTerm>new</SpanTerm>\n"
        + "  </Include>\n"
        + "  <Exclude fieldName=\"headline\">\n"
        + "    <SpanTerm>york</SpanTerm>\n"
        + "  </Exclude>\n"
        + "  <Pre>0</Pre>\n"
        + "  <Post>1</Post>\n"
        + "</SpanNot>";

    Document document = builder.parse(new InputSource(new StringReader(str)));
    SpanQuery spanQuery = coreParser.getSpanQuery(document.getDocumentElement());

    str = "<SpanNot fieldName=\"headline\">\n"
        + "  <Include>\n"
        + "<SpanNear fieldName=\"contents\" inOrder=\"false\">\n"
        + "  <SpanTerm>foo</SpanTerm>\n"
        + "  <SpanTerm>bar</SpanTerm>\n"
        + "</SpanNear>"
        + "  </Include>\n"
        + "  <Exclude fieldName=\"headline\">\n"
        + "    <SpanTerm>york</SpanTerm>\n"
        + "  </Exclude>\n"
        + "  <Pre>0</Pre>\n"
        + "  <Post>1</Post>\n"
        + "</SpanNot>";

    Document document2 = builder.parse(new InputSource(new StringReader(str)));
    SpanQuery spanQuery2 = coreParser.getSpanQuery(document.getDocumentElement());
    System.out.println();
  }

}
