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
package org.hisp.dhis.fileresource;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FilenameUtils;

/**
 * @author Lars Helge Overland
 */
public class FileResourceBlocklist {
  private static final ImmutableSet<String> CONTENT_TYPES =
      ImmutableSet.of(
          // Web
          "text/html",
          "text/css",
          "text/javascript",
          "font/otf",
          "application/x-shockwave-flash",
          // Executable
          "application/vnd.debian.binary-package",
          "application/x-rpm",
          "application/java-archive",
          "application/x-ms-dos-executable",
          "application/vnd.microsoft.portable-executable",
          "application/vnd.apple.installer+xml",
          "application/vnd.mozilla.xul+xml",
          "application/x-httpd-php",
          "application/x-sh",
          "application/x-csh");

  private static final ImmutableSet<String> FILE_EXTENSIONS =
      ImmutableSet.of(
          // Web
          "html",
          "htm",
          "css",
          "js",
          "mjs",
          "otf",
          "swf",
          // Executable
          "deb",
          "rpm",
          "jar",
          "jsp",
          "exe",
          "msi",
          "mpkg",
          "xul",
          "php",
          "bin",
          "sh",
          "csh",
          "bat");

  /**
   * Indicates whether the given file resource has a valid file extension and content type according
   * to the blacklist.
   *
   * @param fileResource the {@link FileResource}.
   * @return true if valid, false if invalid.
   */
  public static boolean isValid(FileResource fileResource) {
    if (fileResource == null
        || fileResource.getContentType() == null
        || fileResource.getName() == null) {
      return false;
    }

    if (CONTENT_TYPES.contains(fileResource.getContentType().toLowerCase())) {
      return false;
    }

    if (FILE_EXTENSIONS.contains(
        FilenameUtils.getExtension(fileResource.getName().toLowerCase()))) {
      return false;
    }

    return true;
  }
}
