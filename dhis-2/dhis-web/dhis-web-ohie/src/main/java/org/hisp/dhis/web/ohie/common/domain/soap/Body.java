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

import org.hisp.dhis.web.ohie.csd.domain.GetModificationsRequest;
import org.hisp.dhis.web.ohie.csd.domain.GetModificationsResponse;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "Body", namespace = "http://www.w3.org/2003/05/soap-envelope" )
public class Body
{
    @XmlElement( name = "getModificationsRequest", namespace = "urn:ihe:iti:csd:2013" )
    private GetModificationsRequest getModificationsRequest;

    @XmlElement( name = "getModificationsResponse", namespace = "urn:ihe:iti:csd:2013" )
    private GetModificationsResponse getModificationsResponse;

    @XmlElement( name = "Fault", namespace = "http://www.w3.org/2003/05/soap-envelope" )
    private Fault fault;

    public Body()
    {
    }

    public GetModificationsRequest getGetModificationsRequest()
    {
        return getModificationsRequest;
    }

    public void setGetModificationsRequest( GetModificationsRequest getModificationsRequest )
    {
        this.getModificationsRequest = getModificationsRequest;
    }

    public GetModificationsResponse getGetModificationsResponse()
    {
        return getModificationsResponse;
    }

    public void setGetModificationsResponse( GetModificationsResponse getModificationsResponse )
    {
        this.getModificationsResponse = getModificationsResponse;
    }

    public Fault getFault()
    {
        return fault;
    }

    public void setFault( Fault fault )
    {
        this.fault = fault;
    }
}
