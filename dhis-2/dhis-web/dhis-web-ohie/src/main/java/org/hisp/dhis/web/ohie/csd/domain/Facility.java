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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@XmlAccessorType( XmlAccessType.FIELD )
@XmlType( name = "facility", namespace = "urn:ihe:iti:csd:2013" )
public class Facility
{
    @XmlAttribute( name = "oid" )
    private String oid;

    @XmlElement( name = "otherID", namespace = "urn:ihe:iti:csd:2013" )
    private List<OtherID> otherID = new ArrayList<>();

    @XmlElement( name = "codedType", namespace = "urn:ihe:iti:csd:2013" )
    private List<CodedType> codedTypes = new ArrayList<>();

    @XmlElement( name = "primaryName", namespace = "urn:ihe:iti:csd:2013" )
    private String primaryName;

    @XmlElement( name = "otherName", namespace = "urn:ihe:iti:csd:2013" )
    private List<Name> otherName = new ArrayList<>();

    @XmlElement( name = "address", namespace = "urn:ihe:iti:csd:2013" )
    private List<Address> addresses = new ArrayList<>();

    @XmlElement( name = "contact", namespace = "urn:ihe:iti:csd:2013" )
    private List<Contact> contacts = new ArrayList<>();

    @XmlElement( name = "geocode", namespace = "urn:ihe:iti:csd:2013" )
    private Geocode geocode;

    @XmlElement( name = "contactPoint", namespace = "urn:ihe:iti:csd:2013" )
    private List<ContactPoint> contactPoints = new ArrayList<>();

    @XmlElement( name = "organization", namespace = "urn:ihe:iti:csd:2013" )
    @XmlElementWrapper( name = "organizations", namespace = "urn:ihe:iti:csd:2013" )
    private List<Organization> organizations = new ArrayList<>();

    @XmlElement( name = "operatingHours", namespace = "urn:ihe:iti:csd:2013" )
    private List<OperatingHours> operatingHours = new ArrayList<>();

    @XmlElement( name = "record", namespace = "urn:ihe:iti:csd:2013" )
    private Record record;

    public Facility()
    {
    }

    public String getOid()
    {
        return oid;
    }

    public void setOid( String oid )
    {
        this.oid = oid;
    }

    public List<OtherID> getOtherID()
    {
        return otherID;
    }

    public void setOtherID( List<OtherID> otherID )
    {
        this.otherID = otherID;
    }

    public List<CodedType> getCodedTypes()
    {
        return codedTypes;
    }

    public void setCodedTypes( List<CodedType> codedTypes )
    {
        this.codedTypes = codedTypes;
    }

    public String getPrimaryName()
    {
        return primaryName;
    }

    public void setPrimaryName( String primaryName )
    {
        this.primaryName = primaryName;
    }

    public List<Name> getOtherName()
    {
        return otherName;
    }

    public void setOtherName( List<Name> otherName )
    {
        this.otherName = otherName;
    }

    public List<Address> getAddresses()
    {
        return addresses;
    }

    public void setAddresses( List<Address> addresses )
    {
        this.addresses = addresses;
    }

    public List<Contact> getContacts()
    {
        return contacts;
    }

    public void setContacts( List<Contact> contacts )
    {
        this.contacts = contacts;
    }

    public Geocode getGeocode()
    {
        return geocode;
    }

    public void setGeocode( Geocode geocode )
    {
        this.geocode = geocode;
    }

    public List<ContactPoint> getContactPoints()
    {
        return contactPoints;
    }

    public void setContactPoints( List<ContactPoint> contactPoints )
    {
        this.contactPoints = contactPoints;
    }

    public List<Organization> getOrganizations()
    {
        return organizations;
    }

    public void setOrganizations( List<Organization> organizations )
    {
        this.organizations = organizations;
    }

    public List<OperatingHours> getOperatingHours()
    {
        return operatingHours;
    }

    public void setOperatingHours( List<OperatingHours> operatingHours )
    {
        this.operatingHours = operatingHours;
    }

    public Record getRecord()
    {
        return record;
    }

    public void setRecord( Record record )
    {
        this.record = record;
    }
}
