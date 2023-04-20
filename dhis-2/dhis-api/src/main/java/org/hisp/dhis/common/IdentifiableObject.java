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
package org.hisp.dhis.common;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObject
    extends PrimaryKeyObject, LinkableObject, Comparable<IdentifiableObject>, Serializable
{
    String getCode();

    String getName();

    String getDisplayName();

    Date getCreated();

    Date getLastUpdated();

    User getLastUpdatedBy();

    Set<AttributeValue> getAttributeValues();

    void setAttributeValues( Set<AttributeValue> attributeValues );

    Set<Translation> getTranslations();

    Set<String> getFavorites();

    boolean isFavorite();

    boolean setAsFavorite( User user );

    boolean removeAsFavorite( User user );

    // -----------------------------------------------------------------------------
    // Sharing
    // -----------------------------------------------------------------------------

    /**
     * Return User who created this object This field is immutable and must not
     * be updated
     */
    User getCreatedBy();

    /**
     * @deprecated This method is replaced by {@link #getCreatedBy()} Currently
     *             it is only used for web api backward compatibility
     */
    @Deprecated
    User getUser();

    void setCreatedBy( User createdBy );

    /**
     * @deprecated This method is replaced by {@link #setCreatedBy(User)} ()}
     *             Currently it is only used for web api backward compatibility
     */
    @Deprecated
    void setUser( User user );

    Access getAccess();

    /**
     * Return all sharing settings of current object
     */
    Sharing getSharing();

    void setSharing( Sharing sharing );
    // -----------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------

    @JsonIgnore
    String getPropertyValue( IdScheme idScheme );

    default void initSharing()
    {
        if ( getSharing() == null )
        {
            setSharing( Sharing.builder().build() );
        }

        if ( getSharing().getUsers() == null )
        {
            getSharing().setUsers( new HashMap<>() );
        }

        if ( getSharing().getUserGroups() == null )
        {
            getSharing().setUserGroups( new HashMap<>() );
        }

    }

    default boolean hasSharing()
    {
        return getSharing() != null;
    }

}
