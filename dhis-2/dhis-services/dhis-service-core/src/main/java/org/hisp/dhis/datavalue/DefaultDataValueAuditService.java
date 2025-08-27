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
package org.hisp.dhis.datavalue;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
@RequiredArgsConstructor
@Service
public class DefaultDataValueAuditService implements DataValueAuditService {

  private final DataValueAuditStore dataValueAuditStore;

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
  public List<DataValueAuditEntry> getDataValueAudits(@Nonnull DataValueQueryParams params) {
    return dataValueAuditStore.getAuditsByKey(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params) {
    return dataValueAuditStore.getDataValueAudits(params);
  }

  @Override
  @Transactional(readOnly = true)
  public int countDataValueAudits(DataValueAuditQueryParams params) {
    return dataValueAuditStore.countDataValueAudits(params);
  }
}
