package org.apache.lucene.queryparser.surround.query;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import java.io.IOException;

public class NotRewriteQuery extends RewriteQuery<NotQuery>{
  NotRewriteQuery(NotQuery srndQuery, String fieldName, BasicQueryFactory qf) {
    super(srndQuery, fieldName, qf);
  }

  @Override
  public Query rewrite(IndexSearcher indexSearcher) throws IOException {
    return srndQuery.getSpanNotQuery(indexSearcher.getIndexReader(), fieldName, qf);
  }

  @Override
  public void visit(QueryVisitor visitor) {
    // TODO implement this
    visitor.visitLeaf(this);
  }
}
