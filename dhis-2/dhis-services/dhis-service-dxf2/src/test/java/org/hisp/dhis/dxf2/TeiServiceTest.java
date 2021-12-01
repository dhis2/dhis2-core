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
package org.hisp.dhis.dxf2;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.context.WorkContextLoader;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.trackedentity.AbstractTrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.DefaultTrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Luca Cambi <luca@dhis2.org>
 */
public class TeiServiceTest
{

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ImportOptions importOptions;

    @Mock
    private WorkContext workContext;

    @Mock
    private JobConfiguration jobConfiguration;

    @Mock
    private Notifier notifier;

    @Mock
    private DefaultTrackedEntityInstanceService defaultTrackedEntityInstanceService;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private TrackedEntityInstance trackedEntityInstance;

    @Mock
    private UserService userService;

    @Mock
    private QueryService queryService;

    @Mock
    private SchemaService schemaService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private TrackedEntityAttributeValueService trackedEntityAttributeValueService;

    @Mock
    private DbmsManager dbmsManager;

    @Mock
    private IdSchemes idSchemes;

    @Mock
    private IdentifiableObjectManager identifiableObjectManager;

    @Mock
    private TrackerAccessManager trackerAccessManager;

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private WorkContextLoader workContextLoader;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private Session session;

    @Mock
    private User user;

    private AbstractTrackedEntityInstanceService trackedEntityInstanceService;

    private static final String trackedEntityTypeUid = "tet";

    private static final String trackedEntityInstanceUid = "tei";

    private static final String orgUnitUid = "orgUnit";

    @Before
    public void setUp()
    {
        trackedEntityInstanceService = mock( AbstractTrackedEntityInstanceService.class, CALLS_REAL_METHODS );

        when( sessionFactory.getCurrentSession() ).thenReturn( session );

        when( importOptions.getUser() ).thenReturn( user );
        when( importOptions.getIdSchemes() ).thenReturn( idSchemes );
        when( workContext.getImportOptions() ).thenReturn( importOptions );

        Map<String, OrganisationUnit> organisationUnitMap = new HashMap<>();
        organisationUnitMap.put( trackedEntityInstanceUid, new OrganisationUnit() );

        when( workContext.getOrganisationUnitMap() ).thenReturn( organisationUnitMap );

        when( idSchemes.getTrackedEntityIdScheme() ).thenReturn( IdScheme.UID );
        when( idSchemes.getOrgUnitIdScheme() ).thenReturn( IdScheme.UID );

        when( notifier.notify( any( JobConfiguration.class ), any( NotificationLevel.class ), anyString(),
            anyBoolean() ) ).thenReturn( notifier );
        when( notifier.clear( any() ) ).thenReturn( notifier );

        when( defaultTrackedEntityInstanceService.getTrackedEntityInstance( trackedEntityInstanceUid ) )
            .thenReturn( new org.hisp.dhis.trackedentity.TrackedEntityInstance() );
        when( defaultTrackedEntityInstanceService.getTrackedEntityInstancesUidsIncludingDeleted( anyList() ) )
            .thenReturn( new ArrayList<>() );

        when( enrollmentService.mergeOrDeleteEnrollments( anyList(), any(), any(), anyBoolean() ) )
            .thenReturn( new ImportSummaries() );
        when( enrollmentService.addEnrollmentList( anyList(), any() ) ).thenReturn( new ImportSummaries() );

        when( relationshipService.processRelationshipList( anyList(), any() ) ).thenReturn( new ImportSummaries() );

        when( userService.getUser( anyString() ) ).thenReturn( new User() );
        when( identifiableObjectManager.getObject( TrackedEntityType.class, IdScheme.UID, trackedEntityTypeUid ) )
            .thenReturn( new TrackedEntityType() );
        when( identifiableObjectManager.getObject( OrganisationUnit.class, IdScheme.UID, orgUnitUid ) )
            .thenReturn( new OrganisationUnit() );
        when( trackerAccessManager.canWrite( any(), any( org.hisp.dhis.trackedentity.TrackedEntityInstance.class ) ) )
            .thenReturn( new ArrayList<>() );

        when( trackedEntityInstance.getOrgUnit() ).thenReturn( orgUnitUid );
        when( trackedEntityInstance.getAttributes() ).thenReturn( new ArrayList<>() );
        when( trackedEntityInstance.getTrackedEntityType() ).thenReturn( trackedEntityTypeUid );
        when( trackedEntityInstance.getTrackedEntityInstance() ).thenReturn( trackedEntityInstanceUid );

        setFieldInAbstractService();
    }

    private void setFieldInAbstractService()
    {
        ReflectionTestUtils.setField( trackedEntityInstanceService, "workContextLoader", workContextLoader );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "sessionFactory", sessionFactory );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "notifier", notifier );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "teiService", defaultTrackedEntityInstanceService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "enrollmentService", enrollmentService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "userService", userService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "queryService", queryService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "schemaService", schemaService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "trackedEntityAttributeValueService",
            trackedEntityAttributeValueService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "relationshipService", relationshipService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "programInstanceService", programInstanceService );

        ReflectionTestUtils.setField( trackedEntityInstanceService, "dbmsManager", dbmsManager );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "manager", identifiableObjectManager );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "trackerAccessManager", trackerAccessManager );

        ReflectionTestUtils.setField( trackedEntityInstanceService, "programCache", new CachingMap<>() );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "trackedEntityCache", new CachingMap<>() );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "trackedEntityAttributeCache", new CachingMap<>() );
    }

    @Test
    public void shouldAddTrackedEntityWithCreateStrategy()
    {
        List<TrackedEntityInstance> trackedEntityInstanceList = Collections.singletonList( trackedEntityInstance );

        when( importOptions.getImportStrategy() ).thenReturn( ImportStrategy.CREATE );

        when( workContextLoader.loadForTei( importOptions, trackedEntityInstanceList ) ).thenReturn( workContext );

        ImportSummaries importSummaries = trackedEntityInstanceService.mergeOrDeleteTrackedEntityInstances(
            trackedEntityInstanceList,
            importOptions, jobConfiguration );

        assertFalse(
            importSummaries.getImportSummaries().stream().anyMatch( is -> is.isStatus( ImportStatus.ERROR ) ) );

        verify( defaultTrackedEntityInstanceService, times( 1 ) ).addTrackedEntityInstance( any() );
    }

    @Test
    public void shouldUpdateTrackedEntityWithUpdateStrategy()
    {
        List<TrackedEntityInstance> trackedEntityInstanceList = Collections.singletonList( trackedEntityInstance );

        when( importOptions.getImportStrategy() ).thenReturn( ImportStrategy.UPDATE );

        Map<String, org.hisp.dhis.trackedentity.TrackedEntityInstance> map = new HashMap<>();
        map.put( trackedEntityInstanceUid, new org.hisp.dhis.trackedentity.TrackedEntityInstance() );

        when( workContext.getTrackedEntityInstanceMap1() ).thenReturn( map );

        when( workContextLoader.loadForTei( importOptions, trackedEntityInstanceList ) ).thenReturn( workContext );

        ImportSummaries importSummaries = trackedEntityInstanceService.mergeOrDeleteTrackedEntityInstances(
            trackedEntityInstanceList,
            importOptions, jobConfiguration );

        assertFalse(
            importSummaries.getImportSummaries().stream().anyMatch( is -> is.isStatus( ImportStatus.ERROR ) ) );

        verify( session, times( 1 ) ).merge( any() );
    }

    @Test
    public void shouldDeleteTrackedEntityWithDeleteStrategy()
    {
        List<TrackedEntityInstance> trackedEntityInstanceList = Collections.singletonList( trackedEntityInstance );

        when( defaultTrackedEntityInstanceService.trackedEntityInstanceExists( trackedEntityInstanceUid ) )
            .thenReturn( true );

        when( importOptions.getImportStrategy() ).thenReturn( ImportStrategy.DELETE );

        when( workContextLoader.loadForTei( importOptions, trackedEntityInstanceList ) ).thenReturn( workContext );

        ImportSummaries importSummaries = trackedEntityInstanceService.mergeOrDeleteTrackedEntityInstances(
            trackedEntityInstanceList,
            importOptions, jobConfiguration );

        assertFalse(
            importSummaries.getImportSummaries().stream().anyMatch( is -> is.isStatus( ImportStatus.ERROR ) ) );

        verify( defaultTrackedEntityInstanceService, times( 1 ) ).deleteTrackedEntityInstance( any() );
    }

}
