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
package org.hisp.dhis.dxf2.metadata.objectbundle;

import static org.hisp.dhis.dxf2.metadata.AtomicMode.NONE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.preheat.PreheatErrorReport;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.preheat.PreheatMode;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.validation.ValidationRule;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class ObjectBundleServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    void testCreateObjectBundle()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        ObjectBundle bundle = objectBundleService.create( params );
        assertNotNull( bundle );
    }

    @Test
    void testCreateDoesPreheating()
    {
        DataElementGroup dataElementGroup = fromJson( "dxf2/degAUidRef.json", DataElementGroup.class );
        defaultSetup();
        ObjectBundleParams params = new ObjectBundleParams();
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.addObject( dataElementGroup );
        ObjectBundle bundle = objectBundleService.create( params );
        assertNotNull( bundle );
        assertFalse( bundle.getPreheat().isEmpty() );
        assertFalse( bundle.getPreheat().isEmpty( PreheatIdentifier.UID ) );
        assertFalse( bundle.getPreheat().isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghA" ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghB" ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghC" ) );
        assertFalse( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghD" ) );
    }

    @Test
    void testObjectBundleShouldAddToObjectAndPreheat()
    {
        DataElementGroup dataElementGroup = fromJson( "dxf2/degAUidRef.json", DataElementGroup.class );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.addObject( dataElementGroup );
        ObjectBundle bundle = objectBundleService.create( params );
        bundle.getPreheat().put( bundle.getPreheatIdentifier(), dataElementGroup );
        assertTrue( StreamSupport.stream( bundle.getObjects( DataElementGroup.class ).spliterator(), false )
            .anyMatch( dataElementGroup::equals ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElementGroup.class,
            dataElementGroup.getUid() ) );
    }

    @Test
    void testPreheatValidations()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.isEmpty() );
        assertTrue( validate.hasErrorReports() );
        validate.forEachErrorReport( errorReport -> {
            assertTrue( errorReport instanceof PreheatErrorReport );
            PreheatErrorReport preheatErrorReport = (PreheatErrorReport) errorReport;
            assertEquals( PreheatIdentifier.UID, preheatErrorReport.getPreheatIdentifier() );
            if ( preheatErrorReport.getValue() instanceof CategoryCombo )
            {
                assertEquals( "p0KPaWEg3cf", preheatErrorReport.getObjectReference().getUid() );
            }
            else if ( preheatErrorReport.getValue() instanceof User )
            {
                assertEquals( "GOLswS44mh8", preheatErrorReport.getObjectReference().getUid() );
            }
            else if ( preheatErrorReport.getValue() instanceof OptionSet )
            {
                assertEquals( "pQYCiuosBnZ", preheatErrorReport.getObjectReference().getUid() );
            }
        } );
    }

    @Test
    void testPreheatValidationsWithCatCombo()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );
        CategoryCombo categoryCombo = manager.getByName( CategoryCombo.class, "default" );
        categoryCombo.setUid( "p0KPaWEg3cf" );
        manager.update( categoryCombo );
        OptionSet optionSet = new OptionSet( "OptionSet: pQYCiuosBnZ", ValueType.TEXT );
        optionSet.setAutoFields();
        optionSet.setUid( "pQYCiuosBnZ" );
        manager.save( optionSet );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.hasErrorReports() );
        validate.forEachErrorReport( errorReport -> {
            assertTrue( errorReport instanceof PreheatErrorReport );
            PreheatErrorReport preheatErrorReport = (PreheatErrorReport) errorReport;
            assertEquals( PreheatIdentifier.UID, preheatErrorReport.getPreheatIdentifier() );
            if ( preheatErrorReport.getValue() instanceof CategoryCombo )
            {
                fail();
            }
            else if ( preheatErrorReport.getValue() instanceof User )
            {
                assertEquals( "GOLswS44mh8", preheatErrorReport.getObjectReference().getUid() );
            }
            else if ( preheatErrorReport.getValue() instanceof OptionSet )
            {
                fail();
            }
        } );
    }

    @Test
    void testCreatePreheatValidationsInvalidObjects()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate2.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.hasErrorReports() );
        assertEquals( 4, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5002 ) );
        assertEquals( 3, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E4000 ) );
    }

    @Test
    void testUpdatePreheatValidationsInvalidObjects()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate2.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.hasErrorReports() );
        assertEquals( 3, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5001 ) );
    }

    @Test
    void testUpdateRequiresValidReferencesUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 3, validate.getTypeReport( DataElement.class ).getObjectReportsCount() );
    }

    @Test
    void testUpdateWithPersistedObjectsRequiresValidReferencesUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate7.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setAtomicMode( NONE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5001 ) );
        assertNotEquals( 0, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E4000 ) );
        assertEquals( 0, bundle.getObjectsCount( DataElement.class ) );
    }

    @Test
    void testUpdateRequiresValidReferencesCODE()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getTypeReport( DataElement.class ).hasObjectReports() );
        assertEquals( 3, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5001 ) );
    }

    @Test
    void testDeleteRequiresValidReferencesUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getTypeReport( DataElement.class ).hasObjectReports() );
        assertEquals( 3, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5001 ) );
    }

    @Test
    void testDeleteRequiresValidReferencesCODE()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getTypeReport( DataElement.class ).hasObjectReports() );
        assertEquals( 3, validate.getErrorReportsCountByCode( DataElement.class, ErrorCode.E5001 ) );
    }

    @Test
    void testPreheatValidationsIncludingMerge()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_validate3.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setMergeMode( MergeMode.REPLACE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertNotNull( validate );
    }

    @Test
    void testSimpleDataElementDeleteUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_simple_delete_uid.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        assertEquals( 1, dataElements.size() );
        assertEquals( "deabcdefghB", dataElements.get( 0 ).getUid() );
    }

    @Test
    void testSimpleDataElementDeleteCODE()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_simple_delete_code.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        assertEquals( 1, dataElements.size() );
        assertEquals( "DataElementCodeD", dataElements.get( 0 ).getCode() );
    }

    @Test
    void testCreateSimpleMetadataUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/simple_metadata.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( dataSets.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = manager.getDefaults();
        DataSet dataSet = dataSets.get( 0 );
        User user = users.get( 0 );
        for ( DataElement dataElement : dataElements )
        {
            assertNotNull( dataElement.getCategoryCombo() );
            assertEquals( defaults.get( CategoryCombo.class ), dataElement.getCategoryCombo() );
        }
        assertFalse( dataSet.getSources().isEmpty() );
        assertFalse( dataSet.getDataSetElements().isEmpty() );
        assertEquals( 1, dataSet.getSources().size() );
        assertEquals( 2, dataSet.getDataSetElements().size() );
        assertEquals( PeriodType.getPeriodTypeByName( "Monthly" ), dataSet.getPeriodType() );
        assertNotNull( user );
        assertEquals( "admin", user.getUsername() );
        assertFalse( user.getUserRoles().isEmpty() );
        assertFalse( user.getOrganisationUnits().isEmpty() );
        assertEquals( "PdWlltZnVZe", user.getOrganisationUnit().getUid() );
    }

    @Test
    void testCreateDataSetsWithUgaUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata_uga.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<UserGroup> userGroups = manager.getAll( UserGroup.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( 2, dataElements.size() );
        assertEquals( 1, userRoles.size() );
        assertEquals( 1, users.size() );
        assertEquals( 2, userGroups.size() );
        assertEquals( 1, dataElements.get( 0 ).getSharing().getUserGroups().size() );
        assertEquals( 1, dataElements.get( 1 ).getSharing().getUserGroups().size() );
    }

    @Test
    void testCreateAndUpdateDataSetsWithUgaUID()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata_uga.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata_uga.json" ).getInputStream(), RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<UserGroup> userGroups = manager.getAll( UserGroup.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( 2, dataElements.size() );
        assertEquals( 1, userRoles.size() );
        assertEquals( 1, users.size() );
        assertEquals( 2, userGroups.size() );
        assertEquals( 1, dataElements.get( 0 ).getSharing().getUserGroups().size() );
        assertEquals( 1, dataElements.get( 1 ).getSharing().getUserGroups().size() );
    }

    @Test
    void testUpdateDataElementsUID()
        throws IOException
    {
        defaultSetup();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_update1.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        Map<String, DataElement> dataElementMap = manager.getIdMap( DataElement.class, IdScheme.UID );
        UserGroup userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertEquals( 4, dataElementMap.size() );
        assertNotNull( userGroup );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        DataElement dataElementA = dataElementMap.get( "deabcdefghA" );
        DataElement dataElementB = dataElementMap.get( "deabcdefghB" );
        DataElement dataElementC = dataElementMap.get( "deabcdefghC" );
        DataElement dataElementD = dataElementMap.get( "deabcdefghD" );
        assertNotNull( dataElementA );
        assertNotNull( dataElementB );
        assertNotNull( dataElementC );
        assertNotNull( dataElementD );
        assertEquals( "DEA", dataElementA.getName() );
        assertEquals( "DEB", dataElementB.getName() );
        assertEquals( "DEC", dataElementC.getName() );
        assertEquals( "DED", dataElementD.getName() );
        assertEquals( "DECA", dataElementA.getCode() );
        assertEquals( "DECB", dataElementB.getCode() );
        assertEquals( "DECC", dataElementC.getCode() );
        assertEquals( "DECD", dataElementD.getCode() );
        assertEquals( "DESA", dataElementA.getShortName() );
        assertEquals( "DESB", dataElementB.getShortName() );
        assertEquals( "DESC", dataElementC.getShortName() );
        assertEquals( "DESD", dataElementD.getShortName() );
        assertEquals( "DEDA", dataElementA.getDescription() );
        assertEquals( "DEDB", dataElementB.getDescription() );
        assertEquals( "DEDC", dataElementC.getDescription() );
        assertEquals( "DEDD", dataElementD.getDescription() );
        assertEquals( 1, dataElementA.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementB.getUserGroupAccesses().size() );
        assertEquals( 1, dataElementC.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementD.getUserGroupAccesses().size() );
    }

    @Test
    void testUpdateDataElementsCODE()
        throws IOException
    {
        defaultSetup();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_update2.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        Map<String, DataElement> dataElementMap = manager.getIdMap( DataElement.class, IdScheme.UID );
        UserGroup userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertEquals( 4, dataElementMap.size() );
        assertNotNull( userGroup );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        DataElement dataElementA = dataElementMap.get( "deabcdefghA" );
        DataElement dataElementB = dataElementMap.get( "deabcdefghB" );
        DataElement dataElementC = dataElementMap.get( "deabcdefghC" );
        DataElement dataElementD = dataElementMap.get( "deabcdefghD" );
        assertNotNull( dataElementA );
        assertNotNull( dataElementB );
        assertNotNull( dataElementC );
        assertNotNull( dataElementD );
        assertEquals( "DEA", dataElementA.getName() );
        assertEquals( "DEB", dataElementB.getName() );
        assertEquals( "DEC", dataElementC.getName() );
        assertEquals( "DED", dataElementD.getName() );
        assertEquals( "DataElementCodeA", dataElementA.getCode() );
        assertEquals( "DataElementCodeB", dataElementB.getCode() );
        assertEquals( "DataElementCodeC", dataElementC.getCode() );
        assertEquals( "DataElementCodeD", dataElementD.getCode() );
        assertEquals( "DESA", dataElementA.getShortName() );
        assertEquals( "DESB", dataElementB.getShortName() );
        assertEquals( "DESC", dataElementC.getShortName() );
        assertEquals( "DESD", dataElementD.getShortName() );
        assertEquals( "DEDA", dataElementA.getDescription() );
        assertEquals( "DEDB", dataElementB.getDescription() );
        assertEquals( "DEDC", dataElementC.getDescription() );
        assertEquals( "DEDD", dataElementD.getDescription() );
        assertEquals( 1, dataElementA.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementB.getUserGroupAccesses().size() );
        assertEquals( 1, dataElementC.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementD.getUserGroupAccesses().size() );
    }

    @Test
    void testCreateDataSetWithSections()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( dataSets.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, dataSets.size() );
        assertEquals( 2, sections.size() );
        DataSet dataSet = dataSets.get( 0 );
        assertEquals( 2, dataSet.getSections().size() );
        Section section1 = sections.get( 0 );
        Section section2 = sections.get( 1 );
        assertEquals( 1, section1.getDataElements().size() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section1.getDataSet() );
        assertNotNull( section2.getDataSet() );
    }

    @Test
    void testCreateDataSetWithSectionsAndGreyedFields()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_gf.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<DataElementOperand> dataElementOperands = manager.getAll( DataElementOperand.class );
        List<TrackedEntityType> trackedEntityTypes = manager.getAll( TrackedEntityType.class );
        List<OrganisationUnitLevel> organisationUnitLevels = manager.getAll( OrganisationUnitLevel.class );
        assertFalse( organisationUnits.isEmpty() );
        assertEquals( 1, organisationUnitLevels.size() );
        assertEquals( 1, trackedEntityTypes.size() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, dataSets.size() );
        assertEquals( 2, sections.size() );
        assertEquals( 1, dataElementOperands.size() );
        DataSet dataSet = dataSets.get( 0 );
        assertEquals( 2, dataSet.getSections().size() );
        Section section1 = sections.get( 0 );
        Section section2 = sections.get( 1 );
        assertEquals( 1, section1.getDataElements().size() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section1.getDataSet() );
        assertNotNull( section2.getDataSet() );
        Section section = manager.get( Section.class, "C50M0WxaI7y" );
        assertNotNull( section.getDataSet() );
        assertEquals( 1, section.getCategoryCombos().size() );
        assertEquals( 1, section.getGreyedFields().size() );
        CategoryCombo categoryCombo = manager.get( CategoryCombo.class, "faV8QvLgIwB" );
        assertNotNull( categoryCombo );
        Category category = manager.get( Category.class, "XJGLlMAMCcn" );
        assertNotNull( category );
        CategoryOption categoryOption1 = manager.get( CategoryOption.class, "JYiFOMKa25J" );
        CategoryOption categoryOption2 = manager.get( CategoryOption.class, "tdaMRD34m8o" );
        assertNotNull( categoryOption1 );
        assertNotNull( categoryOption2 );
    }

    @Test
    void testUpdateDataSetWithSectionsAndGreyedFields()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_gf.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        dbmsManager.clearSession();
        Section section1 = manager.get( Section.class, "JwcV2ZifEQf" );
        assertNotNull( section1.getDataSet() );
        assertEquals( 1, section1.getCategoryCombos().size() );
        assertTrue( section1.getGreyedFields().isEmpty() );
        assertEquals( 1, section1.getDataElements().size() );
        assertNotNull( section1.getDataSet() );
        Section section2 = manager.get( Section.class, "C50M0WxaI7y" );
        assertNotNull( section2.getDataSet() );
        assertEquals( 1, section2.getCategoryCombos().size() );
        assertEquals( 1, section2.getGreyedFields().size() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section2.getDataSet() );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_gf_update.json" ).getInputStream(),
            RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        manager.flush();
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<DataElementOperand> dataElementOperands = manager.getAll( DataElementOperand.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, dataSets.size() );
        assertEquals( 2, sections.size() );
        assertEquals( 1, dataElementOperands.size() );
        DataSet dataSet = dataSets.get( 0 );
        assertEquals( "Updated Data Set", dataSet.getName() );
        assertEquals( 2, dataSet.getSections().size() );
        assertNotNull( dataSet.getCreatedBy() );
        section1 = manager.get( Section.class, "JwcV2ZifEQf" );
        assertNotNull( section1.getDataSet() );
        assertEquals( 1, section1.getCategoryCombos().size() );
        assertEquals( 1, section1.getGreyedFields().size() );
        assertEquals( 1, section1.getDataElements().size() );
        assertNotNull( section1.getDataSet() );
        section2 = manager.get( Section.class, "C50M0WxaI7y" );
        assertNotNull( section2.getDataSet() );
        assertEquals( 1, section2.getCategoryCombos().size() );
        assertTrue( section2.getGreyedFields().isEmpty() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section2.getDataSet() );
    }

    @Test
    void testCreateDataSetWithCompulsoryDataElements()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_compulsory.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        assertDoesNotThrow( () -> objectBundleService.create( params ) );
    }

    @Test
    void testCreateDataSetNoDSEDefaults()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_compulsory.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        assertEquals( 1, dataSets.size() );
        DataSet dataSet = dataSets.get( 0 );
        assertEquals( dataSet.getDataSetElements().size(), 1 );
        DataSetElement dataSetElement = dataSet.getDataSetElements().iterator().next();
        assertNull( dataSetElement.getCategoryCombo() );
    }

    @Test
    void testCreateMetadataWithIndicator()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_indicators.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<Indicator> indicators = manager.getAll( Indicator.class );
        assertFalse( organisationUnits.isEmpty() );
        assertEquals( 3, dataElements.size() );
        assertEquals( 2, indicators.size() );
    }

    @Test
    void testCreateMetadataWithValidationRules()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/metadata_with_vr.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<ValidationRule> validationRules = manager.getAll( ValidationRule.class );
        assertFalse( dataSets.isEmpty() );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 2, validationRules.size() );
        ValidationRule validationRule1 = manager.get( ValidationRule.class, "ztzsVjSIWg7" );
        assertNotNull( validationRule1.getLeftSide() );
        assertNotNull( validationRule1.getRightSide() );
        ValidationRule validationRule2 = manager.get( ValidationRule.class, "TGvH4Hiyduc" );
        assertNotNull( validationRule2.getLeftSide() );
        assertNotNull( validationRule2.getRightSide() );
    }

    @Test
    void testUpdateMetadataWithValidationRules()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/metadata_with_vr.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_vr_update.json" ).getInputStream(), RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        List<ValidationRule> validationRules = manager.getAll( ValidationRule.class );
        assertFalse( dataSets.isEmpty() );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 2, validationRules.size() );
        ValidationRule validationRule1 = manager.get( ValidationRule.class, "ztzsVjSIWg7" );
        assertNotNull( validationRule1.getLeftSide() );
        assertNotNull( validationRule1.getRightSide() );
        ValidationRule validationRule2 = manager.get( ValidationRule.class, "TGvH4Hiyduc" );
        assertNotNull( validationRule2.getLeftSide() );
        assertNotNull( validationRule2.getRightSide() );
    }

    @Test
    void testCreateMetadataWithInvalidExpressionValidationRules()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_vr_invalid_expression.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsCount() );
        assertTrue( validate.hasErrorReport( report -> "leftSide.description".equals( report.getErrorProperty() )
            && ErrorCode.E4001 == report.getErrorCode() ) );
    }

    @Test
    void testUpdateMetadataWithMissingExpressionValidationRules()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_vr_missing_expression.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertEquals( 1, validate.getErrorReportsCount() );
        assertTrue( validate.hasErrorReport(
            report -> "rightSide".equals( report.getErrorProperty() ) && ErrorCode.E4000 == report.getErrorCode() ) );
    }

    @Test
    void testCreateAndUpdateMetadata1()
        throws IOException
    {
        defaultSetup();
        Map<String, DataElement> dataElementMap = manager.getIdMap( DataElement.class, IdScheme.UID );
        UserGroup userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertEquals( 4, dataElementMap.size() );
        assertNotNull( userGroup );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_create_and_update1.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport objectBundleValidationReport = objectBundleValidationService.validate( bundle );
        assertFalse( objectBundleValidationReport.hasErrorReports() );
        objectBundleService.commit( bundle );
        DataElement dataElementA = dataElementMap.get( "deabcdefghA" );
        DataElement dataElementB = dataElementMap.get( "deabcdefghB" );
        DataElement dataElementC = dataElementMap.get( "deabcdefghC" );
        DataElement dataElementD = dataElementMap.get( "deabcdefghD" );
        assertNotNull( dataElementA );
        assertNotNull( dataElementB );
        assertNotNull( dataElementC );
        assertNotNull( dataElementD );
        assertEquals( "DEA", dataElementA.getName() );
        assertEquals( "DEB", dataElementB.getName() );
        assertEquals( "DEC", dataElementC.getName() );
        assertEquals( "DED", dataElementD.getName() );
        assertEquals( "DECA", dataElementA.getCode() );
        assertEquals( "DECB", dataElementB.getCode() );
        assertEquals( "DECC", dataElementC.getCode() );
        assertEquals( "DECD", dataElementD.getCode() );
        assertEquals( "DESA", dataElementA.getShortName() );
        assertEquals( "DESB", dataElementB.getShortName() );
        assertEquals( "DESC", dataElementC.getShortName() );
        assertEquals( "DESD", dataElementD.getShortName() );
        assertEquals( "DEDA", dataElementA.getDescription() );
        assertEquals( "DEDB", dataElementB.getDescription() );
        assertEquals( "DEDC", dataElementC.getDescription() );
        assertEquals( "DEDD", dataElementD.getDescription() );
        assertEquals( 1, dataElementA.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementB.getUserGroupAccesses().size() );
        assertEquals( 1, dataElementC.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementD.getUserGroupAccesses().size() );
    }

    @Test
    void testCreateAndUpdateMetadata2()
        throws IOException
    {
        defaultSetup();
        Map<String, DataElement> dataElementMap = manager.getIdMap( DataElement.class, IdScheme.UID );
        UserGroup userGroup = manager.get( UserGroup.class, "ugabcdefghA" );
        assertEquals( 4, dataElementMap.size() );
        assertNotNull( userGroup );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_create_and_update2.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        DataElement dataElementA = manager.get( DataElement.class, "deabcdefghA" );
        DataElement dataElementB = manager.get( DataElement.class, "deabcdefghB" );
        DataElement dataElementC = manager.get( DataElement.class, "deabcdefghC" );
        DataElement dataElementD = manager.get( DataElement.class, "deabcdefghD" );
        DataElement dataElementE = manager.get( DataElement.class, "deabcdefghE" );
        assertNotNull( dataElementA );
        assertNotNull( dataElementB );
        assertNotNull( dataElementC );
        assertNotNull( dataElementD );
        assertNotNull( dataElementE );
        assertEquals( "DEA", dataElementA.getName() );
        assertEquals( "DEB", dataElementB.getName() );
        assertEquals( "DEC", dataElementC.getName() );
        assertEquals( "DED", dataElementD.getName() );
        assertEquals( "DEE", dataElementE.getName() );
        assertEquals( "DECA", dataElementA.getCode() );
        assertEquals( "DECB", dataElementB.getCode() );
        assertEquals( "DECC", dataElementC.getCode() );
        assertEquals( "DECD", dataElementD.getCode() );
        assertEquals( "DECE", dataElementE.getCode() );
        assertEquals( "DESA", dataElementA.getShortName() );
        assertEquals( "DESB", dataElementB.getShortName() );
        assertEquals( "DESC", dataElementC.getShortName() );
        assertEquals( "DESD", dataElementD.getShortName() );
        assertEquals( "DESE", dataElementE.getShortName() );
        assertEquals( "DEDA", dataElementA.getDescription() );
        assertEquals( "DEDB", dataElementB.getDescription() );
        assertEquals( "DEDC", dataElementC.getDescription() );
        assertEquals( "DEDD", dataElementD.getDescription() );
        assertEquals( "DEDE", dataElementE.getDescription() );
        assertEquals( 1, dataElementA.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementB.getUserGroupAccesses().size() );
        assertEquals( 1, dataElementC.getUserGroupAccesses().size() );
        assertEquals( 0, dataElementD.getUserGroupAccesses().size() );
    }

    @Test
    @Disabled
    void testCreateAndUpdateMetadata3()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_create_and_update3.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        DataElement dataElementE = manager.get( DataElement.class, "deabcdefghE" );
        assertNotNull( dataElementE );
        assertEquals( "DEE", dataElementE.getName() );
        assertEquals( "DECE", dataElementE.getCode() );
        assertEquals( "DESE", dataElementE.getShortName() );
        assertEquals( "DEDE", dataElementE.getDescription() );
    }

    @Test
    void testCreateMetadataWithSuperuserRoleInjected()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_superuser_bug.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.isEmpty() );
        assertEquals( 1, validate.getErrorReportsCountByCode( UserRole.class, ErrorCode.E5003 ) );
    }

    @Test
    void testCreateMetadataWithDuplicateDataElementCode()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_duplicate_code.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( NONE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        assertEquals( 1, manager.getAll( DataElement.class ).size() );
        DataElement dataElement = manager.getByCode( DataElement.class, "DataElementCodeA" );
        assertEquals( "SG4HuKlNEFH", dataElement.getUid() );
        assertEquals( "DataElementA", dataElement.getName() );
    }

    @Test
    void testCreateMetadataWithDuplicateDataElementUid()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_duplicate_uid.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( NONE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        assertEquals( 1, manager.getAll( DataElement.class ).size() );
        DataElement dataElement = manager.get( DataElement.class, "CCwk5Yx440o" );
        assertEquals( "CCwk5Yx440o", dataElement.getUid() );
        assertEquals( "DataElementB", dataElement.getName() );
    }

    @Test
    void testCreateMetadataWithDuplicateDataElementUidALL()
        throws IOException
    {
        createUserAndInjectSecurityContext( true );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_duplicate_uid.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        assertEquals( 0, manager.getAll( DataElement.class ).size() );
    }

    @Test
    void testCreateOrgUnitWithLevels()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/ou_with_levels.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.ALL );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        OrganisationUnit root = manager.get( OrganisationUnit.class, "inVD5SdytkT" );
        assertNull( root.getParent() );
        assertEquals( 3, root.getChildren().size() );
    }

    @Test
    void testCreateAndUpdateDataSetWithSections()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserRole> userRoles = manager.getAll( UserRole.class );
        List<User> users = manager.getAll( User.class );
        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( dataSets.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );
        assertEquals( 1, dataSets.size() );
        assertEquals( 2, sections.size() );
        DataSet dataSet = dataSets.get( 0 );
        assertNotNull( dataSet.getPeriodType() );
        assertEquals( 2, dataSet.getSections().size() );
        Section section1 = sections.get( 0 );
        Section section2 = sections.get( 1 );
        assertEquals( 1, section1.getDataElements().size() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section1.getDataSet() );
        assertNotNull( section2.getDataSet() );
    }

    @Test
    void testCreateOrgUnitWithPersistedParent()
        throws IOException
    {
        OrganisationUnit parentOu = createOrganisationUnit( 'A' );
        parentOu.setUid( "ImspTQPwCqd" );
        manager.save( parentOu );
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/orgunit_create_with_persisted_parent.json" ).getInputStream(),
            RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        objectBundleValidationService.validate( bundle );
        objectBundleService.commit( bundle );
        assertEquals( 3, manager.getAll( OrganisationUnit.class ).size() );
        assertNull( manager.get( OrganisationUnit.class, "ImspTQPwCqd" ).getParent() );
        assertNotNull( manager.get( OrganisationUnit.class, "bFzxXwTkSWA" ).getParent() );
        assertEquals( "ImspTQPwCqd", manager.get( OrganisationUnit.class, "bFzxXwTkSWA" ).getParent().getUid() );
        assertNotNull( manager.get( OrganisationUnit.class, "B8eJEMldsP7" ).getParent() );
        assertEquals( "bFzxXwTkSWA", manager.get( OrganisationUnit.class, "B8eJEMldsP7" ).getParent().getUid() );
    }

    @Test
    void testCreateOrgUnitWithTranslations()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/ou_with_translation.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.ALL );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        OrganisationUnit root = manager.get( OrganisationUnit.class, "inVD5SdytkT" );
        assertNull( root.getParent() );
        assertEquals( 3, root.getChildren().size() );
        assertEquals( 1, root.getTranslations().size() );
    }

    @Test
    void testSetDefaultCategoryCombo()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/de_no_cc.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.ALL );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        assertEquals( 1, dataElements.size() );
        DataElement dataElement = dataElements.get( 0 );
        assertEquals( "CCCC", dataElement.getName() );
        assertEquals( "CCCC", dataElement.getShortName() );
        assertNotNull( dataElement.getCategoryCombo() );
    }

    @Test
    void testMetadataWithoutDefaults()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_no_defaults.json" ).getInputStream(), RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
    }

    @Test
    void testInvalidDefaults()
        throws IOException
    {
        defaultSetup();
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/defaults_invalid.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertEquals( 3, objectBundleValidationService.validate( bundle ).getErrorReportsCount() );
    }

    @Test
    void testCreateUpdateOrgUnitUsingCODE()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/org_unit_code_id.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getCode() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getName() );
        assertNotNull( organisationUnits.get( 0 ).getUid() );
        String objectUid = organisationUnits.get( 0 ).getUid();
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/org_unit_code_id_update.json" ).getInputStream(), RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        organisationUnits = manager.getAll( OrganisationUnit.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getCode() );
        assertEquals( "org-unit-1-new-name", organisationUnits.get( 0 ).getName() );
        assertEquals( objectUid, organisationUnits.get( 0 ).getUid() );
    }

    @Test
    void testCreateOrUpdateOrgUnitUsingCODE()
        throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService
            .fromMetadata( new ClassPathResource( "dxf2/org_unit_code_id.json" ).getInputStream(),
                RenderFormat.JSON );
        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        ObjectBundle bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getCode() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getName() );
        assertNotNull( organisationUnits.get( 0 ).getUid() );
        String objectUid = organisationUnits.get( 0 ).getUid();
        metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/org_unit_code_id_update.json" ).getInputStream(), RenderFormat.JSON );
        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );
        bundle = objectBundleService.create( params );
        assertFalse( objectBundleValidationService.validate( bundle ).hasErrorReports() );
        objectBundleService.commit( bundle );
        organisationUnits = manager.getAll( OrganisationUnit.class );
        assertEquals( 1, organisationUnits.size() );
        assertEquals( "org-unit-1", organisationUnits.get( 0 ).getCode() );
        assertEquals( "org-unit-1-new-name", organisationUnits.get( 0 ).getName() );
        assertEquals( objectUid, organisationUnits.get( 0 ).getUid() );
    }

    private void defaultSetup()
    {
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );
        DataElement de4 = createDataElement( 'D' );
        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );
        manager.save( de4 );
        User user = makeUser( "A" );
        manager.save( user );
        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );
    }
}
