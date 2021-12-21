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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1125;
import static org.hisp.dhis.tracker.report.TrackerErrorReport.newReport;
import static org.hisp.dhis.tracker.report.TrackerWarningReport.newWarningReport;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;

import com.google.common.collect.ImmutableMap;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerDtoValidationHook
    implements TrackerValidationHook
{
    private final Map<TrackerType, BiConsumer<ValidationErrorReporter, TrackerDto>> validationMap = ImmutableMap
        .<TrackerType, BiConsumer<ValidationErrorReporter, TrackerDto>> builder()
        .put( TrackerType.TRACKED_ENTITY, (( report, dto ) -> validateTrackedEntity( report, (TrackedEntity) dto )) )
        .put( TrackerType.ENROLLMENT, (( report, dto ) -> validateEnrollment( report, (Enrollment) dto )) )
        .put( TrackerType.EVENT, (( report, dto ) -> validateEvent( report, (Event) dto )) )
        .put( TrackerType.RELATIONSHIP, (( report, dto ) -> validateRelationship( report, (Relationship) dto )) )
        .build();

    /**
     * This constructor is used by the PreCheck* hooks
     */
    public AbstractTrackerDtoValidationHook()
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Event or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param event entity to validate
     */
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Enrollment or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param enrollment entity to validate
     */
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Relationship or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param relationship entity to validate
     */
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
    }

    /**
     * Template method Must be implemented if dtoTypeClass == TrackedEntity or
     * dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param tei entity to validate
     */
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
    }

    protected <T extends ValueTypedDimensionalItemObject> void validateOptionSet( ValidationErrorReporter reporter,
        T optionalObject, String value )
    {
        Optional.ofNullable( optionalObject.getOptionSet() )
            .ifPresent( optionSet -> addErrorIf( () -> optionSet.getOptions().stream().filter( Objects::nonNull )
                .noneMatch( o -> o.getCode().equalsIgnoreCase( value ) ), reporter, E1125, value,
                optionalObject.getUid(), optionalObject.getClass().getSimpleName(),
                optionalObject.getOptionSet().getOptions().stream().filter( Objects::nonNull ).map( Option::getCode )
                    .collect( Collectors.joining( "," ) ) ) );
    }

    /**
     * Delegating validate method, this delegates validation to the different
     * implementing hooks.
     *
     * @param context validation context
     * @return list of error reports
     */
    @Override
    public ValidationErrorReporter validate( TrackerImportValidationContext context )
    {
        TrackerBundle bundle = context.getBundle();

        /*
         * Validate the bundle, by passing each Tracker entities collection to
         * the validation hooks. If a validation hook reports errors and has
         * 'removeOnError=true' the Tracker entity under validation will be
         * removed from the bundle.
         */

        validateTrackerDtos( context, bundle.getTrackedEntities() );
        validateTrackerDtos( context, bundle.getEnrollments() );
        validateTrackerDtos( context, bundle.getEvents() );
        validateTrackerDtos( context, bundle.getRelationships() );

        return context.getRootReporter();
    }

    private void validateTrackerDtos( TrackerImportValidationContext context, List<? extends TrackerDto> dtos )
    {
        Iterator<? extends TrackerDto> iter = dtos.iterator();
        while ( iter.hasNext() )
        {
            TrackerDto dto = iter.next();
            if ( needsToRun( context.getStrategy( dto ) ) )
            {
                final ValidationErrorReporter reporter = validateTrackerDto( context, dto );
                context.getRootReporter().merge( reporter );
                if ( removeOnError() && didNotPassValidation( reporter, dto.getUid() ) )
                {
                    iter.remove();
                }
            }
        }
    }

    private ValidationErrorReporter validateTrackerDto(
        TrackerImportValidationContext context, TrackerDto dto )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, dto, dto.getTrackerType() );
        reporter.getInvalidDTOs().putAll( context.getRootReporter().getInvalidDTOs() );
        validationMap.get( dto.getTrackerType() ).accept( reporter, dto );
        return reporter;
    }

    protected void addError( ValidationErrorReporter report, TrackerErrorCode errorCode, Object... args )
    {
        report.addError( newReport( errorCode ).addArgs( args ) );
    }

    protected void addWarning( ValidationErrorReporter report, TrackerErrorCode errorCode, Object... args )
    {
        report.addWarning( newWarningReport( errorCode ).addArgs( args ) );
    }

    protected void addErrorIf( Supplier<Boolean> expression, ValidationErrorReporter report, TrackerErrorCode errorCode,
        Object... args )
    {
        if ( expression.get() )
        {
            addError( report, errorCode, args );
        }
    }

    protected void addErrorIfNull( Object object, ValidationErrorReporter report, TrackerErrorCode errorCode,
        Object... args )
    {
        if ( object == null )
        {
            addError( report, errorCode, args );
        }
    }

    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return strategy != TrackerImportStrategy.DELETE;
    }

    /**
     * Signal the implementing Validator hook that, upon validation error, the
     * Tracker entity under validation must be removed from the payload.
     *
     */
    public boolean removeOnError()
    {
        return false;
    }

    private boolean didNotPassValidation( ValidationErrorReporter reporter, String uid )
    {
        return reporter.getReportList().stream().anyMatch( r -> r.getUid().equals( uid ) );
    }
}
