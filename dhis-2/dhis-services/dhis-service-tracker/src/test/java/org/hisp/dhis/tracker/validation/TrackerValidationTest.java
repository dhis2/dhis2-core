package org.hisp.dhis.tracker.validation;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.*;
import org.hisp.dhis.dxf2.metadata.objectbundle.feedback.ObjectBundleValidationReport;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.tracker.TrackerErrorCode;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.bundle.TrackerBundleService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerStatus;
import org.hisp.dhis.tracker.report.TrackerValidationReport;
import org.hisp.dhis.tracker.validation.hooks.AddUpdateTrackedEntityValidationHook;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Every.everyItem;

public class TrackerValidationTest
    extends DhisSpringTest

{
    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private ObjectBundleService objectBundleService;

    @Autowired
    private ObjectBundleValidationService objectBundleValidationService;

    @Autowired
    private DefaultTrackerValidationService trackerValidationService;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private UserService _userService;

    @Override
    protected void setUpTest()
        throws IOException
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "tracker/tracker_basic_metadata.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );

        ObjectBundle bundle = objectBundleService.create( params );
        ObjectBundleValidationReport validationReport = objectBundleValidationService.validate( bundle );
        List<ErrorReport> errorReports = validationReport.getErrorReports();
        assertTrue( errorReports.isEmpty() );

        objectBundleService.commit( bundle );
    }

    @Test
    public void testTeValidationOkAll()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be no errors", 0, errors );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.0
    public void testNoWriteAccessFailFast()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "--netroms--" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );
        trackerBundle.setValidationMode( ValidationMode.FAIL_FAST );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1000", TrackerErrorCode.E1000,
            report.getErrorReports().get( 0 ).getErrorCode() );

    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.0
    public void testNoWriteAccessToOrg()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "--netroms--" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 13 errors", 13, errors );

        assertThat( report.getErrorReports(), everyItem( equalTo( new TrackerErrorReport(
            AddUpdateTrackedEntityValidationHook.class, TrackerErrorCode.E1000, "QfUVllTs6cS" ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.0
    public void testWriteAccessInAcl()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "--morten---" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be no errors", 0, errors );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.2
    public void testNonExistingTeType()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_teType-non-existing.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1005", TrackerErrorCode.E1005,
            report.getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.2
    public void testNoTeType()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_teType-null.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1004", TrackerErrorCode.E1004,
            report.getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.3
    public void testEmptyOrgUnit()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_orgunit-empty.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1010", TrackerErrorCode.E1010,
            report.getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.3
    public void testNoOrgUnit()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_orgunit-null.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1010", TrackerErrorCode.E1010,
            report.getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.3
    public void testNonExistingOrgUnit()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_orgunit-non-existing.json" )
                    .getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );
        assertEquals( "TrackerErrorCode should be E1011", TrackerErrorCode.E1011,
            report.getErrorReports().get( 0 ).getErrorCode() );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.5
    public void testGeoFeatureTypeMismatch()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_geo-ftype-mismatch.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1012 ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.5
    public void testGeoFeatureTypeNone()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_geo-ftype-none.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1012 ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.5
    public void testGeoFailParseCoordinates()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_geo-unparsable.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1013 ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.5
    public void testGeoOk()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson(
                new ClassPathResource( "tracker/validations/te-data_error_geo-ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 1, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 1 error", 1, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1013 ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.1
    public void testTeNotUnique()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be no errors", 0, errors );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );

        report = trackerValidationService.validate( trackerBundle );
        errors = report.getErrorReports().size();

        assertEquals( "There should be 13 errors", 13, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1002 ) ) ) );
    }

    @Test
    //Ref. Tracker Validation sheet: TrackedEntity>c.4.1
    public void testTeAttrNonExistentAttr()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-data_error_attr-non-existing.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 2, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be 2 errors", 2, errors );

        assertThat( report.getErrorReports(),
            everyItem( hasProperty( "errorCode", equalTo( TrackerErrorCode.E1006 ) ) ) );
    }




    @Test
    public void testUpdate()
        throws IOException
    {
        TrackerBundleParams trackerBundleParams = renderService
            .fromJson( new ClassPathResource( "tracker/validations/te-data_ok.json" ).getInputStream(),
                TrackerBundleParams.class );

        User user = userService.getUser( "M5zQapPyTZI" );
        trackerBundleParams.setUser( user );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );
        assertEquals( 13, trackerBundle.getTrackedEntities().size() );

        TrackerValidationReport report = trackerValidationService.validate( trackerBundle );

        int errors = report.getErrorReports().size();

        assertEquals( "There should be no errors", 0, errors );

        TrackerBundleReport bundleReport = trackerBundleService.commit( trackerBundle );
        assertEquals( bundleReport.getStatus(), TrackerStatus.OK );

        trackerBundleParams.setImportStrategy( TrackerImportStrategy.UPDATE );
        TrackerBundle updateBundle = trackerBundleService.create( trackerBundleParams ).get( 0 );

        report = trackerValidationService.validate( updateBundle );
        TrackerBundleReport bundleReport2 = trackerBundleService.commit( updateBundle );

        errors = report.getErrorReports().size();
        assertEquals( "There should be 0 errors", 0, errors );
        assertEquals( bundleReport2.getStatus(), TrackerStatus.OK );
    }
}
