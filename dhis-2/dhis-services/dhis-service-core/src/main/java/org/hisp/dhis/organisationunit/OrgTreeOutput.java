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
package org.hisp.dhis.organisationunit;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.jsontree.JsonBuilder;

/**
 * Utilities to write OU tree responses.
 *
 * @since 2.44
 * @author Jan Bernitt
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OrgTreeOutput {

  private static final JsonBuilder.PrettyPrint MINIMIZED =
      new JsonBuilder.PrettyPrint(0, 0, false, false, true);

  static void toJson(OrgTree tree, OutputStream json) {
    try (json) {
      JsonBuilder.streamObject(
          MINIMIZED,
          json,
          response -> {
            if (tree.pager() != null) {
              OrgTree.Pager p = tree.pager();
              response.addObject(
                  "pager",
                  pager ->
                      pager
                          .addNumber("page", p.page())
                          .addNumber("pageSize", p.pageSize())
                          .addNumber("total", p.total())
                          .addNumber("pageCount", p.pageCount()));
            }
            response.addArray(
                "organisationUnits",
                matches -> matches.addElements(tree.organisationUnits(), OrgTreeOutput::addEntry));
            response.addArray(
                "ancestors",
                ancestors -> ancestors.addElements(tree.ancestors(), OrgTreeOutput::addEntry));
          });
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static void addEntry(JsonBuilder.JsonArrayBuilder arr, OrgTree.OrgTreeEntry entry) {
    arr.addObject(
        ou -> {
          ou.addString("id", entry.id().getValue());
          ou.addString("displayName", entry.displayName());
          ou.addString("path", entry.path().toString());
          ou.addNumber("level", entry.level());
          ou.addBoolean("leaf", entry.leaf());
        });
  }
}
