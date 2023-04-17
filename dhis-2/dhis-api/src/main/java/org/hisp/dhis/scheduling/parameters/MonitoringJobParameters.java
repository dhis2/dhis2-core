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
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

/**
 * @author Henning Håkonsen
 * @author Stian Sandvold
 */
@Getter
@Setter
@NoArgsConstructor
public class MonitoringJobParameters implements JobParameters
{
    @JsonProperty
    private int relativeStart;

    @JsonProperty
    private int relativeEnd;

    @JsonProperty
    private List<String> validationRuleGroups = new ArrayList<>();

    @JsonProperty
    private boolean sendNotifications;

    @JsonProperty
    private boolean persistResults;

    public MonitoringJobParameters( int relativeStart, int relativeEnd, List<String> validationRuleGroups,
        boolean sendNotifications, boolean persistResults )
    {
        this.relativeStart = relativeStart;
        this.relativeEnd = relativeEnd;
        this.validationRuleGroups = validationRuleGroups != null ? validationRuleGroups : Lists.newArrayList();
        this.sendNotifications = sendNotifications;
        this.persistResults = persistResults;
    }

    @Override
    public Optional<ErrorReport> validate()
    {
        // No need to validate relatePeriods, since it will fail in the
        // controller if invalid.

        // Validating validationRuleGroup. Since it's too late to check if the
        // input was an array of strings or
        // something else, this is a best effort to avoid invalid data in the
        // object.
        List<String> invalidUIDs = validationRuleGroups.stream()
            .filter( ( group ) -> !CodeGenerator.isValidUid( group ) )
            .collect( Collectors.toList() );

        if ( !invalidUIDs.isEmpty() )
        {
            return Optional.of( new ErrorReport( this.getClass(), ErrorCode.E4014, invalidUIDs.get( 0 ),
                "validationRuleGroups" ) );
        }

        return Optional.empty();
    }

}
