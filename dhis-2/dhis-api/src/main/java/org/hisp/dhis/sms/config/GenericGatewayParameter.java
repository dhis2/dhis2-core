/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.sms.config;

import java.io.Serializable;
import java.util.Objects;

<<<<<<< HEAD
import com.fasterxml.jackson.annotation.JsonView;
import org.hisp.dhis.sms.config.views.SmsConfigurationViews;
=======
import org.hisp.dhis.sms.config.views.SmsConfigurationViews;

import com.fasterxml.jackson.annotation.JsonView;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
public class GenericGatewayParameter
    implements Serializable
{
    private static final long serialVersionUID = -863990758156009672L;

    @JsonView( SmsConfigurationViews.Public.class )
    private String key;

    @JsonView( SmsConfigurationViews.Public.class )
    private String value;

    @JsonView( SmsConfigurationViews.Public.class )
    private boolean header;

    @JsonView( SmsConfigurationViews.Public.class )
    private boolean encode;

    @JsonView( SmsConfigurationViews.Public.class )
    private boolean confidential;

    @JsonView( SmsConfigurationViews.Public.class )
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public String getValue()
    {
<<<<<<< HEAD
        return confidential ? "" : value;
    }

    public String getDisplayValue()
    {
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public boolean isConfidential()
    {
        return confidential;
    }

    public void setConfidential( boolean confidential )
    {
        this.confidential = confidential;
    }

    public boolean isHeader()
    {
        return header;
    }

    public void setHeader( boolean header )
    {
        this.header = header;
    }

    public boolean isEncode()
    {
        return encode;
    }

    public void setEncode( boolean encode )
    {
        this.encode = encode;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        GenericGatewayParameter parameter = (GenericGatewayParameter) o;
        return header == parameter.header &&
            encode == parameter.encode &&
            confidential == parameter.confidential &&
            Objects.equals( key, parameter.key ) &&
            Objects.equals( value, parameter.value );
    }

    @Override
    public int hashCode()
    {

        return Objects.hash( key, value, header, encode, confidential );
    }
}
