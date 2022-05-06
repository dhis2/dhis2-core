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

import java.util.List;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */

public interface DataValueAuditService
{
    String ID = DataValueAuditService.class.getName();

    /**
     * Adds a data value audit.
     *
     * @param dataValueAudit the DataValueAudit to add.
     */
    void addDataValueAudit( DataValueAudit dataValueAudit );

    /**
     * Deletes all data value audits for the given organisation unit.
     *
     * @param organisationUnit the organisation unit.
     */
    void deleteDataValueAudits( OrganisationUnit organisationUnit );

    /**
     * Deletes all data value audits for the given data element.
     *
     * @param dataElement the data element.
     */
    void deleteDataValueAudits( DataElement dataElement );

    /**
     * Returns all DataValueAudits for the given DataValue.
     *
     * @param dataValue the DataValue to get DataValueAudits for.
     * @return a list of DataValueAudits which match the given DataValue, or an
     *         empty collection if there are no matches.
     */
    List<DataValueAudit> getDataValueAudits( DataValue dataValue );

    /**
     * Returns data value audits for the given parameters.
     *
     * @param dataElement the {@link DataElement}.
     * @param period the {@link Period}.
     * @param organisationUnit the {@link OrganisationUnit}.
     * @param categoryOptionCombo the {@link CategoryOptionCombo}.
     * @param attributeOptionCombo the {@link CategoryOptionCombo}.
     * @return a list of {@link DataValueAudit}.
     */
    List<DataValueAudit> getDataValueAudits( DataElement dataElement, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo );

    /**
     * Returns data value audits for the given parameters.
     *
     * @param dataElements the list of {@link DataElement}.
     * @param periods the list of {@link Period}.
     * @param organisationUnits the list of {@link OrganisationUnit}.
     * @param categoryOptionCombo the {@link CategoryOptionCombo}.
     * @param attributeOptionCombo the {@link CategoryOptionCombo}.
     * @return a list of {@link DataValueAudit}.
     */
    List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType );

    /**
     * Returns data value audits for the given parameters.
     *
     * @param dataElements the list of {@link DataElement}.
     * @param periods the list of {@link Period}.
     * @param organisationUnits the list of {@link OrganisationUnit}.
     * @param categoryOptionCombo the {@link CategoryOptionCombo}.
     * @param attributeOptionCombo the {@link CategoryOptionCombo}.
     * @param offset the item offset.
     * @param limit the item limit.
     * @return a list of {@link DataValueAudit}.
     */
    List<DataValueAudit> getDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType,
        int offset, int limit );

    /**
     * Returns the count of data value audits for the given parameters.
     *
     * @param dataElements the list of {@link DataElement}.
     * @param periods the list of {@link Period}.
     * @param organisationUnits the list of {@link OrganisationUnit}.
     * @param categoryOptionCombo the {@link CategoryOptionCombo}.
     * @param attributeOptionCombo the {@link CategoryOptionCombo}.
     * @return the count of data value audits.
     */
    int countDataValueAudits( List<DataElement> dataElements, List<Period> periods,
        List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType );
}