package org.hisp.dhis.tracker.converter;

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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vividsolutions.jts.geom.Geometry;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service
public class EnrollmentTrackerConverterService
    implements TrackerConverterService<Enrollment, ProgramInstance>, PatchConverterService<Enrollment, ProgramInstance>
{
    private final NotesConverterService notesConverterService;

    @Override
    public Enrollment to( ProgramInstance programInstance )
    {
        Enrollment enrollment = new Enrollment();
        enrollment.setUid( programInstance.getUid() );
        enrollment.setEnrollment( programInstance.getUid() );
        enrollment.setCreatedAt( DateUtils.getIso8601NoTz( programInstance.getCreatedAtClient() ) );
        enrollment.setUpdatedAt( DateUtils.getIso8601NoTz( programInstance.getLastUpdatedAtClient() ) );
        enrollment.setTrackedEntity( programInstance.getEntityInstance().getUid() );
        enrollment.setProgram( programInstance.getProgram().getUid() );
        enrollment.setStatus( convertStatus( programInstance.getStatus() ) );
        enrollment.setOrgUnit( programInstance.getOrganisationUnit().getUid() );
        enrollment.setEnrolledAt( DateUtils.getIso8601NoTz( programInstance.getEnrollmentDate() ) );
        enrollment.setOccurredAt( DateUtils.getIso8601NoTz( programInstance.getIncidentDate() ) );
        enrollment.setFollowUp( programInstance.getFollowup() );
        enrollment.setCompletedBy( programInstance.getCompletedBy() );
        enrollment.setCompletedAt( DateUtils.getIso8601NoTz( programInstance.getEndDate() ) );
        enrollment.setDeleted( programInstance.isDeleted() );
        enrollment.setStoredBy( programInstance.getStoredBy() );
        enrollment.setGeometry( programInstance.getGeometry() );

        for ( AttributeValue attributeValue : programInstance.getAttributeValues() )
        {
            Attribute attribute = new Attribute();
            attribute.setAttribute( attributeValue.getAttribute().getUid() );
            attribute.setCode( attributeValue.getAttribute().getCode() );
            attribute.setCreatedAt( DateUtils.getIso8601NoTz( attributeValue.getAttribute().getCreated() ) );
            attribute.setUpdatedAt( DateUtils.getIso8601NoTz( attributeValue.getAttribute().getLastUpdated() ) );
            //attribute.setStoredBy( attributeValue.getAttribute().get ); // TODO find it
            attribute.setValueType( attributeValue.getAttribute().getValueType() );
            attribute.setValue( attributeValue.getValue() );
        }

        return enrollment;
    }

    private EnrollmentStatus convertStatus( ProgramStatus status )
    {
        switch ( status )
        {
        case ACTIVE:
            return EnrollmentStatus.ACTIVE;
        case COMPLETED:
            return EnrollmentStatus.COMPLETED;
        case CANCELLED:
            return EnrollmentStatus.CANCELLED;
        }

        return null;
    }

    @Override
    public ProgramInstance from( TrackerPreheat preheat, Enrollment enrollment )
    {
        ProgramInstance programInstance = preheat.getEnrollment( TrackerIdScheme.UID, enrollment.getEnrollment() );
        return from( preheat, enrollment, programInstance );
    }

    @Override
    public ProgramInstance fromForPatch( TrackerPreheat preheat, Enrollment enrollment )
    {
        final ProgramInstance pi = preheat.getEnrollment( TrackerIdScheme.UID, enrollment.getEnrollment() );

        List<Field> patchableFields = ConverterUtils.getPatchFields( Enrollment.class, enrollment );

        for ( Field field : patchableFields )
        {
            // TODO
        }

        return pi;
    }

    @Override
    public Enrollment toForPatch( ProgramInstance programInstance, Enrollment enrollment )
    {
        List<Field> patchableFields = ConverterUtils.getPatchFields( Enrollment.class, enrollment );

        Enrollment convertedEnrollment = to( programInstance );

        for ( Field field : patchableFields )
        {
            // TODO
        }

        return convertedEnrollment;
    }

    @Override
    public ProgramInstance fromForRuleEngine( TrackerPreheat preheat, Enrollment enrollment )
    {
        return from( preheat, enrollment, null );
    }

    private ProgramInstance from( TrackerPreheat preheat, Enrollment enrollment, ProgramInstance programInstance )
    {
        OrganisationUnit organisationUnit = preheat
            .get( OrganisationUnit.class, enrollment.getOrgUnit() );

        checkNotNull( organisationUnit, TrackerImporterAssertErrors.ORGANISATION_UNIT_CANT_BE_NULL );

        Program program = preheat.get( Program.class, enrollment.getProgram() );

        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        TrackedEntityInstance trackedEntityInstance = preheat
            .getTrackedEntity( TrackerIdScheme.UID, enrollment.getTrackedEntity() );

        if ( isNewEntity( programInstance ) )
        {
            Date now = new Date();

            programInstance = new ProgramInstance();
            programInstance.setUid( enrollment.getEnrollment() );
            programInstance.setCreated( now );
            programInstance.setCreatedAtClient( now );
            programInstance.setLastUpdated( now );
            programInstance.setLastUpdatedAtClient( now );

            Date enrollmentDate = DateUtils.parseDate( enrollment.getEnrolledAt() );
            Date incidentDate = DateUtils.parseDate( enrollment.getOccurredAt() );

            programInstance.setEnrollmentDate( enrollmentDate );
            programInstance.setIncidentDate( incidentDate != null ? incidentDate : enrollmentDate );
            programInstance.setOrganisationUnit( organisationUnit );
            programInstance.setProgram( program );
            programInstance.setEntityInstance( trackedEntityInstance );
            programInstance.setFollowup( enrollment.getFollowUp() );
            programInstance.setGeometry( enrollment.getGeometry() );

            if ( enrollment.getStatus() == null )
            {
                enrollment.setStatus( EnrollmentStatus.ACTIVE );
            }

            programInstance.setStatus( enrollment.getStatus().getProgramStatus() );

            if ( isNotEmpty( enrollment.getNotes() ) )
            {
                programInstance.getComments()
                    .addAll( notesConverterService.from( preheat, enrollment.getNotes() ) );
            }
        }
        return programInstance;
    }
}
