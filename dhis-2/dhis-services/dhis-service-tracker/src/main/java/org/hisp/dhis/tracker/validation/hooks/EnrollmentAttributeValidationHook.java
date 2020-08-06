package org.hisp.dhis.tracker.validation.hooks;

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

import com.google.common.collect.Maps;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_VALUE_MAP_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_INSTANCE_CANT_BE_NULL;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EnrollmentAttributeValidationHook
    extends AbstractTrackerDtoValidationHook
{
    public EnrollmentAttributeValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Enrollment.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        Program program = context.getProgram( enrollment.getProgram() );
        TrackedEntityInstance tei = context.getTrackedEntityInstance( enrollment.getTrackedEntity() );

        Map<String, String> attributeValueMap = Maps.newHashMap();

        for ( Attribute attribute : enrollment.getAttributes() )
        {
            validateRequiredProperties( reporter, attribute );

            if ( attribute.getAttribute() == null || attribute.getValue() == null ||
                context.getTrackedEntityAttribute( attribute.getAttribute() ) == null )
            {
                continue;
            }

            attributeValueMap.put( attribute.getAttribute(), attribute.getValue() );

            TrackedEntityAttribute teAttribute = context.getTrackedEntityAttribute( attribute.getAttribute() );

            validateAttrValueType( reporter, attribute, teAttribute );

            // TODO: This is ripe for optimization
            validateAttributeUniqueness( reporter,
                attribute.getValue(),
                teAttribute,
                tei,
                tei.getOrganisationUnit() );
        }

        validateMandatoryAttributes( reporter, program, tei, attributeValueMap );
    }

    protected void validateRequiredProperties( ValidationErrorReporter reporter, Attribute attribute )
    {
        if ( attribute.getAttribute() == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1075 )
                .addArg( attribute ) );
        }

        if ( attribute.getValue() == null )
        {
            reporter.addError( newReport( TrackerErrorCode.E1076 )
                .addArg( attribute ) );
        }

        if ( attribute.getAttribute() != null )
        {
            TrackedEntityAttribute teAttribute = reporter.getValidationContext()
                .getTrackedEntityAttribute( attribute.getAttribute() );
            if ( teAttribute == null )
            {
                reporter.addError( newReport( TrackerErrorCode.E1017 )
                    .addArg( attribute.getAttribute() ) );
            }
        }
    }

    private void validateMandatoryAttributes( ValidationErrorReporter reporter,
        Program program, TrackedEntityInstance trackedEntityInstance, Map<String, String> attributeValueMap )
    {
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );
        checkNotNull( trackedEntityInstance, TRACKED_ENTITY_INSTANCE_CANT_BE_NULL );
        checkNotNull( attributeValueMap, ATTRIBUTE_VALUE_MAP_CANT_BE_NULL );

        // 1. Get all tei attributes, map attrValue attr. into set of attr.
        Set<TrackedEntityAttribute> trackedEntityAttributes = trackedEntityInstance.getTrackedEntityAttributeValues()
            .stream()
            .map( TrackedEntityAttributeValue::getAttribute )
            .collect( Collectors.toSet() );

        // 2. Map all program attr. that match tei attr. into map. of attr:is mandatory
        Map<TrackedEntityAttribute, Boolean> mandatoryMap = program.getProgramAttributes().stream()
            .filter( v -> trackedEntityAttributes.contains( v.getAttribute() ) )
            .collect( Collectors.toMap(
                ProgramTrackedEntityAttribute::getAttribute,
                ProgramTrackedEntityAttribute::isMandatory ) );

        for ( Map.Entry<TrackedEntityAttribute, Boolean> entry : mandatoryMap.entrySet() )
        {
            TrackedEntityAttribute attribute = entry.getKey();
            Boolean attributeIsMandatory = entry.getValue();

            // TODO: This is quite ugly and should be considered to be solved differently,
            //  e.i. authorization should be handled in one common place.
            // NB: ! This authority MUST only be used in SYNC mode! This needs to be added to the check
            boolean userIsAuthorizedToIgnoreRequiredValueValidation = !reporter.getValidationContext().getBundle()
                .getUser()
                .isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );

            boolean hasMissingAttribute = attributeIsMandatory
                && !userIsAuthorizedToIgnoreRequiredValueValidation
                && !attributeValueMap.containsKey( attribute.getUid() );

            if ( hasMissingAttribute )
            {
                reporter.addError( newReport( TrackerErrorCode.E1018 ).addArg( attribute ) );
            }

            // Remove program attr. from enrollment attr. list
            attributeValueMap.remove( attribute.getUid() );
        }

        if ( !attributeValueMap.isEmpty() )
        {
            for ( Map.Entry<String, String> entry : attributeValueMap.entrySet() )
            {
                //Only Program attributes is allowed for enrollment
                reporter.addError( newReport( TrackerErrorCode.E1019 )
                    .addArg( entry.getKey() + "=" + entry.getValue() ) );
            }
        }
    }
}