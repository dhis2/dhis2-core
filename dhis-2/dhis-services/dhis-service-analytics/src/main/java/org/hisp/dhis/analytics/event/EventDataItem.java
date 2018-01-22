package org.hisp.dhis.analytics.event;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;

/**
 * @author Henning Håkonsen
 */
public class EventDataItem
{
    private DataElement dataElement;

    private TrackedEntityAttribute trackedEntityAttribute;

    public EventDataItem( DataElement dataElement, TrackedEntityAttribute trackedEntityAttribute )
    {
        this.dataElement = dataElement;
        this.trackedEntityAttribute = trackedEntityAttribute;
    }

    public boolean isInvalid()
    {
        return dataElement == null && trackedEntityAttribute == null;
    }

    public ValueType getValueType()
    {
        if ( isDataElement() )
        {
            return dataElement.getValueType();
        }
        else
        {
            return trackedEntityAttribute.getValueType();
        }
    }

    public OptionSet getOptionSet()
    {
        if ( isDataElement() )
        {
            return dataElement.getOptionSet();
        }
        else
        {
            return trackedEntityAttribute.getOptionSet();
        }
    }

    public boolean hasOptionSet()
    {
        return getOptionSet() != null;
    }

    public LegendSet getLegendSet()
    {
        if ( isDataElement() )
        {
            return dataElement.getLegendSet();
        }
        else
        {
            return trackedEntityAttribute.getLegendSet();
        }
    }

    public boolean hasLegendSet()
    {
        return getLegendSet() != null;
    }

    public String getParentUid()
    {
        if ( isDataElement() )
        {
            return dataElement.getUid();
        }
        else
        {
            return trackedEntityAttribute.getUid();
        }
    }

    public String getDisplayName()
    {
        if ( isDataElement() )
        {
            return dataElement.getDisplayName();
        }
        else
        {
            return trackedEntityAttribute.getDisplayName();
        }
    }

    private boolean isDataElement()
    {
        return dataElement != null;
    }
}
