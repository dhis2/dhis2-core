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
package org.hisp.dhis.tracker.validation.validator;

import java.util.Collection;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.validation.Reporter;
import org.hisp.dhis.tracker.validation.Validator;

/**
 * Each is a {@link Validator} applying a given {@link Validator} to each
 * element in a collection of type R irrespective of whether the
 * {@link Validator} added an error to {@link Reporter} or not.
 *
 * @param <T> type of input to be mapped to a Collection of R
 * @param <R> type of input to be validated by given validator
 */
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
public class Each<T, R> implements Validator<T>
{
    private final Function<T, ? extends Collection<R>> map;

    private final Validator<R> validator;

    /**
     * Create an {@link Each} that will apply given {@link Validator} of type R
     * to each element in the {@code Collection<R>}. The input to the returned
     * {@code Validator} is of type T which is mapped using given {@code map}
     * function.
     * <p>
     * Use it for example when you have a {@code Validator<Note>} and want to
     * validate every {@code Note} on an {@code Enrollment}.
     * </p>
     * You would then write
     *
     * <pre>
     * {@code each(Enrollment::getNotes, noteValidator)}
     * </pre>
     *
     * @param map function taking type T to Collection of R
     * @param validator validator validating a single element of type R
     * @return validator of type T
     * @param <T> type of input to be mapped to a Collection of R
     * @param <R> type of input to be validated by given validator
     */
    public static <T, R> Each<T, R> each( Function<T, ? extends Collection<R>> map, Validator<R> validator )
    {
        return new Each<>( map, validator );
    }

    @Override
    public void validate( Reporter reporter, TrackerBundle bundle, T input )
    {
        for ( R in : map.apply( input ) )
        {
            if ( (in instanceof TrackerDto && !validator.needsToRun( bundle.getStrategy( (TrackerDto) in ) ))
                || (!(in instanceof TrackerDto) && !validator.needsToRun( bundle.getImportStrategy() )) )
            {
                continue;
            }

            validator.validate( reporter, bundle, in );
        }
    }

    @Override
    public boolean needsToRun( TrackerImportStrategy strategy )
    {
        return true; // Each is used to compose other Validators, so it should always run
    }
}
