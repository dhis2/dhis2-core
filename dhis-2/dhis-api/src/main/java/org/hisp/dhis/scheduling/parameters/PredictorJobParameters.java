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
package org.hisp.dhis.scheduling.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Henning Håkonsen
 */
@JacksonXmlRootElement( localName = "predictorJobParameters", namespace = DxfNamespaces.DXF_2_0 )
public class PredictorJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 5526554074518768146L;

    private int relativeStart;

    private int relativeEnd;

    private List<String> predictors = new ArrayList<>();

    private List<String> predictorGroups = new ArrayList<>();

    public PredictorJobParameters()
    {
    }

    public PredictorJobParameters( int relativeStart, int relativeEnd, List<String> predictors,
        List<String> predictorGroups )
    {
        this.relativeStart = relativeStart;
        this.relativeEnd = relativeEnd;
        this.predictors = predictors;
        this.predictorGroups = predictorGroups;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getRelativeStart()
    {
        return relativeStart;
    }

    public void setRelativeStart( int relativeStart )
    {
        this.relativeStart = relativeStart;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getRelativeEnd()
    {
        return relativeEnd;
    }

    public void setRelativeEnd( int relativeEnd )
    {
        this.relativeEnd = relativeEnd;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "predictors", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "predictor", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getPredictors()
    {
        return predictors;
    }

    public void setPredictors( List<String> predictors )
    {
        this.predictors = predictors;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "predictorGroups", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "predictorGroup", namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getPredictorGroups()
    {
        return predictorGroups;
    }

    public void setPredictorGroups( List<String> predictorGroups )
    {
        this.predictorGroups = predictorGroups;
    }

    @Override
    public Optional<ErrorReport> validate()
    {
        return Optional.empty();
    }
}
