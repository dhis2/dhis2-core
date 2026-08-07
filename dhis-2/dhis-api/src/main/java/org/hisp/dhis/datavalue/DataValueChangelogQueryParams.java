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
package org.hisp.dhis.datavalue;

import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.input.PagedParams;
import org.hisp.dhis.common.input.UrlParams;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.jsontree.Collapsed;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * Encapsulation of a web API request for data value audit records.
 *
 * @author Lars Helge Overland
 */
public record DataValueChangelogQueryParams(
    String fields,
    @OpenApi.Property({UID[].class, DataSet.class}) List<UID> ds,
    @OpenApi.Property({UID[].class, DataElement.class}) List<UID> de,
    List<Period> pe,
    @OpenApi.Property({UID[].class, OrganisationUnit.class}) List<UID> ou,
    @OpenApi.Property({UID[].class, CategoryOptionCombo.class}) UID co, // COC
    @OpenApi.Property({UID[].class, CategoryOptionCombo.class}) UID cc, // AOC
    List<DataValueChangelogType> type,
    @Collapsed PagedParams paged)
    implements UrlParams {

  public static final DataValueChangelogQueryParams DEFAULT = ofType();

  public static DataValueChangelogQueryParams ofType(DataValueChangelogType... types) {
    return new DataValueChangelogQueryParams(
        "",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        null,
        null,
        List.of(types),
        PagedParams.DEFAULT);
  }
}
