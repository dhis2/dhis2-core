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
package org.hisp.dhis.util;

import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.hisp.dhis.appmanager.App;

@RequiredArgsConstructor
public class AppHtmlTemplate {

  private final String contextPath;
  private final App app;

  public void apply(InputStream inputStream, OutputStream outputStream) throws IOException {
    PrintWriter output = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

    try (LineIterator iterator = IOUtils.lineIterator(inputStream, StandardCharsets.UTF_8)) {
      while (iterator.hasNext()) {
        String line = iterator.next();
        if (line.contains("__DHIS2_BASE_URL__") || line.contains("__DHIS2_APP_ROOT_URL__")) {
          line = replaceLine(line);
        }
        if (line.contains("<head>")) {
          line =
              line.replace(
                  "<head>",
                  "<head><meta name=\"dhis2-base-url\" content=\"" + this.contextPath + "\" />");
        }
        output.println(line);
      }
    }
  }

  private String replaceLine(String line) {
    return line.replace("__DHIS2_BASE_URL__", this.contextPath)
        .replace("__DHIS2_APP_ROOT_URL__", Strings.nullToEmpty(this.app.getBaseUrl()));
  }
}
