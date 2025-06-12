/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.minmax;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * DTO which represents a {@link MinMaxDataElement} in the API.
 *
 * @author Lars Helge Overland
 */
public record MinMaxValue(
    @JsonProperty @Nonnull @OpenApi.Property({UID.class, DataElement.class}) UID dataElement,
    @JsonProperty @Nonnull @OpenApi.Property({UID.class, OrganisationUnit.class}) UID orgUnit,
    @JsonProperty("categoryOptionCombo")
        @JsonAlias("optionCombo")
        @Nonnull
        @OpenApi.Property({UID.class, CategoryOptionCombo.class})
        UID optionCombo,
    @JsonProperty @Nonnull Integer minValue,
    @JsonProperty @Nonnull Integer maxValue,
    @JsonProperty Boolean generated)
    implements MinMaxValueId {

  @Nonnull
  public static MinMaxValue of(@Nonnull MinMaxDataElement obj) {
    return new MinMaxValue(
        UID.of(obj.getDataElement().getUid()),
        UID.of(obj.getSource().getUid()),
        UID.of(obj.getOptionCombo().getUid()),
        obj.getMin(),
        obj.getMax(),
        obj.isGenerated());
  }

  public MinMaxValue generated(boolean generated) {
    return new MinMaxValue(dataElement, orgUnit, optionCombo, minValue, maxValue, generated);
  }

  public MinMaxValueKey key() {
    return new MinMaxValueKey(dataElement, orgUnit, optionCombo);
  }
}
