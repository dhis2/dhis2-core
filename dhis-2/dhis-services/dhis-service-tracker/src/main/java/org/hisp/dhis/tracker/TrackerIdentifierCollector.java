package org.hisp.dhis.tracker;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrackerIdentifierCollector
{
    public static Map<Class<?>, Map<TrackerIdentifier, Set<String>>> collect( TrackerBundle bundle )
    {
        Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map = new HashMap<>();

        collectTrackedEntities( map, bundle, bundle.getTrackedEntities() );
        collectEnrollments( map, bundle, bundle.getEnrollments() );
        collectEvents( map, bundle, bundle.getEvents() );

        return map;
    }

    private static void collectTrackedEntities( Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map,
        TrackerBundle bundle, List<TrackedEntityInstance> trackedEntities )
    {
        if ( trackedEntities.isEmpty() )
        {
            return;
        }

        trackedEntities.forEach( trackedEntity -> {
            addIdentifier( map, TrackedEntityInstance.class, bundle.getIdentifier(), trackedEntity.getTrackedEntityInstance() );
            addIdentifier( map, OrganisationUnit.class, bundle.getIdentifier(), trackedEntity.getOrgUnit() );

            collectEnrollments( map, bundle, trackedEntity.getEnrollments() );
            collectAttributes( map, bundle, trackedEntity.getAttributes() );
        } );
    }

    private static void collectEnrollments( Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map,
        TrackerBundle bundle, List<Enrollment> enrollments )
    {
        if ( enrollments.isEmpty() )
        {
            return;
        }

        enrollments.forEach( enrollment -> {
            addIdentifier( map, TrackedEntityInstance.class, bundle.getIdentifier(), enrollment.getTrackedEntityInstance() );
            addIdentifier( map, Enrollment.class, bundle.getIdentifier(), enrollment.getEnrollment() );
            addIdentifier( map, Program.class, bundle.getIdentifier(), enrollment.getProgram() );
            addIdentifier( map, OrganisationUnit.class, bundle.getIdentifier(), enrollment.getOrgUnit() );

            collectEvents( map, bundle, enrollment.getEvents() );
            collectAttributes( map, bundle, enrollment.getAttributes() );
        } );
    }

    private static void collectEvents( Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map,
        TrackerBundle bundle, List<Event> events )
    {
        if ( events.isEmpty() )
        {
            return;
        }

        events.forEach( event -> {
            addIdentifier( map, TrackedEntityInstance.class, bundle.getIdentifier(), event.getTrackedEntityInstance() );
            addIdentifier( map, Enrollment.class, bundle.getIdentifier(), event.getEnrollment() );
            addIdentifier( map, Event.class, bundle.getIdentifier(), event.getEvent() );
            addIdentifier( map, Program.class, bundle.getIdentifier(), event.getProgram() );
            addIdentifier( map, ProgramStage.class, bundle.getIdentifier(), event.getProgramStage() );
            addIdentifier( map, OrganisationUnit.class, bundle.getIdentifier(), event.getOrgUnit() );
            addIdentifier( map, CategoryOptionCombo.class, bundle.getIdentifier(), event.getAttributeOptionCombo() );

            event.getDataValues().forEach( dv -> {
                addIdentifier( map, DataElement.class, bundle.getIdentifier(), dv.getDataElement() );
            } );
        } );
    }

    private static void collectAttributes( Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map,
        TrackerBundle bundle, List<Attribute> attributes )
    {
        if ( !attributes.isEmpty() )
        {
            return;
        }

        attributes.forEach( attribute -> {
            addIdentifier( map, TrackedEntityAttribute.class, bundle.getIdentifier(), attribute.getAttribute() );
        } );
    }

    private static <T> void addIdentifier( Map<Class<?>, Map<TrackerIdentifier, Set<String>>> map,
        Class<T> klass, TrackerIdentifier identifier, String str )
    {
        if ( StringUtils.isEmpty( str ) || map == null || klass == null || identifier == null )
        {
            return;
        }

        if ( !map.containsKey( klass ) )
        {
            map.put( klass, new HashMap<>() );
        }

        if ( !map.get( klass ).containsKey( identifier ) )
        {
            map.get( klass ).put( identifier, new HashSet<>() );
        }

        map.get( klass ).get( identifier ).add( str );
    }
}
