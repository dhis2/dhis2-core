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

import com.google.api.client.repackaged.com.google.common.base.Objects;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.commons.util.TextUtils;
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
import java.util.Set;
import java.util.stream.Stream;

/**
 * This class "collects" identifiers from all input objects.
 * This resulting map of all identifiers will then be used to "preheat/cache"
 * all the objects needed into memory to speed up the validation process.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see org.hisp.dhis.tracker.preheat.DefaultTrackerPreheatService
 */
public class TrackerIdentifierCollector
{
    public static Map<Class<?>, Set<String>> collect( TrackerBundleParams params )
    {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        collectTrackedEntities( map, params.getIdentifiers(), params.getTrackedEntities() );
        collectEnrollments( map, params.getIdentifiers(), params.getEnrollments() );
        collectEvents( map, params.getIdentifiers(), params.getEvents() );
        collectRelationships( map, params.getIdentifiers(), params.getRelationships() );

        return map;
    }

    public static Map<Class<?>, Set<String>> collect( TrackerPreheatParams params )
    {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        collectTrackedEntities( map, params.getIdentifiers(), params.getTrackedEntities() );
        collectEnrollments( map, params.getIdentifiers(), params.getEnrollments() );
        collectEvents( map, params.getIdentifiers(), params.getEvents() );
        collectRelationships( map, params.getIdentifiers(), params.getRelationships() );

        return map;
    }

    private static void collectTrackedEntities(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<TrackedEntity> trackedEntities )
    {
        trackedEntities.forEach( trackedEntity -> {
            addIdentifier( map, TrackedEntity.class, params.getIdScheme().getIdScheme(),
                trackedEntity.getTrackedEntity() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(),
                trackedEntity.getOrgUnit() );

            collectEnrollments( map, params, trackedEntity.getEnrollments() );

            collectTrackedEntityAttribute( map, params, trackedEntity.getAttributes() );
        } );
    }

    private static void collectEnrollments(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Enrollment> enrollments )
    {
        enrollments.forEach( enrollment -> {
            addIdentifier( map, TrackedEntity.class, TrackerIdScheme.UID, enrollment.getTrackedEntity() );
            addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, enrollment.getEnrollment() );
            addIdentifier( map, Program.class, params.getProgramIdScheme().getIdScheme(), enrollment.getProgram() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(),
                enrollment.getOrgUnit() );

            collectEvents( map, params, enrollment.getEvents() );
            collectTrackedEntityAttribute( map, params, enrollment.getAttributes() );
        } );
    }

    private static void collectEvents(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Event> events )
    {
        events.forEach( event -> {
            addIdentifier( map, TrackedEntity.class, TrackerIdScheme.UID, event.getTrackedEntity() );
            addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, event.getEnrollment() );
            addIdentifier( map, Event.class, TrackerIdScheme.UID, event.getEvent() );
            addIdentifier( map, Program.class, params.getProgramIdScheme().getIdScheme(), event.getProgram() );
            addIdentifier( map, ProgramStage.class, params.getProgramStageIdScheme().getIdScheme(),
                event.getProgramStage() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(), event.getOrgUnit() );

            Stream.of( Objects.firstNonNull( event.getAttributeCategoryOptions(), "" ).split( TextUtils.SEMICOLON ) )
                .forEach(
                    s -> addIdentifier( map, CategoryOption.class, params.getCategoryOption().getIdScheme(), s ) );

            addIdentifier( map, CategoryOptionCombo.class, params.getCategoryOptionComboIdScheme().getIdScheme(),
                event.getAttributeOptionCombo() );

            event.getDataValues()
                .forEach( dv -> addIdentifier( map, DataElement.class, params.getDataElementIdScheme().getIdScheme(),
                    dv.getDataElement() ) );
        } );
    }

    private static void collectRelationships(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Relationship> relationships )
    {
        relationships.parallelStream().forEach( relationship -> {
            addIdentifier( map, Relationship.class, TrackerIdScheme.UID, relationship.getRelationship() );
            if ( relationship.getFrom() != null )
            {
                addIdentifier( map, TrackedEntity.class, TrackerIdScheme.UID,
                    relationship.getFrom().getTrackedEntity() );
                addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, relationship.getFrom().getEnrollment() );
                addIdentifier( map, Event.class, TrackerIdScheme.UID, relationship.getFrom().getEvent() );
            }
            if ( relationship.getTo() != null )
            {
                addIdentifier( map, TrackedEntity.class, TrackerIdScheme.UID, relationship.getTo().getTrackedEntity() );
                addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, relationship.getTo().getEnrollment() );
                addIdentifier( map, Event.class, TrackerIdScheme.UID, relationship.getTo().getEvent() );
            }
        } );
    }

    private static void collectTrackedEntityAttribute(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Attribute> attributes )
    {
        if ( attributes.isEmpty() )
        {
            return;
        }

        attributes.forEach( attribute ->
            addIdentifier( map, TrackedEntityAttribute.class, TrackerIdScheme.UID, attribute.getAttribute() ) );
    }

    private static <T> void addIdentifier( Map<Class<?>, Set<String>> map,
        Class<T> klass, TrackerIdScheme identifier, String str )
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
