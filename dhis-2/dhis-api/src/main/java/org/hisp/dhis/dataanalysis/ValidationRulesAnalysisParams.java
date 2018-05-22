package org.hisp.dhis.dataanalysis;

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

public class ValidationRulesAnalysisParams
{
    private String validationRuleGroupId;

    private String organisationUnitId;

    private String startDate;

    private String endDate;

    private boolean persist;

    private boolean notification;

    public ValidationRulesAnalysisParams()
    {
    }

    public ValidationRulesAnalysisParams( String validationRuleGroupId, String organisationUnitId, String startDate,
        String endDate, boolean persist, boolean notification )
    {
        this.validationRuleGroupId = validationRuleGroupId;
        this.organisationUnitId = organisationUnitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.persist = persist;
        this.notification = notification;
    }

    public String getValidationRuleGroupId()
    {
        return validationRuleGroupId;
    }

    public void setValidationRuleGroupId( String validationRuleGroupId )
    {
        this.validationRuleGroupId = validationRuleGroupId;
    }

    public String getOrganisationUnitId()
    {
        return organisationUnitId;
    }

    public void setOrganisationUnitId( String organisationUnitId )
    {
        this.organisationUnitId = organisationUnitId;
    }

    public String getStartDate()
    {
        return startDate;
    }

    public void setStartDate( String startDate )
    {
        this.startDate = startDate;
    }

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate( String endDate )
    {
        this.endDate = endDate;
    }

    public boolean isPersist()
    {
        return persist;
    }

    public void setPersist( boolean persist )
    {
        this.persist = persist;
    }

    public boolean isNotification()
    {
        return notification;
    }

    public void setNotification( boolean notification )
    {
        this.notification = notification;
    }
}
