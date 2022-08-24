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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.datavalue.DataValueAuditService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class DataValueSetServiceIntegrationTest extends IntegrationTestBase
{
    private final String ATTRIBUTE_UID = "uh6H2ff562G";

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataValueAuditService dataValueAuditService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueSetService dataValueSetServiceNoMocks;

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private UserService _userService;

    private CategoryOptionCombo ocDef;

    private CategoryOption categoryOptionA;

    private CategoryOption categoryOptionB;

    private CategoryOptionCombo ocA;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataSet dsA;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private Period peA;

    private Period peB;

    private User user;

    private User superUser;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        superUser = preCreateInjectAdminUser();
        injectSecurityContext( superUser );

        CategoryOptionCombo categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        Attribute attribute = new Attribute( "CUSTOM_ID", ValueType.TEXT );
        attribute.setUid( ATTRIBUTE_UID );
        attribute.setUnique( true );
        attribute.setOrganisationUnitAttribute( true );
        attribute.setDataElementAttribute( true );
        idObjectManager.save( attribute );
        categoryOptionA = createCategoryOption( 'A' );
        categoryOptionB = createCategoryOption( 'B' );
        Category categoryA = createCategory( 'A' );
        categoryService.addCategory( categoryA );
        categoryOptionA.getCategories().add( categoryA );
        categoryOptionB.getCategories().add( categoryA );
        categoryService.addCategoryOption( categoryOptionB );
        categoryService.addCategoryOption( categoryOptionA );
        CategoryCombo categoryComboA = createCategoryCombo( 'A', categoryA );
        CategoryCombo categoryComboDef = categoryService.getDefaultCategoryCombo();
        ocDef = categoryService.getDefaultCategoryOptionCombo();
        ocDef.setCode( "OC_DEF_CODE" );
        categoryService.updateCategoryOptionCombo( ocDef );
        OptionSet osA = new OptionSet( "OptionSetA", ValueType.INTEGER );
        osA.getOptions().add( new Option( "Blue", "1" ) );
        osA.getOptions().add( new Option( "Green", "2" ) );
        osA.getOptions().add( new Option( "Yellow", "3" ) );
        ocA = createCategoryOptionCombo( categoryComboA, categoryOptionA );
        CategoryOptionCombo ocB = createCategoryOptionCombo( categoryComboA, categoryOptionB );
        deA = createDataElement( 'A', categoryComboDef );
        deB = createDataElement( 'B', categoryComboDef );
        deC = createDataElement( 'C', categoryComboDef );
        DataElement deD = createDataElement( 'D', categoryComboDef );
        DataElement deE = createDataElement( 'E' );
        deE.setOptionSet( osA );
        DataElement deF = createDataElement( 'F', categoryComboDef );
        deF.setValueType( ValueType.BOOLEAN );
        DataElement deG = createDataElement( 'G', categoryComboDef );
        deG.setValueType( ValueType.TRUE_ONLY );
        dsA = createDataSet( 'A', new MonthlyPeriodType() );
        dsA.setCategoryCombo( categoryComboDef );
        DataSet dsB = createDataSet( 'B' );
        dsB.setCategoryCombo( categoryComboDef );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        OrganisationUnit ouC = createOrganisationUnit( 'C' );
        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 1, 1 ),
            getDate( 2012, 1, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 2, 1 ),
            getDate( 2012, 2, 29 ) );
        Period peC = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 3, 1 ),
            getDate( 2012, 3, 31 ) );
        ocA.setUid( "kjuiHgy67hg" );
        ocB.setUid( "Gad33qy67g5" );
        deA.setUid( "f7n9E0hX8qk" );
        deB.setUid( "Ix2HsbDMLea" );
        deC.setUid( "eY5ehpbEsB7" );
        deE.setUid( "jH26dja2f28" );
        deF.setUid( "jH26dja2f30" );
        deG.setUid( "jH26dja2f31" );
        dsA.setUid( "pBOMPrpg1QX" );
        ouA.setUid( "DiszpKrYNg8" );
        ouB.setUid( "BdfsJfj87js" );
        ouC.setUid( "j7Hg26FpoIa" );
        ocA.setCode( "OC_A" );
        ocB.setCode( "OC_B" );
        deA.setCode( "DE_A" );
        deB.setCode( "DE_B" );
        deC.setCode( "DE_C" );
        deD.setCode( "DE_D" );
        dsA.setCode( "DS_A" );
        ouA.setCode( "OU_A" );
        ouB.setCode( "OU_B" );
        ouC.setCode( "OU_C" );
        categoryService.addCategoryCombo( categoryComboA );
        categoryService.addCategoryOptionCombo( ocA );
        categoryService.addCategoryOptionCombo( ocB );
        AttributeValue av1 = createAttributeValue( attribute, "DE1" );
        dataElementService.addDataElement( deA );
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deF );
        dataElementService.addDataElement( deG );
        attributeService.addAttributeValue( deA, av1 );
        attributeService.addAttributeValue( deB, createAttributeValue( attribute, "DE2" ) );
        attributeService.addAttributeValue( deC, createAttributeValue( attribute, "DE3" ) );
        attributeService.addAttributeValue( deD, createAttributeValue( attribute, "DE4" ) );
        idObjectManager.save( osA );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        dsA.addDataSetElement( deC );
        dsA.addDataSetElement( deD );
        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );
        attributeService.addAttributeValue( ouA, createAttributeValue( attribute, "OU1" ) );
        attributeService.addAttributeValue( ouB, createAttributeValue( attribute, "OU2" ) );
        attributeService.addAttributeValue( ouC, createAttributeValue( attribute, "OU3" ) );
        dsA.addOrganisationUnit( ouA );
        dsA.addOrganisationUnit( ouC );
        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
        periodService.addPeriod( peC );
        dataSetService.addDataSet( dsA );

        user = createAndAddUser( false, "A", null, Authorities.F_SKIP_DATA_IMPORT_AUDIT.getAuthority() );
        user.addOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        userService.updateUser( user );

        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_READ_WRITE );
        userService.updateUser( user );
        injectSecurityContext( user );

        CompleteDataSetRegistration completeDataSetRegistration = new CompleteDataSetRegistration( dsA, peA, ouA,
            categoryOptionCombo, getDate( 2012, 1, 9 ), "userA", new Date(), "userA", true );
        registrationService.saveCompleteDataSetRegistration( completeDataSetRegistration );
    }

    /**
     * Import 1 data value.
     */
    @Test
    void testImportValueJson()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
    }

    /**
     * Import 1 data value, then delete it by using import mode DELETE
     */
    @Test
    void testImportDeleteValueJson()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );

        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_OmittingValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );

        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1, dataValueSetService
            .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteNoValue.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_NewValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );

        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1, dataValueSetService
            .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteNewValue.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_ZeroValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1,
            dataValueSetService
                .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteZeroValue.json" ) ) );
        assertDataValuesCount( 0 );
    }

    /**
     * Import 3 data values, then delete 3 data values.
     */
    @Test
    void testImportDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetA.xml" ) );
        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );

        // Delete values
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetADeleted.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 3, summary );
        assertDataValuesCount( 0 );
    }

    /**
     * Import 12 data values.
     */
    @Test
    void testImportValuesXml()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 12 );
    }

    /**
     * Import 12 data values. Then import 6 data values, where 4 are updates.
     */
    @Test
    void testImportUpdateValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );

        // Update
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetBUpdate.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 2, 4, 0, summary );
        assertDataValuesCount( 14 );
    }

    /**
     * When updating a data value with a specified created date, the specified
     * created date should be used.
     * <p>
     * When updating a data value without a specified created date, the existing
     * created date should remain unchanged.
     */
    @Test
    void testUpdateCreatedDate()
    {
        // Insert:
        // deC, peA, ouA created = 2010-01-01
        // deC, peA, ouB created = 2010-01-01
        dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );
        // Update:
        // deC, peA, ouA created = not specified, should remain unchanged
        // deC, peA, ouB created = 2020-02-02
        dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetBUpdate.xml" ) );

        CategoryOptionCombo cc = categoryService.getDefaultCategoryOptionCombo();
        DataValue dv1 = dataValueService.getDataValue( deC, peA, ouA, cc, cc );
        assertEquals( "2010-01-01", getMediumDateString( dv1.getCreated() ) );
        DataValue dv2 = dataValueService.getDataValue( deC, peA, ouB, cc, cc );
        assertEquals( "2020-02-02", getMediumDateString( dv2.getCreated() ) );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Deleted values
     * should count as imports when there are no existing non-deleted matching
     * values.
     */
    @Test
    void testImportDeletedValuesXml()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetBDeleted.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 8 );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values and update the other 8
     * values.
     */
    @Test
    void testImportReverseDeletedValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetBDeleted.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 8 );

        // Reverse deletion and update
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 4, 8, 0, summary );
        assertDataValuesCount( 12 );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values, update 4 values and add 4
     * values.
     */
    @Test
    void testImportAddAndReverseDeletedValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetBDeleted.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 8 );

        // Reverse deletion and update
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetBNew.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 8, 4, 0, summary );
        assertDataValuesCount( 16 );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted.
     */
    @Test
    void testDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );

        // Delete 4 values
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetBDeleted.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 0, 8, 4, summary );
        assertDataValuesCount( 8 );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted, 6 are updates and 2 are new.
     */
    @Test
    void testImportAndDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );

        // Delete 4 values, add 2 values
        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetBNewDeleted.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 2, 6, 4, summary );
        assertDataValuesCount( 10 );
    }

    /**
     * Import 12 data values. Then import the same 12 data values with import
     * strategy delete.
     */
    @Test
    void testImportValuesDeleteStrategyXml()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ) );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );

        // Import with delete strategy
        ImportOptions options = new ImportOptions().setStrategy( ImportStrategy.DELETE );

        summary = dataValueSetService.importDataValueSetXml( readFile( "datavalueset/dataValueSetB.xml" ), options );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 12, summary );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDataValueSetXml()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertEquals( "10002", dataValues.get( 1 ).getValue() );
        assertEquals( "10003", dataValues.get( 2 ).getValue() );

        List<Executable> audits = dataValues.stream()
            .map( dv -> ((Executable) () -> assertEquals( List.of(), dataValueAuditService.getDataValueAudits( dv ) )) )
            .collect( Collectors.toList() );
        assertAll( "no audit expected", audits );

        // TODO This throw an error : "org.postgresql.util.PSQLException: ERROR:
        // cannot execute UPDATE in a read-only transaction"
        // Need to investigate
        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA,
            ocDef );
        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );
    }

    @Test
    void testImportDataValueSetXmlPreheatCache()
    {
        ImportOptions importOptions = new ImportOptions().setPreheatCache( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertEquals( "10002", dataValues.get( 1 ).getValue() );
        assertEquals( "10003", dataValues.get( 2 ).getValue() );

        List<Executable> audits = dataValues.stream()
            .map( dv -> ((Executable) () -> assertEquals( List.of(), dataValueAuditService.getDataValueAudits( dv ) )) )
            .collect( Collectors.toList() );
        assertAll( "no audit expected", audits );

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA,
            ocDef );
        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );
    }

    @Test
    void testImportDataValuesXmlWithCodeA()
    {

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetACode.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );

        List<Executable> audits = dataValues.stream()
            .map( dv -> ((Executable) () -> assertEquals( List.of(), dataValueAuditService.getDataValueAudits( dv ) )) )
            .collect( Collectors.toList() );
        assertAll( "no audit expected", audits );

        CompleteDataSetRegistration registration = registrationService.getCompleteDataSetRegistration( dsA, peA, ouA,
            ocDef );
        assertNotNull( registration );
        assertEquals( dsA, registration.getDataSet() );
        assertEquals( peA, registration.getPeriod() );
        assertEquals( ouA, registration.getSource() );
        assertEquals( getDate( 2012, 1, 9 ), registration.getDate() );
    }

    @Test
    void testImportDataValuesXml()
    {

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetB.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesXmlWithCodeB()
    {
        ImportOptions importOptions = new ImportOptions().setIdScheme( "CODE" ).setDataElementIdScheme( "CODE" )
            .setOrgUnitIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetBCode.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesXmlWithAttribute()
    {
        ImportOptions importOptions = new ImportOptions().setIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setDataElementIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setOrgUnitIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetBAttribute.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesXmlWithAttributeIdSchemeInPayload()
    {
        // Identifier schemes specified in XML message
        ImportSummary summary = dataValueSetService.importDataValueSetXml(
            readFile( "dxf2/datavalueset/dataValueSetBAttributeIdScheme.xml" ), new ImportOptions() );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesXmlWithAttributePreheatCacheTrue()
    {
        ImportOptions importOptions = new ImportOptions().setPreheatCache( true )
            .setIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setDataElementIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID )
            .setOrgUnitIdScheme( IdScheme.ATTR_ID_SCHEME_PREFIX + ATTRIBUTE_UID );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetBAttribute.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesXmlWithCodePreheatCacheTrue()
    {
        ImportOptions importOptions = new ImportOptions().setPreheatCache( true ).setIdScheme( "CODE" )
            .setDataElementIdScheme( "CODE" ).setOrgUnitIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetBCode.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertImportDataValues( summary );
    }

    @Test
    void testImportDataValuesCsv()
    {
        ImportSummary summary = dataValueSetService
            .importDataValueSetCsv( readFile( "dxf2/datavalueset/dataValueSetB.csv" ), null, null );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
    }

    @Test
    void testImportDataValuesCsvWithDataSetIdParameter()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetCsv( readFile( "dxf2/datavalueset/dataValueSetWithDataSetHeader.csv" ),
                new ImportOptions().setDataSet( "pBOMPrpg1QX" ), null );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );
    }

    @Test
    void testImportDataValuesCsvWithoutHeader()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService.importDataValueSetCsv(
            readFile( "dxf2/datavalueset/dataValueSetBNoHeader.csv" ),
            new ImportOptions().setFirstRowIsHeader( false ), null );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 12 );
    }

    @Test
    void testImportDataValuesBooleanCsv()
    {
        ImportConflicts summary = dataValueSetService
            .importDataValueSetCsv( readFile( "dxf2/datavalueset/dataValueSetBooleanTest.csv" ), null, null );

        String description = summary.getConflictsDescription();
        assertEquals( 4, summary.getTotalConflictOccurrenceCount(), description );
        assertEquals( 4, summary.getConflictOccurrenceCount( ErrorCode.E7619 ), description );
        assertEquals( 2, summary.getConflictCount(), description );
        Iterator<ImportConflict> conflicts = summary.getConflicts().iterator();
        assertArrayEquals( new int[] { 10, 11 }, conflicts.next().getIndexes() );
        assertArrayEquals( new int[] { 16, 17 }, conflicts.next().getIndexes() );
        List<String> expectedBools = Lists.newArrayList( "true", "false" );
        List<DataValue> resultBools = dataValueService.getAllDataValues();
        for ( DataValue dataValue : resultBools )
        {
            assertTrue( expectedBools.contains( dataValue.getValue() ) );
        }
    }

    @Test
    void testImportDataValuesXmlDryRun()
    {
        assertDataValuesCount( 0 );

        ImportOptions importOptions = new ImportOptions().setDryRun( true ).setIdScheme( "UID" )
            .setDataElementIdScheme( "UID" ).setOrgUnitIdScheme( "UID" );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetB.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDataValuesXmlUpdatesOnly()
    {
        assertDataValuesCount( 0 );

        ImportOptions importOptions = new ImportOptions().setImportStrategy( ImportStrategy.UPDATES );
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setIdScheme( "UID" );
        idSchemes.setDataElementIdScheme( "UID" );
        idSchemes.setOrgUnitIdScheme( "UID" );
        importOptions.setIdSchemes( idSchemes );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetB.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 0, 0, 0, 12, summary );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDataValuesWithNewPeriod()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetC.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );
    }

    @Test
    void testImportDataValuesWithCategoryOptionComboIdScheme()
    {
        assertDataValuesCount( 0 );
        ImportOptions options = new ImportOptions().setCategoryOptionComboIdScheme( "CODE" );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetCCode.xml" ), options );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );
    }

    @Test
    void testImportDataValuesWithAttributeOptionCombo()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetD.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 3, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocA ) ) );
    }

    @Test
    void testImportDataValuesWithOrgUnitOutsideHierarchy()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetE.xml" ) );

        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        assertEquals( 2, summary.getConflictCount(), summary.getConflictsDescription() );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
    }

    @Test
    void testImportDataValuesWithInvalidAttributeOptionCombo()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetF.xml" ) );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDataValuesWithNonExistingDataElementOrgUnit()
    {
        assertDataValuesCount( 0 );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetG.xml" ) );

        assertEquals( 2, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 3, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        assertDataValuesCount( 1 );
    }

    @Test
    void testImportDataValuesWithStrictPeriods()
    {
        ImportOptions options = new ImportOptions().setStrictPeriods( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );

        assertEquals( 2, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesWithStrictCategoryOptionCombos()
    {
        ImportOptions options = new ImportOptions().setStrictCategoryOptionCombos( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );

        assertEquals( 1, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesWithStrictAttributeOptionCombos()
    {
        ImportOptions options = new ImportOptions().setStrictAttributeOptionCombos( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );
        assertEquals( 1, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesWithRequiredCategoryOptionCombo()
    {
        ImportOptions options = new ImportOptions().setRequireCategoryOptionCombo( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );

        String description = summary.getConflictsDescription();
        assertEquals( 2, summary.getTotalConflictOccurrenceCount(), description );
        assertEquals( 1, summary.getConflictCount(), description );
        assertArrayEquals( new int[] { 1, 2 }, summary.getConflicts().iterator().next().getIndexes() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesWithRequiredAttributeOptionCombo()
    {
        ImportOptions options = new ImportOptions().setRequireAttributeOptionCombo( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );

        String description = summary.getConflictsDescription();
        assertEquals( 2, summary.getTotalConflictOccurrenceCount(), description );
        assertEquals( 1, summary.getConflictCount(), description );
        assertArrayEquals( new int[] { 0, 2 }, summary.getConflicts().iterator().next().getIndexes() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesWithStrictOrganisationUnits()
    {
        ImportOptions options = new ImportOptions().setStrictOrganisationUnits( true );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetNonStrict.xml" ), options );

        assertEquals( 1, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesInvalidOptionCode()
    {
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetInvalid.xml" ) );

        assertEquals( 1, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    @Test
    void testImportDataValuesInvalidAttributeOptionComboDates()
    {
        injectSecurityContext( superUser );
        categoryOptionA.setStartDate( peB.getStartDate() );
        categoryOptionA.setEndDate( peB.getEndDate() );
        categoryService.updateCategoryOption( categoryOptionA );
        injectSecurityContext( user );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetH.xml" ) );

        assertEquals( 2, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 1, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocA ) ) );
    }

    @Test
    void testImportDataValuesInvalidAttributeOptionComboOrgUnit()
    {
        injectSecurityContext( superUser );
        categoryOptionA.setOrganisationUnits( Sets.newHashSet( ouA, ouB ) );
        categoryService.updateCategoryOption( categoryOptionA );
        injectSecurityContext( user );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetH.xml" ) );

        assertEquals( 1, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 1, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocA ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocA ) ) );
    }

    @Test
    void testImportDataValuesUpdatedAudit()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ), new ImportOptions() );
        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );

        summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetAUpdate.xml" ), new ImportOptions() );

        assertSuccessWithImportedUpdatedDeleted( 0, 3, 0, summary );
        List<DataValue> dataValues = assertDataValuesCount( 3 );
        assertAll( "expected data value update(s) to be audited", dataValues.stream().map( dv -> (Executable) () -> {
            List<DataValueAudit> audits = dataValueAuditService.getDataValueAudits( dv );
            assertNotNull( audits );
            assertEquals( 1, audits.size(),
                () -> String.format( "expected change to dataValue %s to be audited once", dv ) );
            assertEquals( AuditType.UPDATE, audits.get( 0 ).getAuditType() );
        } ).collect( Collectors.toList() ) );
    }

    @Test
    void testImportDataValuesUpdatedSkipAudit()
    {
        assertDataValuesCount( 0 );
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ), new ImportOptions() );
        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );

        ImportOptions importOptions = new ImportOptions();
        importOptions.setSkipAudit( true );

        summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetAUpdate.xml" ), importOptions );

        assertSuccessWithImportedUpdatedDeleted( 0, 3, 0, summary );
        List<DataValue> dataValues = assertDataValuesCount( 3 );
        assertAll( "expected data value update(s) NOT to be audited",
            dataValues.stream()
                .map(
                    dv -> (Executable) () -> assertEquals( List.of(), dataValueAuditService.getDataValueAudits( dv ) ) )
                .collect( Collectors.toList() ) );
    }

    @Test
    void testImportNullDataValues()
    {
        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetANull.xml" ) );

        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        assertEquals( 2, summary.getImportCount().getIgnored() );
        assertEquals( 1, summary.getImportCount().getImported() );
        assertEquals( 2, summary.getConflictCount(), summary.getConflictsDescription() );
        assertDataValuesCount( 1 );
    }

    @Test
    void testImportDataValuesWithDataSetAllowsPeriods()
    {
        injectSecurityContext( superUser );
        Date thisMonth = DateUtils.truncate( new Date(), Calendar.MONTH );
        dsA.setExpiryDays( 62 );
        dsA.setOpenFuturePeriods( 2 );
        dataSetService.updateDataSet( dsA );
        Period tooEarly = createMonthlyPeriod( DateUtils.addMonths( thisMonth, 4 ) );
        Period okBefore = createMonthlyPeriod( DateUtils.addMonths( thisMonth, 1 ) );
        Period okAfter = createMonthlyPeriod( DateUtils.addMonths( thisMonth, -1 ) );
        Period tooLate = createMonthlyPeriod( DateUtils.addMonths( thisMonth, -4 ) );
        Period outOfRange = createMonthlyPeriod( DateUtils.addMonths( thisMonth, 6 ) );
        periodService.addPeriod( tooEarly );
        periodService.addPeriod( okBefore );
        periodService.addPeriod( okAfter );
        periodService.addPeriod( tooLate );
        String importData = "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\" idScheme=\"code\" dataSet=\"DS_A\" orgUnit=\"OU_A\">\n"
            + "  <dataValue dataElement=\"DE_A\" period=\"" + tooEarly.getIsoDate() + "\" value=\"10001\" />\n"
            + "  <dataValue dataElement=\"DE_B\" period=\"" + okBefore.getIsoDate() + "\" value=\"10002\" />\n"
            + "  <dataValue dataElement=\"DE_C\" period=\"" + okAfter.getIsoDate() + "\" value=\"10003\" />\n"
            + "  <dataValue dataElement=\"DE_D\" period=\"" + tooLate.getIsoDate() + "\" value=\"10004\" />\n"
            + "  <dataValue dataElement=\"DE_D\" period=\"" + outOfRange.getIsoDate() + "\" value=\"10005\" />\n"
            + "</dataValueSet>\n";
        injectSecurityContext( user );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( new ByteArrayInputStream( importData.getBytes( StandardCharsets.UTF_8 ) ) );

        assertEquals( 3, summary.getConflictCount(), summary.getConflictsDescription() );
        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertEquals( 3, summary.getImportCount().getIgnored() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 2, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deB, okBefore, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, okAfter, ouA, ocDef, ocDef ) ) );
    }

    /**
     * User does not have data write access for DataSet Expect fail on data
     * sharing check
     *
     */
    @Test
    void testImportValueDataSetWriteFail()
    {
        clearSecurityContext();
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ );
        dataSetService.updateDataSet( dsA );
        injectSecurityContext( user );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ) );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet DataValue use default category
     * combo Expect success
     *
     */
    @Test
    void testImportValueDefaultCatComboOk()
    {
        injectSecurityContext( superUser );
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        dataSetService.updateDataSet( dsA );
        injectSecurityContext( user );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
    }

    /**
     * User has data write access for DataSet and data read access for
     * categoryOptions Expect fail
     *
     */
    @Test
    void testImportValueCatComboFail()
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.READ );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.READ );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetACatCombo.xml" ) );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.WARNING, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet and also categoryOptions Expect
     * success
     *
     */
    @Test
    void testImportValueCatComboOk()
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_WRITE );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetACatCombo.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
    }

    /**
     * User does not have data write access for DataSet Expect fail
     *
     */
    @Test
    void testImportValueCatComboFailDS()
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_WRITE );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetACatCombo.xml" ) );

        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        assertEquals( ImportStatus.ERROR, summary.getStatus() );
    }

    /**
     * User has data write access for DataSet and CategoryOption
     *
     */
    @Test
    void testImportValueCategoryOptionWriteOk()
    {
        enableDataSharing( user, dsA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionA, AccessStringHelper.DATA_READ_WRITE );
        enableDataSharing( user, categoryOptionB, AccessStringHelper.DATA_READ_WRITE );

        ImportSummary summary = dataValueSetService
            .importDataValueSetXml( readFile( "dxf2/datavalueset/dataValueSetA.xml" ) );

        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
    }

    @Test
    void testImportDataValueWithNewPeriods()
    {
        Period period200006 = periodService.getPeriod( "200006" );
        Period period200007 = periodService.getPeriod( "200007" );
        Period period200008 = periodService.getPeriod( "200008" );
        assertNull( period200006 );
        assertNull( period200007 );
        assertNull( period200008 );
        String importData = "<dataValueSet xmlns=\"http://dhis2.org/schema/dxf/2.0\" idScheme=\"code\" dataSet=\"DS_A\" orgUnit=\"OU_A\">\n"
            + "  <dataValue dataElement=\"DE_A\" period=\"200006\" value=\"10001\" />\n"
            + "  <dataValue dataElement=\"DE_B\" period=\"200006\" value=\"10002\" />\n"
            + "  <dataValue dataElement=\"DE_C\" period=\"200007\" value=\"10003\" />\n"
            + "  <dataValue dataElement=\"DE_D\" period=\"200007\" value=\"10004\" />\n"
            + "  <dataValue dataElement=\"DE_D\" period=\"200008\" value=\"10005\" />\n" + "</dataValueSet>\n";

        ImportSummary summary = dataValueSetServiceNoMocks
            .importDataValueSetXml( new ByteArrayInputStream( importData.getBytes( StandardCharsets.UTF_8 ) ) );

        assertSuccessWithImportedUpdatedDeleted( 5, 0, 0, summary );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
    private void assertImportDataValues( ImportSummary summary )
    {
        assertNotNull( summary );
        assertNotNull( summary.getImportCount() );
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertNotNull( dataValues );
        assertEquals( 12, dataValues.size() );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deA, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deB, peB, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peA, ouB, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouA, ocDef, ocDef ) ) );
        assertTrue( dataValues.contains( new DataValue( deC, peB, ouB, ocDef, ocDef ) ) );
    }

    private Period createMonthlyPeriod( Date monthStart )
    {
        Date monthEnd = DateUtils.addDays( DateUtils.addMonths( monthStart, 1 ), -1 );
        return createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), monthStart, monthEnd );
    }

    private InputStream readFile( String filename )
    {
        try
        {
            return new ClassPathResource( filename ).getInputStream();
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    private List<DataValue> assertDataValuesCount( int expected )
    {
        List<DataValue> dataValues = dataValueService.getAllDataValues();
        assertEquals( expected, dataValues.size(),
            () -> String.format( "mismatch in number of expected dataValue(s), got %s", dataValues ) );
        return dataValues;
    }

    private static void assertHasNoConflicts( ImportConflicts summary )
    {
        assertEquals( 0, summary.getConflictCount(), summary.getConflictsDescription() );
    }

    private static void assertSuccessWithImportedUpdatedDeleted( int imported, int updated, int deleted,
        ImportSummary summary )
    {
        assertAll(
            () -> assertHasNoConflicts( summary ),
            () -> assertEquals( imported, summary.getImportCount().getImported(), "unexpected import count" ),
            () -> assertEquals( updated, summary.getImportCount().getUpdated(), "unexpected update count" ),
            () -> assertEquals( deleted, summary.getImportCount().getDeleted(), "unexpected deleted count" ),
            () -> assertEquals( ImportStatus.SUCCESS, summary.getStatus(), summary.getDescription() ) );
    }

    private static void assertSuccessWithImportedUpdatedDeleted( int imported, int updated, int deleted, int ignored,
        ImportSummary summary )
    {
        assertAll(
            () -> assertHasNoConflicts( summary ),
            () -> assertEquals( imported, summary.getImportCount().getImported(), "unexpected import count" ),
            () -> assertEquals( updated, summary.getImportCount().getUpdated(), "unexpected update count" ),
            () -> assertEquals( deleted, summary.getImportCount().getDeleted(), "unexpected deleted count" ),
            () -> assertEquals( ignored, summary.getImportCount().getIgnored(), "unexpected ignored count" ),
            () -> assertEquals( ImportStatus.SUCCESS, summary.getStatus(), summary.getDescription() ) );
    }
}
