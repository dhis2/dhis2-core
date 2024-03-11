package org.hisp.dhis.feedback;

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

import org.hisp.dhis.common.IdentifiableObject;

import javax.annotation.Nonnull;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Container with objects where the index of the object can be retrieved by the
 * object itself.
 *
 * @author Volker Schmidt
 */
public class IndexedObjectContainer implements ObjectIndexProvider
{
    private final Map<IdentifiableObject, Integer> objectsIndexMap = new IdentityHashMap<>();

    @Nonnull
    @Override
    public Integer mergeObjectIndex( @Nonnull IdentifiableObject object )
    {
        return add( object );
    }

    /**
     * @param object the identifiable object that should be checked.
     * @return <code>true</code> if the object is included in the container, <code>false</code> otherwise.
     */
    public boolean containsObject( @Nonnull IdentifiableObject object )
    {
        return objectsIndexMap.containsKey( object );
    }

    /**
     * Adds an object to the container of indexed objects. If the object has
     * already an index assigned, that will not be changed.
     *
     * @param object the object to which an index should be assigned.
     * @return the resulting zero based index of the added object in the container.
     */
    @Nonnull
    protected Integer add( @Nonnull IdentifiableObject object )
    {
        final Integer newIndex = objectsIndexMap.size();
        final Integer existingIndex = objectsIndexMap.putIfAbsent( object, newIndex );
        return ( existingIndex == null ) ? newIndex : existingIndex;
    }
}
