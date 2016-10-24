package org.hisp.dhis.dxf2.datavalueset;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.dataelement.*;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.JacksonUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Lars Helge Overland
 */
public class DataValueSetServiceExportTest
    extends DhisSpringTest
{
    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    private DataElement deA;
    private DataElement deB;

    private DataElementCategoryCombo ccA;

    private DataElementCategoryOptionCombo cocA;
    private DataElementCategoryOptionCombo cocB;

    private DataSet dsA;
    private DataSet dsB;

    private Period peA;
    private Period peB;

    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

    private User user;

    @Override
    public void setUpTest()
    {
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );

        idObjectManager.save( deA );
        idObjectManager.save( deB );

        ccA = createCategoryCombo( 'A' );

        categoryService.addDataElementCategoryCombo( ccA );

        cocA = createCategoryOptionCombo( 'A' );
        cocB = createCategoryOptionCombo( 'B' );

        cocA.setCategoryCombo( ccA );
        cocB.setCategoryCombo( ccA );

        categoryService.addDataElementCategoryOptionCombo( cocA );
        categoryService.addDataElementCategoryOptionCombo( cocB );

        dsA = createDataSet( 'A' );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );

        dsB = createDataSet( 'B' );
        dsB.addDataSetElement( deA );

        dataSetService.addDataSet( dsA );
        dataSetService.addDataSet( dsB );

        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2016, 3, 1 ), getDate( 2016, 3, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2016, 4, 1 ), getDate( 2016, 4, 30 ) );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );

        // Data values

        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouB, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouB, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );

        dataValueService.addDataValue( new DataValue( deA, peB, ouA, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouA, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouB, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouB, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );

        dataValueService.addDataValue( new DataValue( deB, peA, ouA, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouA, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouB, cocA, cocA, "1", "storedBy", new Date(), "comment" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouB, cocB, cocA, "1", "storedBy", new Date(), "comment" ) );

        // Service mocks

        user = createUser( 'A' );
        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );
        setDependency( organisationUnitService, "currentUserService", currentUserService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testExportBasic()
        throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( peA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = JacksonUtils.fromJson( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsA.getUid(), dvs.getDataSet() );
        assertEquals( 4, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( ouA.getUid(), dv.getOrgUnit() );
            assertEquals( peA.getUid(), dv.getPeriod() );
        }
    }

    @Test
    public void testExportOrgUnitChildren()
        throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setIncludeChildren( true )
            .setPeriods( Sets.newHashSet( peA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = JacksonUtils.fromJson( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsA.getUid(), dvs.getDataSet() );
        // TODO assert data values size = 8

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( peA.getUid(), dv.getPeriod() );
        }
    }

    @Test
    public void testExportOutputIdSchemeCode()
        throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IdSchemes idSchemes = new IdSchemes()
            .setOrgUnitIdScheme( IdentifiableProperty.CODE.name() )
            .setDataElementIdScheme( IdentifiableProperty.CODE.name() );

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsB ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( peB ) )
            .setOutputIdSchemes( idSchemes );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = JacksonUtils.fromJson( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsB.getUid(), dvs.getDataSet() );
        assertEquals( 2, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( deA.getCode(), dv.getDataElement() );
            assertEquals( ouA.getCode(), dv.getOrgUnit() );
        }
    }
}
