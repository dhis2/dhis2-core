package org.hisp.dhis.program.message;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class SmsDeliveryChannelStrategy
    extends DeliveryChannelStrategy
{
    private static final Log log = LogFactory.getLog( SmsDeliveryChannelStrategy.class );

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public DeliveryChannel getDeliveryChannel()
    {
        return DeliveryChannel.SMS;
    }

    @Override
    public ProgramMessage setAttributes( ProgramMessage message )
    {
        validate( message );

        OrganisationUnit orgUnit = getOrganisationUnit( message );

        TrackedEntityInstance tei = getTrackedEntityInstance( message );

        if ( orgUnit != null )
        {
            message.getRecipients().getPhoneNumbers().add( getOrganisationUnitRecipient( orgUnit ) );
        }

        if ( tei != null )
        {
            message.getRecipients().getPhoneNumbers()
                .add( getTrackedEntityInstanceRecipient( tei, ValueType.PHONE_NUMBER ) );
        }

        return message;
    }

    @Override
    public void validate( ProgramMessage message )
    {
        String violation = null;

        ProgramMessageRecipients recipient = message.getRecipients();

        if ( message.getDeliveryChannels().contains( DeliveryChannel.SMS ) )
        {
            if ( !recipient.hasOrganisationUnit() && !recipient.hasTrackedEntityInstance()
                && recipient.getPhoneNumbers().isEmpty() )
            {
                violation = "No destination found for SMS";
            }
        }

        if ( violation != null )
        {
            log.info( "Message validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public String getOrganisationUnitRecipient( OrganisationUnit orgUnit )
    {
        if ( orgUnit.getPhoneNumber() == null )
        {
            log.error( "Organisation unit does not have phone number" );

            throw new IllegalQueryException( "Organisation unit does not have phone number" );
        }

        return orgUnit.getPhoneNumber();
    }
}
