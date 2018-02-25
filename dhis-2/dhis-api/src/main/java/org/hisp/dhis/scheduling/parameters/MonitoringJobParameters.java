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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hisp.dhis.schema.annotation.Property.Value.FALSE;

/**
 * @author Henning Håkonsen
 * @author Stian Sandvold
 */
public class MonitoringJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -1683853240301569669L;

    @Property( required = FALSE )
    private int relativeStart;

    @Property( required = FALSE )
    private int relativeEnd;

    @Property( required = FALSE )
    private List<String> validationRuleGroups = new ArrayList<>();

    @Property( required = FALSE )
    private boolean sendNotifications;

    @Property( required = FALSE )
    private boolean persistResults;

    public MonitoringJobParameters()
    {
    }

    public MonitoringJobParameters( int relativeStart, int relativeEnd, List<String> validationRuleGroups,
        boolean sendNotifications, boolean persistResults )
    {
        this.relativeStart = relativeStart;
        this.relativeEnd = relativeEnd;
        this.validationRuleGroups = validationRuleGroups != null ? validationRuleGroups : Lists.newArrayList();
        this.sendNotifications = sendNotifications;
        this.persistResults = persistResults;
    }

    public int getRelativeStart()
    {
        return relativeStart;
    }

    public void setRelativeStart( int relativeStart )
    {
        this.relativeStart = relativeStart;
    }

    public int getRelativeEnd()
    {
        return relativeEnd;
    }

    public void setRelativeEnd( int relativeEnd )
    {
        this.relativeEnd = relativeEnd;
    }

    public List<String> getValidationRuleGroups()
    {
        return validationRuleGroups;
    }

    public void setValidationRuleGroups( List<String> validationRuleGroups )
    {
        this.validationRuleGroups = validationRuleGroups;
    }

    public boolean isSendNotifications()
    {
        return sendNotifications;
    }

    public void setSendNotifications( boolean sendNotifications )
    {
        this.sendNotifications = sendNotifications;
    }

    public boolean isPersistResults()
    {
        return persistResults;
    }

    public void setPersistResults( boolean persistResults )
    {
        this.persistResults = persistResults;
    }

    @Override
    public ErrorReport validate()
    {
        // No need to validate relatePeriods, since it will fail in the controller if invalid.

        // Validating validationRuleGroup. Since it's too late to check if the input was an array of strings or
        // something else, this is a best effort to avoid invalid data in the object.
        List<String> invalidUIDs = validationRuleGroups.stream()
            .filter( ( group ) -> !CodeGenerator.isValidUid( group ) )
            .collect( Collectors.toList() );

        if ( invalidUIDs.size() > 0 )
        {
            return new ErrorReport( this.getClass(), ErrorCode.E4014, invalidUIDs.get( 0 ),
                "validationRuleGroups" );
        }

        return null;
    }
}
