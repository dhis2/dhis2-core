/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.datavalueset;

import java.util.Set;

import lombok.AllArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.staxwax.reader.XMLReader;

import com.google.common.collect.Sets;

/**
 * Reads {@link DataValueSet} from XML input.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public class XmlDataValueSetReader implements DataValueSetReader, AttributeOptionDataValueEntry
{
    private final XMLReader reader;

    @Override
    public DataValueSet readHeader()
    {
        reader.moveToStartElement( "dataValueSet" );
        DataValueSet header = new DataValueSet();
        header.setIdScheme( reader.getAttributeValue( "idScheme" ) );
        header.setDataElementIdScheme( reader.getAttributeValue( "dataElementIdScheme" ) );
        header.setOrgUnitIdScheme( reader.getAttributeValue( "orgUnitIdScheme" ) );
        header.setCategoryOptionComboIdScheme( reader.getAttributeValue( "categoryOptionComboIdScheme" ) );
        header.setDataSetIdScheme( reader.getAttributeValue( "dataSetIdScheme" ) );
        header.setDryRun( "true".equals( reader.getAttributeValue( "dryRun" ) ) ? true : null );
        header.setStrategy( reader.getAttributeValue( "importStrategy" ) );
        header.setDataSet( reader.getAttributeValue( "dataSet" ) );
        header.setCompleteDate( reader.getAttributeValue( "completeDate" ) );
        header.setPeriod( reader.getAttributeValue( "period" ) );
        header.setOrgUnit( reader.getAttributeValue( "orgUnit" ) );
        header.setAttributeOptionCombo( reader.getAttributeValue( "attributeOptionCombo" ) );
        return header;
    }

    @Override
    public AttributeOptionDataValueEntry readNext()
    {
        return reader.moveToStartElement( "dataValue", "dataValueSet" ) ? this : null;
    }

    @Override
    public void close()
    {
        reader.closeReader();
    }

    /*
     * When used as DataValueEntry
     */

    @Override
    public String getDataElement()
    {
        return getString( "dataElement" );
    }

    @Override
    public String getPeriod()
    {
        return getString( "period" );
    }

    @Override
    public String getOrgUnit()
    {
        return getString( "orgUnit" );
    }

    @Override
    public String getCategoryOptionCombo()
    {
        return getString( "categoryOptionCombo" );
    }

    @Override
    public String getAttributeOptionCombo()
    {
        return getString( "attributeOptionCombo" );
    }

    @Override
    public String getValue()
    {
        return getString( "value" );
    }

    @Override
    public String getStoredBy()
    {
        return getString( "storedBy" );
    }

    @Override
    public String getCreated()
    {
        return getString( "created" );
    }

    @Override
    public String getLastUpdated()
    {
        return getString( "lastUpdated" );
    }

    @Override
    public String getComment()
    {
        return getString( "comment" );
    }

    @Override
    public boolean getFollowup()
    {
        return Boolean.parseBoolean( getString( "followUp" ) );
    }

    @Override
    public Boolean getDeleted()
    {
        return Boolean.valueOf( getString( "deleted" ) );
    }

    @Override
    public String getCategoryCombo()
    {
        return getString( "categoryCombo" );
    }

    @Override
    public Set<String> getAttributeCategoryOptions()
    {
        String options = getString( "attributeCategoryOptions" );
        return !StringUtils.isEmpty( options ) ? Sets.newHashSet( options.split( "," ) ) : null;
    }

    private String getString( String name )
    {
        return reader.getAttributeValue( name );
    }
}
