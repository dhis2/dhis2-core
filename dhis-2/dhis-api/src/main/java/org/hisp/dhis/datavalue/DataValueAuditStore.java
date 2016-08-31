package org.hisp.dhis.datavalue;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Quang Nguyen
 * @author Halvdan Hoem Grelland
 */
public interface DataValueAuditStore
{
    String ID = DataValueAuditStore.class.getName();

    /**
     * Adds a DataValueAudit.
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
     * Returns all DataValueAudits which match the DataElement, Period, OrganisationUnit
     * and DataElementCategoryOptionCombo of the given DataValue.
     *
     * @param dataValue the DataValue to get DataValueAudits for.
     * @return a list of DataValueAudits which match the DataElement Period,
     *         OrganisationUnit and DataElementCategoryOptionCombo of the given DataValue,
     *         or an empty list if no DataValueAudits match.
     */
    List<DataValueAudit> getDataValueAudits( DataValue dataValue );

    /**
     * Returns all DataValueAudits which match the given DataElement, Period,
     * OrganisationUnit and DataElementCategoryOptionCombo.
     *
     * @param dataElement the DataElement of the DataValueAudits.
     * @param period the Period of the DataValueAudits.
     * @param organisationUnit the OrganisationUnit of the DataValueAudits.
     * @param categoryOptionCombo the DataElementCategoryOptionCombo of the DataValueAudits.
     * @param attributeOptionCombo the attribute option combo.
     * @return a list of DataValueAudits which match the given DataElement, Period,
     *         OrganisationUnit and DataElementCategoryOptionCombo, or an empty list
     *         if no DataValueAudits match.
     */
    List<DataValueAudit> getDataValueAudits( DataElement dataElement, Period period, OrganisationUnit organisationUnit, 
        DataElementCategoryOptionCombo categoryOptionCombo, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Deletes a DataValueAudit.
     *
     * @param dataValueAudit the DataValueAudit to delete.
     */
    void deleteDataValueAudit( DataValueAudit dataValueAudit );

    /**
     * Deletes all DataValueAudits for the given DataElement.
     *
     * @param dataElement the DataElement for which the DataValueAudits should be deleted.
     * @return the number of deleted DataValueAudits.
     */
    int deleteDataValueAuditByDataElement( DataElement dataElement );

    /**
     * Deletes all DataValueAudits for the given Period.
     *
     * @param period the Period for which the DataValueAudits should be deleted.
     * @return the number of deleted DataValueAudits.
     */
    int deleteDataValueAuditByPeriod( Period period );

    /**
     * Deletes all DataValues for the given OrganisationUnit.
     *
     * @param organisationUnit the OrganisationUnit for which the DataValueAudits should be deleted.
     * @return the number of deleted DataValueAudits.
     */
    int deleteDataValueAuditByOrganisationUnit( OrganisationUnit organisationUnit );

    /**
     * Deletes all DataValues for the given DataElementCategoryOptionCombo.
     *
     * @param categoryOptionCombo the DataElementCategoryOptionCombo for which the DataValueAudits should be deleted.
     * @return the number of deleted DataValueAudits.
     */
    int deleteDataValueAuditByCategoryOptionCombo( DataElementCategoryOptionCombo categoryOptionCombo );
}
