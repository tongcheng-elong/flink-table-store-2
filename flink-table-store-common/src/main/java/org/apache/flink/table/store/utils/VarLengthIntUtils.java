/*
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package org.apache.flink.table.store.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/** Utils for encoding int/long to var length bytes. */
public final class VarLengthIntUtils {

    /** @return bytes length. */
    public static int encodeLong(DataOutput os, long value) throws IOException {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }

        int i = 1;
        while ((value & ~0x7FL) != 0) {
            os.write((((int) value & 0x7F) | 0x80));
            value >>>= 7;
            i++;
        }
        os.write((byte) value);
        return i;
    }

    /** @return bytes length. */
    public static int encodeLong(byte[] bytes, long value) {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }

        int i = 1;
        while ((value & ~0x7FL) != 0) {
            bytes[i - 1] = (byte) (((int) value & 0x7F) | 0x80);
            value >>>= 7;
            i++;
        }
        bytes[i - 1] = (byte) value;
        return i;
    }

    public static long decodeLong(DataInput is) throws IOException {

        long result = 0;
        for (int offset = 0; offset < 64; offset += 7) {
            long b = is.readUnsignedByte();
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed long.");
    }

    public static long decodeLong(byte[] ba, int index) {
        long result = 0;
        for (int offset = 0; offset < 64; offset += 7) {
            long b = ba[index++];
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed long.");
    }

    /** @return bytes length. */
    public static int encodeInt(DataOutput os, int value) throws IOException {

        if (value < 0) {
            throw new IllegalArgumentException("negative value: v=" + value);
        }

        int i = 1;
        while ((value & ~0x7F) != 0) {
            os.write(((value & 0x7F) | 0x80));
            value >>>= 7;
            i++;
        }

        os.write((byte) value);
        return i;
    }

    public static int decodeInt(DataInput is) throws IOException {
        for (int offset = 0, result = 0; offset < 32; offset += 7) {
            int b = is.readUnsignedByte();
            result |= (b & 0x7F) << offset;
            if ((b & 0x80) == 0) {
                return result;
            }
        }
        throw new Error("Malformed integer.");
    }
}
