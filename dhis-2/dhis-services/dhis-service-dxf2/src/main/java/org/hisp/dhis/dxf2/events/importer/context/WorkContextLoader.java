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

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luciano Fiandesio
 */
@Component
@Slf4j
public class WorkContextLoader
{
    private final ProgramSupplier programSupplier;

    private final OrganisationUnitSupplier organisationUnitSupplier;

    private final TrackedEntityInstanceSupplier trackedEntityInstanceSupplier;

    private final EnrollmentSupplier enrollmentSupplier;

    private final ProgramStageInstanceSupplier programStageInstanceSupplier;

    private final CategoryOptionComboSupplier categoryOptionComboSupplier;

    private final DataElementSupplier dataElementSupplier;

    private final NoteSupplier noteSupplier;

    private final ProgramOrgUnitSupplier programOrgUnitSupplier;

    private final AssignedUserSupplier assignedUserSupplier;

    private final ServiceDelegatorSupplier serviceDelegatorSupplier;

    private final static UidGenerator uidGen = new UidGenerator();

    private final SessionFactory sessionFactory;

    public WorkContextLoader(
    // @formatter:off
        ProgramSupplier programSupplier,
        OrganisationUnitSupplier organisationUnitSupplier,
        TrackedEntityInstanceSupplier trackedEntityInstanceSupplier,
        EnrollmentSupplier enrollmentSupplier,
        ProgramStageInstanceSupplier programStageInstanceSupplier,
        CategoryOptionComboSupplier categoryOptionComboSupplier,
        DataElementSupplier dataElementSupplier,
        NoteSupplier noteSupplier,
        AssignedUserSupplier assignedUserSupplier,
        ServiceDelegatorSupplier serviceDelegatorSupplier,
        ProgramOrgUnitSupplier programOrgUnitSupplier,
        SessionFactory sessionFactory
    // @formatter:on
    )
    {
        this.programSupplier = programSupplier;
        this.organisationUnitSupplier = organisationUnitSupplier;
        this.trackedEntityInstanceSupplier = trackedEntityInstanceSupplier;
        this.enrollmentSupplier = enrollmentSupplier;
        this.programStageInstanceSupplier = programStageInstanceSupplier;
        this.categoryOptionComboSupplier = categoryOptionComboSupplier;
        this.dataElementSupplier = dataElementSupplier;
        this.noteSupplier = noteSupplier;
        this.assignedUserSupplier = assignedUserSupplier;
        this.programOrgUnitSupplier = programOrgUnitSupplier;
        this.serviceDelegatorSupplier = serviceDelegatorSupplier;
        this.sessionFactory = sessionFactory;
    }

    @Transactional( readOnly = true )
    public WorkContext load( ImportOptions importOptions, List<org.hisp.dhis.dxf2.events.event.Event> events )
    {
        sessionFactory.getCurrentSession().flush();

        ImportOptions localImportOptions = importOptions;
        // API allows a null Import Options
        if ( localImportOptions == null )
        {
            localImportOptions = ImportOptions.getDefaultImportOptions();
        }

        initializeUser( localImportOptions );

        // Make sure all events have the 'uid' field populated
        events = uidGen.assignUidToEvents( events );

        final Map<String, Event> programStageInstanceMap = programStageInstanceSupplier
            .get( localImportOptions, events );

        final Map<String, Event> persistedProgramStageInstanceMap = programStageInstanceSupplier
            .get( localImportOptions, events );

        final Map<String, Pair<TrackedEntityInstance, Boolean>> teiMap = trackedEntityInstanceSupplier
            .get( localImportOptions, events );

        final Map<String, OrganisationUnit> orgUniMap = organisationUnitSupplier.get( localImportOptions, events );

        return WorkContext.builder()
            .importOptions( localImportOptions )
            .programsMap( programSupplier.get( localImportOptions, events ) )
            .programStageInstanceMap( programStageInstanceMap )
            .persistedProgramStageInstanceMap( persistedProgramStageInstanceMap )
            .organisationUnitMap( orgUniMap )
            .trackedEntityInstanceMap( teiMap )
            .programInstanceMap( enrollmentSupplier.get( localImportOptions, teiMap, events ) )
            .categoryOptionComboMap( categoryOptionComboSupplier.get( localImportOptions, events ) )
            .dataElementMap( dataElementSupplier.get( localImportOptions, events ) )
            .notesMap( noteSupplier.get( localImportOptions, events ) )
            .assignedUserMap( assignedUserSupplier.get( localImportOptions, events ) )
            .eventDataValueMap( new EventDataValueAggregator().aggregateDataValues( events, programStageInstanceMap,
                localImportOptions ) )
            .programWithOrgUnitsMap( programOrgUnitSupplier.get( localImportOptions, events, orgUniMap ) )
            .serviceDelegator( serviceDelegatorSupplier.get() )
            .build();
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
                initUser( currentUser );
                importOptions.setUser( currentUser );
            }
            else
            {
                log.error( "No current user found" );
            }
        }
        else
        {
            initUser( importOptions.getUser() );
        }
    }

    /**
     * Force Hibernate to pre-load all collections for the {@see User} object
     * and fetch the "isSuper()" data. This is required to avoid an Hibernate
     * error later, when this object becomes detached from the Hibernate
     * Session.
     */
    private void initUser( User user )
    {
        user = HibernateProxyUtils.unproxy( user );

        user.isSuper();
    }
}
