package org.hisp.dhis.tracker.validation.hooks;

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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newWarningReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.DATE_STRING_CANT_BE_NULL;

import java.util.Iterator;
import java.util.function.Supplier;

import org.hisp.dhis.tracker.TrackerImportStrategy;
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

    private ValidationErrorReporter validateTrackedEntity(
        TrackerImportValidationContext context, TrackedEntity tei )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, tei );
        reporter.getInvalidDTOs().putAll( context.getRootReporter().getInvalidDTOs() );
        validateTrackedEntity( reporter, tei );
        return reporter;
    }

    private ValidationErrorReporter validateEnrollment(
        TrackerImportValidationContext context, Enrollment enrollment )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, enrollment );
        reporter.getInvalidDTOs().putAll( context.getRootReporter().getInvalidDTOs() );
        validateEnrollment( reporter, enrollment );
        return reporter;
    }

    private ValidationErrorReporter validateEvent(
        TrackerImportValidationContext context, Event event )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, event );
        reporter.getInvalidDTOs().putAll( context.getRootReporter().getInvalidDTOs() );
        validateEvent( reporter, event );
        return reporter;
    }

    private ValidationErrorReporter validateRelationship(
        TrackerImportValidationContext context, Relationship relationship )
    {
        ValidationErrorReporter reporter = new ValidationErrorReporter( context, relationship );
        reporter.getInvalidDTOs().putAll( context.getRootReporter().getInvalidDTOs() );
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

        ValidationErrorReporter rootReporter = context.getRootReporter();

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

        validateTrackedEntities( bundle, context );
        validateEnrollments( bundle, context );
        validateEvents( bundle, context );
        validateRelationships( bundle, context );

        return rootReporter;
    }

    private void validateTrackedEntities( TrackerBundle bundle,
        TrackerImportValidationContext context )
    {
        Iterator<TrackedEntity> iter = bundle.getTrackedEntities().iterator();
        while ( iter.hasNext() )
        {
            TrackedEntity tei = iter.next();
            final ValidationErrorReporter reporter = validateTrackedEntity( context, tei );
            context.getRootReporter().merge( reporter );
            if ( removeOnError() && didNotPassValidation( reporter, tei.getTrackedEntity() ) )
            {
                iter.remove();
            }
        }
    }

    private void validateEnrollments( TrackerBundle bundle,
        TrackerImportValidationContext context )
    {
        Iterator<Enrollment> iterPs = bundle.getEnrollments().iterator();
        while ( iterPs.hasNext() )
        {
            Enrollment ps = iterPs.next();
            final ValidationErrorReporter reporter = validateEnrollment(context, ps);
            context.getRootReporter().merge( reporter );
            if ( removeOnError() && didNotPassValidation( reporter, ps.getEnrollment() ) )
            {
                iterPs.remove();
            }
        }
    }

    private void validateEvents( TrackerBundle bundle,
        TrackerImportValidationContext context )
    {
        Iterator<Event> iterPsi = bundle.getEvents().iterator();
        while ( iterPsi.hasNext() )
        {
            Event psi = iterPsi.next();
            final ValidationErrorReporter reporter = validateEvent( context, psi );
            context.getRootReporter().merge( reporter );
            if ( removeOnError() && didNotPassValidation( reporter, psi.getEvent() ) )
            {
                iterPsi.remove();
            }
        }
    }

    private void validateRelationships( TrackerBundle bundle,
        TrackerImportValidationContext context )
    {
        Iterator<Relationship> iterRel = bundle.getRelationships().iterator();
        while ( iterRel.hasNext() )
        {
            Relationship rel = iterRel.next();
            final ValidationErrorReporter reporter = validateRelationship( context, rel );
            context.getRootReporter().merge( reporter );
            if ( removeOnError() && didNotPassValidation( reporter, rel.getRelationship() ) )
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
