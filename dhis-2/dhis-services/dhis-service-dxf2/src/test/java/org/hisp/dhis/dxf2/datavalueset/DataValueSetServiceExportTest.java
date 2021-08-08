/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataValueSetServiceExportTest
    extends IntegrationTestBase
{
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private UserService _userService;

    @Autowired
    private ObjectMapper jsonMapper;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private Attribute atA;

    private AttributeValue avA;

    private AttributeValue avB;

    private AttributeValue avC;

    private AttributeValue avD;

    private DataSet dsA;

    private DataSet dsB;

    private Period peA;

    private Period peB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private OrganisationUnitGroup ogA;

    private User user;

    private String peAUid;

    @Override
    public void setUpTest()
    {
        userService = _userService;

        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2016, 3, 1 ),
            getDate( 2016, 3, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2016, 4, 1 ),
            getDate( 2016, 4, 30 ) );
        periodService.addPeriod( peA );
        periodService.addPeriod( peB );

        peAUid = peA.getUid();

        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );

        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );

        ccA = createCategoryCombo( 'A' );

        categoryService.addCategoryCombo( ccA );

        cocA = createCategoryOptionCombo( 'A' );
        cocB = createCategoryOptionCombo( 'B' );

        cocA.setCategoryCombo( ccA );
        cocB.setCategoryCombo( ccA );

        categoryService.addCategoryOptionCombo( cocA );
        categoryService.addCategoryOptionCombo( cocB );

        atA = createAttribute( 'A' );
        atA.setDataElementAttribute( true );
        atA.setOrganisationUnitAttribute( true );
        atA.setCategoryOptionComboAttribute( true );

        idObjectManager.save( atA );

        dsA = createDataSet( 'A' );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );

        dsB = createDataSet( 'B' );
        dsB.addDataSetElement( deA );

        dataSetService.addDataSet( dsA );
        dataSetService.addDataSet( dsB );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C' ); // Not in hierarchy of A

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );

        ogA = createOrganisationUnitGroup( 'A' );

        idObjectManager.save( ogA );

        avA = new AttributeValue( "AttributeValueA", atA );
        avB = new AttributeValue( "AttributeValueB", atA );
        avC = new AttributeValue( "AttributeValueC", atA );
        avD = new AttributeValue( "AttributeValueD", atA );

        attributeService.addAttributeValue( deA, avA );
        attributeService.addAttributeValue( ouA, avB );
        attributeService.addAttributeValue( cocA, avC );
        attributeService.addAttributeValue( cocB, avD );

        // Data values

        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocB, cocB, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouB, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouB, cocB, cocB, "1" ) );

        dataValueService.addDataValue( new DataValue( deA, peB, ouA, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouA, cocB, cocB, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouB, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peB, ouB, cocB, cocB, "1" ) );

        dataValueService.addDataValue( new DataValue( deB, peA, ouA, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouA, cocB, cocB, "1" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouB, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deB, peA, ouB, cocB, cocB, "1" ) );

        // Flush session to make data values visible to JDBC query

        // Service mocks

        user = createUser( 'A' );
        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        userService.addUser( user );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );
        setDependency( organisationUnitService, "currentUserService", currentUserService );

        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, dsB, AccessStringHelper.DATA_READ_WRITE );
        dataSetService.updateDataSet( dsA );
        dataSetService.updateDataSet( dsB );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testExportBasic()
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( peA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsA.getUid(), dvs.getDataSet() );
        assertEquals( 4, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( ouA.getUid(), dv.getOrgUnit() );
            assertEquals( peAUid, dv.getPeriod() );
        }
    }

    @Test
    public void testExportAttributeOptionCombo()
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setPeriods( Sets.newHashSet( peA ) )
            .setAttributeOptionCombos( Sets.newHashSet( cocA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( 2, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( ouB.getUid(), dv.getOrgUnit() );
            assertEquals( peAUid, dv.getPeriod() );
        }
    }

    @Test
    public void testExportOrgUnitChildren()
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setIncludeChildren( true )
            .setPeriods( Sets.newHashSet( peA ) );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsA.getUid(), dvs.getDataSet() );
        assertEquals( 8, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( peAUid, dv.getPeriod() );
        }
    }

    @Test
    public void testExportOutputSingleDataValueSetIdSchemeCode()
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        IdSchemes idSchemes = new IdSchemes()
            .setOrgUnitIdScheme( IdentifiableProperty.CODE.name() )
            .setDataElementIdScheme( IdentifiableProperty.CODE.name() )
            .setDataSetIdScheme( IdentifiableProperty.CODE.name() );

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsB ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( peB ) )
            .setOutputIdSchemes( idSchemes );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertNotNull( dvs.getOrgUnit() );
        assertEquals( dsB.getCode(), dvs.getDataSet() );
        assertEquals( ouA.getCode(), dvs.getOrgUnit() );
        assertEquals( 2, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( deA.getCode(), dv.getDataElement() );
            assertEquals( ouA.getCode(), dv.getOrgUnit() );
        }
    }

    @Test
    public void testExportOutputIdSchemeAttribute()
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        String attributeIdScheme = IdScheme.ATTR_ID_SCHEME_PREFIX + atA.getUid();

        IdSchemes idSchemes = new IdSchemes()
            .setDataElementIdScheme( attributeIdScheme )
            .setOrgUnitIdScheme( attributeIdScheme )
            .setCategoryOptionComboIdScheme( attributeIdScheme );

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsB ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( peB ) )
            .setOutputIdSchemes( idSchemes );

        dataValueSetService.writeDataValueSetJson( params, out );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( dsB.getUid(), dvs.getDataSet() );
        assertEquals( 2, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( avA.getValue(), dv.getDataElement() );
            assertEquals( avB.getValue(), dv.getOrgUnit() );
        }
    }

    @Test
    public void testExportLastUpdated()
        throws IOException
    {
        Date lastUpdated = getDate( 1970, 1, 1 );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataValueSetService.writeDataValueSetJson( lastUpdated, out, new IdSchemes() );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertEquals( 12, dvs.getDataValues().size() );

        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
        }
    }

    @Test
    public void testExportLastUpdatedWithDeletedValues()
        throws IOException
    {
        DataValue dvA = new DataValue( deC, peA, ouA, cocA, cocA, "1" );
        DataValue dvB = new DataValue( deC, peB, ouA, cocA, cocA, "2" );

        dataValueService.addDataValue( dvA );
        dataValueService.addDataValue( dvB );

        Date lastUpdated = getDate( 1970, 1, 1 );
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        dataValueSetService.writeDataValueSetJson( lastUpdated, out, new IdSchemes() );

        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertEquals( 14, dvs.getDataValues().size() );

        dataValueService.deleteDataValue( dvA );
        dataValueService.deleteDataValue( dvB );

        out = new ByteArrayOutputStream();

        dataValueSetService.writeDataValueSetJson( lastUpdated, out, new IdSchemes() );

        dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );

        assertNotNull( dvs );
        assertEquals( 14, dvs.getDataValues().size() );
    }

    @Test
    public void testMissingDataSetElementGroup()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setPeriods( Sets.newHashSet( peA ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2001 );
    }

    @Test
    public void testMissingPeriodStartEndDate()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouA ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2002 );
    }

    @Test
    public void testPeriodAndStartEndDate()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setPeriods( Sets.newHashSet( peA ) )
            .setStartDate( getDate( 2019, 1, 1 ) )
            .setEndDate( getDate( 2019, 1, 31 ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2003 );
    }

    @Test
    public void testStartDateAfterEndDate()
    {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setStartDate( getDate( 2019, 3, 1 ) )
            .setEndDate( getDate( 2019, 1, 31 ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2004 );
    }

    @Test
    public void testMissingOrgUnit()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2006 );
    }

    @Test
    public void testAtLestOneOrgUnitWithChildren()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA, peB ) )
            .setOrganisationUnitGroups( Sets.newHashSet( ogA ) )
            .setIncludeChildren( true );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2008 );
    }

    @Test
    public void testLimitLimitNotLessThanZero()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA, peB ) )
            .setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setLimit( -2 );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2009 );
    }

    @Test
    public void testAccessOutsideOrgUnitHierarchy()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        DataExportParams params = new DataExportParams()
            .setDataSets( Sets.newHashSet( dsA ) )
            .setOrganisationUnits( Sets.newHashSet( ouC ) )
            .setPeriods( Sets.newHashSet( peA ) );

        assertIllegalQueryEx(
            assertThrows( IllegalQueryException.class, () -> dataValueSetService.writeDataValueSetJson( params, out ) ),
            ErrorCode.E2012 );
    }

}
