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
package org.hisp.dhis.configuration;

import java.util.Iterator;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@RequiredArgsConstructor
@Service( "org.hisp.dhis.configuration.ConfigurationService" )
public class DefaultConfigurationService
    implements ConfigurationService
{
    @Qualifier( "org.hisp.dhis.configuration.ConfigurationStore" )
    private final GenericStore<Configuration> configurationStore;

    // -------------------------------------------------------------------------
    // ConfigurationService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void setConfiguration( Configuration configuration )
    {
        if ( configuration == null )
        {
            return;
        }
        if ( configuration.getId() > 0 )
        {
            configurationStore.update( configuration );
        }
        else
        {
            configurationStore.save( configuration );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public Configuration getConfiguration()
    {
        Iterator<Configuration> iterator = configurationStore.getAll().iterator();

        return iterator.hasNext() ? iterator.next() : new Configuration();
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isCorsWhitelisted( String origin )
    {
        Set<String> corsWhitelist = getConfiguration().getCorsWhitelist();

        for ( String cors : corsWhitelist )
        {
            String regex = TextUtils.createRegexFromGlob( cors );

            if ( origin.matches( regex ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isUserInFeedbackRecipientUserGroup( User user )
    {
        UserGroup feedbackRecipients = getConfiguration().getFeedbackRecipients();

        return feedbackRecipients != null && feedbackRecipients.getMembers().contains( user );
    }
}
