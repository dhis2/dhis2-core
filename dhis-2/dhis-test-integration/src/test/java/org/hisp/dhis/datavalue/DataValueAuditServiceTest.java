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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by Halvdan Hoem Grelland
 */
class DataValueAuditServiceTest extends SingleSetupIntegrationTestBase
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

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementC;

    private DataElement dataElementD;

    private CategoryOptionCombo optionCombo;

    private Period periodA;

    private Period periodB;

    private Period periodC;

    private Period periodD;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private OrganisationUnit orgUnitC;

    private OrganisationUnit orgUnitD;

    private DataValue dataValueA;

    private DataValue dataValueB;

    private DataValue dataValueC;

    private DataValue dataValueD;

    @Override
    public void setUpTest()
        throws Exception
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementC = createDataElement( 'C' );
        dataElementD = createDataElement( 'D' );
        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );
        dataElementService.addDataElement( dataElementC );
        dataElementService.addDataElement( dataElementD );
        periodA = createPeriod( getDay( 5 ), getDay( 6 ) );
        periodB = createPeriod( getDay( 6 ), getDay( 7 ) );
        periodC = createPeriod( getDay( 7 ), getDay( 8 ) );
        periodD = createPeriod( getDay( 8 ), getDay( 9 ) );
        periodService.addPeriod( periodA );
        periodService.addPeriod( periodB );
        periodService.addPeriod( periodC );
        periodService.addPeriod( periodD );
        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitB = createOrganisationUnit( 'B' );
        orgUnitC = createOrganisationUnit( 'C' );
        orgUnitD = createOrganisationUnit( 'D' );
        organisationUnitService.addOrganisationUnit( orgUnitA );
        organisationUnitService.addOrganisationUnit( orgUnitB );
        organisationUnitService.addOrganisationUnit( orgUnitC );
        organisationUnitService.addOrganisationUnit( orgUnitD );
        optionCombo = categoryService.getDefaultCategoryOptionCombo();
        categoryService.addCategoryOptionCombo( optionCombo );
        dataValueA = createDataValue( dataElementA, periodA, orgUnitA, optionCombo, optionCombo, "1" );
        dataValueB = createDataValue( dataElementB, periodB, orgUnitB, optionCombo, optionCombo, "2" );
        dataValueC = createDataValue( dataElementC, periodC, orgUnitC, optionCombo, optionCombo, "3" );
        dataValueD = createDataValue( dataElementD, periodD, orgUnitD, optionCombo, optionCombo, "4" );
        dataValueService.addDataValue( dataValueA );
        dataValueService.addDataValue( dataValueB );
        dataValueService.addDataValue( dataValueC );
        dataValueService.addDataValue( dataValueD );
    }

    @Test
    void testAddGetDataValueAuditFromDataValue()
    {
        DataValueAudit dataValueAuditA = new DataValueAudit( dataValueA, dataValueA.getValue(),
            dataValueA.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dataValueAuditB = new DataValueAudit( dataValueB, dataValueB.getValue(),
            dataValueB.getStoredBy(), AuditType.UPDATE );
        dataValueAuditService.addDataValueAudit( dataValueAuditA );
        dataValueAuditService.addDataValueAudit( dataValueAuditB );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits( dataValueA );
        assertContainsOnly( List.of( dataValueAuditA ), audits );
    }

    @Test
    void testAddGetDataValueAuditSingleRecord()
    {
        DataValueAudit dataValueAuditA = new DataValueAudit( dataValueA, dataValueA.getValue(),
            dataValueA.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dataValueAuditB = new DataValueAudit( dataValueB, dataValueB.getValue(),
            dataValueB.getStoredBy(), AuditType.UPDATE );
        dataValueAuditService.addDataValueAudit( dataValueAuditA );
        dataValueAuditService.addDataValueAudit( dataValueAuditB );

        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElementA ) )
            .setPeriods( List.of( periodA ) )
            .setOrgUnits( List.of( orgUnitA ) )
            .setCategoryOptionCombo( optionCombo )
            .setAttributeOptionCombo( optionCombo );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits( params );
        assertContainsOnly( List.of( dataValueAuditA ), audits );
    }

    @Test
    void testGetDataValueAudit()
    {
        DataValueAudit dvaA = new DataValueAudit( dataValueA, dataValueA.getValue(),
            dataValueA.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dvaB = new DataValueAudit( dataValueB, dataValueB.getValue(),
            dataValueB.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dvaC = new DataValueAudit( dataValueC, dataValueC.getValue(),
            dataValueC.getStoredBy(), AuditType.CREATE );
        DataValueAudit dvaD = new DataValueAudit( dataValueD, dataValueD.getValue(),
            dataValueD.getStoredBy(), AuditType.DELETE );
        dataValueAuditService.addDataValueAudit( dvaA );
        dataValueAuditService.addDataValueAudit( dvaB );
        dataValueAuditService.addDataValueAudit( dvaC );
        dataValueAuditService.addDataValueAudit( dvaD );

        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElementA ) )
            .setPeriods( List.of( periodA ) )
            .setOrgUnits( List.of( orgUnitA ) )
            .setCategoryOptionCombo( optionCombo )
            .setAuditTypes( List.of( AuditType.UPDATE ) );

        assertContainsOnly( List.of( dvaA ), dataValueAuditService.getDataValueAudits( params ) );

        params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElementA, dataElementB ) )
            .setPeriods( List.of( periodA, periodB ) )
            .setOrgUnits( List.of( orgUnitA, orgUnitB ) )
            .setCategoryOptionCombo( optionCombo )
            .setAuditTypes( List.of( AuditType.UPDATE ) );

        assertContainsOnly( List.of( dvaA, dvaB ), dataValueAuditService.getDataValueAudits( params ) );

        params = new DataValueAuditQueryParams()
            .setAuditTypes( List.of( AuditType.CREATE ) );

        assertContainsOnly( List.of( dvaC ), dataValueAuditService.getDataValueAudits( params ) );

        params = new DataValueAuditQueryParams()
            .setAuditTypes( List.of( AuditType.CREATE, AuditType.DELETE ) );

        assertContainsOnly( List.of( dvaC, dvaD ), dataValueAuditService.getDataValueAudits( params ) );
    }

    @Test
    void testGetDataValueAuditNoResult()
    {
        DataValueAudit dvaA = new DataValueAudit( dataValueA, dataValueA.getValue(),
            dataValueA.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dvaB = new DataValueAudit( dataValueB, dataValueB.getValue(),
            dataValueB.getStoredBy(), AuditType.UPDATE );
        DataValueAudit dvaC = new DataValueAudit( dataValueC, dataValueC.getValue(),
            dataValueC.getStoredBy(), AuditType.CREATE );
        DataValueAudit dvaD = new DataValueAudit( dataValueD, dataValueD.getValue(),
            dataValueD.getStoredBy(), AuditType.DELETE );
        dataValueAuditService.addDataValueAudit( dvaA );
        dataValueAuditService.addDataValueAudit( dvaB );
        dataValueAuditService.addDataValueAudit( dvaC );
        dataValueAuditService.addDataValueAudit( dvaD );

        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElementA ) )
            .setPeriods( List.of( periodD ) )
            .setOrgUnits( List.of( orgUnitA ) )
            .setCategoryOptionCombo( optionCombo )
            .setAuditTypes( List.of( AuditType.UPDATE ) );

        assertEquals( 0, dataValueAuditService.getDataValueAudits( params ).size() );
    }

    @Test
    void testGetDataValueAuditWithFakeCreate()
    {
        DataValueAuditQueryParams params = new DataValueAuditQueryParams()
            .setDataElements( List.of( dataElementA ) )
            .setPeriods( List.of( periodD ) )
            .setOrgUnits( List.of( orgUnitA ) )
            .setCategoryOptionCombo( optionCombo )
            .setAuditTypes( List.of( AuditType.UPDATE ) );

        assertEquals( 0, dataValueAuditService.getDataValueAudits( params ).size() );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElementA, periodA, orgUnitA, optionCombo, optionCombo );

        assertEquals( 1, audits.size() );
        assertEquals( AuditType.CREATE, audits.get( 0 ).getAuditType() );
    }

    @Test
    void testGetDataValueAuditWithFakeCreate2()
    {
        dataValueA.setValue( "10" );
        dataValueService.updateDataValue( dataValueA );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElementA, periodA, orgUnitA, optionCombo, optionCombo );

        assertEquals( 2, audits.size() );
        assertEquals( AuditType.UPDATE, audits.get( 0 ).getAuditType() );
        assertEquals( AuditType.CREATE, audits.get( 1 ).getAuditType() );
    }

    @Test
    void testGetDataValueAuditWithFakeCreateDeleteAndCreate()
    {
        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "10",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "20",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "30",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElementA, periodA, orgUnitA, optionCombo, optionCombo );

        assertEquals( 4, audits.size() );
        assertEquals( AuditType.CREATE, audits.get( 3 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 2 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 1 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 0 ).getAuditType() );
    }

    @Test
    @Disabled
    void testGetDataValueAuditWithFakeCreateDelete2()
    {
        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "10",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "20",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValueA, "30",
            dataValueA.getStoredBy(), AuditType.UPDATE ) );

        dataValueService.deleteDataValue( dataValueA );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElementA, periodA, orgUnitA, optionCombo, optionCombo );

        assertEquals( 4, audits.size() );
        assertEquals( AuditType.CREATE, audits.get( 3 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 2 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 1 ).getAuditType() );
        assertEquals( AuditType.DELETE, audits.get( 0 ).getAuditType() );
    }

    @Test
    @Disabled
    void testGetDataValueAuditWithFakeCreateDeleteAndUndelete()
    {
        DataElement dataElement = createDataElement( 'F' );
        DataValue dataValue = createDataValue( dataElement, periodA, orgUnitA, optionCombo, optionCombo, "1" );

        dataElementService.addDataElement( dataElement );
        dataValueService.addDataValue( dataValue );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValue, "10",
            dataValue.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValue, "20",
            dataValue.getStoredBy(), AuditType.UPDATE ) );

        dataValueAuditService.addDataValueAudit( new DataValueAudit( dataValue, "30",
            dataValue.getStoredBy(), AuditType.UPDATE ) );

        dataValueService.deleteDataValue( dataValue );

        List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits(
            dataElement, periodA, orgUnitA, optionCombo, optionCombo );

        assertContainsOnly( List.of(), audits );

        dataValueService.addDataValue( dataValue );

        audits = dataValueAuditService.getDataValueAudits(
            dataElement, periodA, orgUnitA, optionCombo, optionCombo );

        assertEquals( 6, audits.size() );
        assertEquals( AuditType.UPDATE, audits.get( 0 ).getAuditType() );
        assertEquals( AuditType.CREATE, audits.get( 1 ).getAuditType() );
        assertEquals( AuditType.DELETE, audits.get( 2 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 3 ).getAuditType() );
        assertEquals( AuditType.UPDATE, audits.get( 4 ).getAuditType() );
        assertEquals( AuditType.CREATE, audits.get( 5 ).getAuditType() );
    }
}
