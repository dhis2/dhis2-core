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

import java.util.Set;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public abstract class DeliveryChannelStrategy
{
    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected TrackedEntityInstanceService trackedEntityInstanceService;

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract DeliveryChannel getDeliveryChannel();

    protected abstract ProgramMessage setAttributes( ProgramMessage message );

    protected abstract void validate( ProgramMessage message );

    protected abstract String getOrganisationUnitRecipient( OrganisationUnit orgUnit );
    
    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------
    
    public String getTrackedEntityInstanceRecipient( TrackedEntityInstance tei, ValueType type )
    {
        Set<TrackedEntityAttributeValue> attributeValues = tei.getTrackedEntityAttributeValues();

        for ( TrackedEntityAttributeValue value : attributeValues )
        {
            if ( value != null && value.getAttribute().getValueType().equals( type ) &&
                value.getPlainValue() != null && !value.getPlainValue().trim().isEmpty() )
            {
                return value.getPlainValue();
            }
        }

        throw new IllegalQueryException( "Tracked entity does not have any attribute of value type: " + type.toString() );
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------
    
    protected TrackedEntityInstance getTrackedEntityInstance( ProgramMessage message )
    {
        if ( message.getRecipients().getTrackedEntityInstance() == null )
        {
            return null;
        }

        String uid = message.getRecipients().getTrackedEntityInstance().getUid();

        TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( uid );

        message.getRecipients().setTrackedEntityInstance( tei );

        return tei;
    }

    protected OrganisationUnit getOrganisationUnit( ProgramMessage message )
    {
        if ( message.getRecipients().getOrganisationUnit() == null )
        {
            return null;
        }

        String uid = message.getRecipients().getOrganisationUnit().getUid();

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( uid );

        message.getRecipients().setOrganisationUnit( orgUnit );

        return orgUnit;
    }
}
