package org.hisp.dhis.sms.command.code;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

public class SMSCode
{
    private int id;

    private String code;

    private DataElement dataElement;
    
    private TrackedEntityAttribute trackedEntityAttribute;

    private int optionId;
    
    private String formula;

    public SMSCode( String code, DataElement dataElement, int optionId )
    {
        this.code = code;
        this.dataElement = dataElement;
        this.optionId = optionId;
    }
    
    public SMSCode( String code, TrackedEntityAttribute trackedEntityAttribute)
    {
        this.code = code;
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    public SMSCode()
    {

    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public int getOptionId()
    {
        return optionId;
    }

    public void setOptionId( int optionId )
    {
        this.optionId = optionId;
    }

    public TrackedEntityAttribute getTrackedEntityAttribute()
    {
        return trackedEntityAttribute;
    }

    public void setTrackedEntityAttribute( TrackedEntityAttribute trackedEntityAttribute )
    {
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    public String getFormula()
    {
        return formula;
    }

    public void setFormula( String formula )
    {
        this.formula = formula;
    }
}
