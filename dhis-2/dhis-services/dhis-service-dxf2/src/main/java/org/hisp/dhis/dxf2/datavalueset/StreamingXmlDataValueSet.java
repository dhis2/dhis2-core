package org.hisp.dhis.dxf2.datavalueset;

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

import org.amplecode.staxwax.reader.XMLReader;
import org.amplecode.staxwax.writer.XMLWriter;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalue.StreamingDataValue;

/**
 * @author Lars Helge Overland
 */
public class StreamingXmlDataValueSet
    extends DataValueSet
{
    private static final String XMLNS = "xmlns";
    private static final String NS = "http://dhis2.org/schema/dxf/2.0";
    private static final String TRUE = "true";

    private XMLWriter writer;

    private XMLReader reader;

    //--------------------------------------------------------------------------
    // Constructor
    //--------------------------------------------------------------------------

    public StreamingXmlDataValueSet( XMLWriter writer )
    {
        this.writer = writer;

        this.writer.openDocument();
        this.writer.openElement( FIELD_DATAVALUESET );
        this.writer.writeAttribute( XMLNS, NS );
    }

    public StreamingXmlDataValueSet( XMLReader reader )
    {
        this.reader = reader;
        this.reader.moveToStartElement( FIELD_DATAVALUESET );
    }

    //--------------------------------------------------------------------------
    // Getters
    //--------------------------------------------------------------------------

    @Override
    public String getIdScheme()
    {
        return idScheme = idScheme == null ? reader.getAttributeValue( FIELD_IDSCHEME ) : idScheme;
    }
    
    @Override
    public String getDataElementIdScheme()
    {
        return dataElementIdScheme = dataElementIdScheme == null ? reader.getAttributeValue( FIELD_DATAELEMENTIDSCHEME ) : dataElementIdScheme;
    }

    @Override
    public String getOrgUnitIdScheme()
    {
        return orgUnitIdScheme = orgUnitIdScheme == null ? reader.getAttributeValue( FIELD_ORGUNITIDSCHEME ) : orgUnitIdScheme;
    }

    @Override
    public Boolean getDryRun()
    {
        return dryRun = dryRun == null ? ( TRUE.equals( reader.getAttributeValue( FIELD_DRYRUN ) ) ? Boolean.TRUE : null ) : dryRun;
    }

    @Override
    public String getStrategy()
    {
        return strategy = strategy == null ? reader.getAttributeValue( FIELD_IMPORTSTRATEGY ) : strategy;
    }

    @Override
    public String getDataSet()
    {
        return dataSet = dataSet == null ? reader.getAttributeValue( FIELD_DATASET ) : dataSet;
    }

    @Override
    public String getCompleteDate()
    {
        return completeDate = completeDate == null ? reader.getAttributeValue( FIELD_COMPLETEDATE ) : completeDate;
    }

    @Override
    public String getPeriod()
    {
        return period = period == null ? reader.getAttributeValue( FIELD_PERIOD ) : period;
    }

    @Override
    public String getOrgUnit()
    {
        return orgUnit = orgUnit == null ? reader.getAttributeValue( FIELD_ORGUNIT ) : orgUnit;
    }

    @Override
    public String getAttributeOptionCombo()
    {
        return attributeOptionCombo = attributeOptionCombo == null ? reader.getAttributeValue( FIELD_ATTRIBUTE_OPTION_COMBO ) : attributeOptionCombo;
    }
    
    @Override
    public boolean hasNextDataValue()
    {
        return reader.moveToStartElement( FIELD_DATAVALUE, FIELD_DATAVALUESET );
    }

    @Override
    public DataValue getNextDataValue()
    {
        return new StreamingDataValue( reader );
    }

    //--------------------------------------------------------------------------
    // Setters
    //--------------------------------------------------------------------------

    @Override
    public void setIdScheme( String idScheme )
    {
        writer.writeAttribute( FIELD_IDSCHEME, idScheme );
    }
    
    @Override
    public void setDataElementIdScheme( String dataElementIdScheme )
    {
        writer.writeAttribute( FIELD_DATAELEMENTIDSCHEME, dataElementIdScheme );
    }

    @Override
    public void setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        writer.writeAttribute( FIELD_ORGUNITIDSCHEME, orgUnitIdScheme );
    }

    @Override
    public void setDataSet( String dataSet )
    {
        writer.writeAttribute( FIELD_DATASET, dataSet );
    }

    @Override
    public void setCompleteDate( String completeDate )
    {
        writer.writeAttribute( FIELD_COMPLETEDATE, completeDate );
    }

    @Override
    public void setPeriod( String period )
    {
        writer.writeAttribute( FIELD_PERIOD, period );
    }

    @Override
    public void setOrgUnit( String orgUnit )
    {
        writer.writeAttribute( FIELD_ORGUNIT, orgUnit );
    }
    
    @Override
    public DataValue getDataValueInstance()
    {
        return new StreamingDataValue( writer );
    }

    @Override
    public void close()
    {
        if ( writer != null )
        {
            writer.closeElement();
            writer.closeDocument();
        }
    }
}
