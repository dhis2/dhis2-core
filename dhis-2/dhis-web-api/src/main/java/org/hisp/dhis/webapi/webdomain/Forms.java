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
package org.hisp.dhis.webapi.webdomain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.webapi.webdomain.form.Form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.collect.Maps;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "forms", namespace = DxfNamespaces.DXF_2_0 )
public class Forms
{
    /**
     * Maps ou.uid => org unit.
     */
    private Map<String, FormOrganisationUnit> organisationUnits = new HashMap<>();

    /**
     * Maps dataSet.uid => form instance.
     */
    private Map<String, Form> forms = new HashMap<>();

    private Map<String, List<String>> optionSets = Maps.newHashMap();

    public Forms()
    {
    }

    @JsonProperty( value = "organisationUnits" )
    @JacksonXmlElementWrapper( localName = "organisationUnits", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "organisationUnit", namespace = DxfNamespaces.DXF_2_0 )
    public Map<String, FormOrganisationUnit> getOrganisationUnits()
    {
        return organisationUnits;
    }

    public void setOrganisationUnits( Map<String, FormOrganisationUnit> organisationUnits )
    {
        this.organisationUnits = organisationUnits;
    }

    @JsonProperty( value = "forms" )
    @JacksonXmlElementWrapper( localName = "forms", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "form", namespace = DxfNamespaces.DXF_2_0 )
    public Map<String, Form> getForms()
    {
        return forms;
    }

    public void setForms( Map<String, Form> forms )
    {
        this.forms = forms;
    }

    @JsonProperty( value = "optionSets" )
    @JacksonXmlElementWrapper( localName = "optionSets", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "optionSet", namespace = DxfNamespaces.DXF_2_0 )
    public Map<String, List<String>> getOptionSets()
    {
        return optionSets;
    }

    public void setOptionSets( Map<String, List<String>> optionSets )
    {
        this.optionSets = optionSets;
    }
}
