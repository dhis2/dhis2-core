package org.hisp.dhis.dxf2.metadata.handlers;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public final class ObjectHandlerUtils
{
    public static <T> void preObjectHandlers( T object, List<ObjectHandler<T>> objectHandlers )
    {
        if ( objectHandlers.isEmpty() )
        {
            return;
        }

        objectHandlers.stream().filter( objectHandler -> objectHandler.canHandle( object.getClass() ) )
            .forEach( objectHandler -> objectHandler.preImportObject( object ) );
    }

    public static <T> void postObjectHandlers( T object, List<ObjectHandler<T>> objectHandlers )
    {
        if ( objectHandlers.isEmpty() )
        {
            return;
        }

        objectHandlers.stream().filter( objectHandler -> objectHandler.canHandle( object.getClass() ) )
            .forEach( objectHandler -> objectHandler.postImportObject( object ) );
    }

    public static <T> void preObjectsHandlers( List<T> objects, List<ObjectHandler<T>> objectHandlers )
    {
        if ( objectHandlers.isEmpty() )
        {
            return;
        }

        objectHandlers.stream().filter( objectHandler -> objectHandler.canHandle( objects.get( 0 ).getClass() ) )
            .forEach( objectHandler -> objectHandler.preImportObjects( objects ) );
    }

    public static <T> void postObjectsHandlers( List<T> objects, List<ObjectHandler<T>> objectHandlers )
    {
        if ( objectHandlers.isEmpty() )
        {
            return;
        }

        objectHandlers.stream().filter( objectHandler -> objectHandler.canHandle( objects.get( 0 ).getClass() ) )
            .forEach( objectHandler -> objectHandler.postImportObjects( objects ) );
    }

    private ObjectHandlerUtils()
    {
    }
}
