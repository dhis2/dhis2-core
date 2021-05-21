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
package org.hisp.dhis.tracker.bundle;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleEffects;
import org.hisp.dhis.tracker.*;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder( toBuilder = true )
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
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * What identifiers to match on.
     */
    private TrackerIdScheme identifier = TrackerIdScheme.UID;

    /**
     * Sets import strategy (create, update, etc).
     */
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
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    private ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Preheat bundle for all attached objects (or null if preheater not run
     * yet).
     */
    private TrackerPreheat preheat;

    /**
     * Tracked entities to import.
     */
    private List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    private List<Event> events = new ArrayList<>();

    /**
     * Relationships to import.
     */
    private List<Relationship> relationships = new ArrayList<>();

    /**
     * Rule effects for Enrollments.
     */
    private List<RuleEffects> ruleEffects = new ArrayList<>();

    /**
     * Rule effects for Enrollments.
     */
    private Map<String, List<RuleEffect>> enrollmentRuleEffects = new HashMap<>();

    /**
     * Rule effects for Events.
     */
    private Map<String, List<RuleEffect>> eventRuleEffects = new HashMap<>();

    private final Map<Class<? extends TrackerDto>, Map<String, TrackerImportStrategy>> resolvedStrategyMap;

    public TrackerBundle()
    {
        this.resolvedStrategyMap = new HashMap<>();

        resolvedStrategyMap.put( Event.class, new HashMap<>() );
        resolvedStrategyMap.put( Enrollment.class, new HashMap<>() );
        resolvedStrategyMap.put( TrackedEntity.class, new HashMap<>() );
    }

    @JsonProperty
    public String getUsername()
    {
        return User.username( user );
    }

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

    public TrackerImportStrategy setStrategy( Event event, TrackerImportStrategy strategy )
    {
        return this.getResolvedStrategyMap().get( Event.class ).put( event.getUid(), strategy );
    }

    public TrackerImportStrategy setStrategy( Enrollment enrollment, TrackerImportStrategy strategy )
    {
        return this.getResolvedStrategyMap().get( Enrollment.class ).put( enrollment.getUid(), strategy );
    }

    public TrackerImportStrategy setStrategy( TrackedEntity tei, TrackerImportStrategy strategy )
    {
        return this.getResolvedStrategyMap().get( TrackedEntity.class ).put( tei.getUid(), strategy );
    }
}
