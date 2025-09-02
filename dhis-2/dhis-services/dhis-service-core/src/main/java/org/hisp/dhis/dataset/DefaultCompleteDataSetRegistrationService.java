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
package org.hisp.dhis.dataset;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.notifications.DataSetNotificationEventPublisher;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataExportStoreParams;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.dataset.CompleteDataSetRegistrationService")
@RequiredArgsConstructor
public class DefaultCompleteDataSetRegistrationService
    implements CompleteDataSetRegistrationService {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final CompleteDataSetRegistrationStore completeDataSetRegistrationStore;

  private final CategoryService categoryService;

  private final DataValueService dataValueService;

  private final DataSetNotificationEventPublisher notificationEventPublisher;

  private final AggregateAccessManager accessManager;

  private final MessageService messageService;

  private final PeriodStore periodStore;

  // -------------------------------------------------------------------------
  // CompleteDataSetRegistrationService
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void saveCompleteDataSetRegistration(CompleteDataSetRegistration registration) {
    registration.setPeriod(periodStore.reloadForceAddPeriod(registration.getPeriod()));
    checkCompulsoryDeOperands(registration);

    Date date = new Date();

    if (!registration.hasDate()) {
      registration.setDate(date);
    }

    if (!registration.hasStoredBy()) {
      registration.setStoredBy(CurrentUserUtil.getCurrentUsername());
    }

    if (!registration.hasLastUpdated()) {
      registration.setLastUpdated(date);
    }

    if (!registration.hasLastUpdatedBy()) {
      registration.setLastUpdatedBy(CurrentUserUtil.getCurrentUsername());
    }

    if (registration.getAttributeOptionCombo() == null) {
      registration.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    }

    completeDataSetRegistrationStore.saveCompleteDataSetRegistration(registration);

    if (registration.getDataSet().isNotifyCompletingUser()) {
      messageService.sendCompletenessMessage(registration);
    }

    notificationEventPublisher.publishEvent(registration);
  }

  /**
   * Check if a data set is missing data from its compulsory data element operands. DataSet has the
   * flag compulsoryFieldsCompleteOnly, which when false, actually renders the compulsory elements
   * not compulsory. That flag is checked first to see if the missing data element operands should
   * be retrieved. <br>
   * If there are compulsory elements missing data (and they are compulsory) then an exception is
   * thrown advising that compulsory elements are required to be filled.
   *
   * @param registration registration to check
   */
  private void checkCompulsoryDeOperands(CompleteDataSetRegistration registration) {
    // only get missing compulsory elements if they are actually compulsory
    if (registration.getDataSet().isCompulsoryFieldsCompleteOnly()) {
      List<DataElementOperand> missingDataElementOperands =
          getMissingCompulsoryFields(
              registration.getDataSet(),
              registration.getPeriod(),
              registration.getSource(),
              registration.getAttributeOptionCombo());
      if (!missingDataElementOperands.isEmpty()) {
        String deos =
            missingDataElementOperands.stream()
                .map(DataElementOperand::getDisplayName)
                .collect(Collectors.joining(","));
        throw new IllegalStateException(
            "All compulsory data element operands need to be filled: [%s]".formatted(deos));
      }
    }
  }

  @Override
  @Transactional
  public void updateCompleteDataSetRegistration(CompleteDataSetRegistration registration) {
    checkCompulsoryDeOperands(registration);

    registration.setLastUpdated(new Date());

    registration.setLastUpdatedBy(CurrentUserUtil.getCurrentUsername());

    completeDataSetRegistrationStore.updateCompleteDataSetRegistration(registration);
  }

  @Override
  @Transactional
  public void deleteCompleteDataSetRegistration(CompleteDataSetRegistration registration) {
    completeDataSetRegistrationStore.deleteCompleteDataSetRegistration(registration);
  }

  @Override
  @Transactional
  public void deleteCompleteDataSetRegistrations(List<CompleteDataSetRegistration> registrations) {
    for (CompleteDataSetRegistration registration : registrations) {
      completeDataSetRegistrationStore.deleteCompleteDataSetRegistration(registration);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public CompleteDataSetRegistration getCompleteDataSetRegistration(
      DataSet dataSet,
      Period period,
      OrganisationUnit source,
      CategoryOptionCombo attributeOptionCombo) {
    return completeDataSetRegistrationStore.getCompleteDataSetRegistration(
        dataSet, period, source, attributeOptionCombo);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CompleteDataSetRegistration> getAllCompleteDataSetRegistrations() {
    return completeDataSetRegistrationStore.getAllCompleteDataSetRegistrations();
  }

  @Override
  @Transactional
  public void deleteCompleteDataSetRegistrations(DataSet dataSet) {
    completeDataSetRegistrationStore.deleteCompleteDataSetRegistrations(dataSet);
  }

  @Override
  @Transactional
  public void deleteCompleteDataSetRegistrations(OrganisationUnit unit) {
    completeDataSetRegistrationStore.deleteCompleteDataSetRegistrations(unit);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataElementOperand> getMissingCompulsoryFields(
      DataSet dataSet,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo attributeOptionCombo) {
    List<DataElementOperand> missingDataElementOperands = new ArrayList<>();

    if (!dataSet.getCompulsoryDataElementOperands().isEmpty()) {
      DataExportStoreParams params = new DataExportStoreParams();
      params.setDataElementOperands(dataSet.getCompulsoryDataElementOperands());
      params.setPeriods(Sets.newHashSet(period));
      params.setAttributeOptionCombos(Sets.newHashSet(attributeOptionCombo));
      params.setOrganisationUnits(Sets.newHashSet(organisationUnit));

      List<DeflatedDataValue> deflatedDataValues = dataValueService.getDeflatedDataValues(params);

      MapMapMap<Long, Long, Long, Boolean> dataPresent = new MapMapMap<>();

      for (DeflatedDataValue dv : deflatedDataValues) {
        dataPresent.putEntry(
            dv.getSourceId(), dv.getDataElementId(), dv.getCategoryOptionComboId(), true);
      }

      for (DataElementOperand deo : dataSet.getCompulsoryDataElementOperands()) {
        List<String> errors = accessManager.canWrite(CurrentUserUtil.getCurrentUserDetails(), deo);
        if (!errors.isEmpty()) {
          continue;
        }

        MapMap<Long, Long, Boolean> ouDataPresent = dataPresent.get(organisationUnit.getId());

        if (ouDataPresent != null) {
          Map<Long, Boolean> deDataPresent = ouDataPresent.get(deo.getDataElement().getId());

          if (deDataPresent != null
              && (deo.getCategoryOptionCombo() == null
                  || deDataPresent.get(deo.getCategoryOptionCombo().getId()) != null)) {
            continue;
          }
        }

        missingDataElementOperands.add(deo);
      }
    }

    return missingDataElementOperands;
  }

  @Override
  @Transactional(readOnly = true)
  public int getCompleteDataSetCountLastUpdatedAfter(Date lastUpdated) {
    return completeDataSetRegistrationStore.getCompleteDataSetCountLastUpdatedAfter(lastUpdated);
  }
}
