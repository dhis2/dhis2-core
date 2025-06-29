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
package org.hisp.dhis.orgunitprofile.impl;

import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.orgunitprofile.OrgUnitProfile;
import org.hisp.dhis.orgunitprofile.OrgUnitProfileService;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrgUnitProfileDeletionHandler extends DeletionHandler {
  private final OrgUnitProfileService orgUnitProfileService;

  @Override
  protected void register() {
    whenDeleting(DataElement.class, this::deleteDataElement);
    whenDeleting(Indicator.class, this::deleteIndicator);
    whenDeleting(DataSet.class, this::deleteDataSet);
    whenDeleting(ProgramIndicator.class, this::deleteProgramIndicator);
    whenDeleting(Attribute.class, this::deleteAttribute);
    whenDeleting(OrganisationUnitGroupSet.class, this::deleteOrganisationUnitGroupSet);
  }

  private void deleteDataElement(DataElement dataElement) {
    handleDataItem(dataElement);
  }

  private void deleteIndicator(Indicator indicator) {
    handleDataItem(indicator);
  }

  private void deleteDataSet(DataSet dataSet) {
    handleDataItem(dataSet);
  }

  private void deleteProgramIndicator(ProgramIndicator programIndicator) {
    handleDataItem(programIndicator);
  }

  private void handleDataItem(IdentifiableObject dataItem) {
    try {
      OrgUnitProfile profile = orgUnitProfileService.getOrgUnitProfile();
      if (profile.getDataItems().remove(dataItem.getUid())) {
        orgUnitProfileService.saveOrgUnitProfile(profile);
      }
    } catch (ForbiddenException ex) {
      throw new AccessDeniedException(ex.getMessage());
    }
  }

  private void deleteAttribute(Attribute attribute) {
    try {
      OrgUnitProfile profile = orgUnitProfileService.getOrgUnitProfile();
      if (profile.getAttributes().remove(attribute.getUid())) {
        orgUnitProfileService.saveOrgUnitProfile(profile);
      }
    } catch (ForbiddenException ex) {
      throw new AccessDeniedException(ex.getMessage());
    }
  }

  private void deleteOrganisationUnitGroupSet(OrganisationUnitGroupSet groupSet) {
    try {
      OrgUnitProfile profile = orgUnitProfileService.getOrgUnitProfile();
      if (profile.getGroupSets().remove(groupSet.getUid())) {
        orgUnitProfileService.saveOrgUnitProfile(profile);
      }
    } catch (ForbiddenException ex) {
      throw new AccessDeniedException(ex.getMessage());
    }
  }
}
