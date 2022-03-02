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

import java.util.Optional;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * @author Jan Bernitt
 */
@JacksonXmlRootElement( localName = "jobParameters", namespace = DxfNamespaces.DXF_2_0 )
public class DisableInactiveUsersJobParameters implements JobParameters
{
    private static final long serialVersionUID = -5877578172615705990L;

    private int inactiveMonths;

    private Integer reminderDaysBefore;

    public DisableInactiveUsersJobParameters()
    {

    }

    public DisableInactiveUsersJobParameters( int inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
    }

    @JsonProperty( required = true )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getInactiveMonths()
    {
        return inactiveMonths;
    }

    public void setInactiveMonths( int inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Integer getReminderDaysBefore()
    {
        return reminderDaysBefore;
    }

    public void setReminderDaysBefore( Integer reminderDaysBefore )
    {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    @Override
    public Optional<ErrorReport> validate()
    {
        if ( inactiveMonths < 1 || inactiveMonths > 24 )
        {
            return Optional.of(
                new ErrorReport( getClass(), ErrorCode.E4008, "inactiveMonths", 1, 24, inactiveMonths ) );
        }
        if ( reminderDaysBefore != null && (reminderDaysBefore < 1 || reminderDaysBefore > 24) )
        {
            return Optional.of(
                new ErrorReport( getClass(), ErrorCode.E4008, "reminderDaysBefore", 1, 24, reminderDaysBefore ) );
        }
        return Optional.empty();
    }
}
