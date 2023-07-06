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
package org.hisp.dhis.configuration;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupSet;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.springframework.stereotype.Component;

/**
 * @author Chau Thu Tran
 */
@Component("org.hisp.dhis.configuration.ConfigurationDeletionHandler")
public class ConfigurationDeletionHandler extends DeletionHandler {
  private static final DeletionVeto VETO = new DeletionVeto(Configuration.class);

  private final ConfigurationService configService;

  public ConfigurationDeletionHandler(ConfigurationService configService) {
    checkNotNull(configService);

    this.configService = configService;
  }

  @Override
  protected void register() {
    whenVetoing(UserGroup.class, this::allowDeleteUserGroup);
    whenVetoing(DataElementGroup.class, this::allowDeleteDataElementGroup);
    whenVetoing(IndicatorGroup.class, this::allowDeleteIndicatorGroup);
    whenVetoing(OrganisationUnitLevel.class, this::allowDeleteOrganisationUnitLevel);
    whenVetoing(OrganisationUnitGroupSet.class, this::allowDeleteOrganisationUnitGroupSet);
    whenVetoing(OrganisationUnit.class, this::allowDeleteOrganisationUnit);
    whenVetoing(UserRole.class, this::allowDeleteUserRole);
  }

  private DeletionVeto allowDeleteUserGroup(UserGroup userGroup) {
    UserGroup feedbackRecipients = configService.getConfiguration().getFeedbackRecipients();

    return feedbackRecipients != null && feedbackRecipients.equals(userGroup) ? VETO : ACCEPT;
  }

  private DeletionVeto allowDeleteDataElementGroup(DataElementGroup dataElementGroup) {
    DataElementGroup infraDataElements =
        configService.getConfiguration().getInfrastructuralDataElements();

    return infraDataElements != null && infraDataElements.equals(dataElementGroup) ? VETO : ACCEPT;
  }

  private DeletionVeto allowDeleteIndicatorGroup(IndicatorGroup indicatorGroup) {
    IndicatorGroup infraIndicators =
        configService.getConfiguration().getInfrastructuralIndicators();

    return infraIndicators != null && infraIndicators.equals(indicatorGroup) ? VETO : ACCEPT;
  }

  private DeletionVeto allowDeleteOrganisationUnitLevel(OrganisationUnitLevel level) {
    OrganisationUnitLevel offlineLevel =
        configService.getConfiguration().getOfflineOrganisationUnitLevel();
    OrganisationUnitLevel defaultLevel = configService.getConfiguration().getFacilityOrgUnitLevel();

    return (offlineLevel != null && offlineLevel.equals(level))
            || (defaultLevel != null && defaultLevel.equals(level))
        ? VETO
        : ACCEPT;
  }

  private DeletionVeto allowDeleteOrganisationUnitGroupSet(OrganisationUnitGroupSet groupSet) {
    OrganisationUnitGroupSet defaultGroupSet =
        configService.getConfiguration().getFacilityOrgUnitGroupSet();

    return defaultGroupSet != null && defaultGroupSet.equals(groupSet) ? VETO : ACCEPT;
  }

  private DeletionVeto allowDeleteOrganisationUnit(OrganisationUnit organisationUnit) {
    OrganisationUnit selfRegOrgUnit = configService.getConfiguration().getSelfRegistrationOrgUnit();

    return selfRegOrgUnit != null && selfRegOrgUnit.equals(organisationUnit) ? VETO : ACCEPT;
  }

  private DeletionVeto allowDeleteUserRole(UserRole userRole) {
    UserRole selfRegRole = configService.getConfiguration().getSelfRegistrationRole();

    return selfRegRole != null && selfRegRole.equals(userRole) ? VETO : ACCEPT;
  }
}
