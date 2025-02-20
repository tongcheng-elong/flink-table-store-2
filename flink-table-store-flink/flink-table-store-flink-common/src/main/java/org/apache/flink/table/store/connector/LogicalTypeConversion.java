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

package org.apache.flink.table.store.connector;

import org.apache.flink.table.store.types.DataField;
import org.apache.flink.table.store.types.DataType;
import org.apache.flink.table.store.types.RowType;
import org.apache.flink.table.types.logical.LogicalType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Conversion between {@link LogicalType} and {@link DataType}. */
public class LogicalTypeConversion {

    public static org.apache.flink.table.types.logical.RowType toLogicalType(RowType dataType) {
        return (org.apache.flink.table.types.logical.RowType)
                dataType.accept(DataTypeToLogicalType.INSTANCE);
    }

    public static LogicalType toLogicalType(DataType dataType) {
        return dataType.accept(DataTypeToLogicalType.INSTANCE);
    }

    public static RowType toDataType(org.apache.flink.table.types.logical.RowType logicalType) {
        return (RowType) toDataType(logicalType, new AtomicInteger(-1));
    }

    public static DataType toDataType(LogicalType logicalType) {
        return toDataType(logicalType, new AtomicInteger(-1));
    }

    public static DataType toDataType(
            LogicalType logicalType, AtomicInteger currentHighestFieldId) {
        return logicalType.accept(new LogicalTypeToDataType(currentHighestFieldId));
    }

    public static org.apache.flink.table.types.logical.RowType toRowType(
            List<DataField> dataFields) {
        return (org.apache.flink.table.types.logical.RowType)
                DataTypeToLogicalType.INSTANCE.visit(new RowType(false, dataFields));
    }
}
