/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.json;

import java.time.LocalDateTime;

import org.hisp.dhis.metadata.MetadataProposalStatus;
import org.hisp.dhis.metadata.MetadataProposalTarget;
import org.hisp.dhis.metadata.MetadataProposalType;
import org.hisp.dhis.webapi.json.Expected;
import org.hisp.dhis.webapi.json.JsonDate;
import org.hisp.dhis.webapi.json.JsonObject;

/**
 * JSON representation of a {@link org.hisp.dhis.metadata.MetadataProposal} as
 * returned by the REST API.
 *
 * @author Jan Bernitt
 */
public interface JsonMetadataProposal extends JsonObject
{
    @Expected
    default String getId()
    {
        return getString( "id" ).string();
    }

    @Expected
    default MetadataProposalType getType()
    {
        return getString( "type" ).parsed( MetadataProposalType::valueOf );
    }

    @Expected
    default MetadataProposalStatus getStatus()
    {
        return getString( "status" ).parsed( MetadataProposalStatus::valueOf );
    }

    @Expected
    default MetadataProposalTarget getTarget()
    {
        return getString( "target" ).parsed( MetadataProposalTarget::valueOf );
    }

    default String getTargetUid()
    {
        return getString( "targetUid" ).string();
    }

    default JsonObject getChange()
    {
        return getObject( "change" );
    }

    default String getComment()
    {
        return getString( "comment" ).string();
    }

    @Expected
    default String getCreatedBy()
    {
        return getString( "createdBy" ).string();
    }

    default String getAcceptedBy()
    {
        return getString( "acceptedBy" ).string();
    }

    @Expected
    default LocalDateTime getCreated()
    {
        return get( "created", JsonDate.class ).date();
    }
}
