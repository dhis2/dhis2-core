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
package org.hisp.dhis.analytics.dimension;

import static org.hisp.dhis.analytics.dimension.DimensionMapperTestSupport.asserter;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

public class BaseDimensionMapperTest
{

    public static final String UID = "uid";

    public static final long ID = 100L;

    public static final String NAME = "name";

    public static final Date CREATED = new Date();

    public static final String CODE = "CODE";

    public static final Date LAST_UPDATED = new Date();

    public static final String SHORT_NAME = "shortName";

    private final BaseDimensionMapper mapper = new BaseDimensionMapper()
    {
        @Override
        public Set<Class<? extends BaseIdentifiableObject>> getSupportedClasses()
        {
            return Collections.emptySet();
        }
    };

    @Test
    public void testMapBaseIdentifiable()
    {

        asserter(
            mapper,
            BaseIdentifiableObject::new,
            ImmutableList.of(
                b -> b.setUid( UID ),
                b -> b.setId( ID ),
                b -> b.setName( NAME ),
                b -> b.setCreated( CREATED ),
                b -> b.setCode( CODE ),
                b -> b.setLastUpdated( LAST_UPDATED ) ),
            ImmutableList.of(
                Pair.of( DimensionResponse::getId, UID ),
                Pair.of( DimensionResponse::getUid, UID ),
                Pair.of( DimensionResponse::getDisplayName, NAME ),
                Pair.of( DimensionResponse::getCreated, CREATED ),
                Pair.of( DimensionResponse::getCode, CODE ),
                Pair.of( DimensionResponse::getLastUpdated, LAST_UPDATED ),
                Pair.of( DimensionResponse::getName, NAME ) ) );
    }

    @Test
    public void testMapBaseNameable()
    {
        asserter(
            mapper,
            BaseNameableObject::new,
            ImmutableList.of(
                b -> b.setShortName( SHORT_NAME ) ),
            ImmutableList.of(
                Pair.of( DimensionResponse::getDisplayShortName, SHORT_NAME ) ) );
    }

}
