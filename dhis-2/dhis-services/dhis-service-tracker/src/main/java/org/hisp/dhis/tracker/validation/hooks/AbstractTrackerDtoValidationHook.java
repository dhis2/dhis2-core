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

import com.google.common.collect.ImmutableMap;
import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.*;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;
import org.hisp.dhis.util.DateUtils;
import org.springframework.core.Ordered;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.ATTRIBUTE_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.DATE_STRING_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.GEOMETRY_CANT_BE_NULL;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD;
import static org.hisp.dhis.tracker.validation.hooks.TrackerImporterAssertErrors.TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL;

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

    public void setOrder( int order )
    {
        this.order = order;
    }

    private final TrackedEntityAttributeService teAttrService;

    private final TrackerImportStrategy strategy;

    /**
     * Indicates that if an object fails a validation it will be removed
     * from the input list and unavailable for more validations.
     */
    private final boolean removeOnError;

    private final Class<?> dtoTypeClass;

    /**
     * This constructor is used by the PreCheck* hooks
     */
    public AbstractTrackerDtoValidationHook( TrackedEntityAttributeService teAttrService )
    {
        checkNotNull( teAttrService );

        this.teAttrService = teAttrService;

        this.removeOnError = true;
        this.dtoTypeClass = null;
        this.strategy = null;
    }

    public <T extends TrackerDto> AbstractTrackerDtoValidationHook( Class<T> dtoClass, TrackerImportStrategy strategy,
        TrackedEntityAttributeService teAttrService )
    {
        checkNotNull( teAttrService );

        checkNotNull( dtoClass );
        checkNotNull( strategy );

        this.teAttrService = teAttrService;
        this.removeOnError = false;
        this.dtoTypeClass = dtoClass;
        this.strategy = strategy;
    }

    /**
     * Must be implemented if dtoTypeClass == Event or dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param event    entity to validate
     */
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    /**
     * Must be implemented if dtoTypeClass == Enrollment or dtoTypeClass == null
     *
     * @param reporter   ValidationErrorReporter instance
     * @param enrollment entity to validate
     */
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    /**
     * Must be implemented if dtoTypeClass == Relationship or dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param relationship entity to validate
     */
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    /**
     * Must be implemented if dtoTypeClass == TrackedEntity or dtoTypeClass == null
     *
     * @param reporter ValidationErrorReporter instance
     * @param tei      entity to validate
     */
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
        throw new IllegalStateException( IMPLEMENTING_CLASS_FAIL_TO_OVERRIDE_THIS_METHOD );
    }

    /**
     * Delegating validate method, this delegates validation to the different implementing hooks.
     *
     * @param context validation context
     * @return list of error reports
     */
    @Override
    public ValidationErrorReporter validate( TrackerImportValidationContext context )
    {
        TrackerBundle bundle = context.getBundle();

        ValidationErrorReporter reporter = new ValidationErrorReporter( context, this.getClass() );

        // If this hook impl. has no strategy set, i.e. (strategy == null)
        // it implies it is for all strategies; create/update/delete
        if ( this.strategy != null )
        {
            TrackerImportStrategy importStrategy = bundle.getImportStrategy();
            // If there is a strategy set and it is not delete and the importing strategy is delete,
            // just return as there is nothing to validate.
            if ( importStrategy.isDelete() && !this.strategy.isDelete() )
            {
                return reporter;
            }
        }

        // @formatter:off
        // Setup all the mapping between validation methods and entity lists and dto classes.
        Map<Class<? extends TrackerDto>,
            Pair<ValidationFunction<TrackerDto>,
                List<? extends TrackerDto>>> allValidations = ImmutableMap.of(
            TrackedEntity.class, Pair.of( ( o, r ) ->
                validateTrackedEntity( r, (TrackedEntity) o ), bundle.getTrackedEntities() ),
            Enrollment.class, Pair.of( ( o, r ) ->
                validateEnrollment( r, (Enrollment) o ), bundle.getEnrollments() ),
            Event.class, Pair.of( ( o, r ) ->
                validateEvent( r, (Event) o ), bundle.getEvents() ),
            Relationship.class, Pair.of( ( o, r ) -> 
                validateRelationship( r, (Relationship) o ), bundle.getRelationships() ) );
        // @formatter:on

        // If no dtoTypeClass is set, we will validate all types of entities in bundle
        // i.e. that impl. hook is meant for all types.
        if ( dtoTypeClass == null )
        {
            allValidations.forEach( ( dtoClass, validationMethod ) ->
                validateTrackerDTOs( reporter, validationMethod ) );
        }
        else
        {
            // If not dtoTypeClass == null, this hook class is run for one specific dto class only
            validateTrackerDTOs( reporter, allValidations.get( dtoTypeClass ) );
        }

        return reporter;
    }

    private void validateTrackerDTOs( ValidationErrorReporter reporter,
        Pair<ValidationFunction<TrackerDto>, List<? extends TrackerDto>> pair )
    {
        Iterator<? extends TrackerDto> iterator = pair.getRight().iterator();

        while ( iterator.hasNext() )
        {
            TrackerDto dto = iterator.next();

            // Fork the report in order to be thread-safe so we can support multi-threaded validation in future.
            // Iterator needs to be changed to split variant also...
            ValidationErrorReporter reportFork = reporter.fork( dto );

            pair.getLeft().validateTrackerDto( dto, reportFork );

            // Remove entity that failed validation from the list, i.e. it
            // will not be validated on next hook since it has already failed and can't be used for next "level" of hooks.
            // This feature is used in the prehooks.
            if ( this.removeOnError && reportFork.hasErrors() )
            {
                iterator.remove();
            }

            reporter.merge( reportFork );
        }
    }

    protected void validateAttrValueType( ValidationErrorReporter errorReporter, Attribute attr,
        TrackedEntityAttribute teAttr )
    {
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( attr, ATTRIBUTE_CANT_BE_NULL );
        checkNotNull( teAttr, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        String error;

        // We need to do try/catch here since validateValueType() since validateValueType can cast IllegalArgumentException e.g.
        // on at org.joda.time.format.DateTimeFormatter.parseDateTime(DateTimeFormatter.java:945)
        try
        {
            error = teAttrService.validateValueType( teAttr, attr.getValue() );
        }
        catch ( Exception e )
        {
            error = e.getMessage();
        }

        if ( error != null )
        {
            ValueType valueType = teAttr.getValueType();
            errorReporter.addError( newReport( TrackerErrorCode.E1007 )
                .addArg( valueType.toString() )
                .addArg( error ) );
        }
    }

    protected void validateAttributeUniqueness( ValidationErrorReporter errorReporter,
        String value,
        TrackedEntityAttribute trackedEntityAttribute,
        TrackedEntityInstance trackedEntityInstanceUid,
        OrganisationUnit organisationUnit )
    {
        checkNotNull( trackedEntityAttribute, TRACKED_ENTITY_ATTRIBUTE_CANT_BE_NULL );

        if ( Boolean.FALSE.equals( trackedEntityAttribute.isUnique() ) )
            return;

        String error = teAttrService.validateAttributeUniquenessWithinScope(
            trackedEntityAttribute,
            value,
            trackedEntityInstanceUid,
            organisationUnit );

        if ( error != null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1064 )
                .addArg( error ) );
        }
    }

    protected void validateGeometry( ValidationErrorReporter errorReporter, Geometry geometry, FeatureType featureType )
    {
        checkNotNull( geometry, GEOMETRY_CANT_BE_NULL );

        if ( featureType == null )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1074 ) );
            return;
        }

        FeatureType typeFromName = FeatureType.getTypeFromName( geometry.getGeometryType() );

        if ( FeatureType.NONE == featureType || featureType != typeFromName )
        {
            errorReporter.addError( newReport( TrackerErrorCode.E1012 )
                .addArg( featureType.name() ) );
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
}
