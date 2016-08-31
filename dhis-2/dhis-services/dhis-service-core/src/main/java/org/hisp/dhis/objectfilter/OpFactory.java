package org.hisp.dhis.objectfilter;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import com.google.common.collect.Maps;
import org.hisp.dhis.objectfilter.ops.EmptyCollectionOp;
import org.hisp.dhis.objectfilter.ops.EndsWithOp;
import org.hisp.dhis.objectfilter.ops.EqOp;
import org.hisp.dhis.objectfilter.ops.GtOp;
import org.hisp.dhis.objectfilter.ops.GteOp;
import org.hisp.dhis.objectfilter.ops.InOp;
import org.hisp.dhis.objectfilter.ops.LikeOp;
import org.hisp.dhis.objectfilter.ops.LtOp;
import org.hisp.dhis.objectfilter.ops.LteOp;
import org.hisp.dhis.objectfilter.ops.NLikeOp;
import org.hisp.dhis.objectfilter.ops.NeqOp;
import org.hisp.dhis.objectfilter.ops.NnullOp;
import org.hisp.dhis.objectfilter.ops.NullOp;
import org.hisp.dhis.objectfilter.ops.Op;
import org.hisp.dhis.objectfilter.ops.StartsWithOp;

import java.util.Map;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class OpFactory
{
    protected static Map<String, Class<? extends Op>> REGISTER = Maps.newHashMap();

    static
    {
        register( "eq", EqOp.class );
        register( "ne", NeqOp.class );
        register( "neq", NeqOp.class );
        register( "like", LikeOp.class );
        register( "ilike", LikeOp.class );
        register( "nlike", NLikeOp.class );
        register( "startsWith", StartsWithOp.class );
        register( "endsWith", EndsWithOp.class );
        register( "gt", GtOp.class );
        register( "ge", GteOp.class );
        register( "gte", GteOp.class );
        register( "lt", LtOp.class );
        register( "le", LteOp.class );
        register( "lte", LteOp.class );
        register( "null", NullOp.class );
        register( "nnull", NnullOp.class );
        register( "empty", EmptyCollectionOp.class );
        register( "in", InOp.class );
    }

    public static void register( String type, Class<? extends Op> opClass )
    {
        REGISTER.put( type.toLowerCase(), opClass );
    }

    public static boolean canCreate( String type )
    {
        return REGISTER.containsKey( type.toLowerCase() );
    }

    public static Op create( String type )
    {
        Class<? extends Op> opClass = REGISTER.get( type.toLowerCase() );

        try
        {
            return opClass.newInstance();
        }
        catch ( InstantiationException | IllegalAccessException ignored )
        {
        }

        return null;
    }
}
