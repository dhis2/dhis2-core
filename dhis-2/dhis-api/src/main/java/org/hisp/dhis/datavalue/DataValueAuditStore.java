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
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public interface DataValueAuditStore {
  String ID = DataValueAuditStore.class.getName();

  /**
   * Updates the given audit.
   *
   * <p>OBS! This is for use in tests only!
   *
   * @param dataValueAudit entry to update
   */
  void updateDataValueAudit(DataValueAudit dataValueAudit);

  /**
   * Adds a DataValueAudit.
   *
   * @param dataValueAudit the DataValueAudit to add.
   */
  void addDataValueAudit(DataValueAudit dataValueAudit);

  /**
   * Deletes all data value audits for the given organisation unit.
   *
   * @param organisationUnit the organisation unit.
   */
  void deleteDataValueAudits(OrganisationUnit organisationUnit);

  /**
   * Deletes all data value audits for the given data element.
   *
   * @param dataElement the data element.
   */
  void deleteDataValueAudits(DataElement dataElement);

  /**
   * Deletes all data value audits for the given category option combo. Both properties:
   * categoryOptionCombo & attributeOptionCombo are checked for a match.
   *
   * @param categoryOptionCombo the categoryOptionCombo.
   */
  void deleteDataValueAudits(@Nonnull CategoryOptionCombo categoryOptionCombo);

  /**
   * Returns data value audits for the given query.
   *
   * @param params the {@link DataValueAuditQueryParams}.
   * @return a list of {@link DataValueAudit}.
   */
  List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params);

  /**
   * Gets all audit entries for a single value (all dimensions are fully specified). If COC and/or
   * AOC are unspecified in the parameters the default is used.
   *
   * @param params the key to the value
   * @return the audit events for the value stored most recent to oldest
   */
  List<DataValueAuditEntry> getAuditsByKey(@Nonnull DataValueQueryParams params);

  /**
   * Counts data value audits for the given query.
   *
   * @param params the {@link DataValueAuditQueryParams}.
   * @return a list of {@link DataValueAudit}.
   */
  int countDataValueAudits(DataValueAuditQueryParams params);
}
