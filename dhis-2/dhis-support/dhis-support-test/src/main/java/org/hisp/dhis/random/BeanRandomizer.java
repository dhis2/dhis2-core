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
package org.hisp.dhis.random;

import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.FileTypeValueOptions;
import org.hisp.dhis.common.ValueTypeOptions;
import org.hisp.dhis.period.PeriodType;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.locationtech.jts.geom.Geometry;

/**
 * @author Luciano Fiandesio
 */
public final class BeanRandomizer
{

    private BeanRandomizer()
    {
        // so BeanRandomizer cannot be instantiated and factory methods have to
        // be used
    }

    private static EasyRandomParameters parameters()
    {
        return new EasyRandomParameters()
            .randomize( ValueTypeOptions.class, () -> new FileTypeValueOptions() )
            .randomize( PeriodType.class, new PeriodTypeRandomizer() )
            .randomize( Geometry.class, new GeometryRandomizer() )
            .randomize( FieldPredicates.named( "uid" ).and( FieldPredicates.ofType( String.class ) ),
                new UidRandomizer() )
            .randomize( FieldPredicates.named( "id" ).and( FieldPredicates.ofType( long.class ) ), new IdRandomizer() );
    }

    /**
     * Generates an EasyRandom instance for you to generate random Java beans.
     * It will be pre-configured with DHIS2 specific randomizers (for uid, id,
     * ... fields).
     *
     * @return an instance of EasyRandom
     */
    public static EasyRandom create()
    {
        return new EasyRandom( parameters() );
    }

    /**
     * Generates an EasyRandom instance for you to generate random Java beans.
     * It will be pre-configured with DHIS2 specific randomizers (for uid, id,
     * ... fields).
     *
     * @param inClass exclude fields in given class
     * @param excludeFields exclude fields from being filled with random data
     * @return an instance of EasyRandom
     */
    public static EasyRandom create( final Class inClass, final String... excludeFields )
    {
        EasyRandomParameters params = parameters();
        for ( String field : excludeFields )
        {
            params.excludeField( FieldPredicates.named( field ).and( FieldPredicates.inClass( inClass ) ) );
        }
        return new EasyRandom( params );
    }

    /**
     * Generates an EasyRandom instance for you to generate random Java beans.
     * It will be pre-configured with DHIS2 specific randomizers (for uid, id,
     * ... fields).
     *
     * @param excludeFields exclude fields in given class from being filled with
     *        random data
     * @return an instance of EasyRandom
     */
    public static EasyRandom create( Map<Class, Set<String>> excludeFields )
    {
        EasyRandomParameters params = parameters();
        for ( Map.Entry<Class, Set<String>> field : excludeFields.entrySet() )
        {
            for ( String name : field.getValue() )
            {
                params.excludeField( FieldPredicates.named( name )
                    .and( FieldPredicates.inClass( field.getKey() ) ) );
            }
        }
        return new EasyRandom( params );
    }
}
