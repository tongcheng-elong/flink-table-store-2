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

package org.apache.flink.table.store.connector.sink;

import org.apache.flink.table.store.CoreOptions;
import org.apache.flink.table.store.data.InternalRow;
import org.apache.flink.table.store.file.schema.Schema;
import org.apache.flink.table.store.file.schema.SchemaManager;
import org.apache.flink.table.store.fs.Path;
import org.apache.flink.table.store.fs.local.LocalFileIO;
import org.apache.flink.table.store.options.Options;
import org.apache.flink.table.store.reader.RecordReader;
import org.apache.flink.table.store.reader.RecordReaderIterator;
import org.apache.flink.table.store.table.FileStoreTable;
import org.apache.flink.table.store.table.FileStoreTableFactory;
import org.apache.flink.table.store.table.source.TableRead;
import org.apache.flink.table.store.types.DataType;
import org.apache.flink.table.store.types.DataTypes;
import org.apache.flink.table.store.types.RowType;
import org.apache.flink.table.store.utils.CloseableIterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base test class for {@link AtMostOnceCommitterOperator} and {@link ExactlyOnceCommitOperator}.
 */
public abstract class CommitterOperatorTestBase {

    private static final RowType ROW_TYPE =
            RowType.of(
                    new DataType[] {DataTypes.INT(), DataTypes.BIGINT()}, new String[] {"a", "b"});

    @TempDir public java.nio.file.Path tempDir;
    protected Path tablePath;

    @BeforeEach
    public void before() {
        tablePath = new Path(tempDir.toString());
    }

    protected void assertResults(FileStoreTable table, String... expected) {
        TableRead read = table.newRead();
        List<String> actual = new ArrayList<>();
        table.newScan()
                .plan()
                .splits
                .forEach(
                        s -> {
                            try {
                                RecordReader<InternalRow> recordReader = read.createReader(s);
                                CloseableIterator<InternalRow> it =
                                        new RecordReaderIterator<>(recordReader);
                                while (it.hasNext()) {
                                    InternalRow row = it.next();
                                    actual.add(row.getInt(0) + ", " + row.getLong(1));
                                }
                                it.close();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
        Collections.sort(actual);
        assertThat(actual).isEqualTo(Arrays.asList(expected));
    }

    protected FileStoreTable createFileStoreTable() throws Exception {
        Options conf = new Options();
        conf.set(CoreOptions.PATH, tablePath.toString());
        SchemaManager schemaManager = new SchemaManager(LocalFileIO.create(), tablePath);
        schemaManager.createTable(
                new Schema(
                        ROW_TYPE.getFields(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        conf.toMap(),
                        ""));
        return FileStoreTableFactory.create(LocalFileIO.create(), conf);
    }
}
