/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.web.tomcat;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

/**
 * Manages a {@link ServletWebServerFactory} document root.
 *
 * @author Phillip Webb
 * @see AbstractServletWebServerFactory
 */
@Slf4j
class DocumentRoot {

  private static final String[] COMMON_DOC_ROOTS = {"src/main/webapp", "public", "static"};

  private final Logger logger;

  private File directory;

  DocumentRoot(Logger logger) {
    this.logger = logger;
  }

  File getDirectory() {
    return this.directory;
  }

  void setDirectory(File directory) {
    this.directory = directory;
  }

  /**
   * Returns the absolute document root when it points to a valid directory, logging a warning and
   * returning {@code null} otherwise.
   *
   * @return the valid document root
   */
  final File getValidDirectory() {
    File file = this.directory;
    file = (file != null) ? file : getWarFileDocumentRoot();
    file = (file != null) ? file : getExplodedWarFileDocumentRoot();
    file = (file != null) ? file : getCommonDocumentRoot();
    if (file == null && this.logger.isDebugEnabled()) {
      logNoDocumentRoots();
    } else if (this.logger.isDebugEnabled()) {
      this.logger.debug("Document root: " + file);
    }
    return file;
  }

  private File getWarFileDocumentRoot() {
    return getArchiveFileDocumentRoot(".war");
  }

  private File getArchiveFileDocumentRoot(String extension) {
    File file = getCodeSourceArchive();
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Code archive: " + file);
    }
    if (file != null
        && file.exists()
        && !file.isDirectory()
        && file.getName().toLowerCase(Locale.ENGLISH).endsWith(extension)) {
      return file.getAbsoluteFile();
    }
    return null;
  }

  private File getExplodedWarFileDocumentRoot() {
    return getExplodedWarFileDocumentRoot(getCodeSourceArchive());
  }

  private File getCodeSourceArchive() {
    return getCodeSourceArchive(getClass().getProtectionDomain().getCodeSource());
  }

  File getCodeSourceArchive(CodeSource codeSource) {
    try {
      URL location = (codeSource != null) ? codeSource.getLocation() : null;
      if (location == null) {
        return null;
      }
      String path;
      URLConnection connection = location.openConnection();
      if (connection instanceof JarURLConnection jarURLConnection) {
        path = jarURLConnection.getJarFile().getName();
      } else {
        path = location.toURI().getPath();
      }
      int index = path.indexOf("!/");
      if (index != -1) {
        path = path.substring(0, index);
      }
      return new File(path);
    } catch (Exception ex) {
      return null;
    }
  }

  final File getExplodedWarFileDocumentRoot(File codeSourceFile) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Code archive: " + codeSourceFile);
    }
    if (codeSourceFile != null && codeSourceFile.exists()) {
      String path = codeSourceFile.getAbsolutePath();
      int webInfPathIndex = path.indexOf(File.separatorChar + "WEB-INF" + File.separatorChar);
      if (webInfPathIndex >= 0) {
        path = path.substring(0, webInfPathIndex);
        return new File(path);
      }
    }
    return null;
  }

  private File getCommonDocumentRoot() {
    for (String commonDocRoot : COMMON_DOC_ROOTS) {
      File root = new File(commonDocRoot);
      if (root.exists() && root.isDirectory()) {
        return root.getAbsoluteFile();
      }
    }
    return null;
  }

  private void logNoDocumentRoots() {
    this.logger.debug(
        "None of the document roots "
            + Arrays.asList(COMMON_DOC_ROOTS)
            + " point to a directory and will be ignored.");
  }
}
