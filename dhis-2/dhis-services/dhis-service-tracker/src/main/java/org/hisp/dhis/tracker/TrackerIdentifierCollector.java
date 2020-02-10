package org.hisp.dhis.tracker;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundleParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerIdentifierCollector
{
    public static Map<Class<?>, Set<String>> collect( TrackerBundleParams params )
    {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        collectTrackedEntities( map, params.getIdentifier(), params.getTrackedEntities() );
        collectEnrollments( map, params.getIdentifier(), params.getEnrollments() );
        collectEvents( map, params.getIdentifier(), params.getEvents() );
        collectRelationships( map, params.getIdentifier(), params.getRelationships() );

        return map;
    }

    public static Map<Class<?>, Set<String>> collect( TrackerPreheatParams params )
    {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        collectTrackedEntities( map, params.getIdentifier(), params.getTrackedEntities() );
        collectEnrollments( map, params.getIdentifier(), params.getEnrollments() );
        collectEvents( map, params.getIdentifier(), params.getEvents() );
        collectRelationships( map, params.getIdentifier(), params.getRelationships() );

        return map;
    }

    private static void collectTrackedEntities(
        Map<Class<?>, Set<String>> map, TrackerIdentifier identifier, List<TrackedEntity> trackedEntities )
    {
        trackedEntities.forEach( trackedEntity -> {
            addIdentifier( map, TrackedEntity.class, identifier, trackedEntity.getTrackedEntity() );
            addIdentifier( map, OrganisationUnit.class, identifier, trackedEntity.getOrgUnit() );

            collectEnrollments( map, identifier, trackedEntity.getEnrollments() );
            collectAttributes( map, identifier, trackedEntity.getAttributes() );
        } );
    }

    private static void collectEnrollments(
        Map<Class<?>, Set<String>> map, TrackerIdentifier identifier, List<Enrollment> enrollments )
    {
        enrollments.forEach( enrollment -> {
            addIdentifier( map, TrackedEntity.class, identifier, enrollment.getTrackedEntityInstance() );
            addIdentifier( map, Enrollment.class, identifier, enrollment.getEnrollment() );
            addIdentifier( map, Program.class, identifier, enrollment.getProgram() );
            addIdentifier( map, OrganisationUnit.class, identifier, enrollment.getOrgUnit() );

            collectEvents( map, identifier, enrollment.getEvents() );
            collectAttributes( map, identifier, enrollment.getAttributes() );
        } );
    }

    private static void collectEvents(
        Map<Class<?>, Set<String>> map, TrackerIdentifier identifier, List<Event> events )
    {
        events.forEach( event -> {
            addIdentifier( map, TrackedEntity.class, identifier, event.getTrackedEntityInstance() );
            addIdentifier( map, Enrollment.class, identifier, event.getEnrollment() );
            addIdentifier( map, Event.class, identifier, event.getEvent() );
            addIdentifier( map, Program.class, identifier, event.getProgram() );
            addIdentifier( map, ProgramStage.class, identifier, event.getProgramStage() );
            addIdentifier( map, OrganisationUnit.class, identifier, event.getOrgUnit() );
            addIdentifier( map, CategoryOptionCombo.class, identifier, event.getAttributeOptionCombo() );

            event.getDataValues().forEach( dv -> {
                addIdentifier( map, DataElement.class, identifier, dv.getDataElement() );
            } );
        } );
    }

    private static void collectRelationships(
        Map<Class<?>, Set<String>> map, TrackerIdentifier identifier, List<Relationship> relationships )
    {
        relationships.forEach( relationship -> {
            addIdentifier( map, Relationship.class, identifier, relationship.getRelationship() );
            collectTrackedEntities( map, identifier,
                getEntities( relationship.getFrom().getTrackedEntity(), relationship.getTo().getTrackedEntity() ) );
            collectEnrollments( map, identifier,
                getEntities( relationship.getFrom().getEnrollment(), relationship.getTo().getEnrollment() ) );
            collectEvents( map, identifier,
                getEntities( relationship.getFrom().getEvent(), relationship.getTo().getEvent() ) );
        } );
    }

    private static <T extends Object> List<T> getEntities( T first, T second )
    {
        return Lists.newArrayList( first, second )
            .stream()
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
    }

    private static void collectAttributes(
        Map<Class<?>, Set<String>> map, TrackerIdentifier identifier, List<Attribute> attributes )
    {
        if ( attributes.isEmpty() )
        {
            return;
        }

        attributes.forEach( attribute -> {
            addIdentifier( map, TrackedEntityAttribute.class, identifier, attribute.getAttribute() );
        } );
    }

    private static <T> void addIdentifier( Map<Class<?>, Set<String>> map,
        Class<T> klass, TrackerIdentifier identifier, String str )
    {
        if ( StringUtils.isEmpty( str ) || map == null || klass == null || identifier == null )
        {
            return;
        }

        if ( !map.containsKey( klass ) )
        {
            map.put( klass, new HashSet<>() );
        }

        map.get( klass ).add( str );
    }
}
