/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.store.spark;

import org.apache.spark.sql.catalyst.expressions.SpecializedGetters;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.BinaryType;
import org.apache.spark.sql.types.BooleanType;
import org.apache.spark.sql.types.ByteType;
import org.apache.spark.sql.types.CalendarIntervalType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DateType;
import org.apache.spark.sql.types.DecimalType;
import org.apache.spark.sql.types.DoubleType;
import org.apache.spark.sql.types.FloatType;
import org.apache.spark.sql.types.IntegerType;
import org.apache.spark.sql.types.LongType;
import org.apache.spark.sql.types.MapType;
import org.apache.spark.sql.types.NullType;
import org.apache.spark.sql.types.ShortType;
import org.apache.spark.sql.types.StringType;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.types.TimestampType;
import org.apache.spark.sql.types.UserDefinedType;

/**
 * Reader of Spark {@link SpecializedGetters}. Copied from Spark to avoid conflict between Spark2
 * and Spark3 .
 */
public final class SpecializedGettersReader {

    private SpecializedGettersReader() {}

    public static Object read(SpecializedGetters obj, int ordinal, DataType dataType) {
        if (obj.isNullAt(ordinal) || dataType instanceof NullType) {
            return null;
        }
        if (dataType instanceof BooleanType) {
            return obj.getBoolean(ordinal);
        }
        if (dataType instanceof ByteType) {
            return obj.getByte(ordinal);
        }
        if (dataType instanceof ShortType) {
            return obj.getShort(ordinal);
        }
        if (dataType instanceof IntegerType) {
            return obj.getInt(ordinal);
        }
        if (dataType instanceof LongType) {
            return obj.getLong(ordinal);
        }
        if (dataType instanceof FloatType) {
            return obj.getFloat(ordinal);
        }
        if (dataType instanceof DoubleType) {
            return obj.getDouble(ordinal);
        }
        if (dataType instanceof StringType) {
            return obj.getUTF8String(ordinal);
        }
        if (dataType instanceof DecimalType) {
            DecimalType dt = (DecimalType) dataType;
            return obj.getDecimal(ordinal, dt.precision(), dt.scale());
        }
        if (dataType instanceof DateType) {
            return obj.getInt(ordinal);
        }
        if (dataType instanceof TimestampType) {
            return obj.getLong(ordinal);
        }
        if (dataType instanceof CalendarIntervalType) {
            return obj.getInterval(ordinal);
        }
        if (dataType instanceof BinaryType) {
            return obj.getBinary(ordinal);
        }
        if (dataType instanceof StructType) {
            return obj.getStruct(ordinal, ((StructType) dataType).size());
        }
        if (dataType instanceof ArrayType) {
            return obj.getArray(ordinal);
        }
        if (dataType instanceof MapType) {
            return obj.getMap(ordinal);
        }
        if (dataType instanceof UserDefinedType) {
            return obj.get(ordinal, ((UserDefinedType) dataType).sqlType());
        }

        throw new UnsupportedOperationException("Unsupported data type " + dataType.simpleString());
    }
}
