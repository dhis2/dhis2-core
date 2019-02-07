/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.random;

import io.github.benas.randombeans.FieldDefinitionBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.hisp.dhis.period.PeriodType;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.benas.randombeans.EnhancedRandomBuilder.aNewEnhancedRandomBuilder;

/**
 * @author Luciano Fiandesio
 */
public class BeanRandomizer
{
    private EnhancedRandom rnd;

    public BeanRandomizer() {
        rnd = aNewEnhancedRandomBuilder()
                .randomize(PeriodType.class, new PeriodTypeRandomizer())
                .randomize(FieldDefinitionBuilder.field().named("uid").ofType(String.class).get(),
                        new UidRandomizer())
                .build();
    }

    /**
     * Generates an instance of the specified type and fill the instance's properties with random data
     * @param type The bean type
     * @param excludedFields a list of fields to exclude from the random population
     *
     * @return an instance of the specified type
     */
    public <T> T randomObject(final Class<T> type, final String... excludedFields )
    {
        return rnd.nextObject( type, excludedFields );
    }

    /**
     * Generates multiple instances of the specified type and fills each instance's properties with random data
     * @param type The bean type
     * @param amount the amount of beans to generate
     * @param excludedFields a list of fields to exclude from the random population
     *
     * @return an instance of the specified type
     */
    public <T> List<T> randomObjects(final Class<T> type, int amount, final String... excludedFields )
    {
        return rnd.objects( type, amount, excludedFields ).collect( Collectors.toList() );
    }

}
