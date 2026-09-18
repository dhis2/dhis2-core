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
package org.hisp.dhis.dxf2.metadata;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * A reference to a single metadata object, as named by an {@code object=type:id} request parameter
 * of the multi-object dependency export.
 *
 * <p>{@code type} is a schema name. The singular form is canonical, matching {@code
 * /api/sharing?type=} and {@code AclService#classForType}; the plural form is also accepted because
 * it is what appears as the key in the exported payload.
 *
 * <p>One parameter may name several objects of the same type, {@code object=optionSet:abc,def}, so
 * a caller repeats {@code object} only when the type changes.
 *
 * @author David Mackessy
 */
public record MetadataObjectReference(String type, String id) {

  /**
   * Parses a {@code type:id} token, where the id part may be a comma separated list.
   *
   * <p>Splits on the first colon only, so a token with extra colons keeps them in the id and is
   * rejected later as an invalid UID rather than being silently re-interpreted.
   *
   * @param token the raw parameter value
   * @return one reference per id, in the order given, or an empty list when the token is not of the
   *     form {@code type:id[,id...]}
   */
  @Nonnull
  public static List<MetadataObjectReference> parseAll(String token) {
    if (token == null) {
      return List.of();
    }

    String trimmed = token.trim();
    int separator = trimmed.indexOf(':');

    if (separator < 0) {
      return List.of();
    }

    String type = trimmed.substring(0, separator).trim();
    String ids = trimmed.substring(separator + 1);

    if (type.isEmpty() || ids.isBlank()) {
      return List.of();
    }

    List<MetadataObjectReference> references = new ArrayList<>();

    for (String id : ids.split(",", -1)) {
      String trimmedId = id.trim();

      if (trimmedId.isEmpty()) {
        return List
            .of(); // a blank entry makes the whole token malformed, rather than silently dropped
      }

      references.add(new MetadataObjectReference(type, trimmedId));
    }

    return List.copyOf(references);
  }

  @Override
  public String toString() {
    return type + ":" + id;
  }
}
