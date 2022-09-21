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
package org.hisp.dhis.tracker.programrule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.rules.models.RuleEffect;

/**
 * @Author Enrico Colasante
 */
public interface RuleActionImplementer<T>
{
    /**
     * @return the class of the action that the implementer work with
     */
    Class<T> getActionClass();

    /**
     * Filter the actions by the action class of the implementer
     *
     * @param effects a map of enrollments/events and effects
     * @return the same map with the actions in the effects filtered
     */
    default Map<String, List<RuleEffect>> getEffects( Map<String, List<RuleEffect>> effects )
    {
        return effects
            .entrySet()
            .stream()
            .collect( Collectors.toMap( e -> e.getKey(),
                e -> e.getValue()
                    .stream()
                    .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
                    .collect( Collectors.toList() ) ) );
    }

    /**
     * Filter the actions by the action class of the implementer
     *
     * @param effects a map of enrollments/events and effects
     * @return the same map with the actions in the effects filtered
     */
    default List<RuleEffect> getEffects( List<RuleEffect> effects )
    {
        return effects
            .stream()
            .filter( effect -> getActionClass().isAssignableFrom( effect.ruleAction().getClass() ) )
            .collect( Collectors.toList() );
    }
}
