/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.commons.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;

/**
 * @author Lars Helge Overland
 */
public class GetOrganisationUnitsAction extends ActionPagingSupport<OrganisationUnit> {
  private static final int ALL = 0;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private OrganisationUnitService organisationUnitService;

  public void setOrganisationUnitService(OrganisationUnitService organisationUnitService) {
    this.organisationUnitService = organisationUnitService;
  }

  // -------------------------------------------------------------------------
  // Input
  // -------------------------------------------------------------------------

  private Integer level;

  public void setLevel(Integer level) {
    this.level = level;
  }

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private List<OrganisationUnit> organisationUnits;

  public List<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    canReadType(OrganisationUnit.class);

    if (level == null || level == ALL) {
      organisationUnits = new ArrayList<>(organisationUnitService.getAllOrganisationUnits());
    } else {
      organisationUnits =
          new ArrayList<>(organisationUnitService.getOrganisationUnitsAtLevel(level));
    }

    Collections.sort(organisationUnits);

    UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    organisationUnits.forEach(instance -> canReadInstance(instance, currentUserDetails));

    if (usePaging) {
      this.paging = createPaging(organisationUnits.size());

      organisationUnits = organisationUnits.subList(paging.getStartPos(), paging.getEndPos());
    }

    return SUCCESS;
  }
}
