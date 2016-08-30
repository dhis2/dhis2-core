package org.hisp.dhis.dxf2.metadata.objectbundle;

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
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dxf2.metadata.AtomicMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
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
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.validation.ValidationRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class ObjectBundleServiceTest
    extends DhisSpringTest
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
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void testCreateObjectBundle()
    {
        ObjectBundleParams params = new ObjectBundleParams();
        ObjectBundle bundle = objectBundleService.create( params );

        assertNotNull( bundle );
    }

    @Test
    public void testCreateDoesPreheating()
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
    public void testObjectBundleShouldAddToObjectAndPreheat()
    {
        DataElementGroup dataElementGroup = fromJson( "dxf2/degAUidRef.json", DataElementGroup.class );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.addObject( dataElementGroup );

        ObjectBundle bundle = objectBundleService.create( params );
        bundle.getPreheat().put( bundle.getPreheatIdentifier(), dataElementGroup );

        assertTrue( bundle.getObjectMap().get( DataElementGroup.class ).contains( dataElementGroup ) );
        assertTrue( bundle.getPreheat().containsKey( PreheatIdentifier.UID, DataElementGroup.class, dataElementGroup.getUid() ) );
    }

    @Test
    public void testPreheatValidations() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertFalse( validate.isEmpty() );

        List<ObjectReport> objectReports = validate.getObjectReports( DataElement.class );
        assertFalse( objectReports.isEmpty() );

        for ( ObjectReport objectReport : objectReports )
        {
            for ( ErrorCode errorCode : objectReport.getErrorCodes() )
            {
                List<ErrorReport> errorReports = objectReport.getErrorReportsByCode().get( errorCode );

                assertFalse( errorReports.isEmpty() );

                for ( ErrorReport errorReport : errorReports )
                {
                    assertTrue( PreheatErrorReport.class.isInstance( errorReport ) );
                    PreheatErrorReport preheatErrorReport = (PreheatErrorReport) errorReport;
                    assertEquals( PreheatIdentifier.UID, preheatErrorReport.getPreheatIdentifier() );

                    if ( DataElementCategoryCombo.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertEquals( "p0KPaWEg3cf", preheatErrorReport.getObjectReference().getUid() );
                    }
                    else if ( User.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertEquals( "GOLswS44mh8", preheatErrorReport.getObjectReference().getUid() );
                    }
                    else if ( OptionSet.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertEquals( "pQYCiuosBnZ", preheatErrorReport.getObjectReference().getUid() );
                    }
                }
            }
        }
    }

    @Test
    public void testPreheatValidationsWithCatCombo() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate1.json" ).getInputStream(), RenderFormat.JSON );

        DataElementCategoryCombo categoryCombo = manager.getByName( DataElementCategoryCombo.class, "default" );
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
        assertFalse( validate.getTypeReportMap().isEmpty() );

        List<ObjectReport> objectReports = validate.getObjectReports( DataElement.class );
        assertFalse( objectReports.isEmpty() );

        for ( ObjectReport objectReport : objectReports )
        {
            for ( ErrorCode errorCode : objectReport.getErrorCodes() )
            {
                List<ErrorReport> errorReports = objectReport.getErrorReportsByCode().get( errorCode );

                assertFalse( errorReports.isEmpty() );

                for ( ErrorReport errorReport : errorReports )
                {
                    assertTrue( PreheatErrorReport.class.isInstance( errorReport ) );
                    PreheatErrorReport preheatErrorReport = (PreheatErrorReport) errorReport;
                    assertEquals( PreheatIdentifier.UID, preheatErrorReport.getPreheatIdentifier() );

                    if ( DataElementCategoryCombo.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertFalse( true );
                    }
                    else if ( User.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertEquals( "GOLswS44mh8", preheatErrorReport.getObjectReference().getUid() );
                    }
                    else if ( OptionSet.class.isInstance( preheatErrorReport.getValue() ) )
                    {
                        assertFalse( true );
                    }
                }
            }
        }
    }

    @Test
    public void testCreatePreheatValidationsInvalidObjects() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate2.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap().isEmpty() );

        System.err.println( "V: " + validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5002 ) );

        assertEquals( 5, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5002 ).size() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E4000 ).size() );
    }

    @Test
    public void testUpdatePreheatValidationsInvalidObjects() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate2.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertEquals( 3, validate.getTypeReportMap( DataElement.class ).getObjectReports().size() );
    }

    @Test
    public void testUpdateWithPersistedObjectsRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate7.json" ).getInputStream(), RenderFormat.JSON );
        defaultSetup();

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertEquals( 1, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
        assertFalse( validate.getErrorReportsByCode( DataElement.class, ErrorCode.E4000 ).isEmpty() );
        assertEquals( 0, bundle.getObjectMap().get( DataElement.class ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesCODE() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap( DataElement.class ).getObjectReports().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testUpdateRequiresValidReferencesAUTO() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate6.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap( DataElement.class ).getObjectReports().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate4.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap( DataElement.class ).getObjectReports().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesCODE() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate5.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap( DataElement.class ).getObjectReports().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testDeleteRequiresValidReferencesAUTO() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate6.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.VALIDATE );
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setImportStrategy( ImportStrategy.DELETE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );

        assertFalse( validate.getTypeReportMap( DataElement.class ).getObjectReports().isEmpty() );
        assertEquals( 3, validate.getErrorReportsByCode( DataElement.class, ErrorCode.E5001 ).size() );
    }

    @Test
    public void testPreheatValidationsIncludingMerge() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_validate3.json" ).getInputStream(), RenderFormat.JSON );
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
    public void testSimpleDataElementDeleteUID() throws IOException
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
    public void testSimpleDataElementDeleteCODE() throws IOException
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
    public void testCreateSimpleMetadataUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata.json" ).getInputStream(), RenderFormat.JSON );

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
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
            assertEquals( defaults.get( DataElementCategoryCombo.class ), dataElement.getCategoryCombo() );
        }

        assertFalse( dataSet.getSources().isEmpty() );
        assertFalse( dataSet.getDataElements().isEmpty() );
        assertEquals( 1, dataSet.getSources().size() );
        assertEquals( 2, dataSet.getDataElements().size() );
        assertEquals( PeriodType.getPeriodTypeByName( "Monthly" ), dataSet.getPeriodType() );

        assertNotNull( user.getUserCredentials() );
        assertEquals( "admin", user.getUserCredentials().getUsername() );
        assertFalse( user.getUserCredentials().getUserAuthorityGroups().isEmpty() );
        assertFalse( user.getOrganisationUnits().isEmpty() );
        assertEquals( "PdWlltZnVZe", user.getOrganisationUnit().getUid() );
    }


    @Test
    public void testCreateDataSetsWithUgaUID() throws IOException
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
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
        List<User> users = manager.getAll( User.class );
        List<UserGroup> userGroups = manager.getAll( UserGroup.class );

        assertEquals( 1, organisationUnits.size() );
        assertEquals( 2, dataElements.size() );
        assertEquals( 1, userRoles.size() );
        assertEquals( 1, users.size() );
        assertEquals( 2, userGroups.size() );

        assertEquals( 1, dataElements.get( 0 ).getUserGroupAccesses().size() );
        assertEquals( 1, dataElements.get( 1 ).getUserGroupAccesses().size() );
    }

    @Test
    public void testCreateAndUpdateDataSetsWithUgaUID() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/simple_metadata_uga.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );
        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/simple_metadata_uga.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );
        objectBundleService.commit( bundle );

        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
        List<User> users = manager.getAll( User.class );
        List<UserGroup> userGroups = manager.getAll( UserGroup.class );

        assertEquals( 1, organisationUnits.size() );
        assertEquals( 2, dataElements.size() );
        assertEquals( 1, userRoles.size() );
        assertEquals( 1, users.size() );
        assertEquals( 2, userGroups.size() );

        assertEquals( 1, dataElements.get( 0 ).getUserGroupAccesses().size() );
        assertEquals( 1, dataElements.get( 1 ).getUserGroupAccesses().size() );
    }

    @Test
    public void testUpdateDataElementsUID() throws IOException
    {
        defaultSetup();

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_update1.json" ).getInputStream(), RenderFormat.JSON );

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
    public void testUpdateDataElementsCODE() throws IOException
    {
        defaultSetup();

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_update2.json" ).getInputStream(), RenderFormat.JSON );

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
    public void testCreateDataSetWithSections() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
    public void testCreateDataSetWithSectionsAndGreyedFields() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_gf.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
        List<User> users = manager.getAll( User.class );
        List<DataElementOperand> dataElementOperands = manager.getAll( DataElementOperand.class );
        List<TrackedEntity> trackedEntities = manager.getAll( TrackedEntity.class );
        List<OrganisationUnitLevel> organisationUnitLevels = manager.getAll( OrganisationUnitLevel.class );

        assertFalse( organisationUnits.isEmpty() );
        assertEquals( 1, organisationUnitLevels.size() );
        assertEquals( 1, trackedEntities.size() );
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
        assertNotNull( section.getCategoryCombo() );
        assertEquals( 1, section.getGreyedFields().size() );

        DataElementCategoryCombo categoryCombo = manager.get( DataElementCategoryCombo.class, "faV8QvLgIwB" );
        assertNotNull( categoryCombo );

        DataElementCategory category = manager.get( DataElementCategory.class, "XJGLlMAMCcn" );
        assertNotNull( category );

        DataElementCategoryOption categoryOption1 = manager.get( DataElementCategoryOption.class, "JYiFOMKa25J" );
        DataElementCategoryOption categoryOption2 = manager.get( DataElementCategoryOption.class, "tdaMRD34m8o" );

        assertNotNull( categoryOption1 );
        assertNotNull( categoryOption2 );
    }

    @Test
    public void testUpdateDataSetWithSectionsAndGreyedFields() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections_gf.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        Section section1 = manager.get( Section.class, "JwcV2ZifEQf" );
        assertNotNull( section1.getDataSet() );
        assertNotNull( section1.getCategoryCombo() );
        assertTrue( section1.getGreyedFields().isEmpty() );
        assertEquals( 1, section1.getDataElements().size() );
        assertNotNull( section1.getDataSet() );

        Section section2 = manager.get( Section.class, "C50M0WxaI7y" );
        assertNotNull( section2.getDataSet() );
        assertNotNull( section2.getCategoryCombo() );
        assertEquals( 1, section2.getGreyedFields().size() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section2.getDataSet() );

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/dataset_with_sections_gf_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
        assertNotNull( dataSet.getUser() );

        section1 = manager.get( Section.class, "JwcV2ZifEQf" );
        assertNotNull( section1.getDataSet() );
        assertNotNull( section1.getCategoryCombo() );
        assertEquals( 1, section1.getGreyedFields().size() );
        assertEquals( 1, section1.getDataElements().size() );
        assertNotNull( section1.getDataSet() );

        section2 = manager.get( Section.class, "C50M0WxaI7y" );
        assertNotNull( section2.getDataSet() );
        assertNotNull( section2.getCategoryCombo() );
        assertTrue( section2.getGreyedFields().isEmpty() );
        assertEquals( 1, section2.getDataElements().size() );
        assertNotNull( section2.getDataSet() );
    }

    @Test
    public void testCreateDataSetWithCompulsoryDataElements() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_compulsory.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
        List<User> users = manager.getAll( User.class );
        List<DataElementOperand> dataElementOperands = manager.getAll( DataElementOperand.class );

        assertFalse( organisationUnits.isEmpty() );
        assertFalse( dataElements.isEmpty() );
        assertFalse( users.isEmpty() );
        assertFalse( userRoles.isEmpty() );

        assertEquals( 1, dataSets.size() );
        assertEquals( 1, dataElementOperands.size() );

        DataSet dataSet = dataSets.get( 0 );
        assertEquals( "DataSetA", dataSet.getName() );
        assertTrue( dataSet.getSections().isEmpty() );
        assertNotNull( dataSet.getUser() );
        assertEquals( 1, dataSet.getCompulsoryDataElementOperands().size() );
    }

    @Test
    public void testCreateMetadataWithIndicator() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_indicators.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<Indicator> indicators = manager.getAll( Indicator.class );

        assertFalse( organisationUnits.isEmpty() );
        assertEquals( 3, dataElements.size() );
        assertEquals( 1, indicators.size() );
    }

    @Test
    public void testCreateMetadataWithValidationRules() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_vr.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
        assertFalse( validationRule1.getLeftSide().getDataElementsInExpression().isEmpty() );
        assertFalse( validationRule1.getRightSide().getDataElementsInExpression().isEmpty() );
        assertEquals( "jocQSivF2ry", validationRule1.getLeftSide().getDataElementsInExpression().iterator().next().getUid() );
        assertEquals( "X0ypiOyoDbw", validationRule1.getRightSide().getDataElementsInExpression().iterator().next().getUid() );

        ValidationRule validationRule2 = manager.get( ValidationRule.class, "TGvH4Hiyduc" );
        assertNotNull( validationRule2.getLeftSide() );
        assertNotNull( validationRule2.getRightSide() );
        assertFalse( validationRule2.getLeftSide().getDataElementsInExpression().isEmpty() );
        assertFalse( validationRule2.getRightSide().getDataElementsInExpression().isEmpty() );
        assertEquals( "jocQSivF2ry", validationRule2.getLeftSide().getDataElementsInExpression().iterator().next().getUid() );
        assertEquals( "X0ypiOyoDbw", validationRule2.getRightSide().getDataElementsInExpression().iterator().next().getUid() );
    }

    @Test
    public void testUpdateMetadataWithValidationRules() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/metadata_with_vr.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/metadata_with_vr_update.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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
        assertFalse( validationRule1.getLeftSide().getDataElementsInExpression().isEmpty() );
        assertFalse( validationRule1.getRightSide().getDataElementsInExpression().isEmpty() );
        assertEquals( "vAczVs4mxna", validationRule1.getLeftSide().getDataElementsInExpression().iterator().next().getUid() );
        assertEquals( "X0ypiOyoDbw", validationRule1.getRightSide().getDataElementsInExpression().iterator().next().getUid() );

        ValidationRule validationRule2 = manager.get( ValidationRule.class, "TGvH4Hiyduc" );
        assertNotNull( validationRule2.getLeftSide() );
        assertNotNull( validationRule2.getRightSide() );
        assertFalse( validationRule2.getLeftSide().getDataElementsInExpression().isEmpty() );
        assertFalse( validationRule2.getRightSide().getDataElementsInExpression().isEmpty() );
        assertEquals( "jocQSivF2ry", validationRule2.getLeftSide().getDataElementsInExpression().iterator().next().getUid() );
        assertEquals( "vAczVs4mxna", validationRule2.getRightSide().getDataElementsInExpression().iterator().next().getUid() );
    }

    @Test
    public void testCreateAndUpdateMetadata1() throws IOException
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
        assertTrue( objectBundleValidationService.validate( bundle ).getErrorReports().isEmpty() );
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
    public void testCreateAndUpdateMetadata2() throws IOException
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
        assertTrue( objectBundleValidationService.validate( bundle ).getErrorReports().isEmpty() );
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
    @Ignore //TODO fix
    public void testCreateAndUpdateMetadata3() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_create_and_update3.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        assertTrue( objectBundleValidationService.validate( bundle ).getErrorReports().isEmpty() );
        objectBundleService.commit( bundle );

        DataElement dataElementE = manager.get( DataElement.class, "deabcdefghE" );

        assertNotNull( dataElementE );
        assertEquals( "DEE", dataElementE.getName() );
        assertEquals( "DECE", dataElementE.getCode() );
        assertEquals( "DESE", dataElementE.getShortName() );
        assertEquals( "DEDE", dataElementE.getDescription() );
    }

    @Test
    public void testCreateMetadataWithSuperuserRoleInjected() throws IOException
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
        assertEquals( 1, validate.getErrorReportsByCode( UserAuthorityGroup.class, ErrorCode.E5003 ).size() );
    }

    @Test
    public void testCreateMetadataWithDuplicateDataElementCode() throws IOException
    {
        createUserAndInjectSecurityContext( true );

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/de_duplicate_code.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.NONE );
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
    public void testCreateOrgUnitWithLevels() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/ou_with_levels.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE_AND_UPDATE );
        params.setAtomicMode( AtomicMode.ALL );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        assertTrue( objectBundleValidationService.validate( bundle ).getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        OrganisationUnit root = manager.get( OrganisationUnit.class, "inVD5SdytkT" );
        assertNull( root.getParent() );
        assertEquals( 3, root.getChildren().size() );
    }

    @Test
    public void testCreateAndUpdateDataSetWithSections() throws IOException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        metadata = renderService.fromMetadata( new ClassPathResource( "dxf2/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.UPDATE );
        params.setObjects( metadata );

        bundle = objectBundleService.create( params );
        validate = objectBundleValidationService.validate( bundle );
        assertTrue( validate.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        List<DataSet> dataSets = manager.getAll( DataSet.class );
        List<Section> sections = manager.getAll( Section.class );
        List<OrganisationUnit> organisationUnits = manager.getAll( OrganisationUnit.class );
        List<DataElement> dataElements = manager.getAll( DataElement.class );
        List<UserAuthorityGroup> userRoles = manager.getAll( UserAuthorityGroup.class );
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

        User user = createUser( 'A' );
        manager.save( user );

        UserGroup userGroup = createUserGroup( 'A', Sets.newHashSet( user ) );
        manager.save( userGroup );
    }
}