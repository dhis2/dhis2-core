/*
 * Copyright (c) 2004-2023, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.webapi.controller.tracker.export;

import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CompressionUtil {

  private CompressionUtil() {
    throw new IllegalStateException(
        "Utility class to compress exported objects in Zip o GZip format");
  }

  /**
   * @param requestOutputStream Output stream from request
   * @param toCompress Objects to compress
   * @param objectWriter Object writer from a mapper
   * @param attachment Attachment file name
   * @param <T>
   * @throws IOException
   */
  public static <T> void writeZip(
      OutputStream requestOutputStream, T toCompress, ObjectWriter objectWriter, String attachment)
      throws IOException {
    ZipOutputStream outputStream = new ZipOutputStream(requestOutputStream);
    outputStream.putNextEntry(new ZipEntry(attachment));

    objectWriter.writeValue(outputStream, toCompress);
    outputStream.close();
  }

  /**
   * @param requestOutputStream Output stream from request
   * @param toCompress Objects to compress
   * @param objectWriter Object writer from a mapper
   * @param <T>
   * @throws IOException
   */
  public static <T> void writeGzip(
      OutputStream requestOutputStream, T toCompress, ObjectWriter objectWriter)
      throws IOException {
    GZIPOutputStream outputStream = new GZIPOutputStream(requestOutputStream);

    objectWriter.writeValue(outputStream, toCompress);
    outputStream.close();
  }
}
