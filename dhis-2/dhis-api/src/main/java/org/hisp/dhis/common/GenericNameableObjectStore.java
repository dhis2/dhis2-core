package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 * @author Lars Helge Overland
 */
public interface GenericNameableObjectStore<T>
    extends GenericIdentifiableObjectStore<T>
{
    /**
     * Retrieves the object with the given short name.
     *
     * @param shortName the short name.
     * @return the object with the given short name.
     */
    T getByShortName( String shortName );

    /**
     * Gets the count of objects which shortName is equal the given shortName.
     *
     * @param shortName the shortName which result object shortNames must be like.
     * @return the count of objects.
     */
    int getCountEqShortName( String shortName );

    /**
     * Gets the count of objects which shortName is like the given shortName.
     *
     * @param shortName the shortName which result object shortNames must be like.
     * @return the count of objects.
     */
    int getCountLikeShortName( String shortName );

    /**
     * Return the number of objects where the name is equal the given name.
     *
     * @param shortName the name.
     * @return Count of objects.
     */
    int getCountEqShortNameNoAcl( String shortName );

    /**
     * Retrieves a List of objects where the shortName is like the given shortName.
     *
     * @param shortName the shortName.
     * @return a List of objects.
     */
    List<T> getAllLikeShortName( String shortName );

    /**
     * Retrieves a List of objects where the name is like the given name.
     *
     * @param shortName the name.
     * @return a List of objects.
     */
    List<T> getAllEqShortName( String shortName );

    /**
     * Retrieves a List of objects where the name is like the given name (ignore case).
     *
     * @param shortName the name.
     * @return a List of objects.
     */
    List<T> getAllEqShortNameIgnoreCase( String shortName );
}
