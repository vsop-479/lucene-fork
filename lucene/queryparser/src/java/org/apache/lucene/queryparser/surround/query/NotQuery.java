/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.queryparser.surround.query;

import java.io.IOException;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.spans.SpanNearQuery;
import org.apache.lucene.queries.spans.SpanNotQuery;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;

/** Factory for prohibited clauses */
public class NotQuery extends ComposedQuery {
  public NotQuery(List<SrndQuery> queries, String opName) {
    super(queries, true /* infix */, opName);
  }

//  @Override
//  public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
//    List<Query> luceneSubQueries = makeLuceneSubQueriesField(fieldName, qf);
//    BooleanQuery.Builder bq = new BooleanQuery.Builder();
//    bq.add(luceneSubQueries.get(0), BooleanClause.Occur.MUST);
//    SrndBooleanQuery.addQueriesToBoolean(
//        bq,
//        // FIXME: do not allow weights on prohibited subqueries.
//        luceneSubQueries.subList(1, luceneSubQueries.size()),
//        // later subqueries: not required, prohibited
//        BooleanClause.Occur.MUST_NOT);
//    return bq.build();
//  }

  @Override
  public Query makeLuceneQueryFieldNoBoost(String fieldName, BasicQueryFactory qf) {
    return new NotRewriteQuery(this, fieldName, qf);
  }

  public Query getSpanNotQuery(IndexReader reader, String fieldName, BasicQueryFactory qf) throws IOException {
    List<Query> luceneSubQueries = makeLuceneSubQueriesField(fieldName, qf);
    Query query = luceneSubQueries.get(0);

    // TODO: support more query types for include query.
    assert query instanceof DistanceRewriteQuery;
    if (query instanceof DistanceRewriteQuery) {
      DistanceRewriteQuery dQ = (DistanceRewriteQuery) query;
      // include query.
      Query spanNearQuery = dQ.srndQuery.getSpanNearQuery(reader, fieldName, qf);
      // if one term match no docs, we will get a MatchNoDocsQuery from getSpanNearQuery.
      if (spanNearQuery instanceof SpanNearQuery == false) {
        return new MatchNoDocsQuery();
      }

      // TODO: support more query types for exclude query.
      Query notQuery = luceneSubQueries.get(1);
      if (notQuery instanceof SimpleTermRewriteQuery) {
        SimpleTermRewriteQuery sQ = (SimpleTermRewriteQuery) notQuery;
        SpanTermQuery spanTermQuery =
            new SpanTermQuery(new Term(fieldName, ((SrndTermQuery) sQ.srndQuery).getTermText()));
        return new SpanNotQuery((SpanNearQuery)spanNearQuery, spanTermQuery);
      }
    }
    return new MatchNoDocsQuery();
  }
}
