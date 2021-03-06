package org.apache.solr.schema;
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

import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.valuesource.ByteFieldSource;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.SortField;

import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.search.QParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * A numeric field that can contain 8-bit signed two's complement integer 
 * values, encoded as simple Strings.
 *
 * <p>
 * Field values will sort numerically, but Range Queries (and other features 
 * that rely on numeric ranges) will not work as expected: values will be 
 * evaluated in unicode String order, not numeric order.
 * </p>
 *
 * <ul>
 *  <li>Min Value Allowed: -128</li>
 *  <li>Max Value Allowed: 127</li>
 * </ul>
 *
 * @see Byte
 * @deprecated Use {@link TrieIntField} instead.
 */
@Deprecated
public class ByteField extends PrimitiveFieldType {

  private static final Logger LOGGER = LoggerFactory.getLogger(ByteField.class);

  @Override
  protected void init(IndexSchema schema, Map<String, String> args) {
    super.init(schema, args);
    restrictProps(SORT_MISSING_FIRST | SORT_MISSING_LAST);
    LOGGER.warn("ByteField is deprecated and will be removed in 5.0. You should use TrieIntField instead.");
  }

  /////////////////////////////////////////////////////////////
  @Override
  public SortField getSortField(SchemaField field, boolean reverse) {
    field.checkSortability();
    return new SortField(field.name, SortField.Type.BYTE, reverse);
  }

  @Override
  public ValueSource getValueSource(SchemaField field, QParser qparser) {
    field.checkFieldCacheSource(qparser);
    return new ByteFieldSource(field.name);
  }

  @Override
  public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException {
    String s = f.stringValue();

    // these values may be from a legacy lucene index, which may
    // not be properly formatted in some output formats, or may
    // incorrectly have a zero length.

    if (s.length()==0) {
      // zero length value means someone mistakenly indexed the value
      // instead of simply leaving it out.  Write a null value instead of a numeric.
      writer.writeNull(name);
      return;
    }

    try {
      byte val = Byte.parseByte(s);
      writer.writeInt(name, val);
    } catch (NumberFormatException e){
      // can't parse - write out the contents as a string so nothing is lost and
      // clients don't get a parse error.
      writer.writeStr(name, s, true);
    }
  }

  @Override
  public Byte toObject(IndexableField f) {
    return Byte.valueOf(toExternal(f));
  }
}
