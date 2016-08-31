package org.hisp.dhis.dxf2.metadata;

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

import java.util.Set;

/**
 * Acts as a bridge between the importer and the persistence/cache layer.
 * <p>
 * The flag {@code writeEnabled} is used to indicate if writing to the persistence layer
 * is enabled or not.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface ObjectBridge
{
    /**
     * Initialize bridge.
     * 
     * @param preheatClasses the classes to preheat.
     */
    @SuppressWarnings( "rawtypes" )
    void init( Set<Class> preheatClasses );

    /**
     * Destroy bridge.
     */
    void destroy();

    /**
     * Save object. Will save to persistence layer if {@code writeEnabled} is {@code true}.
     *
     * @param object Object to write
     */
    void saveObject( Object object );

    /**
     * Save object. Will save to persistence layer if {@code writeEnabled} is {@code true}.
     *
     * @param object Object to write
     */
    void saveObject( Object object, boolean clearSharing );

    /**
     * Update object. Will save to persistence layer if {@code writeEnabled} is {@code true}.
     *
     * @param object Object to update
     */
    void updateObject( Object object );

    /**
     * Delete object. Will delete from persistence layer if {@code writeEnabled} is {@code true}.
     *
     * @param object Object to delete
     */
    void deleteObject( Object object );

    /**
     * Get an object from the internal store. This object might not be a persisted object
     * depending on the flag {@code writeEnabled}.
     *
     * @param object Object to match against
     * @return Matched object or {@code null} if matched > 1 or no match found
     */
    <T> T getObject( T object );

    /**
     * Return all matches for a given object. These objects might not be a persisted object
     * depending on the flag {@code writeEnabled}.
     *
     * @param object Object to match against
     * @return A collection of matched objects
     */
    <T> Set<T> getObjects( T object );

    /**
     * Get all objects for a specified class. These objects might not be persisted
     * depending on the flag {@code writeEnabled}.
     *
     * @param clazz Clazz to match against
     * @return Collection of matches
     */
    <T> Set<T> getAllObjects( Class<T> clazz );

    /**
     * Enable or disable writing to the persistence store.
     *
     * @param enabled {@code boolean} turning writing on or off
     */
    void setWriteEnabled( boolean enabled );

    /**
     * Is persistence storage enabled?
     *
     * @return {@code boolean} indicating status of {@code writeEnabled}
     */
    boolean isWriteEnabled();

    /**
     * Enable or disable preheating the internal cache. This should be left on for most cases,
     * but for very small imports (1-10 objects) turning this off will generally speed up import by a factor of 100.
     *
     * @param enabled {@code boolean} turning preheating on or off
     */
    void setPreheatCache( boolean enabled );

    /**
     * Is preheat cache enabled?
     *
     * @return {@code boolean} indicating status of {@code preheatCache}
     */
    boolean isPreheatCache();
}
