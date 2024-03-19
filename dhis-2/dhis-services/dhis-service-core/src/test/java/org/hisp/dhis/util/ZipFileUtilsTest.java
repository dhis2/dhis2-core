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

import static org.hisp.dhis.util.ZipFileUtils.getTopLevelDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.Test;

/**
 * @author Austin McGee
 */
class ZipFileUtilsTest {

  @Test
  void testGetTopLevelDirectorySlash() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("test-dir/manifest.webapp"));
    entries.add(new ZipEntry("test-dir/index.html"));
    entries.add(new ZipEntry("test-dir/js/bundle.js"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("test-dir/", prefix);
  }

  @Test
  void testGetTopLevelDirectoryBackslash() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("test-dir\\manifest.webapp"));
    entries.add(new ZipEntry("test-dir\\index.html"));
    entries.add(new ZipEntry("test-dir\\js/bundle.js"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("test-dir\\", prefix);
  }

  @Test
  void testGetTopLevelDirectoryNone() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("manifest.webapp"));
    entries.add(new ZipEntry("index.html"));
    entries.add(new ZipEntry("js/bundle.js"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("", prefix);
  }

  @Test
  void testGetTopLevelDirectoryMixed() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("test-dir/manifest.webapp"));
    entries.add(new ZipEntry("test-dir/index.html"));
    entries.add(new ZipEntry("another-dir/bundle.js"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("", prefix);
  }

  @Test
  void testGetTopLevelDirectorySome() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("test-dir/manifest.webapp"));
    entries.add(new ZipEntry("test-dir/index.html"));
    entries.add(new ZipEntry("top-level-bundle.js"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("", prefix);
  }

  @Test
  void testGetTopLevelDirectorySomeReversed() {
    List<ZipEntry> entries = new ArrayList<ZipEntry>();
    entries.add(new ZipEntry("top-level-bundle.js"));
    entries.add(new ZipEntry("test-dir/manifest.webapp"));
    entries.add(new ZipEntry("test-dir/index.html"));
    String prefix = getTopLevelDirectory(entries.iterator());

    assertEquals("", prefix);
  }
}
