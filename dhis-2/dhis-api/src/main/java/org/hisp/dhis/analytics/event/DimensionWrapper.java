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
package org.hisp.dhis.analytics.event;

import java.util.Date;

import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.BaseNameableObject;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DimensionWrapper
{

    @JsonIgnore
    private final BaseNameableObject item;

    public DimensionWrapper( BaseNameableObject item )
    {
        this.item = item;
    }

    @JsonProperty
    public String getValueType()
    {
        if ( item instanceof ValueTypedDimensionalItemObject )
        {
            return ((ValueTypedDimensionalItemObject) item).getValueType().name();
        }
        return null;
    }

    @JsonProperty
    public String getDimensionType()
    {
        if ( item instanceof BaseDimensionalItemObject )
        {
            return ((BaseDimensionalItemObject) item).getDimensionItemType().name();
        }
        if ( item instanceof BaseDimensionalObject )
        {
            return ((BaseDimensionalObject) item).getDimensionType().name();
        }
        return null;
    }

    @JsonProperty
    public Date getCreated()
    {
        return item.getCreated();
    }

    @JsonProperty
    public Date getLastUpdated()
    {
        return item.getLastUpdated();
    }

    @JsonProperty
    public String getName()
    {
        return item.getName();
    }

    @JsonProperty
    public String getDisplayName()
    {
        return item.getDisplayName();
    }

    @JsonProperty
    public String getId()
    {
        return item.getUid();
    }

    @JsonProperty
    public String getCode()
    {
        return item.getCode();
    }

    @JsonProperty
    public String getDisplayShortName()
    {
        return item.getDisplayShortName();
    }

}
