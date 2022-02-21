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
package org.hisp.dhis.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.*;
import java.util.stream.Collectors;

import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeStore;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ValueTypeValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Abyot Asalefew
 */
@Service( "org.hisp.dhis.trackedentity.TrackedEntityAttributeService" )
public class DefaultTrackedEntityAttributeService
    implements TrackedEntityAttributeService
{
    private static final int VALUE_MAX_LENGTH = 50000;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TrackedEntityAttributeStore attributeStore;

    private final ProgramService programService;

    private final TrackedEntityTypeService trackedEntityTypeService;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final TrackedEntityAttributeStore trackedEntityAttributeStore;

    private final TrackedEntityTypeAttributeStore entityTypeAttributeStore;

    private final ProgramTrackedEntityAttributeStore programAttributeStore;

    private final ValueTypeValidationUtils valueTypeValidationUtils;

    public DefaultTrackedEntityAttributeService( TrackedEntityAttributeStore attributeStore,
        ProgramService programService, TrackedEntityTypeService trackedEntityTypeService,
        CurrentUserService currentUserService,
        AclService aclService, TrackedEntityAttributeStore trackedEntityAttributeStore,
        TrackedEntityTypeAttributeStore entityTypeAttributeStore,
        ProgramTrackedEntityAttributeStore programAttributeStore,
        ValueTypeValidationUtils valueTypeValidationUtils )
    {
        checkNotNull( attributeStore );
        checkNotNull( programService );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( currentUserService );
        checkNotNull( aclService );
        checkNotNull( trackedEntityAttributeStore );
        checkNotNull( entityTypeAttributeStore );
        checkNotNull( programAttributeStore );
        checkNotNull( valueTypeValidationUtils );

        this.attributeStore = attributeStore;
        this.programService = programService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.currentUserService = currentUserService;
        this.aclService = aclService;
        this.trackedEntityAttributeStore = trackedEntityAttributeStore;
        this.entityTypeAttributeStore = entityTypeAttributeStore;
        this.programAttributeStore = programAttributeStore;
        this.valueTypeValidationUtils = valueTypeValidationUtils;

    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void deleteTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        attributeStore.delete( attribute );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getAllTrackedEntityAttributes()
    {
        return attributeStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityAttribute getTrackedEntityAttribute( long id )
    {
        return attributeStore.get( id );
    }

    @Override
    @Transactional
    public long addTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        attributeStore.save( attribute );
        return attribute.getId();
    }

    @Override
    @Transactional
    public void updateTrackedEntityAttribute( TrackedEntityAttribute attribute )
    {
        attributeStore.update( attribute );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityAttribute getTrackedEntityAttributeByName( String name )
    {
        return attributeStore.getByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public TrackedEntityAttribute getTrackedEntityAttribute( String uid )
    {
        return attributeStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getTrackedEntityAttributes( List<String> uids )
    {
        return attributeStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getTrackedEntityAttributesByDisplayOnVisitSchedule(
        boolean displayOnVisitSchedule )
    {
        return attributeStore.getByDisplayOnVisitSchedule( displayOnVisitSchedule );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getTrackedEntityAttributesDisplayInListNoProgram()
    {
        return attributeStore.getDisplayInListNoProgram();
    }

    @Override
    @Transactional( readOnly = true )
    public String validateAttributeUniquenessWithinScope( TrackedEntityAttribute trackedEntityAttribute,
        String value, TrackedEntityInstance trackedEntityInstance, OrganisationUnit organisationUnit )
    {
        Assert.notNull( trackedEntityAttribute, "tracked entity attribute is required." );
        Assert.notNull( value, "tracked entity attribute value is required." );

        TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
        params.addAttribute(
            new QueryItem( trackedEntityAttribute, QueryOperator.EQ, value, trackedEntityAttribute.getValueType(),
                trackedEntityAttribute.getAggregationType(), trackedEntityAttribute.getOptionSet() ) );

        if ( trackedEntityAttribute.getOrgUnitScopeNullSafe() )
        {
            Assert.notNull( organisationUnit, "organisation unit is required for org unit scope" );
            params.addOrganisationUnit( organisationUnit );
        }

        Optional<String> fetchedTeiUid = trackedEntityAttributeStore
            .getTrackedEntityInstanceUidWithUniqueAttributeValue( params );

        if ( fetchedTeiUid.isPresent()
            && (trackedEntityInstance == null || !fetchedTeiUid.get().equals( trackedEntityInstance.getUid() )) )
        {
            return "Non-unique attribute value '" + value + "' for attribute " + trackedEntityAttribute.getUid();
        }

        return null;
    }

    @Override
    @Transactional( readOnly = true )
    public String validateValueType( TrackedEntityAttribute trackedEntityAttribute, String value )
    {
        Assert.notNull( trackedEntityAttribute, "tracked entity attribute is required" );

        if ( value.length() > VALUE_MAX_LENGTH )
        {
            return "Value length is greater than 50000 chars for attribute " + trackedEntityAttribute.getUid();
        }

        ErrorMessage errorMessage = valueTypeValidationUtils.dataValueIsValid( trackedEntityAttribute, value );

        return errorMessage == null ? null : errorMessage.getMessage();
    }

    @Override
    @Transactional( readOnly = true )
    public Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes()
    {
        return getAllUserReadableTrackedEntityAttributes( currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional( readOnly = true )
    public Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes( User user )
    {
        List<Program> programs = programService.getAllPrograms();
        List<TrackedEntityType> trackedEntityTypes = trackedEntityTypeService.getAllTrackedEntityType();

        return getAllUserReadableTrackedEntityAttributes( user, programs, trackedEntityTypes );
    }

    @Override
    @Transactional( readOnly = true )
    public Set<TrackedEntityAttribute> getAllUserReadableTrackedEntityAttributes( User user, List<Program> programs,
        List<TrackedEntityType> trackedEntityTypes )
    {
        Set<TrackedEntityAttribute> attributes = new HashSet<>();

        if ( programs != null && !programs.isEmpty() )
        {
            attributes.addAll( programAttributeStore.getAttributes(
                programs.stream().filter( program -> aclService.canDataRead( user, program ) )
                    .collect( Collectors.toList() ) ) );
        }

        if ( trackedEntityTypes != null && !trackedEntityTypes.isEmpty() )
        {
            attributes.addAll( entityTypeAttributeStore.getAttributes(
                trackedEntityTypes.stream()
                    .filter( trackedEntityType -> aclService.canDataRead( user, trackedEntityType ) ).collect(
                        Collectors.toList() ) ) );
        }

        return attributes;
    }

    @Override
    public ProgramTrackedEntityAttribute getProgramTrackedEntityAttribute( Program program,
        TrackedEntityAttribute trackedEntityAttribute )
    {
        return programAttributeStore.get( program, trackedEntityAttribute );
    }

    @Override
    @Transactional( readOnly = true )
    public Set<TrackedEntityAttribute> getAllTrigramIndexableTrackedEntityAttributes()
    {
        return attributeStore.getAllSearchableAndUniqueTrackedEntityAttributes();
    }

    // -------------------------------------------------------------------------
    // ProgramTrackedEntityAttribute
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getAllSystemWideUniqueTrackedEntityAttributes()
    {
        return getAllTrackedEntityAttributes().stream().filter( TrackedEntityAttribute::isSystemWideUnique )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public List<TrackedEntityAttribute> getAllUniqueTrackedEntityAttributes()
    {
        return getAllTrackedEntityAttributes().stream().filter( TrackedEntityAttribute::isUnique )
            .collect( Collectors.toList() );
    }

    @Override
    @Transactional( readOnly = true )
    public Set<TrackedEntityAttribute> getTrackedEntityAttributesByTrackedEntityTypes()
    {
        return this.trackedEntityAttributeStore.getTrackedEntityAttributesByTrackedEntityTypes();
    }

    @Override
    @Transactional( readOnly = true )
    public Map<Program, Set<TrackedEntityAttribute>> getTrackedEntityAttributesByProgram()
    {
        return this.trackedEntityAttributeStore.getTrackedEntityAttributesByProgram();
    }
}
