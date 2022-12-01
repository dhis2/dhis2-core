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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1125;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.tracker.domain.TrackerDto;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerValidationHook;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public abstract class AbstractTrackerDtoValidationHook
    implements TrackerValidationHook
{
    /**
     * This constructor is used by the PreCheck* hooks
     */
    protected AbstractTrackerDtoValidationHook()
    {
    }

    protected <T extends ValueTypedDimensionalItemObject> void validateOptionSet( ValidationErrorReporter reporter,
        TrackerDto dto,
        T optionalObject, String value )
    {
        if ( value == null )
        {
            return;
        }

        Optional.ofNullable( optionalObject.getOptionSet() )
            .ifPresent( optionSet -> reporter.addErrorIf(
                () -> optionSet.getOptions().stream().filter( Objects::nonNull )
                    .noneMatch( o -> o.getCode().equalsIgnoreCase( value ) ),
                dto, E1125, value,
                optionalObject.getUid(), optionalObject.getClass().getSimpleName(),
                optionalObject.getOptionSet().getOptions().stream().filter( Objects::nonNull ).map( Option::getCode )
                    .collect( Collectors.joining( "," ) ) ) );
    }
}
