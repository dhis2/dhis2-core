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
package org.hisp.dhis.dataset;

import java.util.Map;
import lombok.AllArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.JdbcDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Lars Helge Overland
 */
@Component
@AllArgsConstructor
public class CompleteDataSetRegistrationDeletionHandler extends JdbcDeletionHandler {
  private static final DeletionVeto VETO = new DeletionVeto(CompleteDataSetRegistration.class);

  private final CompleteDataSetRegistrationService completeDataSetRegistrationService;

  @Override
  protected void register() {
    whenDeleting(DataSet.class, this::deleteDataSet);
    whenVetoing(Period.class, this::allowDeletePeriod);
    whenDeleting(OrganisationUnit.class, this::deleteOrganisationUnit);
    whenVetoing(CategoryOptionCombo.class, this::allowDeleteCategoryOptionCombo);
  }

  private void deleteDataSet(DataSet dataSet) {
    completeDataSetRegistrationService.deleteCompleteDataSetRegistrations(dataSet);
  }

  private DeletionVeto allowDeletePeriod(Period period) {
    return vetoIfExists(
        VETO,
        "select 1 from completedatasetregistration where periodid= :id limit 1",
        Map.of("id", period.getId()));
  }

  private void deleteOrganisationUnit(OrganisationUnit unit) {
    completeDataSetRegistrationService.deleteCompleteDataSetRegistrations(unit);
  }

  private DeletionVeto allowDeleteCategoryOptionCombo(CategoryOptionCombo optionCombo) {
    return vetoIfExists(
        VETO,
        "select 1 from completedatasetregistration where attributeoptioncomboid= :id limit 1",
        Map.of("id", optionCombo.getId()));
  }
}
