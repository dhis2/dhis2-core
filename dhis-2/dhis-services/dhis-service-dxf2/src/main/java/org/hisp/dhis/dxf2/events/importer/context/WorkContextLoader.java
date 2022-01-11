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
package org.hisp.dhis.dxf2.events.importer.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * @author Luciano Fiandesio
 */
@Component
@RequiredArgsConstructor
public class WorkContextLoader
{
    private final ProgramEventSupplier programEventSupplier;

    private final ProgramSupplier programSupplier;

    private final OrganisationUnitSupplier organisationUnitSupplier;

    private final TrackedEntityInstanceSupplier trackedEntityInstanceSupplier;

    private final ProgramInstanceEventSupplier programInstanceEventSupplier;

    private final ProgramInstanceSupplier programInstanceSupplier;

    private final ProgramStageInstanceSupplier programStageInstanceSupplier;

    private final CategoryOptionComboSupplier categoryOptionComboSupplier;

    private final DataElementSupplier dataElementSupplier;

    private final NoteSupplier noteSupplier;

    private final ProgramOrgUnitSupplier programOrgUnitSupplier;

    private final AssignedUserSupplier assignedUserSupplier;

    private final ServiceDelegatorSupplier serviceDelegatorSupplier;

    private final TrackedEntityAttributeSupplier trackedEntityAttributeSupplier;

    private final static UidGenerator uidGen = new UidGenerator();

    private final SessionFactory sessionFactory;

    private final AclService aclService;

    @Transactional( readOnly = true )
    public WorkContext loadForEvent( ImportOptions importOptions, List<Event> events )
    {
        sessionFactory.getCurrentSession().flush();

        // API allows a null Import Options
        ImportOptions localImportOptions = Optional.ofNullable( importOptions )
            .orElse( ImportOptions.getDefaultImportOptions() );

        initializeUser( localImportOptions );

        // Make sure all events have the 'uid' field populated
        events = uidGen.assignUidToEvents( events );

        final Map<String, ProgramStageInstance> programStageInstanceMap = programStageInstanceSupplier
            .get( localImportOptions, events );

        final Map<String, Pair<TrackedEntityInstance, Boolean>> teiMap = getEventToTeiMap( localImportOptions,
            events );

        Multimap<String, String> orgUnitToEvent = HashMultimap.create();

        events.forEach( ev -> orgUnitToEvent.put( ev.getOrgUnit(), ev.getUid() ) );

        final Map<String, OrganisationUnit> organisationUnitMap = organisationUnitSupplier.get( localImportOptions,
            events.stream()
                .map( Event::getOrgUnit ).filter( Objects::nonNull )
                .collect( Collectors.toSet() ),
            orgUnitToEvent );

        return WorkContext.builder()
            .importOptions( localImportOptions )
            .programsMap( programEventSupplier.get( localImportOptions, events ) )
            .programStageInstanceMap( programStageInstanceMap )
            .organisationUnitMap( organisationUnitMap )
            .eventToTeiMap( teiMap )
            .programInstanceMap( programInstanceEventSupplier.get( localImportOptions, teiMap, events ) )
            .categoryOptionComboMap( categoryOptionComboSupplier.get( localImportOptions, events ) )
            .dataElementMap( dataElementSupplier.get( localImportOptions, events ) )
            .notesMap( noteSupplier.get( events ) )
            .assignedUserMap( assignedUserSupplier.get( events ) )
            .eventDataValueMap( new EventDataValueAggregator().aggregateDataValues( events, programStageInstanceMap,
                localImportOptions ) )
            .programWithOrgUnitsMap( programOrgUnitSupplier.get( events, organisationUnitMap ) )
            .serviceDelegator( serviceDelegatorSupplier.get() )
            .build();
    }

    private Map<String, Pair<TrackedEntityInstance, Boolean>> getEventToTeiMap( ImportOptions importOptions,
        List<Event> events )
    {
        Map<String, Pair<TrackedEntityInstance, Boolean>> teiMap = new HashMap<>();

        Multimap<String, String> teiToEvent = HashMultimap.create();

        for ( Event event : events )
        {
            teiToEvent.put( event.getTrackedEntityInstance(), event.getUid() );
        }

        Set<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceSupplier
            .get( events.stream()
                .map( Event::getTrackedEntityInstance )
                .filter( Objects::nonNull ).collect( Collectors.toSet() ) );

        for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
        {
            boolean canUpdate = aclService.canUpdate( importOptions.getUser(), trackedEntityInstance );
            for ( String event : teiToEvent.get( trackedEntityInstance.getUid() ) )
            {
                teiMap.put( event,
                    Pair.of( trackedEntityInstance,
                        !importOptions.isSkipLastUpdated()
                            ? canUpdate
                            : null ) );
            }
        }

        return teiMap;
    }

    /**
     * Make sure that the {@see User} object's properties are properly
     * initialized, to avoid running into Hibernate-related issues during
     * validation
     *
     * @param importOptions the {@see ImportOptions} object
     */
    private void initializeUser( ImportOptions importOptions )
    {
        if ( importOptions.getUser() == null )
        {
            final User currentUser = this.serviceDelegatorSupplier.get().getEventImporterUserService().getCurrentUser();

            //
            // This should never really happen!
            //
            if ( currentUser != null )
            {
                UserCredentials userCredentials = currentUser.getUserCredentials();
                initUserCredentials( userCredentials );
                importOptions.setUser( currentUser );
            }
        }
        else
        {
            final User user = importOptions.getUser();
            UserCredentials userCredentials = user.getUserCredentials();
            initUserCredentials( userCredentials );
        }
    }

    private void initUserCredentials( UserCredentials userCredentials )
    {
        userCredentials = HibernateProxyUtils.unproxy( userCredentials );

        userCredentials.isSuper();
    }

    @Transactional( readOnly = true )
    public WorkContext loadForTei( ImportOptions importOptions,
        List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> trackedEntityInstances )
    {
        sessionFactory.getCurrentSession().flush();

        Set<TrackedEntityInstance> teis = trackedEntityInstanceSupplier
            .get( trackedEntityInstances.stream()
                .map( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance::getTrackedEntityInstance )
                .filter( Objects::nonNull ).collect( Collectors.toSet() ) );

        final Map<String, TrackedEntityInstance> teiMap = new HashMap<>();

        teis.forEach( tei -> teiMap.put( tei.getUid(), tei ) );

        Multimap<String, String> orgUnitToTei = HashMultimap.create();

        trackedEntityInstances.forEach( trackedEntityInstance -> orgUnitToTei.put( trackedEntityInstance.getOrgUnit(),
            trackedEntityInstance.getTrackedEntityInstance() ) );

        Set<String> attributes = new HashSet<>();

        trackedEntityInstances.stream()
            .map( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance::getAttributes )
            .flatMap( Collection::stream ).forEach( a -> attributes.add( a.getAttribute() ) );

        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap = trackedEntityAttributeSupplier
            .get( importOptions, attributes );

        final Map<String, OrganisationUnit> organisationUnitMap = organisationUnitSupplier.get( importOptions,
            trackedEntityInstances.stream()
                .map( org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance::getOrgUnit )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() ),
            orgUnitToTei );

        return WorkContext.builder().importOptions( importOptions )
            .trackedEntityInstanceMap( teiMap )
            .organisationUnitMap( organisationUnitMap ).trackedEntityAttributeMap( trackedEntityAttributeMap )
            .build();
    }

    @Transactional( readOnly = true )
    public WorkContext loadForEnrollment( ImportOptions importOptions,
        List<Enrollment> enrollments )
    {
        sessionFactory.getCurrentSession().flush();

        Set<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceSupplier
            .get( enrollments.stream()
                .map( Enrollment::getTrackedEntityInstance )
                .filter( Objects::nonNull ).collect( Collectors.toSet() ) );

        final Map<String, TrackedEntityInstance> teiMap = new HashMap<>();

        trackedEntityInstances.forEach( tei -> teiMap.put( tei.getUid(), tei ) );

        Multimap<String, String> orgUnitToTei = HashMultimap.create();

        enrollments.forEach( enrollment -> orgUnitToTei.put( enrollment.getOrgUnit(), enrollment.getEnrollment() ) );

        final Map<String, OrganisationUnit> organisationUnitMap = organisationUnitSupplier.get( importOptions,
            enrollments.stream()
                .map( Enrollment::getOrgUnit )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() ),
            orgUnitToTei );

        Set<String> attributes = new HashSet<>();

        enrollments.stream().map( Enrollment::getAttributes )
            .flatMap( Collection::stream ).forEach( a -> attributes.add( a.getAttribute() ) );

        Map<String, TrackedEntityAttribute> trackedEntityAttributeMap = trackedEntityAttributeSupplier
            .get( importOptions, attributes );

        return WorkContext.builder().importOptions( importOptions )
            .programInstanceMap( programInstanceSupplier.get( enrollments ) )
            .programsMap( programSupplier.get( importOptions, enrollments.stream()
                .map( Enrollment::getProgram )
                .filter( Objects::nonNull ).collect( Collectors.toSet() ) ) )
            .trackedEntityInstanceMap( teiMap )
            .organisationUnitMap( organisationUnitMap ).trackedEntityAttributeMap( trackedEntityAttributeMap )
            .build();
    }
}
