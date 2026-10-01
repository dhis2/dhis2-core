/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.test.e2e.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlSeederTest {

  @Test
  void seedScriptsFailsWhenDirectoryDoesNotExist(@TempDir Path dir) {
    Path absent = dir.resolve("absent");

    IllegalStateException e =
        assertThrows(IllegalStateException.class, () -> SqlSeeder.seedScripts(absent));

    assertTrue(
        e.getMessage().contains(absent.toString()),
        "the message should name the directory it looked in: " + e.getMessage());
    assertTrue(
        e.getMessage().contains("working directory"),
        "the message should point at the likely cause: " + e.getMessage());
  }

  @Test
  void seedScriptsIsEmptyWhenDirectoryHasNoScripts(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("README.md"), "not a script");

    assertTrue(SqlSeeder.seedScripts(dir).isEmpty());
  }

  @Test
  void seedScriptsAreOrderedByFileName(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("020-second.sql"), "select 2;");
    Files.writeString(dir.resolve("010-first.sql"), "select 1;");
    Files.writeString(dir.resolve("030-third.sql"), "select 3;");

    List<String> names = fileNames(dir);

    assertEquals(List.of("010-first.sql", "020-second.sql", "030-third.sql"), names);
  }

  @Test
  void seedScriptsIgnoresNonSqlFiles(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("010-applied.sql"), "select 1;");
    Files.writeString(dir.resolve("020-notes.txt"), "ignored");
    Files.writeString(dir.resolve("030-disabled.sql.off"), "ignored");
    Files.createDirectory(dir.resolve("040-a-directory.sql"));

    List<String> names = fileNames(dir);

    assertEquals(List.of("010-applied.sql"), names);
  }

  private static List<String> fileNames(Path dir) {
    return SqlSeeder.seedScripts(dir).stream().map(path -> path.getFileName().toString()).toList();
  }
}
