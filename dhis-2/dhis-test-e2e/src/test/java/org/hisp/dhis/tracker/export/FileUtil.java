/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.tracker.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Luca Cambi
 */
public class FileUtil {

  /**
   * @return Map of zip entry file to uncompressed zip string UTF8
   * @throws IOException zip decompression exception
   */
  public static Map<String, String> mapZipEntryToStringContent(byte[] buff) throws IOException {

    Map<String, String> map = new HashMap<>();

    try (final var zIn = new ZipInputStream(new ByteArrayInputStream(buff))) {
      ZipEntry entry;

      while ((entry = zIn.getNextEntry()) != null) {
        var outputStream = new ByteArrayOutputStream();

        for (var c = zIn.read(); c != -1; c = zIn.read()) {
          outputStream.write(c);
        }

        map.put(entry.getName(), outputStream.toString(StandardCharsets.UTF_8));

        outputStream.close();
        zIn.closeEntry();
      }
    }
    return map;
  }

  public static String mapGzipEntryToStringContent(byte[] buff) throws IOException {
    String result;
    try (final var gzIn = new GZIPInputStream(new ByteArrayInputStream(buff))) {
      var outputStream = new ByteArrayOutputStream();
      for (var c = gzIn.read(); c != -1; c = gzIn.read()) {
        outputStream.write(c);
      }
      result = outputStream.toString(StandardCharsets.UTF_8);
      outputStream.close();
    }
    return result;
  }

  /**
   * @return uncompressed gzip string UTF8
   * @throws IOException gzip decompression exception
   */
  public static String gZipToStringContent(byte[] buff) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    try (final var gzIn = new GZIPInputStream(new ByteArrayInputStream(buff))) {

      for (var c = gzIn.read(); c != -1; c = gzIn.read()) {
        outputStream.write(c);
      }

      outputStream.close();
    }

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
