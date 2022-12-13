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
package org.hisp.dhis.tracker.validation.validators;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerWarningReport;
import org.hisp.dhis.tracker.validation.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.Validator;

/**
 * Validator applying a sequence of validators in order until a
 * {@link Validator} fails i.e. adds an error.
 *
 * @param <T> type to validate
 */
@RequiredArgsConstructor
public class Seq<T> implements Validator<T>
{

    private final List<Validator<T>> validators;

    public Seq( Validator<T> v1 )
    {
        this( List.of( v1 ) );
    }

    public Seq( Validator<T> v1, Validator<T> v2 )
    {
        this( List.of( v1, v2 ) );
    }

    public Seq( Validator<T> v1, Validator<T> v2, Validator<T> v3 )
    {
        this( List.of( v1, v2, v3 ) );
    }

    public Seq( Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4 )
    {
        this( List.of( v1, v2, v3, v4 ) );
    }

    // TODO if I do not pass the class the compiler does not have enough
    // information to infer the type. So without it
    // the client code is working with an Object. Casting on the client could
    // also work but that's even more awkward.
    public static <T> Seq<T> seq( Class<T> klass, Validator<T> v1 )
    {
        return new Seq<>( v1 );
    }

    public static <T> Seq<T> seq( Class<T> klass, Validator<T> v1, Validator<T> v2 )
    {
        return new Seq<>( v1, v2 );
    }

    public static <T> Seq<T> seq( Class<T> klass, Validator<T> v1, Validator<T> v2, Validator<T> v3 )
    {
        return new Seq<>( v1, v2, v3 );
    }

    public static <T> Seq<T> seq( Class<T> klass, Validator<T> v1, Validator<T> v2, Validator<T> v3, Validator<T> v4 )
    {
        return new Seq<>( v1, v2, v3, v4 );
    }

    public static <T> Seq<T> seq( Class<T> klass, List<Validator<T>> validators )
    {
        return new Seq<>( validators );
    }

    @Override
    public void validate( ValidationErrorReporter reporter, TrackerBundle bundle, T input )
    {
        Reporter wrappingReporter = new Reporter( reporter );

        for ( Validator<T> validator : validators )
        {
            validator.validate( wrappingReporter, bundle, input );
            if ( wrappingReporter.validationFailed )
            {
                return; // only apply next validator if previous one was successful
            }
        }
    }

    /**
     * Reporter delegates to a {@link ValidationErrorReporter} to capture
     * whether a {@link Validator} added an error. This is needed to know when
     * to stop executing the {@link Validator} sequence.
     */
    private class Reporter extends ValidationErrorReporter
    {

        private final ValidationErrorReporter original;

        private boolean validationFailed = false;

        Reporter( ValidationErrorReporter original )
        {
            super( original.getIdSchemes(), false );
            this.original = original;
        }

        public boolean hasErrors()
        {
            return original.hasErrors();
        }

        public boolean hasErrorReport( Predicate<TrackerErrorReport> test )
        {
            return original.hasErrorReport( test );
        }

        public void addErrorIf( BooleanSupplier expression, TrackerDto dto, TrackerErrorCode code, Object... args )
        {
            validationFailed = true;
            original.addErrorIf( expression, dto, code, args );
        }

        public void addErrorIfNull( Object object, TrackerDto dto, TrackerErrorCode code, Object... args )
        {
            validationFailed = true;
            original.addErrorIfNull( object, dto, code, args );
        }

        public void addError( TrackerDto dto, TrackerErrorCode code, Object... args )
        {
            validationFailed = true;
            original.addError( dto, code, args );
        }

        public void addError( TrackerErrorReport error )
        {
            validationFailed = true;
            original.addError( error );
        }

        public boolean hasWarnings()
        {
            return original.hasWarnings();
        }

        public boolean hasWarningReport( Predicate<TrackerWarningReport> test )
        {
            return original.hasWarningReport( test );
        }

        public void addWarningIf( BooleanSupplier expression, TrackerDto dto, TrackerErrorCode code, Object... args )
        {
            original.addWarningIf( expression, dto, code, args );
        }

        public void addWarning( TrackerDto dto, TrackerErrorCode code, Object... args )
        {
            original.addWarning( dto, code, args );
        }

        public void addWarning( TrackerWarningReport warning )
        {
            original.addWarning( warning );
        }

        public boolean isInvalid( TrackerDto dto )
        {
            return original.isInvalid( dto );
        }

        public boolean isInvalid( TrackerType trackerType, String uid )
        {
            return original.isInvalid( trackerType, uid );
        }
    }
}
