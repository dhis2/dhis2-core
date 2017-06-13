package org.hisp.dhis.outboundmessage;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.DeliveryChannel;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */

@JacksonXmlRootElement( localName = "messageResponseSummary", namespace = DxfNamespaces.DXF_2_0 )
public class OutboundMessageResponseSummary
{
    private int total;
    
    private int failed;
    
    private int pending;
    
    private int sent;
    
    private OutboundMessageBatchStatus batchStatus;
    
    private String responseMessage;
    
    private String errorMessage;
    
    private DeliveryChannel channel;
    
    public OutboundMessageResponseSummary()
    {
    }
    
    public OutboundMessageResponseSummary( String errorMessage, DeliveryChannel channel, OutboundMessageBatchStatus status )
    {
        this.errorMessage = errorMessage;
        this.channel = channel;
        this.batchStatus = status;
    }

    @JsonProperty( value = "total" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getTotal()
    {
        return total;
    }

    public void setTotal( int total )
    {
        this.total = total;
    }

    @JsonProperty( value = "failed" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getFailed()
    {
        return failed;
    }

    public void setFailed( int failed )
    {
        this.failed = failed;
    }

    @JsonProperty( value = "pending" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getPending()
    {
        return pending;
    }

    public void setPending( int pending )
    {
        this.pending = pending;
    }

    @JsonProperty( value = "sent" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getSent()
    {
        return sent;
    }

    public void setSent( int sent )
    {
        this.sent = sent;
    }

    @JsonProperty( value = "status" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OutboundMessageBatchStatus getBatchStatus()
    {
        return batchStatus;
    }

    public void setBatchStatus( OutboundMessageBatchStatus batchStatus )
    {
        this.batchStatus = batchStatus;
    }

    @JsonProperty( value = "responseMessage" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getResponseMessage()
    {
        return responseMessage;
    }

    public void setResponseMessage( String responseMessage )
    {
        this.responseMessage = responseMessage;
    }

    @JsonProperty( value = "errorMessage" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }

    @JsonProperty( value = "batchType" )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DeliveryChannel getChannel()
    {
        return channel;
    }

    public void setChannel( DeliveryChannel channel )
    {
        this.channel = channel;
    }

    @Override public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "total", total )
            .add( "failed", failed )
            .add( "pending", pending )
            .add( "sent", sent )
            .add( "batchStatus", batchStatus )
            .add( "responseMessage", responseMessage )
            .add( "errorMessage", errorMessage )
            .add( "channel", channel )
            .toString();
    }
}
