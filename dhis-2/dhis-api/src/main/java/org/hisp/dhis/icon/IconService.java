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
package org.hisp.dhis.icon;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.springframework.core.io.Resource;

/**
 * @author Kristian Wærstad
 */
public interface IconService
{
    /**
     * Gets data about all the icons in the system
     *
     * @param contextApiPath the api path of the context
     * @return a collection of data about all the icons in the system
     */
    Collection<BaseIcon> getIcons( String contextApiPath );

    /**
     * Gets info about the icons in the system tagged with all the keywords in a
     * collection
     *
     * @param keywords collection of keywords
     * @param contextApiPath the api path of the context
     * @return a collection of matching icons
     */
    Collection<BaseIcon> getIcons( Collection<String> keywords, String contextApiPath );

    /**
     * Gets the icon associated to a key, if it exists
     *
     * @param key key of the icon to find
     * @param contextApiPath the api path of the context
     * @return icon associated to the key, if found
     * @throws NotFoundException if no icon exists in the database with the
     *         provided key
     */
    BaseIcon getIcon( String key, String contextApiPath )
        throws NotFoundException;

    /**
     * Gets the custom icon associated to a key, if it exists
     *
     * @param key key of the icon to find
     * @return custom icon associated to the key, if found
     * @throws NotFoundException if no custom icon exists with the provided key
     */
    CustomIcon getCustomIcon( String key )
        throws NotFoundException;

    /**
     * Gets the icon with the correct key if one exists
     *
     * @param key key of the icon
     * @return the icon resource
     * @throws BadRequestException if no standard icon exists with the provided
     *         key
     */
    Optional<Resource> getIconResource( String key )
        throws BadRequestException;

    /**
     * Gets a set of all unique keywords assigned to icons
     *
     * @return set of unique keywords
     */
    List<String> getKeywords();

    /**
     * Checks whether an icon with a given key exists, either standard or custom
     *
     * @param key key of the icon
     * @return true if the icon exists, false otherwise
     */
    boolean iconExists( String key );

    /**
     * Persists the provided custom icon to the database
     *
     * @param customIcon the icon to be persisted
     * @throws BadRequestException when an icon already exists with the same key
     *         or the file resource id is not specified
     * @throws NotFoundException when no file resource with the provided id
     *         exists
     */
    void addCustomIcon( CustomIcon customIcon )
        throws BadRequestException,
        NotFoundException;

    /**
     * Updates the description of a given custom icon
     *
     * @param key the key of the icon to update
     * @param description the new icons description
     * @param keywords the new icons keywords
     * @throws BadRequestException when icon key is not specified
     * @throws NotFoundException when no icon with the provided key exists
     */
    void updateCustomIcon( String key, String description, List<String> keywords )
        throws BadRequestException,
        NotFoundException;

    /**
     * Deletes a custom icon given its key
     *
     * @param key the key of the icon to delete
     * @throws BadRequestException when icon key is not specified
     * @throws NotFoundException when no icon with the provided key exists
     */
    void deleteCustomIcon( String key )
        throws BadRequestException,
        NotFoundException;
}
