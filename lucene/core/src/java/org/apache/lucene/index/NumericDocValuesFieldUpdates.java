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
package org.apache.lucene.index;

import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.util.BitSet;
import org.apache.lucene.util.BitSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.RamUsageEstimator;
import org.apache.lucene.util.SparseFixedBitSet;
import org.apache.lucene.util.packed.AbstractPagedMutable;
import org.apache.lucene.util.packed.PackedInts;
import org.apache.lucene.util.packed.PagedGrowableWriter;
import org.apache.lucene.util.packed.PagedMutable;

/**
 * A {@link DocValuesFieldUpdates} which holds updates of documents, of a single {@link
 * NumericDocValuesField}.
 *
 * @lucene.experimental
 */
final class NumericDocValuesFieldUpdates extends DocValuesFieldUpdates {
  // TODO: can't this just be NumericDocValues now?  avoid boxing the long value...
  static final class Iterator extends DocValuesFieldUpdates.AbstractIterator {
    private final AbstractPagedMutable<?> values;
    private final long minValue;
    private long value;

    Iterator(
        int size, long minValue, AbstractPagedMutable<?> values, PagedMutable docs, long delGen) {
      super(size, docs, delGen);
      this.values = values;
      this.minValue = minValue;
    }

    @Override
    long longValue() {
      return value;
    }

    @Override
    BytesRef binaryValue() {
      throw new UnsupportedOperationException();
    }

    @Override
    protected void set(long idx) {
      value = values.get(idx) + minValue;
    }
  }

  private AbstractPagedMutable<?> values;
  private final long minValue;

  NumericDocValuesFieldUpdates(long delGen, String field, int maxDoc) {
    super(maxDoc, delGen, field, DocValuesType.NUMERIC);
    // we don't know the min/max range so we use the growable writer here to adjust as we go.
    values = new PagedGrowableWriter(1, PAGE_SIZE, 1, PackedInts.DEFAULT);
    minValue = 0;
  }

  NumericDocValuesFieldUpdates(
      long delGen, String field, long minValue, long maxValue, int maxDoc) {
    super(maxDoc, delGen, field, DocValuesType.NUMERIC);
    assert minValue <= maxValue
        : "minValue must be <= maxValue [" + minValue + " > " + maxValue + "]";
    int bitsPerValue = PackedInts.unsignedBitsRequired(maxValue - minValue);
    values = new PagedMutable(1, PAGE_SIZE, bitsPerValue, PackedInts.DEFAULT);
    this.minValue = minValue;
  }

  @Override
  void add(int doc, BytesRef value) {
    throw new UnsupportedOperationException();
  }

  @Override
  void add(int docId, DocValuesFieldUpdates.Iterator iterator) {
    add(docId, iterator.longValue());
  }

  @Override
  synchronized void add(int doc, long value) {
    int add = add(doc);
    values.set(add, value - minValue);
  }

  @Override
  protected void swap(int i, int j) {
    super.swap(i, j);
    long tmpVal = values.get(j);
    values.set(j, values.get(i));
    values.set(i, tmpVal);
  }

  @Override
  protected void grow(int size) {
    super.grow(size);
    values = values.grow(size);
  }

  @Override
  protected void resize(int size) {
    super.resize(size);
    values = values.resize(size);
  }

  @Override
  Iterator iterator() {
    ensureFinished();
    return new Iterator(size, minValue, values, docs, delGen);
  }

  @Override
  public long ramBytesUsed() {
    return values.ramBytesUsed()
        + super.ramBytesUsed()
        + Long.BYTES
        + RamUsageEstimator.NUM_BYTES_OBJECT_REF;
  }

  static class SingleValueNumericDocValuesFieldUpdates extends DocValuesFieldUpdates {

    private final long value;
    private final BitSet bitSet;
    private BitSet hasNoValue;
    private boolean hasAtLeastOneValue;

    SingleValueNumericDocValuesFieldUpdates(long delGen, String field, int maxDoc, long value) {
      super(maxDoc, delGen, field, DocValuesType.NUMERIC);
      this.bitSet = new SparseFixedBitSet(maxDoc);
      this.value = value;
    }

    // pkg private for testing
    long longValue() {
      return value;
    }

    @Override
    void add(int doc, long value) {
      assert this.value == value;
      bitSet.set(doc);
      this.hasAtLeastOneValue = true;
      if (hasNoValue != null) {
        hasNoValue.clear(doc);
      }
    }

    @Override
    void add(int doc, BytesRef value) {
      throw new UnsupportedOperationException();
    }

    @Override
    synchronized void reset(int doc) {
      bitSet.set(doc);
      this.hasAtLeastOneValue = true;
      if (hasNoValue == null) {
        hasNoValue = new SparseFixedBitSet(maxDoc);
      }
      hasNoValue.set(doc);
    }

    @Override
    void add(int docId, Iterator iterator) {
      throw new UnsupportedOperationException();
    }

    @Override
    synchronized boolean any() {
      return super.any() || hasAtLeastOneValue;
    }

    @Override
    public long ramBytesUsed() {
      return super.ramBytesUsed()
          + bitSet.ramBytesUsed()
          + (hasNoValue == null ? 0 : hasNoValue.ramBytesUsed());
    }

    @Override
    Iterator iterator() {
      BitSetIterator iterator = new BitSetIterator(bitSet, maxDoc);
      return new DocValuesFieldUpdates.Iterator() {

        @Override
        public int docID() {
          return iterator.docID();
        }

        @Override
        public int nextDoc() {
          return iterator.nextDoc();
        }

        @Override
        long longValue() {
          return value;
        }

        @Override
        BytesRef binaryValue() {
          throw new UnsupportedOperationException();
        }

        @Override
        long delGen() {
          return delGen;
        }

        @Override
        boolean hasValue() {
          if (hasNoValue != null) {
            return hasNoValue.get(docID()) == false;
          }
          return true;
        }
      };
    }
  }
}
