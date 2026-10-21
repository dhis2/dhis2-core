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
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public interface DataValueAuditService {
  String ID = DataValueAuditService.class.getName();

  /**
   * Adds a data value audit.
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
   * Returns the DataValueAudits for the given DataValue that the current user may read.
   *
   * @param dataValue the DataValue to get DataValueAudits for.
   * @return a list of DataValueAudits which match the given DataValue, or an empty collection if
   *     there are no matches.
   */
  List<DataValueAudit> getDataValueAudits(DataValue dataValue);

  /**
   * Returns data value audits for the given parameters. This is a special version that also adds
   * current data value, and marks the first audit as type CREATE.
   *
   * @param dataElement the {@link DataElement}.
   * @param period the {@link Period}.
   * @param organisationUnit the {@link OrganisationUnit}.
   * @param categoryOptionCombo the {@link CategoryOptionCombo}.
   * @param attributeOptionCombo the {@link CategoryOptionCombo}.
   * @return a list of {@link DataValueAudit}.
   * @throws ForbiddenException when the current user may not read the data of the data value
   */
  List<DataValueAudit> getDataValueAudits(
      DataElement dataElement,
      Period period,
      OrganisationUnit organisationUnit,
      CategoryOptionCombo categoryOptionCombo,
      CategoryOptionCombo attributeOptionCombo)
      throws ForbiddenException;

  /**
   * Returns data value audits for the given parameters that the current user may read.
   *
   * @param params the {@link DataValueAuditQueryParams}.
   * @return a list of {@link DataValueAudit}; paged, one more than the page size when there is a
   *     next page
   * @throws ConflictException when an unpaged read of a user who is not a superuser has more audits
   *     than allowed, or a page of such a user ends beyond the allowed entries
   */
  List<DataValueAudit> getDataValueAudits(DataValueAuditQueryParams params)
      throws ConflictException;

  /**
   * Returns the count of data value audits for the given parameters that the current user may read.
   *
   * @param params the {@link DataValueAuditQueryParams}.
   * @return a count of {@link DataValueAudit}.
   */
  int countDataValueAudits(DataValueAuditQueryParams params);
}
