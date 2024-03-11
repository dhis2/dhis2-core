package org.hisp.dhis.option;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.VersionedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "optionSet", namespace = DxfNamespaces.DXF_2_0 )
public class OptionSet
    extends BaseIdentifiableObject
    implements VersionedObject, MetadataObject
{
    private List<Option> options = new ArrayList<>();

    private ValueType valueType;

    private int version;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public OptionSet()
    {
    }

    public OptionSet( String name, ValueType valueType )
    {
        this.name = name;
        this.valueType = valueType;
    }

    public OptionSet( String name, ValueType valueType, List<Option> options )
    {
        this.name = name;
        this.valueType = valueType;
        this.options = options;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    public void addOption( Option option )
    {
        if ( option.getSortOrder() == null )
        {
            this.options.add( option );
        }
        else
        {
            boolean added = false;
            final int size = this.options.size();
            for ( int i = 0; i < size; i++ )
            {
                Option thisOption = this.options.get( i );
                if ( thisOption.getSortOrder() == null || thisOption.getSortOrder() > option.getSortOrder() )
                {
                    this.options.add( i, option );
                    added = true;
                    break;
                }
            }
            if ( !added )
            {
                this.options.add( option );
            }
        }
        option.setOptionSet( this );
    }

    public void removeAllOptions()
    {
        options.clear();
    }

    @Override
    public int increaseVersion()
    {
        return ++version;
    }

    public List<String> getOptionValues()
    {
        return options.stream().map( Option::getName ).collect( Collectors.toList() );
    }

    public List<String> getOptionCodes()
    {
        return options.stream().map( Option::getCode ).collect( Collectors.toList() );
    }

    public Set<String> getOptionCodesAsSet()
    {
        return options.stream().map( Option::getCode ).collect( Collectors.toSet() );
    }

    public Option getOptionByCode( String code )
    {
        for ( Option option : options )
        {
            if ( option != null && option.getCode().equals( code ) )
            {
                return option;
            }
        }

        return null;
    }

    public Map<String, String> getOptionCodePropertyMap( IdScheme idScheme )
    {
        return options.stream().collect( Collectors.toMap( Option::getCode, o -> o.getPropertyValue( idScheme ) ) );
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( contentAs = BaseIdentifiableObject.class )
    @JacksonXmlElementWrapper( localName = "options", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "option", namespace = DxfNamespaces.DXF_2_0 )
    public List<Option> getOptions()
    {
        return options;
    }

    public void setOptions( List<Option> options )
    {
        this.options = options;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType( ValueType valueType )
    {
        this.valueType = valueType;
    }

    @Override
    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getVersion()
    {
        return version;
    }

    @Override
    public void setVersion( int version )
    {
        this.version = version;
    }
}
