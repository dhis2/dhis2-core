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
package org.hisp.dhis.tracker.bundle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@AllArgsConstructor
public class TrackerBundle
{
    /**
     * User to use for import job.
     */
    private User user;

    /**
     * Should import be imported or just validated.
     */
    @Builder.Default
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * Sets import strategy (create, update, etc).
     */
    @Builder.Default
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

    /**
     * Should text pattern validation be skipped or not, default is not.
     */
    @JsonProperty
    private boolean skipTextPatternValidation;

    /**
     * Should side effects be skipped or not, default is not.
     */
    @JsonProperty
    private boolean skipSideEffects;

    /**
     * Should rule engine call be skipped or not, default is to skip.
     */
    @JsonProperty
    private boolean skipRuleEngine;

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    @Builder.Default
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    @Builder.Default
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    @Builder.Default
    private ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Preheat bundle for all attached objects (or null if preheater not run
     * yet).
     */
    private TrackerPreheat preheat;

    /**
     * Tracked entities to import.
     */
    @Builder.Default
    private List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    @Builder.Default
    private List<Event> events = new ArrayList<>();

    /**
     * Relationships to import.
     */
    @Builder.Default
    private List<Relationship> relationships = new ArrayList<>();

    /**
     * Rule effects for Enrollments.
     */
    @Builder.Default
    private List<RuleEffects> ruleEffects = new ArrayList<>();

    /**
     * Rule effects for Enrollments.
     */
    @Builder.Default
    private Map<String, List<RuleEffect>> enrollmentRuleEffects = new HashMap<>();

    /**
     * Rule effects for Events.
     */
    @Builder.Default
    private Map<String, List<RuleEffect>> eventRuleEffects = new HashMap<>();

    @Builder.Default
    private Map<TrackerType, Map<String, TrackerImportStrategy>> resolvedStrategyMap = initStrategyMap();

    private static Map<TrackerType, Map<String, TrackerImportStrategy>> initStrategyMap()
    {
        Map<TrackerType, Map<String, TrackerImportStrategy>> resolvedStrategyMap = new EnumMap<>( TrackerType.class );

        resolvedStrategyMap.put( TrackerType.RELATIONSHIP, new HashMap<>() );
        resolvedStrategyMap.put( TrackerType.EVENT, new HashMap<>() );
        resolvedStrategyMap.put( TrackerType.ENROLLMENT, new HashMap<>() );
        resolvedStrategyMap.put( TrackerType.TRACKED_ENTITY, new HashMap<>() );

        return resolvedStrategyMap;
    }

    @JsonProperty
    public String getUsername()
    {
        return User.username( user );
    }

    @Builder.Default
    @JsonIgnore
    private Set<String> updatedTeis = new HashSet<>();

    public Optional<TrackedEntity> getTrackedEntity( String id )
    {
        return this.trackedEntities.stream().filter( t -> t.getTrackedEntity().equals( id ) ).findFirst();
    }

    public Optional<Event> getEvent( String id )
    {
        return this.events.stream().filter( t -> t.getEvent().equals( id ) ).findFirst();
    }

    public Optional<Enrollment> getEnrollment( String id )
    {
        return this.enrollments.stream().filter( t -> t.getEnrollment().equals( id ) ).findFirst();
    }

    public Map<String, List<RuleEffect>> getEnrollmentRuleEffects()
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEnrollment )
            .filter( e -> getEnrollment( e.getTrackerObjectUid() ).isPresent() )
            .collect( Collectors.toMap( RuleEffects::getTrackerObjectUid, RuleEffects::getRuleEffects ) );
    }

    public Map<String, List<RuleEffect>> getEventRuleEffects()
    {
        return ruleEffects.stream()
            .filter( RuleEffects::isEvent )
            .filter( e -> getEvent( e.getTrackerObjectUid() ).isPresent() )
            .collect( Collectors.toMap( RuleEffects::getTrackerObjectUid, RuleEffects::getRuleEffects ) );
    }

    public TrackerImportStrategy setStrategy( TrackerDto dto, TrackerImportStrategy strategy )
    {
        return this.getResolvedStrategyMap().get( dto.getTrackerType() ).put( dto.getUid(), strategy );
    }

    public TrackerImportStrategy getStrategy( TrackerDto dto )
    {
        return getResolvedStrategyMap().get( dto.getTrackerType() ).get( dto.getUid() );
    }

    public TrackedEntityInstance getTrackedEntityInstance( String id )
    {
        return getPreheat().getTrackedEntity( id );
    }

    public ProgramInstance getProgramInstance( String id )
    {
        return getPreheat().getEnrollment( id );
    }

    public ProgramStageInstance getProgramStageInstance( String event )
    {
        return getPreheat().getEvent( event );
    }

    public org.hisp.dhis.relationship.Relationship getRelationship( String relationship )
    {
        return getPreheat().getRelationship( relationship );
    }
}
