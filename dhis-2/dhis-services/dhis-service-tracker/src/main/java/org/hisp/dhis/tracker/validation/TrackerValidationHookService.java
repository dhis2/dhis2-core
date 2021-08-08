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
package org.hisp.dhis.tracker.validation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackerValidationHookService
{
    @Qualifier( "validationOrder" )
    private final List<Class<? extends TrackerValidationHook>> validationOrder;

    @Qualifier( "ruleEngineValidationHooks" )
    private final List<Class<? extends TrackerValidationHook>> ruleEngineValidationHooks;

    @Qualifier( "validationOrderMap" )
    private final Map<Class<? extends TrackerValidationHook>, Integer> validationOrderMap;

    /**
     * Sort the hooks in the order they are represented in the validation order
     * list
     *
     * @param hooks list to sort
     */
    public List<TrackerValidationHook> sortValidationHooks( List<TrackerValidationHook> hooks )
    {
        return hooks
            .stream().filter( h -> validationOrder.contains( h.getClass() ) )
            .sorted( Comparator
                .comparingInt( o -> validationOrderMap.get( o.getClass() ) ) )
            .collect( Collectors.toList() );
    }

    /**
     * Get just rule engine validation hooks.
     *
     * @param hooks list to filter
     */
    public List<TrackerValidationHook> getRuleEngineValidationHooks( List<TrackerValidationHook> hooks )
    {
        return hooks
            .stream()
            .filter( h -> ruleEngineValidationHooks.contains( h.getClass() ) )
            .collect( Collectors.toList() );
    }

}
