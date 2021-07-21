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
package org.hisp.dhis.tracker;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.relationship.RelationshipKey;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.RelationshipPreheatKeySupport;
import org.springframework.stereotype.Component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

/**
 * This class "collects" identifiers from all input objects. This resulting map
 * of all identifiers will then be used to "preheat/cache" all the objects
 * needed into memory to speed up the validation process.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see org.hisp.dhis.tracker.preheat.DefaultTrackerPreheatService
 */
@Component
@RequiredArgsConstructor
public class TrackerIdentifierCollector
{
    public final static String ID_WILDCARD = "*";

    private final ProgramRuleService programRuleService;

    public Map<Class<?>, Set<String>> collect( TrackerImportParams params,
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults )
    {
        Map<Class<?>, Set<String>> map = new HashMap<>();

        collectTrackedEntities( map, params.getIdentifiers(), params.getTrackedEntities() );
        collectEnrollments( map, params.getIdentifiers(), params.getEnrollments() );
        collectEvents( map, params.getIdentifiers(), params.getEvents() );
        collectRelationships( map, params.getRelationships() );
        // Using "*" signals that all the entities of the given type have to be
        // preloaded in the Preheat
        map.put( TrackedEntityType.class, ImmutableSet.of( ID_WILDCARD ) );
        map.put( RelationshipType.class, ImmutableSet.of( ID_WILDCARD ) );
        collectDefaults( map, params.getIdentifiers(), defaults );

        collectProgramRulesFields( map, params.getIdentifiers() );
        return map;
    }

    private void collectProgramRulesFields( Map<Class<?>, Set<String>> map,
        TrackerIdentifierParams params )
    {
        Set<String> programStages = map.get( ProgramStage.class );
        if ( programStages != null )
        {
            List<ProgramRule> programRules = programRuleService.getProgramRuleByProgramStage( programStages );
            Set<String> dataElements = programRules.stream()
                .flatMap( pr -> pr.getProgramRuleActions().stream() )
                .filter( a -> Objects.nonNull( a.getDataElement() ) )
                .map( a -> a.getDataElement().getUid() )
                .collect( Collectors.toSet() );

            dataElements
                .forEach(
                    de -> addIdentifier( map, DataElement.class, params.getDataElementIdScheme().getIdScheme(), de ) );

            Set<String> attributes = programRules.stream()
                .flatMap( pr -> pr.getProgramRuleActions().stream() )
                .filter( a -> Objects.nonNull( a.getAttribute() ) )
                .map( a -> a.getAttribute().getUid() )
                .collect( Collectors.toSet() );

            attributes.forEach(
                attribute -> addIdentifier( map, TrackedEntityAttribute.class, TrackerIdScheme.UID, attribute ) );
        }
    }

    private void collectDefaults( Map<Class<?>, Set<String>> map,
        TrackerIdentifierParams params,
        Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults )
    {
        defaults.forEach( ( defaultClass, defaultMetadata ) -> addIdentifier( map, defaultClass,
            params.getIdScheme().getIdScheme(), defaultMetadata.getUid() ) );
    }

    private void collectTrackedEntities(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<TrackedEntity> trackedEntities )
    {
        trackedEntities.forEach( trackedEntity -> {
            addIdentifier( map, TrackedEntity.class, params.getIdScheme().getIdScheme(),
                trackedEntity.getTrackedEntity() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(),
                trackedEntity.getOrgUnit() );

            collectEnrollments( map, params, trackedEntity.getEnrollments() );

            trackedEntity.getAttributes().forEach( attribute -> addIdentifier( map, TrackedEntityAttribute.class,
                TrackerIdScheme.UID, attribute.getAttribute() ) );
        } );
    }

    private void collectEnrollments(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Enrollment> enrollments )
    {
        enrollments.forEach( enrollment -> {
            addIdentifier( map, TrackedEntity.class, TrackerIdScheme.UID, enrollment.getTrackedEntity() );
            addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, enrollment.getEnrollment() );
            addIdentifier( map, Program.class, params.getProgramIdScheme().getIdScheme(), enrollment.getProgram() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(),
                enrollment.getOrgUnit() );

            collectNotes( map, enrollment.getNotes() );
            collectEvents( map, params, enrollment.getEvents() );
            enrollment.getAttributes().forEach( attribute -> addIdentifier( map, TrackedEntityAttribute.class,
                TrackerIdScheme.UID, attribute.getAttribute() ) );
        } );
    }

    private void collectNotes( Map<Class<?>, Set<String>> map, List<Note> notes )
    {
        notes.forEach(
            note -> {
                if ( !StringUtils.isEmpty( note.getNote() ) && !StringUtils.isEmpty( note.getValue() ) )
                {
                    addIdentifier( map, TrackedEntityComment.class, TrackerIdScheme.UID, note.getNote() );
                }
            } );
    }

    private void collectEvents(
        Map<Class<?>, Set<String>> map, TrackerIdentifierParams params, List<Event> events )
    {
        events.forEach( event -> {
            addIdentifier( map, Enrollment.class, TrackerIdScheme.UID, event.getEnrollment() );
            addIdentifier( map, Event.class, TrackerIdScheme.UID, event.getEvent() );
            addIdentifier( map, Program.class, params.getProgramIdScheme().getIdScheme(), event.getProgram() );
            addIdentifier( map, ProgramStage.class, params.getProgramStageIdScheme().getIdScheme(),
                event.getProgramStage() );
            addIdentifier( map, OrganisationUnit.class, params.getOrgUnitIdScheme().getIdScheme(), event.getOrgUnit() );

            Stream
                .of( MoreObjects.firstNonNull( event.getAttributeCategoryOptions(), "" ).split( TextUtils.SEMICOLON ) )
                .forEach(
                    s -> addIdentifier( map, CategoryOption.class, params.getCategoryOptionIdScheme().getIdScheme(),
                        s ) );

            addIdentifier( map, CategoryOptionCombo.class, params.getCategoryOptionComboIdScheme().getIdScheme(),
                event.getAttributeOptionCombo() );

            event.getDataValues()
                .forEach( dv -> addIdentifier( map, DataElement.class, params.getDataElementIdScheme().getIdScheme(),
                    dv.getDataElement() ) );

            collectNotes( map, event.getNotes() );

        } );
    }

    private void collectRelationships(
        Map<Class<?>, Set<String>> map, List<Relationship> relationships )
    {
        relationships.parallelStream().forEach( relationship -> {

            RelationshipKey relationshipKey = RelationshipPreheatKeySupport.getRelationshipKey( relationship );

            addIdentifier( map, Relationship.class, TrackerIdScheme.UID, relationshipKey.asString() );
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

    private <T> void addIdentifier( Map<Class<?>, Set<String>> map,
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
