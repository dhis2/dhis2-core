/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.data.DimensionalObjectProvider;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrganisationUnitResolver {

  private final DimensionalObjectProvider dimensionalObjectProducer;

  /**
   * Resolve organisation units like ou:USER_ORGUNIT;USER_ORGUNIT_CHILDREN;LEVEL-XXX;OUGROUP-XXX
   * into a list of organisation unit dimension uids.
   *
   * @param queryFilter the query filter containing the organisation unit filter
   * @param userOrgUnits the user organisation units
   * @return the organisation unit dimension uids
   */
  public String resolveOrgUnits(QueryFilter queryFilter, List<OrganisationUnit> userOrgUnits) {
    List<String> filterItem = QueryFilter.getFilterItems(queryFilter.getFilter());
    List<String> orgUnitDimensionUid =
        dimensionalObjectProducer.getOrgUnitDimensionUid(filterItem, userOrgUnits);
    return String.join(OPTION_SEP, orgUnitDimensionUid);
  }
}
