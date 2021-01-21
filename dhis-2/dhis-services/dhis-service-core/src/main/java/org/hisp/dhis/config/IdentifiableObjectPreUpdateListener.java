package org.hisp.dhis.config;/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * This class will listen to all update event on IdentifiableObject
 * It will then make sure "createdBy" field of current object is immutable.
 */
@Component
public class IdentifiableObjectPreUpdateListener implements PreUpdateEventListener
{
    @Override
    public boolean onPreUpdate( PreUpdateEvent preUpdateEvent )
    {
        if ( !BaseIdentifiableObject.class.isAssignableFrom( preUpdateEvent.getEntity().getClass() ) )
        {
            return true;
        }

        String[] propertyNames = preUpdateEvent.getPersister().getPropertyNames();

        for ( int i = 0; i < propertyNames.length; i++ )
        {
            if ( propertyNames[i].equalsIgnoreCase( BaseIdentifiableObject_.CREATED_BY ) )
            {
                Object oldValue = preUpdateEvent.getOldState()[i];

                if ( oldValue == null )
                {
                    return true;
                }

                preUpdateEvent.getState()[i] = oldValue;
                ((BaseIdentifiableObject) preUpdateEvent.getEntity()).setCreatedBy( (User) oldValue );

                return true;
            }
        }

        return true;
    }
}
