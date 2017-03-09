package org.hisp.dhis.web.ohie.csd.domain;

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "name", namespace = "urn:ihe:iti:csd:2013" )
public class Name
{
    @XmlElement( name = "commonName", namespace = "urn:ihe:iti:csd:2013", required = true )
    private List<CommonName> commonNames = new ArrayList<>();

    @XmlElement( name = "honorific", namespace = "urn:ihe:iti:csd:2013" )
    private String honorific;

    @XmlElement( name = "forename", namespace = "urn:ihe:iti:csd:2013" )
    private String forename;

    @XmlElement( name = "otherNames", namespace = "urn:ihe:iti:csd:2013" )
    private List<String> otherNames = new ArrayList<>();

    @XmlElement( name = "surname", namespace = "urn:ihe:iti:csd:2013" )
    private String surname;

    @XmlElement( name = "suffix", namespace = "urn:ihe:iti:csd:2013" )
    private String suffix;

    public Name()
    {
    }

    public Name( CommonName commonName )
    {
        getCommonNames().add( commonName );
    }

    public List<CommonName> getCommonNames()
    {
        return commonNames;
    }

    public void setCommonNames( List<CommonName> commonNames )
    {
        this.commonNames = commonNames;
    }

    public String getHonorific()
    {
        return honorific;
    }

    public void setHonorific( String honorific )
    {
        this.honorific = honorific;
    }

    public String getForename()
    {
        return forename;
    }

    public void setForename( String forename )
    {
        this.forename = forename;
    }

    public List<String> getOtherNames()
    {
        return otherNames;
    }

    public void setOtherNames( List<String> otherNames )
    {
        this.otherNames = otherNames;
    }

    public String getSurname()
    {
        return surname;
    }

    public void setSurname( String surname )
    {
        this.surname = surname;
    }

    public String getSuffix()
    {
        return suffix;
    }

    public void setSuffix( String suffix )
    {
        this.suffix = suffix;
    }
}
