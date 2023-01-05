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
package org.hisp.dhis.deletedobject;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.deletedobject.DeletedObjectService" )
public class DefaultDeletedObjectService
    implements DeletedObjectService
{
    private final DeletedObjectStore deletedObjectStore;

    @Override
    @Transactional
    public void addDeletedObject( DeletedObject deletedObject )
    {
        deletedObjectStore.save( deletedObject );
    }

    @Override
    @Transactional
    public void deleteDeletedObject( DeletedObject deletedObject )
    {
        deletedObjectStore.delete( deletedObject );
    }

    @Override
    @Transactional
    public void deleteDeletedObjects( DeletedObjectQuery query )
    {
        deletedObjectStore.delete( query );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DeletedObject> getDeletedObjectsByKlass( String klass )
    {
        return deletedObjectStore.getByKlass( klass );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DeletedObject> getDeletedObjects()
    {
        return deletedObjectStore.query( DeletedObjectQuery.EMPTY );
    }

    @Override
    @Transactional
    public int countDeletedObjects()
    {
        return deletedObjectStore.count( DeletedObjectQuery.EMPTY );
    }

    @Override
    @Transactional( readOnly = true )
    public List<DeletedObject> getDeletedObjects( DeletedObjectQuery query )
    {
        return deletedObjectStore.query( query );
    }

    @Override
    @Transactional( readOnly = true )
    public int countDeletedObjects( DeletedObjectQuery query )
    {
        return deletedObjectStore.count( query );
    }
}
