package org.hisp.dhis.tracker.bundle;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.HibernateCacheManager;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerProgramRuleService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.hisp.dhis.tracker.sideeffect.SideEffectHandlerService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.report.TrackerBundleReport;
import org.hisp.dhis.tracker.report.TrackerObjectReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class DefaultTrackerBundleService
    implements TrackerBundleService
{
    private final TrackerPreheatService trackerPreheatService;

    private final TrackerConverterService<TrackedEntity, org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityTrackerConverterService;

    private final TrackerConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService;

    private final TrackerConverterService<Event, ProgramStageInstance> eventTrackerConverterService;

    private final TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipTrackerConverterService;

    private final CurrentUserService currentUserService;

    private final IdentifiableObjectManager manager;

    private final SessionFactory sessionFactory;

    private final HibernateCacheManager cacheManager;

    private final DbmsManager dbmsManager;

    private final TrackerProgramRuleService trackerProgramRuleService;

    private final ReservedValueService reservedValueService;

    private List<TrackerBundleHook> bundleHooks = new ArrayList<>();
    private List<SideEffectHandlerService> sideEffectHandlers = new ArrayList<>();

    @Autowired( required = false )
    public void setBundleHooks( List<TrackerBundleHook> bundleHooks )
    {
        this.bundleHooks = bundleHooks;
    }

    @Autowired( required = false )
    public void setSideEffectHandlers( List<SideEffectHandlerService> sideEffectHandlers )
    {
        this.sideEffectHandlers = sideEffectHandlers;
    }

    public DefaultTrackerBundleService(
        TrackerPreheatService trackerPreheatService,
        TrackerConverterService<TrackedEntity, TrackedEntityInstance> trackedEntityTrackerConverterService,
        TrackerConverterService<Enrollment, ProgramInstance> enrollmentTrackerConverterService,
        TrackerConverterService<Event, ProgramStageInstance> eventTrackerConverterService,
        TrackerConverterService<Relationship, org.hisp.dhis.relationship.Relationship> relationshipTrackerConverterService,
        CurrentUserService currentUserService,
        IdentifiableObjectManager manager,
        SessionFactory sessionFactory,
        HibernateCacheManager cacheManager,
        DbmsManager dbmsManager,
        TrackerProgramRuleService trackerProgramRuleService,
        ReservedValueService reservedValueService )

    {
        this.trackerPreheatService = trackerPreheatService;
        this.trackedEntityTrackerConverterService = trackedEntityTrackerConverterService;
        this.enrollmentTrackerConverterService = enrollmentTrackerConverterService;
        this.eventTrackerConverterService = eventTrackerConverterService;
        this.relationshipTrackerConverterService = relationshipTrackerConverterService;
        this.currentUserService = currentUserService;
        this.manager = manager;
        this.sessionFactory = sessionFactory;
        this.cacheManager = cacheManager;
        this.dbmsManager = dbmsManager;
        this.trackerProgramRuleService = trackerProgramRuleService;
        this.reservedValueService = reservedValueService;
    }

    @Override
    @Transactional
    public List<TrackerBundle> create( TrackerBundleParams params )
    {
        TrackerBundle trackerBundle = params.toTrackerBundle();
        TrackerPreheatParams preheatParams = params.toTrackerPreheatParams();
        preheatParams.setUser( getUser( preheatParams.getUser(), preheatParams.getUserId() ) );

        TrackerPreheat preheat = trackerPreheatService.preheat( preheatParams );
        trackerBundle.setPreheat( preheat );

        Map<String, List<RuleEffect>> enrollmentRuleEffects =
            trackerProgramRuleService.calculateEnrollmentRuleEffects( trackerBundle );
        Map<String, List<RuleEffect>> eventRuleEffects =
            trackerProgramRuleService.calculateEventRuleEffects( trackerBundle );
        trackerBundle.setEnrollmentRuleEffects( enrollmentRuleEffects );
        trackerBundle.setEventRuleEffects( eventRuleEffects );

        return Collections.singletonList( trackerBundle ); // for now we don't split the bundles
    }

    @Override
    @Transactional
    public TrackerBundleReport commit( TrackerBundle bundle )
    {
        TrackerBundleReport bundleReport = new TrackerBundleReport();

        if ( TrackerBundleMode.VALIDATE == bundle.getImportMode() )
        {
            return bundleReport;
        }

        Session session = sessionFactory.getCurrentSession();

        bundleHooks.forEach( hook -> hook.preCommit( bundle ) );

        TrackerTypeReport trackedEntityReport = handleTrackedEntities( session, bundle );
        TrackerTypeReport enrollmentReport = handleEnrollments( session, bundle );
        TrackerTypeReport eventReport = handleEvents( session, bundle );
        TrackerTypeReport relationshipReport = handleRelationships( session, bundle );

        bundleReport.getTypeReportMap().put( TrackerType.TRACKED_ENTITY, trackedEntityReport );
        bundleReport.getTypeReportMap().put( TrackerType.ENROLLMENT, enrollmentReport );
        bundleReport.getTypeReportMap().put( TrackerType.EVENT, eventReport );
        bundleReport.getTypeReportMap().put( TrackerType.RELATIONSHIP, relationshipReport );

        bundleHooks.forEach( hook -> hook.postCommit( bundle ) );

        dbmsManager.clearSession();
        cacheManager.clearCache();

        return bundleReport;
    }

    private TrackerTypeReport handleTrackedEntities( Session session, TrackerBundle bundle )
    {
        List<TrackedEntity> trackedEntities = bundle.getTrackedEntities();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.TRACKED_ENTITY );

        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( TrackedEntity.class, o, bundle ) ) );
        session.flush();

        Date now = new Date();

        for ( int idx = 0; idx < trackedEntities.size(); idx++ )
        {
            TrackedEntity trackedEntity = trackedEntities.get( idx );
            org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = trackedEntityTrackerConverterService.from(
                bundle.getPreheat(), trackedEntity );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.TRACKED_ENTITY, trackedEntityInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            if ( bundle.getImportStrategy().isCreate() )
            {
                trackedEntityInstance.setCreated( now );
                trackedEntityInstance.setCreatedAtClient( now );
            }

            trackedEntityInstance.setLastUpdated( now );
            trackedEntityInstance.setLastUpdatedAtClient( now );
            trackedEntityInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( trackedEntityInstance );
            bundle.getPreheat().putTrackedEntities( bundle.getIdentifier(), Collections.singletonList( trackedEntityInstance ) );

            handleTrackedEntityAttributeValues( session, bundle.getPreheat(), trackedEntity.getAttributes(), trackedEntityInstance );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        session.flush();
        trackedEntities.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( TrackedEntity.class, o, bundle ) ) );

        return typeReport;
    }

    private TrackerTypeReport handleEnrollments( Session session, TrackerBundle bundle )
    {
        List<Enrollment> enrollments = bundle.getEnrollments();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.ENROLLMENT );

        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Enrollment.class, o, bundle ) ) );
        session.flush();

        Date now = new Date();

        for ( int idx = 0; idx < enrollments.size(); idx++ )
        {
            Enrollment enrollment = enrollments.get( idx );
            ProgramInstance programInstance = enrollmentTrackerConverterService.from( bundle.getPreheat(), enrollment );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.ENROLLMENT, programInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            if ( bundle.getImportStrategy().isCreate() )
            {
                programInstance.setCreated( now );
                programInstance.setCreatedAtClient( now );
            }

            programInstance.setLastUpdated( now );
            programInstance.setLastUpdatedAtClient( now );
            programInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( programInstance );
            bundle.getPreheat().putEnrollments( bundle.getIdentifier(), Collections.singletonList( programInstance ) );

            handleTrackedEntityAttributeValues( session, bundle.getPreheat(), enrollment.getAttributes(),
                programInstance.getEntityInstance() );

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }

            TrackerSideEffectDataBundle sideEffectDataBundle = TrackerSideEffectDataBundle.builder()
                .klass( ProgramInstance.class )
                .enrollmentRuleEffects( bundle.getEnrollmentRuleEffects() )
                .eventRuleEffects( bundle.getEventRuleEffects() )
                .object( programInstance )
                .importStrategy( bundle.getImportStrategy() )
                .accessedBy( bundle.getUsername() )
                .build();

            sideEffectHandlers.forEach( handler -> handler.handleSideEffect( sideEffectDataBundle ) );
        }

        session.flush();
        enrollments.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Enrollment.class, o, bundle ) ) );

        return typeReport;
    }

    private TrackerTypeReport handleEvents( Session session, TrackerBundle bundle )
    {
        List<Event> events = bundle.getEvents();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.EVENT );

        events.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Event.class, o, bundle ) ) );
        session.flush();

        for ( int idx = 0; idx < events.size(); idx++ )
        {
            Event event = events.get( idx );
            ProgramStageInstance programStageInstance = eventTrackerConverterService.from( bundle.getPreheat(), event );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.EVENT, programStageInstance.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            Date now = new Date();

            if ( bundle.getImportStrategy().isCreate() )
            {
                programStageInstance.setCreated( now );
                programStageInstance.setCreatedAtClient( now );
            }

            programStageInstance.setLastUpdated( now );
            programStageInstance.setLastUpdatedAtClient( now );
            programStageInstance.setLastUpdatedBy( bundle.getUser() );

            session.persist( programStageInstance );
            bundle.getPreheat().putEvents( bundle.getIdentifier(), Collections.singletonList( programStageInstance ) );

            typeReport.getStats().incCreated();

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }

            TrackerSideEffectDataBundle sideEffectDataBundle = TrackerSideEffectDataBundle.builder()
                .klass( ProgramStageInstance.class )
                .enrollmentRuleEffects( bundle.getEnrollmentRuleEffects() )
                .eventRuleEffects( bundle.getEventRuleEffects() )
                .object( programStageInstance )
                .importStrategy( bundle.getImportStrategy() )
                .accessedBy( bundle.getUsername() )
                .build();

            sideEffectHandlers.forEach( handler -> handler.handleSideEffect( sideEffectDataBundle ) );
        }

        session.flush();
        events.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Event.class, o, bundle ) ) );

        return typeReport;
    }

    private TrackerTypeReport handleRelationships( Session session, TrackerBundle bundle )
    {
        List<Relationship> relationships = bundle.getRelationships();
        TrackerTypeReport typeReport = new TrackerTypeReport( TrackerType.RELATIONSHIP );

        relationships.forEach( o -> bundleHooks.forEach( hook -> hook.preCreate( Relationship.class, o, bundle ) ) );

        for ( int idx = 0; idx < relationships.size(); idx++ )
        {
            Relationship relationship = relationships.get( idx );
            org.hisp.dhis.relationship.Relationship toRelationship = relationshipTrackerConverterService
                .from( bundle.getPreheat(), relationship );

            TrackerObjectReport objectReport = new TrackerObjectReport( TrackerType.EVENT,
                toRelationship.getUid(), idx );
            typeReport.addObjectReport( objectReport );

            Date now = new Date();

            if ( bundle.getImportStrategy().isCreate() )
            {
                toRelationship.setCreated( now );
            }

            toRelationship.setLastUpdated( now );
            toRelationship.setLastUpdatedBy( bundle.getUser() );

            session.persist( toRelationship );
            typeReport.getStats().incCreated();

            if ( FlushMode.OBJECT == bundle.getFlushMode() )
            {
                session.flush();
            }
        }

        relationships.forEach( o -> bundleHooks.forEach( hook -> hook.postCreate( Relationship.class, o, bundle ) ) );

        return typeReport;
    }

    //-----------------------------------------------------------------------------------
    // Utility Methods
    //-----------------------------------------------------------------------------------

    private void handleTrackedEntityAttributeValues( Session session, TrackerPreheat preheat,
        List<Attribute> attributes, TrackedEntityInstance trackedEntityInstance )
    {
        List<TrackedEntityAttributeValue> attributeValues = new ArrayList<>();
        List<String> attributeValuesForDeletion = new ArrayList<>();

        List<String> assignedFileResources = new ArrayList<>();
        List<String> unassignedFileResources = new ArrayList<>();

        Map<String, TrackedEntityAttributeValue> attributeValueMap = trackedEntityInstance.getTrackedEntityAttributeValues().stream()
            .collect( Collectors.toMap( teav -> teav.getAttribute().getUid(), trackedEntityAttributeValue -> trackedEntityAttributeValue ) );

        for ( Attribute at : attributes )
        {
            // TEAV.getValue has a lot of trickery behind it since its being used for encryption, so we can't rely on that to
            // get empty/null values, instead we build a simple list here to compare with.
            if ( StringUtils.isEmpty( at.getValue() ) )
            {
                attributeValuesForDeletion.add( at.getAttribute() );

                if ( attributeValueMap.containsKey( at.getAttribute() ) &&
                    attributeValueMap.get( at.getAttribute() ).getAttribute().getValueType().isFile() )
                {
                    unassignedFileResources.add( attributeValueMap.get( at.getAttribute() ).getValue() );
                }
            }

            TrackedEntityAttribute attribute = preheat.get( TrackerIdScheme.UID, TrackedEntityAttribute.class, at.getAttribute() );
            TrackedEntityAttributeValue attributeValue = null;

            if ( attributeValueMap.containsKey( at.getAttribute() ) )
            {
                TrackedEntityAttributeValue av = attributeValueMap.get( at.getAttribute() );

                av.setAttribute( attribute )
                    .setValue( at.getValue() )
                    .setStoredBy( at.getStoredBy() );

                attributeValue = av;
                attributeValues.add( attributeValue );
            }

            // new attribute value
            if ( attributeValue == null )
            {
                attributeValue = new TrackedEntityAttributeValue();

                attributeValue.setAttribute( attribute )
                    .setValue( at.getValue() )
                    .setStoredBy( at.getStoredBy() );

                attributeValues.add( attributeValue );
            }

            if ( !attributeValuesForDeletion.contains( at.getAttribute() ) &&
                attributeValue.getAttribute().getValueType().isFile() )
            {
                assignedFileResources.add( at.getValue() );
            }
        }

        for ( TrackedEntityAttributeValue attributeValue : attributeValues )
        {
            // since TEAV is the owning side here, we don't bother updating the TE.teav collection
            // as it will be reloaded on session clear
            if ( attributeValuesForDeletion.contains( attributeValue.getAttribute().getUid() ) )
            {
                session.remove( attributeValue );
            }
            else
            {
                attributeValue.setEntityInstance( trackedEntityInstance );
                session.persist( attributeValue );
            }

            if ( attributeValue.getAttribute().isGenerated() && attributeValue.getAttribute().getTextPattern() != null )
            {
                reservedValueService.useReservedValue(
                    attributeValue.getAttribute().getTextPattern(), attributeValue.getValue() );
            }
        }

        assignedFileResources.forEach( fr -> assignFileResource( session, preheat, fr ) );
        unassignedFileResources.forEach( fr -> unassignFileResource( session, preheat, fr ) );
    }

    private void assignFileResource( Session session, TrackerPreheat preheat, String fr )
    {
        FileResource fileResource = preheat.get( TrackerIdScheme.UID, FileResource.class, fr );

        if ( fileResource == null )
        {
            return;
        }

        fileResource.setAssigned( true );
        session.persist( fileResource );
    }

    private void unassignFileResource( Session session, TrackerPreheat preheat, String fr )
    {
        FileResource fileResource = preheat.get( TrackerIdScheme.UID, FileResource.class, fr );

        if ( fileResource == null )
        {
            return;
        }

        fileResource.setAssigned( false );
        session.persist( fileResource );
    }

    private User getUser( User user, String userUid )
    {
        if ( user != null ) // ıf user already set, reload the user to make sure its loaded in the current tx
        {
            return manager.get( User.class, user.getUid() );
        }

        if ( !StringUtils.isEmpty( userUid ) )
        {
            user = manager.get( User.class, userUid );
        }

        if ( user == null )
        {
            user = currentUserService.getCurrentUser();
        }

        return user;
    }
}
