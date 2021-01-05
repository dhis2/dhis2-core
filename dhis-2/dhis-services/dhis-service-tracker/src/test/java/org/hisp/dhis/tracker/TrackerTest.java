package org.hisp.dhis.tracker;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleParams;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleService;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleValidationService;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.beans.Introspector;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Luciano Fiandesio
 */
public abstract class TrackerTest extends TransactionalIntegrationTest
{
    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    protected UserService _userService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Override
    protected void setUpTest()
        throws IOException
    {

        preCreateInjectAdminUserWithoutPersistence();

        renderService = _renderService;
        userService = _userService;

        initTest();

        // Clear the session to simulate different API call after the setup
        manager.clear();
    }

    protected abstract void initTest()
        throws IOException;

    protected ObjectBundle setUpMetadata( String path )
        throws IOException
    {

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( path ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        assertTrue( validationReport.getErrorReports().isEmpty() );

        objectBundleService.commit( bundle );

        return bundle;
    }

    protected TrackerImportParams fromJson( String path )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUser( currentUserService.getCurrentUser() );
        return trackerImportParams;
    }

    protected TrackerImportParams fromJson( String path, String userUid )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUserId( userUid );
        return trackerImportParams;
    }

    protected TrackerImportParams fromJson( String path, User user )
        throws IOException
    {
        TrackerImportParams trackerImportParams = _fromJson( path );
        trackerImportParams.setUser( user );
        return trackerImportParams;
    }

    protected TrackerImportParams _fromJson( String path )
        throws IOException
    {
        return syncIdentifiers( renderService.fromJson(
            new ClassPathResource( path ).getInputStream(),
            TrackerImportParams.class ) );
    }

    /**
     * Makes sure that the Tracker entities in the provided TrackerBundle have the
     * 'uid' attribute identical to the json identifier.
     */
    protected TrackerImportParams syncIdentifiers( TrackerImportParams trackerImportParams )
    {
        trackerImportParams.getTrackedEntities().forEach( this::syncUid );
        trackerImportParams.getEnrollments().forEach( this::syncUid );
        trackerImportParams.getEvents().forEach( this::syncUid );

        return trackerImportParams;
    }

    private void syncUid( TrackerDto trackerDto )
    {
        try
        {
            String field = Introspector.decapitalize( trackerDto.getClass().getSimpleName() );
            Object val = FieldUtils.readDeclaredField( trackerDto, field, true );
            String identifier = val != null ? val.toString() : null;
            if ( StringUtils.isEmpty( trackerDto.getUid() )
                && StringUtils.isEmpty( identifier ) )
            {
                String uid = CodeGenerator.generateUid();
                FieldUtils.writeDeclaredField( trackerDto, "uid", uid, true );
                FieldUtils.writeDeclaredField( trackerDto, field, uid, true );
            }
            if ( StringUtils.isEmpty( trackerDto.getUid() )
                && StringUtils.isNotEmpty( identifier ) )
            {
                FieldUtils.writeDeclaredField( trackerDto, "uid", identifier, true );
            }
            if ( StringUtils.isNotEmpty( trackerDto.getUid() )
                && StringUtils.isEmpty( identifier ) )
            {
                FieldUtils.writeDeclaredField( trackerDto, field, trackerDto.getUid(), true );
            }
        }
        catch ( IllegalAccessException e )
        {
            e.printStackTrace();
        }
    }

    protected void assertNoImportErrors( TrackerImportReport report )
    {
        assertTrue( report.getValidationReport().getErrorReports().isEmpty() );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}
