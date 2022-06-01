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
package org.hisp.dhis.dxf2.adx;

import static org.hisp.dhis.common.IdScheme.CODE;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetQueryParams;
import org.hisp.dhis.dxf2.datavalueset.DataValueSetService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/*
 * @author Jim Grace
 */
@DirtiesContext
class AdxDataServiceIntegrationTest extends DhisTest
{

    @Autowired
    private AdxDataService adxDataService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private UserService _userService;

    private CategoryOption coUnder5;

    private CategoryOption coOver5;

    private CategoryOption coF;

    private CategoryOption coM;

    private CategoryOption coPepfar;

    private CategoryOption coMcDonalds;

    private Category cAge;

    private Category cSex;

    private Category cMechanism;

    private CategoryCombo ccAgeAndSex;

    private CategoryCombo ccMechanism;

    private CategoryOptionCombo cocFUnder5;

    private CategoryOptionCombo cocMUnder5;

    private CategoryOptionCombo cocFOver5;

    private CategoryOptionCombo cocMOver5;

    private CategoryOptionCombo cocPepfar;

    private CategoryOptionCombo cocMcDonalds;

    private CategoryOptionCombo cocDefault;

    private DataElement deA;

    private DataElement deB;

    private Period pe202001;

    private Period pe202002;

    private Period pe2021Q1;

    private DataSet dsA;

    private DataSet dsB;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnitGroup ougA;

    private User user;

    @Override
    public void setUpTest()
    {
        // UserService
        userService = _userService;
        // Category Option
        coUnder5 = new CategoryOption( "Under 5" );
        coOver5 = new CategoryOption( "Over 5" );
        coF = new CategoryOption( "Female" );
        coM = new CategoryOption( "Male" );
        coPepfar = new CategoryOption( "PEPFAR mechanism" );
        coMcDonalds = new CategoryOption( "McDonalds mechanism" );
        coUnder5.setCode( "under5" );
        coOver5.setCode( "over5" );
        coF.setCode( "F" );
        coM.setCode( "M" );
        coPepfar.setCode( "PEPFAR" );
        coMcDonalds.setCode( "McDonalds" );
        coUnder5.setUid( "under555555" );
        coOver5.setUid( "over5555555" );
        coF.setUid( "FFFFFFFFFFF" );
        coM.setUid( "MMMMMMMMMMM" );
        coPepfar.setUid( "PEPFARRRRRR" );
        coMcDonalds.setUid( "McDonaldsss" );
        idObjectManager.save( coUnder5 );
        idObjectManager.save( coOver5 );
        idObjectManager.save( coF );
        idObjectManager.save( coM );
        idObjectManager.save( coPepfar );
        idObjectManager.save( coMcDonalds );
        // Category
        cAge = createCategory( 'A', coUnder5, coOver5 );
        cSex = createCategory( 'B', coF, coM );
        cMechanism = createCategory( 'C', coPepfar, coMcDonalds );
        cAge.setName( "Age_category" );
        cSex.setName( "Sex_category" );
        cMechanism.setName( "Mechanism_category" );
        cAge.setCode( "age" );
        cSex.setCode( "sex" );
        cMechanism.setCode( "mechanism" );
        cAge.setUid( "ageeeeeeeee" );
        cSex.setUid( "sexxxxxxxxx" );
        cMechanism.setUid( "mechanismmm" );
        idObjectManager.save( cAge );
        idObjectManager.save( cSex );
        idObjectManager.save( cMechanism );
        // Category Combo
        ccAgeAndSex = createCategoryCombo( 'A', cAge, cSex );
        ccMechanism = createCategoryCombo( 'B', cMechanism );
        ccAgeAndSex.setName( "Age and Sex Category Combo" );
        ccMechanism.setName( "Mechanism Category Combo" );
        idObjectManager.save( ccAgeAndSex );
        idObjectManager.save( ccMechanism );
        cAge.setCategoryCombos( Lists.newArrayList( ccAgeAndSex ) );
        cSex.setCategoryCombos( Lists.newArrayList( ccAgeAndSex ) );
        cMechanism.setCategoryCombos( Lists.newArrayList( ccMechanism ) );
        idObjectManager.update( cAge );
        idObjectManager.update( cSex );
        idObjectManager.update( cMechanism );
        // Category Option Combo
        cocFUnder5 = createCategoryOptionCombo( ccAgeAndSex, coF, coUnder5 );
        cocMUnder5 = createCategoryOptionCombo( ccAgeAndSex, coM, coUnder5 );
        cocFOver5 = createCategoryOptionCombo( ccAgeAndSex, coF, coOver5 );
        cocMOver5 = createCategoryOptionCombo( ccAgeAndSex, coM, coOver5 );
        cocPepfar = createCategoryOptionCombo( ccMechanism, coPepfar );
        cocMcDonalds = createCategoryOptionCombo( ccMechanism, coMcDonalds );
        cocFUnder5.setName( "Female Under 5" );
        cocMUnder5.setName( "Male Under 5" );
        cocFOver5.setName( "Female Over 5" );
        cocMOver5.setName( "Male Over 5" );
        cocPepfar.setName( "PEPFAR CategoryOptionCombo" );
        cocMcDonalds.setName( "McDonalds CategoryOptionCombo" );
        cocFUnder5.setCode( "F_Under5" );
        cocMUnder5.setCode( "M_Under5" );
        cocFOver5.setCode( "F_Over5" );
        cocMOver5.setCode( "M_Over5" );
        cocPepfar.setCode( "coc_PEPFAR" );
        cocMcDonalds.setCode( "coc_McDonalds" );
        cocFUnder5.setUid( "FUnder55555" );
        cocMUnder5.setUid( "MUnder55555" );
        cocFOver5.setUid( "FOver555555" );
        cocMOver5.setUid( "MOver555555" );
        cocPepfar.setUid( "cocPEPFARRR" );
        cocMcDonalds.setUid( "cocMcDonald" );
        idObjectManager.save( cocFUnder5 );
        idObjectManager.save( cocMUnder5 );
        idObjectManager.save( cocFOver5 );
        idObjectManager.save( cocMOver5 );
        idObjectManager.save( cocPepfar );
        idObjectManager.save( cocMcDonalds );
        ccAgeAndSex.getOptionCombos().add( cocFUnder5 );
        ccAgeAndSex.getOptionCombos().add( cocMUnder5 );
        ccAgeAndSex.getOptionCombos().add( cocFOver5 );
        ccAgeAndSex.getOptionCombos().add( cocMOver5 );
        ccMechanism.getOptionCombos().add( cocPepfar );
        ccMechanism.getOptionCombos().add( cocMcDonalds );
        idObjectManager.update( ccAgeAndSex );
        idObjectManager.update( ccMechanism );
        cocDefault = categoryService.getDefaultCategoryOptionCombo();
        // Data Element
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deA.setName( "Malaria numeric" );
        deB.setName( "Malaria text" );
        deA.setCode( "Mal_num" );
        deB.setCode( "Mal_text" );
        deA.setUid( "MalNummmmmm" );
        deB.setUid( "MalTexttttt" );
        deA.setCategoryCombo( ccAgeAndSex );
        deB.setValueType( ValueType.TEXT );
        idObjectManager.save( deA );
        idObjectManager.save( deB );
        // Period
        pe202001 = PeriodType.getPeriodFromIsoString( "202001" );
        pe202002 = PeriodType.getPeriodFromIsoString( "202002" );
        pe2021Q1 = PeriodType.getPeriodFromIsoString( "2021Q1" );
        periodService.addPeriod( pe202001 );
        periodService.addPeriod( pe202002 );
        periodService.addPeriod( pe2021Q1 );
        // Data Set
        dsA = createDataSet( 'A', PeriodType.getPeriodTypeByName( "Monthly" ) );
        dsB = createDataSet( 'B', PeriodType.getPeriodTypeByName( "Quarterly" ) );
        dsA.setName( "Malaria DS" );
        dsB.setName( "Malaria Mechanism DS" );
        dsA.setCode( "MalariaDS" );
        dsB.setCode( "MalariaMechanismDS" );
        dsA.setUid( "MalariaDSSS" );
        dsB.setUid( "MalariaMech" );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        dsB.addDataSetElement( deA );
        dsB.addDataSetElement( deB );
        dsB.setCategoryCombo( ccMechanism );
        idObjectManager.save( dsA );
        idObjectManager.save( dsB );
        // Organisation Unit
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouA.setName( "Provincial Hospital" );
        ouB.setName( "District Hospital" );
        ouA.setCode( "123" );
        ouB.setCode( "456" );
        ouA.setUid( "P1233333333" );
        ouB.setUid( "D4566666666" );
        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        // Organisation Unit Group
        ougA = createOrganisationUnitGroup( 'A' );
        ougA.addOrganisationUnit( ouA );
        ougA.addOrganisationUnit( ouB );
        organisationUnitGroupService.addOrganisationUnitGroup( ougA );
        // User & Current User Service
        user = createAndInjectAdminUser();
        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        userService.addUser( user );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, dataValueSetService, organisationUnitService );
    }

    // --------------------------------------------------------------------------
    // Test get data export params from URL arguments
    // --------------------------------------------------------------------------
    @Test
    void testGetFromUrl1()
    {
        Date now = new Date();
        DataExportParams expected = new DataExportParams().setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( pe202001 ) ).setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setIncludeDescendants( true ).setIncludeDeleted( false ).setLastUpdated( now ).setLimit( 999 )
            .setOutputIdSchemes( new IdSchemes().setIdScheme( "CODE" ) );
        DataExportParams actual = adxDataService
            .getFromUrl( DataValueSetQueryParams.builder().dataSet( Sets.newHashSet( dsA.getUid() ) )
                .period( Sets.newHashSet( "202001" ) ).orgUnit( Sets.newHashSet( ouA.getUid() ) ).children( true )
                .includeDeleted( false ).lastUpdated( now ).limit( 999 ).build() );
        assertEquals( expected.toString(), actual.toString() );
    }

    @Test
    void testGetFromUrl2()
    {
        Date then = new Date( 1L );
        Date now = new Date();
        DataExportParams expected = new DataExportParams().setDataSets( Sets.newHashSet( dsB ) ).setStartDate( then )
            .setEndDate( now ).setLastUpdatedDuration( "10d" ).setOrganisationUnits( Sets.newHashSet( ouB ) )
            .setOrganisationUnitGroups( Sets.newHashSet( ougA ) )
            .setAttributeOptionCombos( Sets.newHashSet( cocMcDonalds ) ).setIncludeDescendants( false )
            .setIncludeDeleted( true ).setLastUpdated( now ).setOutputIdSchemes( new IdSchemes().setIdScheme( "UID" ) );
        DataExportParams actual = adxDataService
            .getFromUrl( DataValueSetQueryParams.builder().dataSet( Sets.newHashSet( dsB.getCode() ) ).startDate( then )
                .endDate( now ).orgUnit( Sets.newHashSet( ouB.getCode() ) ).children( false )
                .orgUnitGroup( Sets.newHashSet( ougA.getCode() ) )
                .attributeOptionCombo( Sets.newHashSet( cocMcDonalds.getUid() ) ).includeDeleted( true )
                .lastUpdated( now ).lastUpdatedDuration( "10d" ).idScheme( IdentifiableProperty.UID.name() ).build() );
        assertEquals( expected.toString(), actual.toString() );
    }

    // --------------------------------------------------------------------------
    // Test export
    // --------------------------------------------------------------------------
    @Test
    void testWriteDataValueSetA()
        throws AdxException,
        IOException
    {
        testExport( "adx/exportA.adx.xml",
            getCommonExportParams().setOutputIdSchemes( new IdSchemes().setDefaultIdScheme( CODE )
                .setDataElementIdScheme( "NAME" ).setCategoryIdScheme( "NAME" ).setCategoryOptionIdScheme( "UID" ) ) );
    }

    @Test
    void testWriteDataValueSetB()
        throws AdxException,
        IOException
    {
        testExport( "adx/exportB.adx.xml", getCommonExportParams()
            .setOutputIdSchemes( new IdSchemes().setDefaultIdScheme( CODE ).setDataSetIdScheme( "NAME" )
                .setOrgUnitIdScheme( "UID" ).setDataElementIdScheme( "UID" ).setCategoryOptionComboIdScheme( "NAME" ) )
            .setOrganisationUnitGroups( Sets.newHashSet( ougA ) ) );
    }

    @Test
    void testWriteDataValueSetC()
        throws AdxException,
        IOException
    {
        testExport( "adx/exportC.adx.xml",
            getCommonExportParams()
                .setOutputIdSchemes( new IdSchemes().setDefaultIdScheme( CODE ).setDataSetIdScheme( "UID" )
                    .setOrgUnitIdScheme( "NAME" ).setCategoryIdScheme( "UID" ).setCategoryOptionIdScheme( "NAME" ) )
                .setIncludeDescendants( true ) );
    }

    @Test
    void testWriteDataValueSetD()
        throws AdxException,
        IOException
    {
        testExport( "adx/exportD.adx.xml",
            getCommonExportParams()
                .setOutputIdSchemes( new IdSchemes().setDefaultIdScheme( CODE ).setDataSetIdScheme( "UID" )
                    .setOrgUnitIdScheme( "NAME" ).setCategoryIdScheme( "UID" ).setCategoryOptionIdScheme( "NAME" ) )
                .setIncludeDescendants( true ).setAttributeOptionCombos( Sets.newHashSet( cocMcDonalds ) ) );
    }

    // --------------------------------------------------------------------------
    // Test import
    // --------------------------------------------------------------------------
    @Test
    void testGetAllDataValuesA()
        throws IOException
    {
        testImport( "adx/importA.adx.xml", new IdSchemes().setDefaultIdScheme( CODE ).setDataElementIdScheme( "NAME" )
            .setCategoryIdScheme( "NAME" ).setCategoryOptionIdScheme( "UID" ).setCategoryOptionComboIdScheme( "UID" ) );
    }

    @Test
    void testGetAllDataValuesB()
        throws IOException
    {
        testImport( "adx/importB.adx.xml", new IdSchemes().setDefaultIdScheme( CODE ).setDataSetIdScheme( "NAME" )
            .setOrgUnitIdScheme( "UID" ).setDataElementIdScheme( "UID" ).setCategoryOptionComboIdScheme( "NAME" ) );
    }

    @Test
    void testGetAllDataValuesC()
        throws IOException
    {
        testImport( "adx/importC.adx.xml", new IdSchemes().setDefaultIdScheme( CODE ).setDataSetIdScheme( "UID" )
            .setOrgUnitIdScheme( "NAME" ).setCategoryIdScheme( "UID" ).setCategoryOptionIdScheme( "NAME" ) );
    }

    // --------------------------------------------------------------------------
    // Supportive methods
    // --------------------------------------------------------------------------
    private DataExportParams getCommonExportParams()
    {
        return new DataExportParams().setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setPeriods( Sets.newHashSet( pe202001, pe202002 ) ).setDataSets( Sets.newHashSet( dsA, dsB ) );
    }

    private void testExport( String filePath, DataExportParams params )
        throws AdxException,
        IOException
    {
        dataValueService.addDataValue( new DataValue( deA, pe202001, ouA, cocFUnder5, cocDefault, "1" ) );
        dataValueService.addDataValue( new DataValue( deB, pe202002, ouA, cocDefault, cocDefault, "Some text" ) );
        dataValueService.addDataValue( new DataValue( deA, pe202001, ouB, cocMOver5, cocMcDonalds, "2" ) );
        dataValueService.addDataValue( new DataValue( deA, pe202001, ouB, cocFOver5, cocPepfar, "3" ) );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        adxDataService.writeDataValueSet( params, out );
        String result = out.toString( "UTF-8" );
        InputStream expectedStream = new ClassPathResource( filePath ).getInputStream();
        String expected = new BufferedReader( new InputStreamReader( expectedStream ) ).lines().map( String::trim )
            .collect( Collectors.joining() );
        assertEquals( adxGroups( expected ), adxGroups( result ) );
    }

    // The adx groups could be in any order, but each contains only one value
    private Set<String> adxGroups( String adx )
    {
        return Sets.newHashSet( adx.split( "</*group" ) );
    }

    private void testImport( String filePath, IdSchemes idSchemes )
        throws IOException
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );
        InputStream in = new ClassPathResource( filePath ).getInputStream();
        ImportOptions importOptions = ImportOptions.getDefaultImportOptions();
        importOptions.setIdSchemes( idSchemes );
        adxDataService.saveDataValueSet( in, importOptions, null );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertContainsOnly( dataValues, new DataValue( deA, pe202001, ouA, cocFUnder5, cocDefault, "1" ),
            new DataValue( deA, pe202001, ouA, cocMUnder5, cocDefault, "2" ),
            new DataValue( deA, pe202001, ouA, cocFOver5, cocDefault, "3" ),
            new DataValue( deA, pe202001, ouA, cocMOver5, cocDefault, "4" ),
            new DataValue( deB, pe202001, ouA, cocDefault, cocDefault, "Text data value" ),
            new DataValue( deA, pe202002, ouB, cocFUnder5, cocDefault, "6" ),
            new DataValue( deA, pe2021Q1, ouB, cocFUnder5, cocPepfar, "10" ),
            new DataValue( deA, pe2021Q1, ouB, cocFOver5, cocMcDonalds, "20" ),
            new DataValue( deA, pe2021Q1, ouB, cocMUnder5, cocMcDonalds, "30" ) );
    }
}
