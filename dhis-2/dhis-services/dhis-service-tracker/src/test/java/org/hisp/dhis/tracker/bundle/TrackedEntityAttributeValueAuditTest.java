package org.hisp.dhis.tracker.bundle;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAudit;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

/**
 * @author Zubair Asghar
 */
public class TrackedEntityAttributeValueAuditTest extends TrackerTest
{
    @Autowired
    private TrackerBundleService trackerBundleService;

    @Autowired
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService;

    @Override
    protected void initTest()
            throws IOException
    {
        setUpMetadata("tracker/te_program_with_tea_allow_audit_metadata.json");
    }

    @Test
    public void testTrackedEntityAttributeValueAudit()
            throws IOException
    {
        TrackerImportParams trackerImportParams = fromJson( "tracker/te_program_with_tea_data.json" );
        trackerImportParams.setUser( currentUserService.getCurrentUser() );

        TrackerBundle trackerBundle = trackerBundleService.create( trackerImportParams );

        trackerBundleService.commit( trackerBundle );

        List<TrackedEntityInstance> trackedEntityInstances = manager.getAll( TrackedEntityInstance.class );
        assertEquals( 1, trackedEntityInstances.size() );

        List<TrackedEntityAttribute> trackedEntityAttributes = trackedEntityInstances.stream()
                .flatMap( tei -> tei.getTrackedEntityAttributeValues().stream().map(TrackedEntityAttributeValue::getAttribute) )
                .collect(Collectors.toList());

        TrackedEntityInstance trackedEntityInstance = trackedEntityInstances.get( 0 );

        List<TrackedEntityAttributeValue> attributeValues = trackedEntityAttributeValueService
                .getTrackedEntityAttributeValues(
                        trackedEntityInstance );

        assertEquals( 4, attributeValues.size() );

        List<TrackedEntityAttributeValueAudit> teaAudits = trackedEntityAttributeValueAuditService
                .getTrackedEntityAttributeValueAudits( trackedEntityAttributes, trackedEntityInstances, AuditType.CREATE );

        System.out.println( "size @@@ " + teaAudits.size() );
    }
}
