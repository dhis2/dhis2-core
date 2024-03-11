package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.translation.TranslationProperty;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.hisp.dhis.user.UserGroupAccess;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public interface IdentifiableObject
    extends LinkableObject, Comparable<IdentifiableObject>, Serializable
{
    String[] I18N_PROPERTIES = { TranslationProperty.NAME.getName() };

    int getId();

    String getUid();

    String getCode();

    String getName();

    String getDisplayName();

    Date getCreated();

    Date getLastUpdated();

    User getLastUpdatedBy();

    Set<AttributeValue> getAttributeValues();

    Set<Translation> getTranslations();
    
    Set<String> getFavorites();

    boolean isFavorite();
    
    boolean setAsFavorite( User user );
    
    boolean removeAsFavorite( User user );
    
    //-----------------------------------------------------------------------------
    // Sharing
    //-----------------------------------------------------------------------------

    User getUser();

    String getPublicAccess();

    boolean getExternalAccess();

    Set<UserGroupAccess> getUserGroupAccesses();

    Set<UserAccess> getUserAccesses();

    Access getAccess();

    //-----------------------------------------------------------------------------
    // Utility methods
    //-----------------------------------------------------------------------------

    @JsonIgnore
    String getPropertyValue( IdScheme idScheme );
}
