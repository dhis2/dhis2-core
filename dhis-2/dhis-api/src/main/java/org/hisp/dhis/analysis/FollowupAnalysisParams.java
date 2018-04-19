package org.hisp.dhis.analysis;

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

public class FollowupAnalysisParams
{
    private String fromDate;

    private String toDate;

    private String organisationUnitId;

    private String dataSetId;

    public FollowupAnalysisParams()
    {
    }

    public FollowupAnalysisParams( String startDate, String endDate, String organisationUnitId, String dataSetId )
    {
        this.fromDate = startDate;
        this.toDate = endDate;
        this.organisationUnitId = organisationUnitId;
        this.dataSetId = dataSetId;
    }

    public String getFromDate()
    {
        return fromDate;
    }

    public void setFromDate( String startDate )
    {
        this.fromDate = startDate;
    }

    public String getToDate()
    {
        return toDate;
    }

    public void setToDate( String endDate )
    {
        this.toDate = endDate;
    }

    public String getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    public String getDataSetId()
    {
        return dataSetId;
    }

    public void setDataSetId( String dataSetId )
    {
        this.dataSetId = dataSetId;
    }
}
