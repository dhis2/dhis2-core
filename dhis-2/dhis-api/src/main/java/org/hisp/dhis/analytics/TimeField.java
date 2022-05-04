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
package org.hisp.dhis.analytics;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import lombok.Getter;

import com.google.common.collect.ImmutableList;

/**
 * Enum that maps database column names to their respective "business" names.
 */
public enum TimeField
{
    EVENT_DATE( "executiondate" ),
    ENROLLMENT_DATE( "enrollmentdate" ),
    INCIDENT_DATE( "incidentdate" ),
    // Not a typo, different naming convention between FE and database
    SCHEDULED_DATE( "duedate" ),
    COMPLETED_DATE( "completeddate" ),
    CREATED( "created" ),
    LAST_UPDATED( "lastupdated" );

    @Getter
    private String field;

    public static final Collection<String> DEFAULT_TIME_FIELDS = ImmutableList.of( EVENT_DATE.name(),
        LAST_UPDATED.name(), ENROLLMENT_DATE.name() );

    /**
     * These constants represent the columns that can be compared using the raw
     * period column (in the analytics tables), instead of dates. This is
     * preferable for performance reasons.
     */
    private static final Collection<TimeField> TIME_FIELDS_SUPPORT_RAW_PERIODS = ImmutableList.of( EVENT_DATE,
        SCHEDULED_DATE, ENROLLMENT_DATE );

    private static final Set<String> FIELD_NAMES = newHashSet( TimeField.values() )
        .stream().map( TimeField::name )
        .collect( toSet() );

    TimeField( final String field )
    {
        this.field = field;
    }

    public static Optional<TimeField> of( final String timeField )
    {
        return Arrays.stream( values() )
            .filter( tf -> tf.name().equals( timeField ) )
            .findFirst();
    }

    public static Optional<TimeField> from( final String field )
    {
        return Arrays.stream( values() )
            .filter( tf -> tf.getField().equals( field ) )
            .findFirst();
    }

    public static boolean fieldIsValid( final String field )
    {
        return isNotBlank( field ) && FIELD_NAMES.contains( field );
    }

    public boolean supportsRawPeriod()
    {
        return isNotBlank( field ) && from( field ).isPresent()
            && TIME_FIELDS_SUPPORT_RAW_PERIODS.contains( from( field ).get() );
    }
}
