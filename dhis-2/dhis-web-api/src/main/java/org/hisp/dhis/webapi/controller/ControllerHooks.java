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
package org.hisp.dhis.webapi.controller;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjects;

/**
 * Pre- and post- hooks for {@link IdentifiableObject} controllers that do
 * nothing by default.
 *
 * @author Jan Bernitt
 * @param <T> type of the object the hooks apply to
 */
public interface ControllerHooks<T extends IdentifiableObject>
{
    default void preCreateEntity( T entity )
        throws Exception
    {
        // by default do nothing
    }

    default void postCreateEntity( T entity )
    {
        // by default do nothing
    }

    default void preUpdateEntity( T entity, T newEntity )
        throws Exception
    {
        // by default do nothing
    }

    default void postUpdateEntity( T entity )
    {
        // by default do nothing
    }

    default void preDeleteEntity( T entity )
        throws Exception
    {
        // by default do nothing
    }

    default void postDeleteEntity( String entityUid )
    {
        // by default do nothing
    }

    default void prePatchEntity( T entity )
        throws Exception
    {
        // by default do nothing
    }

    default void postPatchEntity( T entity )
    {
        // by default do nothing
    }

    default void preUpdateItems( T entity, IdentifiableObjects items )
        throws Exception
    {
        // by default do nothing
    }

    default void postUpdateItems( T entity, IdentifiableObjects items )
    {
        // by default do nothing
    }
}
