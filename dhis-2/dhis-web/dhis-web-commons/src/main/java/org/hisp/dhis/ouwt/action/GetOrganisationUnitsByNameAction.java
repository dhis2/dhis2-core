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
package org.hisp.dhis.ouwt.action;

import com.opensymphony.xwork2.Action;
import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class GetOrganisationUnitsByNameAction implements Action {
  private static final int MAX = 14;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private UserService userService;

  // -------------------------------------------------------------------------
  // Input
  // -------------------------------------------------------------------------

  private String term;

  public void setTerm(String term) {
    this.term = term;
  }

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private List<OrganisationUnit> organisationUnits = new ArrayList<>();

  public List<OrganisationUnit> getOrganisationUnits() {
    return organisationUnits;
  }

  // -------------------------------------------------------------------------
  // Implementation Action
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    term = term.toLowerCase();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();

    if (currentUser != null && currentUser.hasOrganisationUnit()) {
      params.setParents(currentUser.getOrganisationUnits());
    }

    params.setQuery(term);
    params.setMax(MAX);

    organisationUnits = organisationUnitService.getOrganisationUnitsByQuery(params);

    return SUCCESS;
  }
}
