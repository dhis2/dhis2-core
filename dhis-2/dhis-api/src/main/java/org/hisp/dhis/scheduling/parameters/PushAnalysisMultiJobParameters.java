package org.hisp.dhis.scheduling.parameters;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PushAnalysisMultiJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -1848833906375595488L;

    @JsonProperty( required = true )
    private List<String> pushAnalysis = new ArrayList<>();

    public PushAnalysisMultiJobParameters()
    {
    }

    public PushAnalysisMultiJobParameters( String pushAnalysis )
    {
        this.pushAnalysis.add( pushAnalysis );
    }

    public PushAnalysisMultiJobParameters( List<String> pushAnalysis )
    {
        this.pushAnalysis = pushAnalysis;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public List<String> getPushAnalysis()
    {
        return pushAnalysis;
    }

    public void setPushAnalysis( List<String> pushAnalysis )
    {
        this.pushAnalysis = pushAnalysis;
    }

    @Override
    public ErrorReport validate()
    {
        if ( pushAnalysis == null || pushAnalysis.isEmpty() )
        {
            return new ErrorReport( this.getClass(), ErrorCode.E4014, pushAnalysis, "pushAnalysis" );
        }

        return null;
    }
}
