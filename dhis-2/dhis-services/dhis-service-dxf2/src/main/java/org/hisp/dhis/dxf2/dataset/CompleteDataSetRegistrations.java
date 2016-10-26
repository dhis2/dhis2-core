package org.hisp.dhis.dxf2.dataset;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Halvdan Hoem Grelland
 */
@JacksonXmlRootElement( localName = "completeDataSetRegistrations", namespace = DxfNamespaces.DXF_2_0 )
public class CompleteDataSetRegistrations
{
    //--------------------------------------------------------------------------
    // Options
    //--------------------------------------------------------------------------

    protected String idScheme;

    protected String dataSetIdScheme;

    protected String orgUnitIdScheme;

    protected String attributeOptionComboIdScheme;

    protected String period;

    protected Boolean dryRun;

    protected String strategy;

    //--------------------------------------------------------------------------
    // Properties
    //--------------------------------------------------------------------------

    protected List<CompleteDataSetRegistration> completeDataSetRegistrations = new ArrayList<>();

    //--------------------------------------------------------------------------
    // Constructors
    //--------------------------------------------------------------------------

    public CompleteDataSetRegistrations()
    {
    }

    //--------------------------------------------------------------------------
    // Logic
    //--------------------------------------------------------------------------

    private Iterator<CompleteDataSetRegistration> itemIterator;

    private void refreshIterator()
    {
        itemIterator = completeDataSetRegistrations.iterator();
    }

    public boolean hasNextCompleteDataSetRegistration()
    {
        if ( itemIterator == null )
        {
            refreshIterator();
        }

        return itemIterator.hasNext();
    }

    public CompleteDataSetRegistration getNextCompleteDataSetRegistration()
    {
        if ( itemIterator == null )
        {
            refreshIterator();
        }

        return itemIterator.next();
    }

    public CompleteDataSetRegistration getCompleteDataSetRegistrationInstance()
    {
        return new CompleteDataSetRegistration();
    }

    public void open()
    {
    }

    public void close()
    {
    }

    public IdScheme getIdSchemeProperty()
    {
        return IdScheme.from( getIdScheme() );
    }

    public IdScheme getDataSetIdSchemeProperty()
    {
        return IdScheme.from( StringUtils.defaultIfBlank( getDataSetIdScheme(), getIdScheme() ) );
    }

    public IdScheme getOrgUnitIdSchemeProperty()
    {
        return IdScheme.from( StringUtils.defaultIfBlank( getOrgUnitIdScheme(), getIdScheme() ) );
    }

    public IdScheme getAttributeOptionComboIdSchemeProperty()
    {
        return IdScheme.from( StringUtils.defaultIfBlank( getAttributeOptionComboIdScheme(), getIdScheme() ) );
    }

    @Override public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "completeDataSetRegistrations", completeDataSetRegistrations )
            .add( "idScheme", idScheme )
            .add( "dataSetIdScheme", dataSetIdScheme )
            .add( "orgUnitIdScheme", orgUnitIdScheme )
            .add( "attributeOptionComboIdScheme", attributeOptionComboIdScheme )
            .add( "period", period )
            .add( "dryRun", dryRun )
            .add( "strategy", strategy )
            .toString();
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( localName = "completeDataSetRegistration", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlElementWrapper( localName = "completeDataSetRegistrations", useWrapping = false, namespace = DxfNamespaces.DXF_2_0 )
    public List<CompleteDataSetRegistration> getCompleteDataSetRegistrations()
    {
        return completeDataSetRegistrations;
    }

    public void setCompleteDataSetRegistrations( List<CompleteDataSetRegistration> completeDataSetRegistrations )
    {
        this.completeDataSetRegistrations = completeDataSetRegistrations;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getIdScheme()
    {
        return idScheme;
    }

    public CompleteDataSetRegistrations setIdScheme( String idScheme )
    {
        this.idScheme = idScheme;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getDataSetIdScheme()
    {
        return dataSetIdScheme;
    }

    public CompleteDataSetRegistrations setDataSetIdScheme( String dataSetIdScheme )
    {
        this.dataSetIdScheme = dataSetIdScheme;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getOrgUnitIdScheme()
    {
        return orgUnitIdScheme;
    }

    public CompleteDataSetRegistrations setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        this.orgUnitIdScheme = orgUnitIdScheme;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getAttributeOptionComboIdScheme()
    {
        return attributeOptionComboIdScheme;
    }

    public CompleteDataSetRegistrations setAttributeOptionComboIdScheme( String attributeOptionComboIdScheme )
    {
        this.attributeOptionComboIdScheme = attributeOptionComboIdScheme;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getPeriod()
    {
        return period;
    }

    public CompleteDataSetRegistrations setPeriod( String period )
    {
        this.period = period;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public Boolean getDryRun()
    {
        return dryRun;
    }

    public CompleteDataSetRegistrations setDryRun( Boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getStrategy()
    {
        return strategy;
    }

    public CompleteDataSetRegistrations setStrategy( String strategy )
    {
        this.strategy = strategy;
        return this;
    }
}
