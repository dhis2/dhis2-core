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
import java.util.ArrayList;
import java.util.List;
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

  /**
   * Fields allow {@code property[sub,sub]} syntax where a comma occurs as part of the property
   * name. These commas need to be ignored when splitting a {@code fields} parameter list.
   */
  private static final String FIELD_SPLIT = ",(?![^\\[\\]]*\\]|[^\\(\\)]*\\)|([a-zA-Z0-9]+,?)+\\))";

  @OpenApi.Description(
      """
    Limit the response to specific field(s).\s
    See [Metadata-field-filter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata.html#webapi_metadata_field_filter).
    """)
  @OpenApi.Shared.Inline
  @OpenApi.Property(OpenApi.PropertyNames[].class)
  @JsonProperty
  @CheckForNull
  List<String> fields;

  @Setter @JsonProperty @Nonnull Defaults defaults = Defaults.INCLUDE;

  /**
   * Since splitting on comma is too unsophisticated to be correct this needs custom split code when
   * called by spring injecting URL parmaeters
   *
   * @param fields field expression
   */
  public void setFields(String fields) {
    this.fields = fields == null ? null : List.of(fields.split(FIELD_SPLIT));
  }

  public GetObjectParams addField(String field) {
    if (fields == null) fields = new ArrayList<>();
    fields.add(field);
    return this;
  }

  /**
   * @return list of fields with defaults as used when viewing a single object
   */
  @Nonnull
  @JsonIgnore
  public List<String> getFieldsObject() {
    if (fields == null || fields.isEmpty()) return List.of("*");
    return fields;
  }

  /**
   * @return list of fields with defaults as used when viewing a list as JSON
   */
  @Nonnull
  @JsonIgnore
  public List<String> getFieldsJsonList() {
    if (fields == null || fields.isEmpty()) return FieldPreset.defaultPreset().getFields();
    return fields;
  }

  /**
   * @return list of fields with defaults as used when viewing a list as CSV
   */
  @Nonnull
  @JsonIgnore
  public List<String> getFieldsCsvList() {
    List<String> res = new ArrayList<>();
    if (fields != null) res.addAll(fields);
    if (res.isEmpty() || res.contains("*") || res.contains(":all"))
      res.addAll(FieldPreset.defaultPreset().getFields());
    return res;
  }

  /**
   * @return elevates a single object params object to a list params object as a way to adapt to the
   *     list based query engine that is also used for single object lookup
   */
  public GetObjectListParams toListParams() {
    GetObjectListParams res = new GetObjectListParams();
    if (fields != null) fields.forEach(res::addField);
    res.setDefaults(defaults);
    return res;
  }
}
