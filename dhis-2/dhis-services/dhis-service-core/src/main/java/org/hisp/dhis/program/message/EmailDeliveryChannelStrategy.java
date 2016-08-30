package org.hisp.dhis.program.message;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class EmailDeliveryChannelStrategy
    extends DeliveryChannelStrategy
{
    private static final Log log = LogFactory.getLog( EmailDeliveryChannelStrategy.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public ProgramMessage fillAttributes( ProgramMessage message )
    {
        validate( message );

        OrganisationUnit orgUnit = getOrganisationUnit( message );

        TrackedEntityInstance tei = getTrackedEntityInstance( message );

        if ( orgUnit != null )
        {
            message.getRecipients().getEmailAddresses().add( getOrgnisationUnitRecipient( orgUnit ) );
        }

        if ( tei != null )
        {
            message.getRecipients().getEmailAddresses()
                .add( getTrackedEntityInstanceRecipient( tei, ValueType.EMAIL ) );
        }

        return message;
    }

    @Override
    public void validate( ProgramMessage message )
    {
        String violation = null;

        ProgramMessageRecipients recipient = message.getRecipients();

        if ( message.getDeliveryChannels().contains( DeliveryChannel.EMAIL ) )
        {
            if ( !recipient.hasOrganisationUnit() && !recipient.hasTrackedEntityInstance()
                && recipient.getEmailAddresses().isEmpty() )
            {
                violation = "No destination found for EMAIL";
            }
        }

        if ( violation != null )
        {
            log.info( "Message validation failed: " + violation );

            throw new IllegalQueryException( violation );

        }
    }

    @Override
    public DeliveryChannel getDeliveryChannel()
    {
        return DeliveryChannel.EMAIL;
    }

    @Override
    public String getOrgnisationUnitRecipient( OrganisationUnit orgUnit )
    {
        if ( orgUnit.getEmail() == null )
        {
            log.error( "OrganisationUnit does not have email address" );

            throw new IllegalQueryException( "OrganisationUnit does not have email address" );
        }

        return orgUnit.getEmail();
    }

    // ---------------------------------------------------------------------
    // Supportive Methods
    // ---------------------------------------------------------------------

    private TrackedEntityInstance getTrackedEntityInstance( ProgramMessage message )
    {
        TrackedEntityInstance tei = null;

        if ( message.getRecipients().getTrackedEntityInstance() == null )
        {
            return tei;
        }

        String teiUid = message.getRecipients().getTrackedEntityInstance().getUid();

        tei = trackedEntityInstanceService.getTrackedEntityInstance( teiUid );

        message.getRecipients().setTrackedEntityInstance( tei );

        return tei;
    }

    private OrganisationUnit getOrganisationUnit( ProgramMessage message )
    {
        OrganisationUnit orgUnit = null;

        if ( message.getRecipients().getOrganisationUnit() == null )
        {
            return orgUnit;
        }

        String ou = message.getRecipients().getOrganisationUnit().getUid();

        orgUnit = organisationUnitService.getOrganisationUnit( ou );

        message.getRecipients().setOrganisationUnit( orgUnit );

        return orgUnit;
    }
}
