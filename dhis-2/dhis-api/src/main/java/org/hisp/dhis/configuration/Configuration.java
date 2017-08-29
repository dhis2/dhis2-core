package org.hisp.dhis.configuration;

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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.adapter.JacksonPeriodTypeSerializer;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.indicator.IndicatorGroup;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.annotation.Property;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "configuration", namespace = DxfNamespaces.DXF_2_0 )
public class Configuration
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 936186436040704261L;
    
    private static final PeriodType DEFAULT_INFRASTRUCTURAL_PERIODTYPE = new YearlyPeriodType();
    
    private int id;

    // -------------------------------------------------------------------------
    // Various
    // -------------------------------------------------------------------------

    private String systemId;
    
    private UserGroup feedbackRecipients;
    
    private OrganisationUnitLevel offlineOrganisationUnitLevel;

    private IndicatorGroup infrastructuralIndicators;

    private DataElementGroup infrastructuralDataElements;
    
    private PeriodType infrastructuralPeriodType;
    
    private UserAuthorityGroup selfRegistrationRole;
    
    private OrganisationUnit selfRegistrationOrgUnit;
    
    private Set<String> corsWhitelist = new HashSet<>();
    
    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public Configuration()
    {
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public PeriodType getInfrastructuralPeriodTypeDefaultIfNull()
    {
        return infrastructuralPeriodType != null ? infrastructuralPeriodType : DEFAULT_INFRASTRUCTURAL_PERIODTYPE;
    }
    
    public boolean selfRegistrationAllowed()
    {
        return selfRegistrationRole != null && selfRegistrationOrgUnit != null;
    }
    
    // -------------------------------------------------------------------------
    // Set and get methods
    // -------------------------------------------------------------------------

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }
    
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getSystemId()
    {
        return systemId;
    }

    public void setSystemId( String systemId )
    {
        this.systemId = systemId;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserGroup getFeedbackRecipients()
    {
        return feedbackRecipients;
    }

    public void setFeedbackRecipients( UserGroup feedbackRecipients )
    {
        this.feedbackRecipients = feedbackRecipients;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnitLevel getOfflineOrganisationUnitLevel()
    {
        return offlineOrganisationUnitLevel;
    }
    
    public void setOfflineOrganisationUnitLevel( OrganisationUnitLevel offlineOrganisationUnitLevel )
    {
        this.offlineOrganisationUnitLevel = offlineOrganisationUnitLevel;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public IndicatorGroup getInfrastructuralIndicators()
    {
        return infrastructuralIndicators;
    }

    public void setInfrastructuralIndicators( IndicatorGroup infrastructuralIndicators )
    {
        this.infrastructuralIndicators = infrastructuralIndicators;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DataElementGroup getInfrastructuralDataElements()
    {
        return infrastructuralDataElements;
    }

    public void setInfrastructuralDataElements( DataElementGroup infrastructuralDataElements )
    {
        this.infrastructuralDataElements = infrastructuralDataElements;
    }

    @JsonProperty
    @JsonSerialize( using = JacksonPeriodTypeSerializer.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @Property( PropertyType.TEXT )
    public PeriodType getInfrastructuralPeriodType()
    {
        return infrastructuralPeriodType;
    }

    public void setInfrastructuralPeriodType( PeriodType infrastructuralPeriodType )
    {
        this.infrastructuralPeriodType = infrastructuralPeriodType;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public UserAuthorityGroup getSelfRegistrationRole()
    {
        return selfRegistrationRole;
    }

    public void setSelfRegistrationRole( UserAuthorityGroup selfRegistrationRole )
    {
        this.selfRegistrationRole = selfRegistrationRole;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public OrganisationUnit getSelfRegistrationOrgUnit()
    {
        return selfRegistrationOrgUnit;
    }

    public void setSelfRegistrationOrgUnit( OrganisationUnit selfRegistrationOrgUnit )
    {
        this.selfRegistrationOrgUnit = selfRegistrationOrgUnit;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Set<String> getCorsWhitelist()
    {
        return corsWhitelist;
    }

    public void setCorsWhitelist( Set<String> corsWhitelist )
    {
        this.corsWhitelist = corsWhitelist;
    }
}
