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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.security.AuthorityType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Morten Olav Hansen
 */
class DataValueAuditServiceGetAuditsTest extends TransactionalIntegrationTest
{
    @Autowired
    private DataValueAuditService dataValueAuditService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Test
    void testGetSingleDataValueAudit()
    {
        DataElement dataElement = createDataElement( 'A' );
        Period period = createPeriod( "202008" );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        dataElementService.addDataElement( dataElement );
        periodService.addPeriod( period );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        categoryService.addCategoryOptionCombo( defaultCategoryOptionCombo );

        DataValue dataValue = createDataValue( dataElement, period, organisationUnit, "10",
            defaultCategoryOptionCombo );
        dataValueService.addDataValue( dataValue );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElement, period, organisationUnit, defaultCategoryOptionCombo, defaultCategoryOptionCombo );

        assertEquals( 1, audits.size() );
        validateAudit( audits.get( 0 ), AuditType.CREATE, "10" );
    }

    @Test
    void testGetSingleDataValueAudits()
    {
        DataElement dataElement = createDataElement( 'A' );
        Period period = createPeriod( "202008" );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        dataElementService.addDataElement( dataElement );
        periodService.addPeriod( period );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        categoryService.addCategoryOptionCombo( defaultCategoryOptionCombo );

        DataValue dataValue = createDataValue( dataElement, period, organisationUnit, "10",
            defaultCategoryOptionCombo );
        dataValueService.addDataValue( dataValue );

        dataValue.setValue( "20" );
        dataValueService.updateDataValue( dataValue );

        dataValue.setValue( "30" );
        dataValueService.updateDataValue( dataValue );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElement, period, organisationUnit, defaultCategoryOptionCombo, defaultCategoryOptionCombo );

        assertEquals( 3, audits.size() );
        validateAudit( audits.get( 0 ), AuditType.UPDATE, "30" );
        validateAudit( audits.get( 1 ), AuditType.UPDATE, "20" );
        validateAudit( audits.get( 2 ), AuditType.CREATE, "10" );
    }

    @Test
    void testGetSingleDataValueAuditWithDelete()
    {
        DataElement dataElement = createDataElement( 'A' );
        Period period = createPeriod( "202008" );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        dataElementService.addDataElement( dataElement );
        periodService.addPeriod( period );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        categoryService.addCategoryOptionCombo( defaultCategoryOptionCombo );

        DataValue dataValue = createDataValue( dataElement, period, organisationUnit, "10",
            defaultCategoryOptionCombo );
        dataValueService.addDataValue( dataValue );
        dataValueService.deleteDataValue( dataValue );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElement, period, organisationUnit, defaultCategoryOptionCombo, defaultCategoryOptionCombo );

        assertEquals( 2, audits.size() );
        validateAudit( audits.get( 0 ), AuditType.DELETE, "10" );
        validateAudit( audits.get( 1 ), AuditType.CREATE, "10" );
    }

    @Test
    void testGetSingleDataValueAuditWithDeleteThenCreate()
    {
        DataElement dataElement = createDataElement( 'A' );
        Period period = createPeriod( "202008" );
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        CategoryOptionCombo defaultCategoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();

        dataElementService.addDataElement( dataElement );
        periodService.addPeriod( period );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        categoryService.addCategoryOptionCombo( defaultCategoryOptionCombo );

        DataValue dataValue = createDataValue( dataElement, period, organisationUnit, "10",
            defaultCategoryOptionCombo );
        dataValueService.addDataValue( dataValue );
        dataValueService.deleteDataValue( dataValue );

        dataValue.setValue( "20" );
        dataValueService.addDataValue( dataValue );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElement, period, organisationUnit, defaultCategoryOptionCombo, defaultCategoryOptionCombo );

        assertEquals( 3, audits.size() );
        validateAudit( audits.get( 0 ), AuditType.CREATE, "20" );
        validateAudit( audits.get( 1 ), AuditType.DELETE, "10" );
        validateAudit( audits.get( 2 ), AuditType.CREATE, "10" );
    }

    private void validateAudit( DataValueAudit audit, AuditType type, String value )
    {
        assertEquals( type, audit.getAuditType() );
        assertEquals( value, audit.getValue() );
    }
}
