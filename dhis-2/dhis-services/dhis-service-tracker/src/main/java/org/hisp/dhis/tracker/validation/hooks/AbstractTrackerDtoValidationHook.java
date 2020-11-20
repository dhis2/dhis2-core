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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newWarningReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.DATE_STRING_CANT_BE_NULL;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.util.DateUtils;
import org.springframework.core.Ordered;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerDtoValidationHook
    implements TrackerValidationHook
{
    private int order = Ordered.LOWEST_PRECEDENCE;

    @Override
    public int getOrder()
    {
        return order;
    }

    @Override
    public void setOrder( int order )
    {
        this.order = order;
    }

    private final TrackerImportStrategy strategy;

    /**
     * This constructor is used by the PreCheck* hooks
     */
    public AbstractTrackerDtoValidationHook()
    {
        this.strategy = null;
    }

    public AbstractTrackerDtoValidationHook( TrackerImportStrategy strategy )
    {
        checkNotNull( strategy );
        this.strategy = strategy;
    }

    /**
     * Template method Must be implemented if dtoTypeClass == Event or dtoTypeClass
     * == null
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

    private ValidationErrorReporter validateTrackedEntity( Map<TrackerType, List<String>> invalidDtos,
        TrackerImportValidationContext context, TrackedEntity tei )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, tei );
        reporter.getInvalidDTOs().putAll( invalidDtos );
        validateTrackedEntity( reporter, tei );
        return reporter;
    }

    private ValidationErrorReporter validateEnrollment( Map<TrackerType, List<String>> invalidDtos,
        TrackerImportValidationContext context, Enrollment enrollment )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, enrollment );
        reporter.getInvalidDTOs().putAll( invalidDtos );
        validateEnrollment( reporter, enrollment );
        return reporter;
    }

    private ValidationErrorReporter validateEvent( Map<TrackerType, List<String>> invalidDtos,
        TrackerImportValidationContext context, Event event )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, event );
        reporter.getInvalidDTOs().putAll( invalidDtos );
        validateEvent( reporter, event );
        return reporter;
    }

    private ValidationErrorReporter validateRelationship( Map<TrackerType, List<String>> invalidDtos,
        TrackerImportValidationContext context, Relationship relationship )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, relationship );
        reporter.getInvalidDTOs().putAll( invalidDtos );
        validateRelationship( reporter, relationship );
        return reporter;
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

        ValidationErrorReporter rootReporter = ValidationErrorReporter.emptyReporter();

        // If this hook impl. has no strategy set, i.e. (strategy == null)
        // it implies it is for all strategies; create/update/delete
        if ( this.strategy != null )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();
            // If there is a strategy set and it is not delete and the importing strategy is
            // delete,
            // just return as there is nothing to validate.
            if ( importStrategy.isDelete() && !this.strategy.isDelete() )
            {
                return rootReporter;
            }
        }

        /*
         * Validate the bundle, by passing each Tracker entities collection to the
         * validation hooks. If a validation hook reports errors and has
         * 'removeOnError=true' the Tracker entity under validation will be removed from
         * the bundle.
         */

        validateTrackedEntities( bundle, rootReporter, context );
        validateEnrollments( bundle, rootReporter, context );
        validateEvents( bundle, rootReporter, context );
        validateRelationships( bundle, rootReporter, context );

        return rootReporter;
    }

    private void validateTrackedEntities( TrackerBundle bundle, ValidationErrorReporter rootReporter,
        TrackerImportValidationContext context )
    {
        Iterator<TrackedEntity> iter = bundle.getTrackedEntities().iterator();
        while ( iter.hasNext() )
        {
            TrackedEntity tei = iter.next();
            rootReporter.merge( validateTrackedEntity( rootReporter.getInvalidDTOs(), context, tei ) );
            if ( removeOnError() && rootReporter.isInvalid( TrackerType.TRACKED_ENTITY, tei.getTrackedEntity() ) )
            {
                iter.remove();
            }
        }
    }

    private void validateEnrollments( TrackerBundle bundle, ValidationErrorReporter rootReporter,
        TrackerImportValidationContext context )
    {
        Iterator<Enrollment> iterPs = bundle.getEnrollments().iterator();
        while ( iterPs.hasNext() )
        {
            Enrollment ps = iterPs.next();
            rootReporter.merge( validateEnrollment( rootReporter.getInvalidDTOs(), context, ps ) );
            if ( removeOnError() && rootReporter.isInvalid( TrackerType.ENROLLMENT, ps.getEnrollment() ) )
            {
                iterPs.remove();
            }
        }
    }

    private void validateEvents( TrackerBundle bundle, ValidationErrorReporter rootReporter,
        TrackerImportValidationContext context )
    {
        Iterator<Event> iterPsi = bundle.getEvents().iterator();
        while ( iterPsi.hasNext() )
        {
            Event psi = iterPsi.next();
            rootReporter.merge( validateEvent( rootReporter.getInvalidDTOs(), context, psi ) );
            if ( removeOnError() && rootReporter.isInvalid( TrackerType.EVENT, psi.getEvent() ) )
            {
                iterPsi.remove();
            }
        }
    }

    private void validateRelationships( TrackerBundle bundle, ValidationErrorReporter rootReporter,
        TrackerImportValidationContext context )
    {
        Iterator<Relationship> iterRel = bundle.getRelationships().iterator();
        while ( iterRel.hasNext() )
        {
            Relationship rel = iterRel.next();
            rootReporter.merge( validateRelationship( rootReporter.getInvalidDTOs(), context, rel ) );
            if ( removeOnError() && rootReporter.isInvalid( TrackerType.RELATIONSHIP, rel.getRelationship() ) )
            {
                iterRel.remove();
            }
        }
    }

    public boolean isNotValidDateString( String dateString )
    {
        checkNotNull( dateString, DATE_STRING_CANT_BE_NULL );

        return !DateUtils.dateIsValid( dateString );
    }

    public boolean isValidDateStringAndNotNull( String dateString )
    {
        return dateString != null && DateUtils.dateIsValid( dateString );
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

    public boolean removeOnError()
    {
        return false;
    }
}
