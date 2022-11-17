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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportParamKey.ID_SCHEME_KEY;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.webapi.controller.exception.InvalidEnumValueException;

import com.google.common.base.Enums;

public class TrackerImportParamsValidator
{
    private TrackerImportParamsValidator()
    {
    }

    public static void validateRequest( TrackerImportRequest request )
        throws InvalidEnumValueException
    {
        Map<String, List<String>> parameters = request.getContextService().getParameterValuesMap();

        validateEnum( TrackerIdScheme.class, parameters, ID_SCHEME_KEY );
    }

    private static <T extends Enum<T>> void validateEnum( Class<T> enumKlass, Map<String, List<String>> parameters,
        TrackerImportParamKey trackerImportParamKey )
        throws InvalidEnumValueException
    {
        if ( parameters == null || parameters.get( trackerImportParamKey.getKey() ) == null
            || parameters.get( trackerImportParamKey.getKey() ).isEmpty() )
        {
            return;
        }

        if ( TrackerIdScheme.class.equals( enumKlass )
            && IdScheme.isAttribute( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) ) )
        {
            return;
        }

        String value = String.valueOf( parameters.get( trackerImportParamKey.getKey() ).get( 0 ) );

        Optional<T> optionalEnumValue = Enums.getIfPresent( enumKlass, value ).toJavaUtil();
        if ( optionalEnumValue.isPresent() )
        {
            return;
        }

        throw new InvalidEnumValueException(
            value, trackerImportParamKey.getKey(),
            Arrays.stream( enumKlass.getEnumConstants() ).map( Objects::toString )
                .collect( Collectors.toList() ) );
    }
}
