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
package org.hisp.dhis.datavalue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionComboStore;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@Service("org.hisp.dhis.datavalue.DataValueAuditService")
public class DefaultDataValueAuditService implements DataValueAuditService {

  /** The most entries an unpaged read returns to a user who is not a superuser. */
  static final int MAX_UNPAGED_ENTRIES = 10_000;

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final DataValueAuditStore dataValueAuditStore;

  private final DataValueStore dataValueStore;

  private final CategoryOptionComboStore categoryOptionComboStore;

  private final CurrentUserService currentUserService;

  public DefaultDataValueAuditService(
      DataValueAuditStore dataValueAuditStore,
      DataValueStore dataValueStore,
      CategoryOptionComboStore categoryOptionComboStore,
      CurrentUserService currentUserService) {
    checkNotNull(dataValueAuditStore);
    checkNotNull(dataValueStore);
    checkNotNull(categoryOptionComboStore);
    checkNotNull(currentUserService);

    this.dataValueAuditStore = dataValueAuditStore;
    this.dataValueStore = dataValueStore;
    this.categoryOptionComboStore = categoryOptionComboStore;
    this.currentUserService = currentUserService;
  }

  // -------------------------------------------------------------------------
  // DataValueAuditService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public void addDataValueAudit(DataValueAudit dataValueAudit) {
    dataValueAuditStore.addDataValueAudit(dataValueAudit);
  }

  @Override
  @Transactional
  public void deleteDataValueAudits(OrganisationUnit organisationUnit) {
    dataValueAuditStore.deleteDataValueAudits(organisationUnit);
  }

  @Override
  @Transactional
  public void deleteDataValueAudits(DataElement dataElement) {
    dataValueAuditStore.deleteDataValueAudits(dataElement);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueAudit> getDataValueAudits(DataValue dataValue) {
    DataValueAuditQueryParams params =
        new DataValueAuditQueryParams()
            .setDataElements(List.of(dataValue.getDataElement()))
            .setPeriods(List.of(dataValue.getPeriod()))
            .setOrgUnits(List.of(dataValue.getSource()))
            .setCategoryOptionCombo(dataValue.getCategoryOptionCombo())
            .setAttributeOptionCombo(dataValue.getAttributeOptionCombo());

    return dataValueAuditStore.getDataValueAudits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueAudit> getDataValueAudits(
      DataElement dataElement,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo)
      throws AccessDeniedException {
    CategoryOptionCombo coc =
        ObjectUtils.firstNonNull(
            categoryOptionCombo,
            categoryOptionComboStore.getByName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME));

    CategoryOptionCombo aoc =
        ObjectUtils.firstNonNull(
            attributeOptionCombo,
            categoryOptionComboStore.getByName(CategoryCombo.DEFAULT_CATEGORY_COMBO_NAME));

    DataValue dataValue =
        dataValueStore.getDataValue(dataElement, period, organisationUnit, coc, aoc, true);

    DataValueAuditQueryParams params =
        new DataValueAuditQueryParams()
            .setDataElements(List.of(dataElement))
            .setPeriods(List.of(period))
            .setOrgUnits(List.of(organisationUnit))
            .setCategoryOptionCombo(coc)
            .setAttributeOptionCombo(aoc);

    // the store only returns audits the user may read, so only no audits need the check
    List<DataValueAudit> dataValueAudits =
        dataValue == null
            ? List.of()
            : dataValueAuditStore.getDataValueAudits(params).stream()
                .map(x -> DataValueAudit.from(x, x.getCreated()))
                .collect(Collectors.toList());

    if (dataValueAudits.isEmpty()
        && !dataValueAuditStore.isReadable(dataElement, organisationUnit, coc, aoc)) {
      throw new AccessDeniedException(
          new ErrorMessage(
                  ErrorCode.E2050,
                  dataElement.getUid(),
                  period.getIsoDate(),
                  organisationUnit.getUid())
              .getMessage());
    }

    if (dataValue == null) {
      return List.of();
    }

    if (dataValueAudits.isEmpty()) {
      dataValueAudits.add(createDataValueAudit(dataValue));
      return dataValueAudits;
    }

    // case if the audit trail started out with DELETE
    if (dataValueAudits.get(dataValueAudits.size() - 1).getAuditType() == AuditType.DELETE) {
      DataValueAudit valueAudit = createDataValueAudit(dataValue);
      valueAudit.setValue(dataValueAudits.get(dataValueAudits.size() - 1).getValue());
      dataValueAudits.add(valueAudit);
    }

    // unless top is CREATE, inject current DV as audit on top
    if (!dataValue.isDeleted() && dataValueAudits.get(0).getAuditType() != AuditType.CREATE) {
      DataValueAudit dataValueAudit = createDataValueAudit(dataValue);
      dataValueAudit.setAuditType(AuditType.UPDATE);
      dataValueAudit.setCreated(dataValue.getLastUpdated());
      dataValueAudits.add(0, dataValueAudit);
    }

    dataValueAudits.get(dataValueAudits.size() - 1).setAuditType(AuditType.CREATE);

    return dataValueAudits;
  }

  private static DataValueAudit createDataValueAudit(DataValue dataValue) {
    DataValueAudit dataValueAudit =
        new DataValueAudit(
            dataValue, dataValue.getValue(), dataValue.getStoredBy(), AuditType.CREATE);
    dataValueAudit.setCreated(dataValue.getCreated());

    return dataValueAudit;
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params) {
    if (params.hasPaging() || currentUserService.currentUserIsSuper())
      return dataValueAuditStore.getDataValueAudits(params);
    // the access checked read is bounded: one more row than allowed tells there are more
    List<DataValueAudit> audits =
        dataValueAuditStore.getDataValueAudits(
            withPager(params, new SlimPager(1, MAX_UNPAGED_ENTRIES + 1, null)));
    if (audits.size() > MAX_UNPAGED_ENTRIES)
      throw new IllegalQueryException(ErrorCode.E2051, MAX_UNPAGED_ENTRIES);
    return audits;
  }

  private static DataValueAuditQueryParams withPager(
      DataValueAuditQueryParams params, Pager pager) {
    return new DataValueAuditQueryParams()
        .setDataElements(params.getDataElements())
        .setPeriods(params.getPeriods())
        .setOrgUnits(params.getOrgUnits())
        .setCategoryOptionCombo(params.getCategoryOptionCombo())
        .setAttributeOptionCombo(params.getAttributeOptionCombo())
        .setAuditTypes(params.getAuditTypes())
        .setPager(pager);
  }

  @Override
  @Transactional(readOnly = true)
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    return dataValueAuditStore.countDataValueAudits(params);
  }
}
