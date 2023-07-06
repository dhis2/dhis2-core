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
package org.hisp.dhis.util;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

/**
 * @author Austin McGee
 */
public class ZipFileUtils {
  private static final Pattern TOP_LEVEL_DIRECTORY_PREFIX_PATTERN =
      Pattern.compile("^([^/\\\\]+[/\\\\]).*");

  private ZipFileUtils() {
    throw new UnsupportedOperationException("util");
  }

  public static String getTopLevelDirectory(Iterator<? extends ZipEntry> zipEntries) {
    if (!zipEntries.hasNext()) {
      return "";
    }

    ZipEntry firstEntry = zipEntries.next();

    Matcher m = TOP_LEVEL_DIRECTORY_PREFIX_PATTERN.matcher(firstEntry.getName());
    if (m.find()) {
      final String prefix = m.group(1);

      Stream<ZipEntry> stream =
          StreamSupport.stream(
              Spliterators.spliteratorUnknownSize(zipEntries, Spliterator.ORDERED), false);

      boolean allMatch = stream.allMatch((ZipEntry ze) -> ze.getName().startsWith(prefix));

      if (allMatch) {
        return prefix;
      }
    }

    return "";
  }
}
