/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.store.table.sink;

import org.apache.flink.table.store.data.InternalRow;
import org.apache.flink.table.store.types.BigIntType;
import org.apache.flink.table.store.types.CharType;
import org.apache.flink.table.store.types.DataType;
import org.apache.flink.table.store.types.DataTypeDefaultVisitor;
import org.apache.flink.table.store.types.DateType;
import org.apache.flink.table.store.types.DecimalType;
import org.apache.flink.table.store.types.DoubleType;
import org.apache.flink.table.store.types.FloatType;
import org.apache.flink.table.store.types.IntType;
import org.apache.flink.table.store.types.LocalZonedTimestampType;
import org.apache.flink.table.store.types.RowType;
import org.apache.flink.table.store.types.SmallIntType;
import org.apache.flink.table.store.types.TimestampType;
import org.apache.flink.table.store.types.TinyIntType;
import org.apache.flink.table.store.types.VarCharType;
import org.apache.flink.table.store.utils.RowDataUtils;

/** Generate sequence number. */
public class SequenceGenerator {

    private final int index;

    private final Generator generator;

    public SequenceGenerator(String field, RowType rowType) {
        index = rowType.getFieldNames().indexOf(field);
        if (index == -1) {
            throw new RuntimeException(
                    String.format(
                            "Can not find sequence field %s in table schema: %s", field, rowType));
        }
        generator = rowType.getTypeAt(index).accept(new SequenceGeneratorVisitor());
    }

    public long generate(InternalRow row) {
        return generator.generate(row, index);
    }

    private interface Generator {
        long generate(InternalRow row, int i);
    }

    private static class SequenceGeneratorVisitor extends DataTypeDefaultVisitor<Generator> {

        @Override
        public Generator visit(CharType charType) {
            return stringGenerator();
        }

        @Override
        public Generator visit(VarCharType varCharType) {
            return stringGenerator();
        }

        private Generator stringGenerator() {
            return (row, i) -> Long.parseLong(row.getString(i).toString());
        }

        @Override
        public Generator visit(DecimalType decimalType) {
            return (row, i) ->
                    RowDataUtils.castToIntegral(
                            row.getDecimal(i, decimalType.getPrecision(), decimalType.getScale()));
        }

        @Override
        public Generator visit(TinyIntType tinyIntType) {
            return InternalRow::getByte;
        }

        @Override
        public Generator visit(SmallIntType smallIntType) {
            return InternalRow::getShort;
        }

        @Override
        public Generator visit(IntType intType) {
            return InternalRow::getInt;
        }

        @Override
        public Generator visit(BigIntType bigIntType) {
            return InternalRow::getLong;
        }

        @Override
        public Generator visit(FloatType floatType) {
            return (row, i) -> (long) row.getFloat(i);
        }

        @Override
        public Generator visit(DoubleType doubleType) {
            return (row, i) -> (long) row.getDouble(i);
        }

        @Override
        public Generator visit(DateType dateType) {
            return InternalRow::getInt;
        }

        @Override
        public Generator visit(TimestampType timestampType) {
            return (row, i) -> row.getTimestamp(i, timestampType.getPrecision()).getMillisecond();
        }

        @Override
        public Generator visit(LocalZonedTimestampType localZonedTimestampType) {
            return (row, i) ->
                    row.getTimestamp(i, localZonedTimestampType.getPrecision()).getMillisecond();
        }

        @Override
        protected Generator defaultMethod(DataType dataType) {
            throw new UnsupportedOperationException("Unsupported type: " + dataType);
        }
    }
}
