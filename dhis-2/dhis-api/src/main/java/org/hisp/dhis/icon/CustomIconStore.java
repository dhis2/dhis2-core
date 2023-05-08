/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.icon;

import java.util.Collection;
import java.util.List;

public interface CustomIconStore
{
    /**
     * Returns a custom icon that contains a given key
     *
     * @param key of the icon
     * @return the custom icon matching the key, or null instead
     */
    CustomIcon getIconByKey( String key );

    /**
     * Returns a list of custom icons that contain all the specified keywords
     *
     * @param keywords the icon needs to contain
     * @return the list of custom icons that contain all the keywords
     */
    List<CustomIcon> getIconsByKeywords( Collection<String> keywords );

    /**
     * Gets all custom icons present in the database
     *
     * @return a list containing all icons
     */
    List<CustomIcon> getAllIcons();

    /**
     * Returns a list with all the custom icon keywords
     *
     * @return a list will all the custom icon keywords
     */
    List<String> getKeywords();

    /**
     * Persists a custom icon to the database
     *
     * @param customIcon Icon to be saved
     */
    void save( CustomIcon customIcon );

    /**
     * Deletes a custom icon from the database
     *
     * @param customIcon Icon to be deleted
     */
    void delete( CustomIcon customIcon );

    /**
     * Updates a custom icon from the database
     *
     * @param customIcon Icon to be updated
     */
    void update( CustomIcon customIcon );
}
