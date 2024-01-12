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
package org.hisp.dhis.de.action;

import com.opensymphony.xwork2.Action;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitDataSetAssociationSet;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class GetDataSetAssociationsAction implements Action {
  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private IdentifiableObjectManager identifiableObjectManager;

  @Autowired private UserService userService;

  // -------------------------------------------------------------------------
  // Output
  // -------------------------------------------------------------------------

  private List<Set<String>> dataSetAssociationSets = new ArrayList<>();

  public List<Set<String>> getDataSetAssociationSets() {
    return dataSetAssociationSets;
  }

  private Map<String, Integer> organisationUnitAssociationSetMap = new HashMap<>();

  public Map<String, Integer> getOrganisationUnitAssociationSetMap() {
    return organisationUnitAssociationSetMap;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() {
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    // TODO MAS: not longer in use
    Integer level = organisationUnitService.getOfflineOrganisationUnitLevels(currentUser);

    Date lastUpdated =
        DateUtils.max(
            identifiableObjectManager.getLastUpdated(DataSet.class),
            identifiableObjectManager.getLastUpdated(OrganisationUnit.class));

    String tag = ContextUtils.getEtag(lastUpdated, CurrentUserUtil.getCurrentUserDetails());

    if (ContextUtils.isNotModified(
        ServletActionContext.getRequest(), ServletActionContext.getResponse(), tag)) {
      return SUCCESS;
    }

    OrganisationUnitDataSetAssociationSet organisationUnitSet =
        organisationUnitService.getOrganisationUnitDataSetAssociationSet(currentUser);

    dataSetAssociationSets = organisationUnitSet.getDataSetAssociationSets();

    organisationUnitAssociationSetMap = organisationUnitSet.getOrganisationUnitAssociationSetMap();

    return SUCCESS;
  }
}
