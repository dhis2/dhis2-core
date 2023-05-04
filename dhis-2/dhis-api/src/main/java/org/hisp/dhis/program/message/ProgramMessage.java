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
package org.hisp.dhis.program.message;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Getter
@Setter
@Builder( builderClassName = "ProgramMessageBuilder" )
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement( localName = "programMessage", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramMessage
    extends BaseIdentifiableObject
    implements Serializable
{
    private static final long serialVersionUID = -5882823752156937730L;

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    private Enrollment enrollment;

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    private Event event;

    @JsonProperty
    private ProgramMessageRecipients recipients;

    @JsonProperty
    private Set<DeliveryChannel> deliveryChannels = new HashSet<>();

    @JsonProperty
    private ProgramMessageStatus messageStatus;

    @JsonProperty
    private String notificationTemplate;

    @JsonProperty
    private String subject;

    @JsonProperty
    private String text;

    @JsonProperty
    private Date processedDate;

    @JsonProperty
    private transient boolean storeCopy = true;

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public boolean hasEnrollment()
    {
        return this.enrollment != null;
    }

    public boolean hasEvent()
    {
        return this.event != null;
    }

    @JsonPOJOBuilder( withPrefix = "" )
    public static final class ProgramMessageBuilder
    {
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "uid", uid )
            .add( "event", event )
            .add( "enrollment", enrollment )
            .add( "recipients", recipients )
            .add( "delivery channels", deliveryChannels )
            .add( "subject", subject )
            .add( "text", text )
            .toString();
    }
}
