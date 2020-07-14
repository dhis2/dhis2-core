package org.hisp.dhis.tracker.validation;

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
 *
 */

import com.google.common.base.Preconditions;
import lombok.Data;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Data
public class TrackerImportValidationContext
{

    private final Map<Class<? extends TrackerDto>, Map<String, TrackerImportStrategy>> resolvedStrategyMap = new HashMap<>();

    private Map<String, CategoryOptionCombo> eventCocCacheMap = new HashMap<>();

    private Map<String, String> cachedEventAOCProgramCC = new HashMap<>();

    private TrackerBundle bundle;

    public TrackerImportValidationContext( TrackerBundle bundle )
    {
        this.bundle = bundle;

        Map<Class<? extends TrackerDto>, Map<String, TrackerImportStrategy>> resolvedMap = this
            .getResolvedStrategyMap();

        resolvedMap.put( Event.class, new HashMap<>() );
        resolvedMap.put( Enrollment.class, new HashMap<>() );
        resolvedMap.put( TrackedEntity.class, new HashMap<>() );
    }

    public TrackerImportStrategy getStrategy( Enrollment enrollment )
    {
        return getResolvedStrategyMap().get( Enrollment.class ).get( enrollment.getEnrollment() );
    }

    public TrackerImportStrategy setStrategy( Enrollment enrollment, TrackerImportStrategy strategy )
    {
        return getResolvedStrategyMap().get( Enrollment.class ).put( enrollment.getEnrollment(), strategy );
    }

    public TrackerImportStrategy getStrategy( Event event )
    {
        return getResolvedStrategyMap().get( Event.class ).get( event.getEvent() );
    }

    public TrackerImportStrategy setStrategy( Event event, TrackerImportStrategy strategy )
    {
        return getResolvedStrategyMap().get( Event.class ).put( event.getEvent(), strategy );
    }

    public TrackerImportStrategy getStrategy( TrackedEntity tei )
    {
        return getResolvedStrategyMap().get( TrackedEntity.class ).get( tei.getTrackedEntity() );
    }

    public TrackerImportStrategy setStrategy( TrackedEntity trackedEntity, TrackerImportStrategy strategy )
    {
        return getResolvedStrategyMap().get( TrackedEntity.class ).put( trackedEntity.getTrackedEntity(), strategy );
    }

    public void cacheEventCategoryOptionCombo( String key, CategoryOptionCombo categoryOptionCombo )
    {
        Preconditions.checkArgument( !StringUtils.isEmpty( key ),
            "Event Category Option Combo cache key 'event uid', can't be null or empty" );

        Preconditions.checkNotNull( categoryOptionCombo, "Event Category Option Combo can't be null or empty" );

        if ( !eventCocCacheMap.containsKey( key ) )
        {
            eventCocCacheMap.put( key, categoryOptionCombo );
        }
    }

    public CategoryOptionCombo getCachedEventCategoryOptionCombo( String key )
    {
        return eventCocCacheMap.get( key );
    }

    public void putCachedEventAOCProgramCC( String cacheKey, String value )
    {
        cachedEventAOCProgramCC.put( cacheKey, value );
    }

    public Optional<String> getCachedEventAOCProgramCC( String cacheKey )
    {
        String cached = cachedEventAOCProgramCC.get( cacheKey );
        if ( cached == null )
        {
            return Optional.empty();
        }
        return Optional.of( cached );
    }

    public OrganisationUnit getOrganisationUnit( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), OrganisationUnit.class, id );
    }

    public TrackedEntityInstance getTrackedEntityInstance( String id )
    {
        return bundle.getPreheat().getTrackedEntity( bundle.getIdentifier(), id );
    }

    public TrackedEntityAttribute getTrackedEntityAttribute( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), TrackedEntityAttribute.class, id );
    }

    public TrackedEntityType getTrackedEntityType( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), TrackedEntityType.class, id );
    }

    public Program getProgram( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), Program.class, id );
    }

    public ProgramInstance getProgramInstance( String id )
    {
        return bundle.getPreheat().getEnrollment( bundle.getIdentifier(), id );
    }

    public ProgramStage getProgramStage( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), ProgramStage.class, id );
    }

    public ProgramStageInstance getProgramStageInstance( String event )
    {
        return bundle.getPreheat().getEvent( bundle.getIdentifier(), event );
    }

    public CategoryOptionCombo getCategoryOptionCombo( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), CategoryOptionCombo.class, id );
    }

    public CategoryOption getCategoryOption( String id )
    {
        return bundle.getPreheat().get( bundle.getIdentifier(), CategoryOption.class, id );
    }

    public Map<String, List<ProgramInstance>> getEventToProgramInstancesMap()
    {
        return bundle.getPreheat().getProgramInstances();
    }
}
