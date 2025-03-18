/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.fieldfiltering;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum FieldPreset {
  ID("id", List.of("id")),
  CODE("code", List.of("code")),
  ID_NAME("idName", List.of("id", "displayName")),
  ALL("all", List.of("*")),
  IDENTIFIABLE(
      "identifiable", List.of("id", "name", "code", "created", "lastUpdated", "lastUpdatedBy")),
  NAMEABLE(
      "nameable",
      List.of("id", "name", "shortName", "description", "code", "created", "lastUpdated")),
  OWNER("owner", List.of("owner")),
  PERSISTED("persisted", List.of("persisted")),
  SIMPLE("simple", List.of("simple"));

  private final String name;

  private final List<String> fields;

  FieldPreset(String name, List<String> fields) {
    this.name = name;
    this.fields = List.copyOf(fields);
  }

  public String getName() {
    return name;
  }

  public List<String> getFields() {
    return fields;
  }

  public static FieldPreset defaultPreset() {
    return FieldPreset.ID_NAME;
  }

  public static FieldPreset defaultAssociationPreset() {
    return FieldPreset.ID;
  }
}
