package org.hisp.dhis.caseaggregation;

/*
 * Copyright (c) 2004-2014, University of Oslo
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

public class AggregationQuery
{
    private String id;

    private String name;

    private String dataSet;

    private String dataElement;

    private String categoryOptionCombo;

    private String dataElementForGroupBy;

    private String operator;

    private String expression;

    @JsonProperty
    public String getId()
    {
        return id;
    }

    @JsonProperty
    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @JsonProperty
    public String getDataSet()
    {
        return dataSet;
    }

    public void setDataSet( String dataSet )
    {
        this.dataSet = dataSet;
    }

    @JsonProperty
    public String getDataElement()
    {
        return dataElement;
    }

    public void setDataElement( String dataElement )
    {
        this.dataElement = dataElement;
    }

    @JsonProperty
    public String getCategoryOptionCombo()
    {
        return categoryOptionCombo;
    }

    public void setCategoryOptionCombo( String categoryOptionCombo )
    {
        this.categoryOptionCombo = categoryOptionCombo;
    }

    @JsonProperty
    public String getDataElementForGroupBy()
    {
        return dataElementForGroupBy;
    }

    public void setDataElementForGroupBy( String dataElementForGroupBy )
    {
        this.dataElementForGroupBy = dataElementForGroupBy;
    }

    @JsonProperty
    public String getOperator()
    {
        return operator;
    }

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    @JsonProperty
    public String getExpression()
    {
        return expression;
    }

    public void setExpression( String expression )
    {
        this.expression = expression;
    }
}
