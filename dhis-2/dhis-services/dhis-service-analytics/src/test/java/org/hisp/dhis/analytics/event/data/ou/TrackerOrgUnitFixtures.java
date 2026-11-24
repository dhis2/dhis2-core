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
package org.hisp.dhis.analytics.event.data.ou;

import static org.hisp.dhis.analytics.event.data.ou.TrackerOrgUnitDimension.REGISTRATION_OU;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;

import java.util.List;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Builds org unit hierarchies whose units carry their hierarchy level, and query params carrying a
 * tracker org unit dimension or filter.
 */
public final class TrackerOrgUnitFixtures {

  private TrackerOrgUnitFixtures() {}

  /** Creates a root org unit, at hierarchy level 1. */
  public static OrganisationUnit levelOne(char uniqueCharacter) {
    OrganisationUnit ou = createOrganisationUnit(uniqueCharacter);
    ou.updatePath();
    return ou;
  }

  /** Creates an org unit one hierarchy level below the given parent. */
  public static OrganisationUnit childOf(OrganisationUnit parent, char uniqueCharacter) {
    OrganisationUnit ou = createOrganisationUnit(uniqueCharacter);
    ou.setParent(parent);
    ou.updatePath();
    return ou;
  }

  /**
   * Adds the dimension and filter items of the given tracker org unit dimension to the builder. A
   * null dimension list leaves the dimension out entirely, which differs from an empty one for
   * REGISTRATION_OU.
   */
  public static EventQueryParams.Builder withTrackerOrgUnits(
      EventQueryParams.Builder builder,
      TrackerOrgUnitDimension dimension,
      List<OrganisationUnit> dimensionItems,
      List<OrganisationUnit> filterItems) {
    if (dimensionItems != null) {
      dimension.withDimensionItems(builder, dimensionItems);
    }

    return dimension == REGISTRATION_OU
        ? builder.withRegistrationOuFilter(filterItems)
        : builder.withEnrollmentOuFilter(filterItems);
  }
}
