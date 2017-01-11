package org.hisp.dhis.configuration;

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

import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultConfigurationService
    implements ConfigurationService
{
    private GenericStore<Configuration> configurationStore;

    @Autowired
    CurrentUserService currentUserService;

    public void setConfigurationStore( GenericStore<Configuration> configurationStore )
    {
        this.configurationStore = configurationStore;
    }

    // -------------------------------------------------------------------------
    // ConfigurationService implementation
    // -------------------------------------------------------------------------
    
    @Override
    public void setConfiguration( Configuration configuration )
    {
        if ( configuration != null && configuration.getId() > 0 )
        {
            configurationStore.update( configuration );
        }
        else
        {
            configurationStore.save( configuration );
        }
    }
    
    @Override
    public Configuration getConfiguration()
    {
        Iterator<Configuration> iterator = configurationStore.getAll().iterator();
        
        return iterator.hasNext() ? iterator.next() : new Configuration();
    }

    @Override
    public boolean isCorsWhitelisted( String origin )
    {
        return getConfiguration().getCorsWhitelist().contains( origin );
    }

    @Override
    public boolean isUserInFeedbackRecipientUserGroup( User user )
    {
        user = ( user == null ? currentUserService.getCurrentUser() : user );

        UserGroup feedbackRecipients = getConfiguration().getFeedbackRecipients();

        return feedbackRecipients != null && feedbackRecipients.getMembers().contains( user );
    }
}
