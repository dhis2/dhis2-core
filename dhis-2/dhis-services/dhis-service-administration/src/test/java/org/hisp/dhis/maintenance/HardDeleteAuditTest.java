package org.hisp.dhis.maintenance;

import com.google.common.collect.Sets;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditQuery;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.maintenance.jdbc.JdbcMaintenanceStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@ActiveProfiles( profiles = { "test-audit" } )
public class HardDeleteAuditTest
    extends IntegrationTestBase
{
    private static final int TIMEOUT = 5;
    @Autowired
    private AuditService auditService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private JdbcMaintenanceStore jdbcMaintenanceStore;

    @Test
    public void testHardDeleteTei()
    {
        OrganisationUnit ou = createOrganisationUnit( 'A' );
        TrackedEntityAttribute attribute = createTrackedEntityAttribute( 'A' );
        manager.save( ou );
        manager.save( attribute );

        TrackedEntityInstance tei = createTrackedEntityInstance( 'A', ou, attribute );

        trackedEntityInstanceService.addTrackedEntityInstance( tei );

        trackedEntityInstanceService.deleteTrackedEntityInstance( tei );

        final AuditQuery query = AuditQuery.builder()
            .uid( Sets.newHashSet( tei.getUid() ) )
            .build();

        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( query ) > 0 );

        List<Audit> audits = auditService.getAudits( query );

        assertEquals( 2, audits.size() );

        jdbcMaintenanceStore.deleteSoftDeletedTrackedEntityInstances();

        final AuditQuery deleteQuery = AuditQuery.builder()
            .uid( Sets.newHashSet( tei.getUid() ) )
            .auditType( Sets.newHashSet( AuditType.DELETE ) )
            .build();

        audits = auditService.getAudits( deleteQuery );

        await().atMost( TIMEOUT, TimeUnit.SECONDS ).until( () -> auditService.countAudits( deleteQuery ) > 0 );

        assertEquals( 1, audits.size() );
        Audit audit = audits.get( 0 );
        assertEquals( AuditType.DELETE, audit.getAuditType() );
        assertEquals( TrackedEntityInstance.class.getName(), audit.getKlass() );
        assertEquals( tei.getUid(), audit.getUid() );
    }

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }
}
