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
package org.hisp.dhis.webapi.webdomain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.Data;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryDto;

/**
 * @author Lars Helge Overland
 */
@Data
@OpenApi.Shared
public class DataValueFollowUpRequest {
  @JsonProperty
  @OpenApi.Property({UID.class, DataElement.class})
  private String dataElement;

  @JsonProperty
  @OpenApi.Property({Period.class})
  private String period;

  @JsonProperty
  @OpenApi.Property({UID.class, OrganisationUnit.class})
  private String orgUnit;

  @JsonProperty
  @OpenApi.Property({UID.class, CategoryOptionCombo.class})
  private String categoryOptionCombo;

  @JsonProperty
  @OpenApi.Property({UID.class, CategoryOptionCombo.class})
  private String attributeOptionCombo;

  @JsonProperty private DataValueCategoryDto attribute;

  @JsonProperty private Boolean followup;

  public DataEntryValue.Input toDataEntryValue() {
    // note: here null for non-key properties means "keep current value"
    // because this uses partial update later
    String attributeCombo = attribute == null ? null : attribute.getCombo();
    Set<String> attributeOptions = attribute == null ? null : attribute.getOptions();
    return new DataEntryValue.Input(
        dataElement,
        orgUnit,
        categoryOptionCombo,
        null,
        attributeOptionCombo,
        attributeCombo,
        attributeOptions,
        period,
        null,
        null,
        followup,
        null);
  }
}
