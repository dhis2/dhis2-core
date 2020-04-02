package org.hisp.dhis.dxf2.events.event.validation;

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

import static com.google.common.base.Preconditions.checkNotNull;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.AttributeOptionComboLoader;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Responsible for loading the Validation Context cache with enough data to
 * resolve all validation logic without accessing the database.
 *
 *
 * @author Luciano Fiandesio
 */
@Component
public class ValidationContextLoader
{
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ProgramInstanceStore programInstanceStore;

    private final TrackerAccessManager trackerAccessManager;

    private final AttributeOptionComboLoader attributeOptionComboLoader;

    public ValidationContextLoader( @Qualifier( "readOnlyJdbcTemplate" ) JdbcTemplate jdbcTemplate,
        ProgramInstanceStore programInstanceStore, TrackerAccessManager trackerAccessManager,
        AttributeOptionComboLoader attributeOptionComboLoader )
    {
        checkNotNull( jdbcTemplate );
        checkNotNull( programInstanceStore );
        checkNotNull( trackerAccessManager );
        checkNotNull( attributeOptionComboLoader );

        this.jdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
        this.programInstanceStore = programInstanceStore;
        this.trackerAccessManager = trackerAccessManager;
        this.attributeOptionComboLoader = attributeOptionComboLoader;

    }

    public ValidationContext load( ImportOptions importOptions, List<Event> events )
    {
        Map<String, Program> programMap = loadPrograms();
        // @formatter:off
        return ValidationContext.builder()
            .importOptions( importOptions )
            .programInstanceStore( this.programInstanceStore )
            .trackerAccessManager( this.trackerAccessManager )
            .programsMap( programMap )
            .organisationUnitMap( loadOrganisationUnits( events ) )
            .trackedEntityInstanceMap( loadTrackedEntityInstances( events ) )
            .programInstanceMap( loadProgramInstances( events, programMap ) )
            .programStageInstanceMap( loadProgramStageInstances( events ) )
            .categoryOptionComboMap( loadCategoryOptionCombos( events, programMap, importOptions) )
            .build();
        // @formatter:on
    }

    private Map<String, CategoryOptionCombo> loadCategoryOptionCombos( List<Event> events,
        Map<String, Program> programMap, ImportOptions importOptions )
    {
        IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();
        Map<String, CategoryOptionCombo> eventToCocMap = new HashMap<>();
        for ( Event event : events )
        {
            Program program = programMap.get( event.getProgram() );

            // if event has "attribute option combo" set only, fetch the aoc directly
            if ( StringUtils.isNotEmpty( event.getAttributeOptionCombo() )
                && StringUtils.isEmpty( event.getAttributeCategoryOptions() ) )
            {
                eventToCocMap.put( event.getEvent(),
                    attributeOptionComboLoader.getCategoryOptionCombo( idScheme, event.getAttributeOptionCombo() ) );
            }
            // if event has no "attribute option combo", fetch the default aoc
            else if ( StringUtils.isEmpty( event.getAttributeOptionCombo() )
                && StringUtils.isEmpty( event.getAttributeCategoryOptions() ) && program.getCategoryCombo() != null )
            {
                eventToCocMap.put( event.getEvent(), attributeOptionComboLoader.getDefault() );
            }
            else if ( StringUtils.isNotEmpty( event.getAttributeOptionCombo() )
                && StringUtils.isNotEmpty( event.getAttributeCategoryOptions() ) && program.getCategoryCombo() != null )
            {
                eventToCocMap.put( event.getEvent(),
                    attributeOptionComboLoader.getAttributeOptionCombo( program.getCategoryCombo(),
                        event.getAttributeCategoryOptions(), event.getAttributeOptionCombo(), idScheme ) );
            }

        }

        return eventToCocMap;
    }

    private Map<String, ProgramStageInstance> loadProgramStageInstances( List<Event> events )
    {
        Set<String> psiUid = events.stream().map( Event::getEnrollment ).collect( Collectors.toSet() );

        final String sql = "select psi.programinstanceid, psi.uid, psi.status, psi.deleted from programstageinstance psi where psi.uid in (:ids)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", psiUid );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, ProgramStageInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                ProgramStageInstance psi = new ProgramStageInstance();
                psi.setId( rs.getLong( "programinstanceid" ) );
                psi.setUid( rs.getString( "uid" ) );
                psi.setStatus( EventStatus.valueOf( rs.getString( "status" ) ) );
                psi.setDeleted( rs.getBoolean( "deleted" ) );
                results.put( psi.getUid(), psi );

            }
            return results;
        } );
    }

    private Map<String, ProgramInstance> loadProgramInstances( List<Event> events, Map<String, Program> programMap )
    {
        // @formatter:off
        // Collect all the org unit uids to pass as SQL query argument
        Set<String> programInstanceUids = events.stream()
            .filter( e -> e.getEnrollment() != null )
            .map( Event::getEnrollment ).collect( Collectors.toSet() );

        // Create a bi-directional map tei uid -> org unit id
        Map<String, String> programInstanceToEvent = events.stream()
            .filter( e -> e.getEnrollment() != null )
            .collect( Collectors.toMap( Event::getEnrollment, Event::getEvent  ) );
        // @formatter:on

        final String sql = "select pi.programinstanceid, pi.programid, pi.uid from programinstance pi where pi.uid in (:ids)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", programInstanceUids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, ProgramInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                ProgramInstance pi = new ProgramInstance();
                pi.setId( rs.getLong( "programinstanceid" ) );
                pi.setUid( rs.getString( "uid" ) );
                pi.setProgram( getProgramById( rs.getLong( "programid" ), programMap.values() ) );
                results.put( programInstanceToEvent.get( pi.getUid() ), pi );

            }
            return results;
        } );
    }

    private Program getProgramById( long id, Collection<Program> programs )
    {
        for ( Program program : programs )
        {
            if ( program.getId() == id )
            {
                return program;
            }
        }
        return null;
    }

    private Map<String, TrackedEntityInstance> loadTrackedEntityInstances( List<Event> events )
    {
        // @formatter:off
        // Collect all the org unit uids to pass as SQL query argument
        Set<String> teiUids = events.stream()
            .filter( e -> e.getTrackedEntityInstance() != null )
            .map( Event::getTrackedEntityInstance ).collect( Collectors.toSet() );

        // Create a bi-directional map tei uid -> org unit id
        Map<String, String> teiToEvent = events.stream()
            .filter( e -> e.getTrackedEntityInstance() != null )
            .collect( Collectors.toMap( Event::getTrackedEntityInstance, Event::getEvent  ) );
        // @formatter:on

        final String sql = "select tei.trackedentityinstanceid, tei.uid, tei.code from trackedentityinstance tei where tei.uid in (:ids)";
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", teiUids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, TrackedEntityInstance> results = new HashMap<>();

            while ( rs.next() )
            {
                TrackedEntityInstance tei = new TrackedEntityInstance();
                tei.setId( rs.getLong( "trackedentityinstanceid" ) );
                tei.setUid( rs.getString( "uid" ) );
                tei.setCode( rs.getString( "code" ) );

                results.put( teiToEvent.get( tei.getUid() ), tei );

            }
            return results;
        } );
    }

    /**
     * OrgUnit -> List of events
     *
     * @param events
     * @return
     */
    private Map<String, OrganisationUnit> loadOrganisationUnits( List<Event> events )
    {
        // @formatter:off
        // Collect all the org unit uids to pass as SQL query argument
        Set<String> orgUnitUids = events.stream()
            .filter( e -> e.getOrgUnit() != null ).map( Event::getOrgUnit )
            .collect( Collectors.toSet() );

        // Create a bi-directional map event uid -> org unit uid
        Map<String, String> orgUnitToEvent = events.stream()
            .filter( e -> e.getOrgUnit() != null )
            .collect( Collectors.toMap( Event::getOrgUnit, Event::getEvent  ) );
        // @formatter:on

        final String sql = "select ou.organisationunitid, ou.uid, ou.code, ou.path, ou.hierarchylevel from organisationunit ou where ou.uid in (:ids)";

        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue( "ids", orgUnitUids );

        return jdbcTemplate.query( sql, parameters, ( ResultSet rs ) -> {
            Map<String, OrganisationUnit> results = new HashMap<>();

            while ( rs.next() )
            {
                OrganisationUnit ou = new OrganisationUnit();
                ou.setId( rs.getLong( "organisationunitid" ) );
                ou.setUid( rs.getString( "uid" ) );
                ou.setCode( rs.getString( "code" ) );
                ou.setPath( rs.getString( "path" ) );
                ou.setHierarchyLevel( rs.getInt( "hierarchylevel" ) );

                results.put( orgUnitToEvent.get( ou.getUid() ), ou );

            }
            return results;
        } );

    }

    private Map<String, Program> loadPrograms()
    {
        final String sql = "select p.programid, p.uid, p.name, p.type, c.uid, c.name, ps.uid as ps_uid, ps.featuretype as ps_feature_type, ps.sort_order, string_agg(ou.uid, ', ') ous\n"
            + "from program p\n" + "         LEFT JOIN categorycombo c on p.categorycomboid = c.categorycomboid\n"
            + "        LEFT JOIN programstage ps on p.programid = ps.programid\n"
            + "        LEFT JOIN program_organisationunits pou on p.programid = pou.programid\n"
            + "        LEFT JOIN organisationunit ou on pou.organisationunitid = ou.organisationunitid\n" + "\n"
            + "group by p.programid, p.uid, p.name, p.type, c.uid, c.name, ps.uid , ps.featuretype, ps.sort_order\n"
            + "order by p.programid, ps.sort_order;";

        return jdbcTemplate.query( sql, ( ResultSet rs ) -> {
            Map<String, Program> results = new HashMap<>();
            long programId = 0;
            while ( rs.next() )
            {
                if ( programId != rs.getLong( "programid" ) )
                {
                    Set<ProgramStage> programStages = new HashSet<>();
                    Program program = new Program();
                    program.setId( rs.getLong( "programid" ) );
                    program.setUid( rs.getString( "uid" ) );
                    program.setName( rs.getString( "name" ) );
                    program.setProgramType( ProgramType.fromValue( rs.getString( "type" ) ) );

                    ProgramStage programStage = new ProgramStage();
                    programStage.setUid( rs.getString( "ps_uid" ) );
                    programStage.setFeatureType( FeatureType.getTypeFromName( rs.getString( "ps_feature_type" ) ) );
                    programStage.setSortOrder( rs.getInt( "ps_sort_order" ) );
                    programStages.add( programStage );

                    CategoryCombo categoryCombo = new CategoryCombo();
                    categoryCombo.setUid( rs.getString( "catcombo_uid" ) );
                    categoryCombo.setName( rs.getString( "catcombo_name" ) );
                    program.setCategoryCombo( categoryCombo );

                    program.setProgramStages( programStages );
                    results.put( rs.getString( "uid" ), program );
                    String ous = rs.getString( "ous" );
                    if ( StringUtils.isNotEmpty( ous ) )
                    {
                        program.setOrganisationUnits( Arrays.stream( ous.split( "," ) ).map( s -> {
                            OrganisationUnit ou = new OrganisationUnit();
                            ou.setUid( s );
                            return ou;
                        } ).collect( Collectors.toSet() ) );
                    }

                    programId = program.getId();
                }
                else
                {
                    ProgramStage programStage = new ProgramStage();
                    programStage.setUid( rs.getString( "ps_uid" ) );
                    programStage.setSortOrder( rs.getInt( "ps_sort_order" ) );
                    results.get( rs.getString( "uid" ) ).getProgramStages().add( programStage );
                }
            }
            return results;
        } );
    }

}
