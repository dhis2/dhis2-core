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
package org.hisp.dhis.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfiltering.FieldPreset;

/**
 * Base for parameters supported by CRUD {@code CRUD.getObject}.
 *
 * @author Jan Bernitt
 */
@Getter
@EqualsAndHashCode
@ToString
@Accessors(chain = true)
@OpenApi.Shared
public class GetObjectParams {

  @OpenApi.Description(
      """
    Limit the response to specific field(s).\s
    See [Metadata-field-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter).
    """)
  @OpenApi.Shared.Inline
  @OpenApi.Property(OpenApi.PropertyNames[].class)
  @JsonProperty
  @CheckForNull
  @Setter
  String fields;

  @Setter @JsonProperty @Nonnull Defaults defaults = Defaults.INCLUDE;

  @Setter
  @JsonProperty
  @OpenApi.Description(
      "Force to use (`true`) or not use (`false`) gist API backend for metadata API, default (`null`) auto-detect based on `fields` and `filter`s")
  @OpenApi.Since(44)
  Boolean gist;

  public void addField(String field) {
    if (fields == null || fields.isEmpty()) {
      fields = field;
    } else fields += "," + field;
  }

  /**
   * @return list of fields with defaults as used when viewing a single object
   */
  @Nonnull
  @JsonIgnore
  public String getFieldsObject() {
    if (fields == null || fields.isEmpty()) return "*";
    return fields;
  }

  /**
   * @return list of fields with defaults as used when viewing a list as JSON
   */
  @Nonnull
  @JsonIgnore
  public String getFieldsJsonList() {
    if (fields == null || fields.isEmpty())
      return String.join(",", FieldPreset.defaultPreset().getFields());
    return fields;
  }

  /**
   * @return list of fields with defaults as used when viewing a list as CSV
   */
  @Nonnull
  @JsonIgnore
  public String getFieldsCsvList() {
    String res = fields;
    if (res == null || res.isEmpty()) res = "*";
    if (res.contains("*"))
      res = res.replace("*", String.join(",", FieldPreset.defaultPreset().getFields()));
    if (res.contains(":all"))
      res = res.replace(":all", String.join(",", FieldPreset.defaultPreset().getFields()));
    return res;
  }

  /**
   * @return elevates a single object params object to a list params object as a way to adapt to the
   *     list based query engine that is also used for single object lookup
   */
  public GetObjectListParams toListParams() {
    GetObjectListParams res = new GetObjectListParams();
    res.setFields(fields);
    res.setDefaults(defaults);
    return res;
  }
}
