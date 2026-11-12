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
 * A reference to one metadata object, from an {@code objects=type:id} parameter of the multi-object
 * dependency export. One parameter may name several objects of a type, {@code
 * objects=optionSet:abc,def}.
 *
 * @author David Mackessy
 */
public record MetadataObjectReference(String type, String id) {

  /**
   * Splits on the first colon only, so extra colons stay in the id and fail UID validation later.
   *
   * @param token a {@code type:id} value, where the id part may be a comma separated list
   * @return one reference per id, in order, or empty if the token is malformed or any id is blank
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
