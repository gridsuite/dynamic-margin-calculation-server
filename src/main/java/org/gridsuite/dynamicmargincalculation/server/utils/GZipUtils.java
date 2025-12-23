/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.dynamicmargincalculation.server.utils;

import org.apache.logging.log4j.util.Strings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class GZipUtils {
    private GZipUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Compresses a string using GZIP and encodes it to Base64.
     */
    public static String compress(String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compress string", e);
        }
    }

    /**
     * Decodes a Base64 string and decompresses it using GZIP.
     */
    public static String decompress(String compressedStr) {
        if (Strings.isEmpty(compressedStr)) {
            return compressedStr;
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(compressedStr);
            try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzip = new GZIPInputStream(in)) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to decompress string", e);
        }
    }
}
