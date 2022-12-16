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
package org.hisp.dhis.tracker.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.TrackerDto;

/**
 * Collects {@link Error}s, {@link Warning}s and invalid entities the errors are
 * attributed to.
 * <p>
 * Long-term we would want to remove the responsibility of tracking invalid
 * entities from here. This could allow us to merge this class with
 * {@link ValidationResult}.
 * </p>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Value
public class Reporter
{
    List<Error> errors;

    List<Warning> warnings;

    boolean isFailFast;

    TrackerIdSchemeParams idSchemes;

    List<Timing> timings;

    @Getter( AccessLevel.PACKAGE )
    /*
     * Keeps track of all the invalid Tracker objects (i.e. objects with at
     * least one Error in the Reporter) encountered during the validation
     * process.
     */
    EnumMap<TrackerType, Set<String>> invalidDTOs;

    /**
     * Create a {@link Reporter} reporting all errors and warnings with
     * identifiers in given idSchemes. {@link #addError(Error)} will only throw
     * a {@link ValidationFailFastException} if {@code failFast} true is given.
     *
     * @param idSchemes idSchemes in which to report errors and warnings
     * @param failFast reporter throws exception on first error added when true
     */
    public Reporter( TrackerIdSchemeParams idSchemes, boolean failFast )
    {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.invalidDTOs = new EnumMap<>( Map.of(
            TrackerType.TRACKED_ENTITY, new HashSet<>(),
            TrackerType.ENROLLMENT, new HashSet<>(),
            TrackerType.EVENT, new HashSet<>(),
            TrackerType.RELATIONSHIP, new HashSet<>() ) );
        this.idSchemes = idSchemes;
        this.isFailFast = failFast;
        this.timings = new ArrayList<>();
    }

    /**
     * Create a {@link Reporter} reporting all errors and warnings
     * ({@link #isFailFast} = false) with identifiers in given idSchemes.
     * {@link #addError(Error)} will not throw a
     * {@link ValidationFailFastException}.
     *
     * @param idSchemes idSchemes in which to report errors and warnings
     */
    public Reporter( TrackerIdSchemeParams idSchemes )
    {
        this( idSchemes, false );
    }

    public boolean hasErrors()
    {
        return !this.errors.isEmpty();
    }

    public boolean hasErrorReport( Predicate<Error> test )
    {
        return errors.stream().anyMatch( test );
    }

    public void addErrorIf( BooleanSupplier expression, TrackerDto dto, ValidationCode code, Object... args )
    {
        if ( expression.getAsBoolean() )
        {
            addError( dto, code, args );
        }
    }

    public void addErrorIfNull( Object object, TrackerDto dto, ValidationCode code, Object... args )
    {
        if ( object == null )
        {
            addError( dto, code, args );
        }
    }

    public void addError( TrackerDto dto, ValidationCode code, Object... args )
    {
        addError( new Error( MessageFormatter.format( idSchemes, code.getMessage(), args ),
            code, dto.getTrackerType(), dto.getUid() ) );
    }

    public void addError( Error error )
    {
        getErrors().add( error );
        this.invalidDTOs.computeIfAbsent( error.getTrackerType(), k -> new HashSet<>() ).add( error.getUid() );

        if ( isFailFast() )
        {
            throw new ValidationFailFastException( getErrors() );
        }
    }

    public boolean hasWarnings()
    {
        return !this.warnings.isEmpty();
    }

    public boolean hasWarningReport( Predicate<Warning> test )
    {
        return warnings.stream().anyMatch( test );
    }

    public void addWarningIf( BooleanSupplier expression, TrackerDto dto, ValidationCode code, Object... args )
    {
        if ( expression.getAsBoolean() )
        {
            addWarning( dto, code, args );
        }
    }

    public void addWarning( TrackerDto dto, ValidationCode code, Object... args )
    {
        addWarning( new Warning( MessageFormatter.format( idSchemes, code.getMessage(), args ),
            code, dto.getTrackerType(), dto.getUid() ) );
    }

    public void addWarning( Warning warning )
    {
        getWarnings().add( warning );
    }

    /**
     * Checks if a TrackerDto is invalid (i.e. has at least one Error in the
     * Reporter).
     */
    public boolean isInvalid( TrackerDto dto )
    {
        return this.isInvalid( dto.getTrackerType(), dto.getUid() );
    }

    /**
     * Checks if a TrackerDto with given type and uid is invalid (i.e. has at
     * least one Error in the Reporter).
     */
    public boolean isInvalid( TrackerType trackerType, String uid )
    {
        return this.invalidDTOs.getOrDefault( trackerType, new HashSet<>() ).contains( uid );
    }

    public Reporter addTiming( Timing timing )
    {
        timings.add( timing );
        return this;
    }

    public Reporter addTimings( List<Timing> timings )
    {
        this.timings.addAll( timings );
        return this;
    }

    public List<Timing> getTimings()
    {
        return Collections.unmodifiableList( timings );
    }

    public boolean hasTimings()
    {
        return !timings.isEmpty();
    }
}