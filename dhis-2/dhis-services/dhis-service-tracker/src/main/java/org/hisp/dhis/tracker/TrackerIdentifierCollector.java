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
package org.hisp.dhis.tracker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
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
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.Note;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.RelationshipPreheatKeySupport;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableSet;

/**
 * This class "collects" identifiers from all input objects. This resulting map
 * of all identifiers will then be used to "preheat/cache" all the objects
 * needed into memory to speed up the validation process.
 *
 * The metadata identifiers can be of different idSchemes as specified by the
 * user on import.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see org.hisp.dhis.tracker.preheat.DefaultTrackerPreheatService
 */
@Component
@RequiredArgsConstructor
public class TrackerIdentifierCollector
{
    public static final String ID_WILDCARD = "*";

    private final ProgramRuleService programRuleService;

    public Map<Class<?>, Set<String>> collect( TrackerImportParams params )
    {
        final Map<Class<?>, Set<String>> identifiers = new HashMap<>();

        collectTrackedEntities( identifiers, params.getTrackedEntities() );
        collectEnrollments( identifiers, params.getEnrollments() );
        collectEvents( identifiers, params.getEvents() );
        collectRelationships( identifiers, params.getRelationships() );
        // Using "*" signals that all the entities of the given type have to be
        // preloaded in the Preheat
        identifiers.put( TrackedEntityType.class, ImmutableSet.of( ID_WILDCARD ) );
        identifiers.put( RelationshipType.class, ImmutableSet.of( ID_WILDCARD ) );

        collectProgramRulesFields( identifiers );
        return identifiers;
    }

    private void collectProgramRulesFields( Map<Class<?>, Set<String>> map )
    {
        List<ProgramRule> programRules = programRuleService.getProgramRulesLinkedToTeaOrDe();
        Set<String> dataElements = programRules.stream()
            .flatMap( pr -> pr.getProgramRuleActions().stream() )
            .filter( a -> Objects.nonNull( a.getDataElement() ) )
            .map( a -> a.getDataElement().getUid() )
            .collect( Collectors.toSet() );

        dataElements.forEach( de -> addIdentifier( map, DataElement.class, de ) );

        Set<String> attributes = programRules.stream()
            .flatMap( pr -> pr.getProgramRuleActions().stream() )
            .filter( a -> Objects.nonNull( a.getAttribute() ) )
            .map( a -> a.getAttribute().getUid() )
            .collect( Collectors.toSet() );

        attributes.forEach( attribute -> addIdentifier( map, TrackedEntityAttribute.class, attribute ) );
    }

    private void collectTrackedEntities( Map<Class<?>, Set<String>> identifiers, List<TrackedEntity> trackedEntities )
    {
        trackedEntities.forEach( trackedEntity -> {
            addIdentifier( identifiers, TrackedEntity.class, trackedEntity.getTrackedEntity() );
            addIdentifier( identifiers, OrganisationUnit.class, trackedEntity.getOrgUnit() );

            collectEnrollments( identifiers, trackedEntity.getEnrollments() );

            trackedEntity.getAttributes()
                .forEach( attribute -> addIdentifier( identifiers, TrackedEntityAttribute.class,
                    attribute.getAttribute() ) );
        } );
    }

    private void collectEnrollments( Map<Class<?>, Set<String>> identifiers, List<Enrollment> enrollments )
    {
        enrollments.forEach( enrollment -> {
            addIdentifier( identifiers, TrackedEntity.class, enrollment.getTrackedEntity() );
            addIdentifier( identifiers, Enrollment.class, enrollment.getEnrollment() );
            addIdentifier( identifiers, Program.class, enrollment.getProgram() );
            addIdentifier( identifiers, OrganisationUnit.class, enrollment.getOrgUnit() );

            collectNotes( identifiers, enrollment.getNotes() );
            collectEvents( identifiers, enrollment.getEvents() );
            enrollment.getAttributes().forEach( attribute -> addIdentifier( identifiers, TrackedEntityAttribute.class,
                attribute.getAttribute() ) );
        } );
    }

    private void collectNotes( Map<Class<?>, Set<String>> identifiers, List<Note> notes )
    {
        notes.forEach(
            note -> {
                if ( !StringUtils.isEmpty( note.getNote() ) && !StringUtils.isEmpty( note.getValue() ) )
                {
                    addIdentifier( identifiers, TrackedEntityComment.class, note.getNote() );
                }
            } );
    }

    private void collectEvents( Map<Class<?>, Set<String>> identifiers, List<Event> events )
    {
        events.forEach( event -> {
            addIdentifier( identifiers, Enrollment.class, event.getEnrollment() );
            addIdentifier( identifiers, Event.class, event.getEvent() );
            addIdentifier( identifiers, Program.class, event.getProgram() );
            addIdentifier( identifiers, ProgramStage.class, event.getProgramStage() );
            addIdentifier( identifiers, OrganisationUnit.class, event.getOrgUnit() );

            event.getAttributeCategoryOptions()
                .forEach( s -> addIdentifier( identifiers, CategoryOption.class, s ) );

            addIdentifier( identifiers, CategoryOptionCombo.class, event.getAttributeOptionCombo() );

            event.getDataValues()
                .forEach( dv -> addIdentifier( identifiers, DataElement.class, dv.getDataElement() ) );

            collectNotes( identifiers, event.getNotes() );

        } );
    }

    private void collectRelationships( Map<Class<?>, Set<String>> identifiers, List<Relationship> relationships )
    {
        relationships.forEach( relationship -> {

            addIdentifier( identifiers, Relationship.class, relationship.getRelationship() );

            if ( RelationshipPreheatKeySupport.hasRelationshipKey( relationship ) )
            {
                RelationshipKey relationshipKey = RelationshipPreheatKeySupport.getRelationshipKey( relationship );

                addIdentifier( identifiers, TrackedEntity.class, relationshipKey.getFrom().getTrackedEntity() );
                addIdentifier( identifiers, Enrollment.class, relationshipKey.getFrom().getEnrollment() );
                addIdentifier( identifiers, Event.class, relationshipKey.getFrom().getEvent() );

                addIdentifier( identifiers, TrackedEntity.class, relationshipKey.getTo().getTrackedEntity() );
                addIdentifier( identifiers, Enrollment.class, relationshipKey.getTo().getEnrollment() );
                addIdentifier( identifiers, Event.class, relationshipKey.getTo().getEvent() );
            }
        } );
    }

    private <T> void addIdentifier( Map<Class<?>, Set<String>> identifiers, Class<T> klass,
        MetadataIdentifier identifier )
    {
        addIdentifier( identifiers, klass, identifier == null ? null : identifier.getIdentifierOrAttributeValue() );
    }

    private <T> void addIdentifier( Map<Class<?>, Set<String>> identifiers, Class<T> klass, String identifier )
    {
        if ( StringUtils.isEmpty( identifier ) || identifiers == null || klass == null )
        {
            return;
        }

        identifiers
            .computeIfAbsent( klass, k -> new HashSet<>() )
            .add( identifier );
    }
}
