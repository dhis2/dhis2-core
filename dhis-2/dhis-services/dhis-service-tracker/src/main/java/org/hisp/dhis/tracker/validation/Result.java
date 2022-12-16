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

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
class Result implements ValidationResult
{
    private final Set<Error> errors;

    private final Set<Warning> warnings;

    public static Result empty()
    {
        return new Result( Collections.emptySet(), Collections.emptySet() );
    }

    public static Result ofValidations( Set<Error> errors, Set<Warning> warnings )
    {
        return new Result( errors, warnings );
    }

    public static Result ofErrors( Set<Error> errors )
    {
        return new Result( errors, Collections.emptySet() );
    }

    public static Result ofWarnings( Set<Warning> warnings )
    {
        return new Result( Collections.emptySet(), warnings );
    }

    public Set<Validation> getErrors()
    {
        return Collections.unmodifiableSet( errors );
    }

    public Set<Validation> getWarnings()
    {
        return Collections.unmodifiableSet( warnings );
    }

    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    public boolean hasError( Predicate<Error> test )
    {
        return errors.stream().anyMatch( test );
    }

    public boolean hasWarnings()
    {
        return !warnings.isEmpty();
    }

    public boolean hasWarning( Predicate<Warning> test )
    {
        return warnings.stream().anyMatch( test );
    }
}
