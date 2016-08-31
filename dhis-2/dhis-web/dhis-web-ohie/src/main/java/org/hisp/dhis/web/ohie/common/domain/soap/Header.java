package org.hisp.dhis.web.ohie.common.domain.soap;

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

import org.hisp.dhis.web.ohie.common.domain.wsa.Action;
import org.hisp.dhis.web.ohie.common.domain.wsa.MessageID;
import org.hisp.dhis.web.ohie.common.domain.wsa.RelatesTo;
import org.hisp.dhis.web.ohie.common.domain.wsa.ReplyTo;
import org.hisp.dhis.web.ohie.common.domain.wsa.To;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "Header", namespace = "http://www.w3.org/2003/05/soap-envelope" )
public class Header
{
    @XmlElement( name = "Action", namespace = "http://www.w3.org/2005/08/addressing" )
    private Action action = new Action();

    @XmlElement( name = "MessageID", namespace = "http://www.w3.org/2005/08/addressing" )
    private MessageID messageID = new MessageID();

    @XmlElement( name = "ReplyTo", namespace = "http://www.w3.org/2005/08/addressing" )
    private ReplyTo replyTo;

    @XmlElement( name = "To", namespace = "http://www.w3.org/2005/08/addressing" )
    private To to = new To();

    @XmlElement( name = "RelatesTo", namespace = "http://www.w3.org/2005/08/addressing" )
    private RelatesTo relatesTo;

    public Header()
    {
    }

    public Action getAction()
    {
        return action;
    }

    public void setAction( Action action )
    {
        this.action = action;
    }

    public MessageID getMessageID()
    {
        return messageID;
    }

    public void setMessageID( MessageID messageID )
    {
        this.messageID = messageID;
    }

    public ReplyTo getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo( ReplyTo replyTo )
    {
        this.replyTo = replyTo;
    }

    public To getTo()
    {
        return to;
    }

    public void setTo( To to )
    {
        this.to = to;
    }

    public RelatesTo getRelatesTo()
    {
        return relatesTo;
    }

    public void setRelatesTo( RelatesTo relatesTo )
    {
        this.relatesTo = relatesTo;
    }
}
