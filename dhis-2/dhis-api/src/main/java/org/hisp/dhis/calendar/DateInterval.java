package org.hisp.dhis.calendar;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import javax.validation.constraints.NotNull;

/**
 * Class representing a date interval.
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @see DateTimeUnit
 * @see Calendar
 */
public class DateInterval
{
    /**
     * Start of interval. Required.
     */
    @NotNull
    private final DateTimeUnit from;

    /**
     * End of interval. Required.
     */
    @NotNull
    private final DateTimeUnit to;

    /**
     * Interval type this interval represents.
     */
    private DateIntervalType type;

    public DateInterval( DateTimeUnit from, DateTimeUnit to )
    {
        this.from = from;
        this.to = to;
    }

    public DateInterval( DateTimeUnit from, DateTimeUnit to, DateIntervalType type )
    {
        this.from = from;
        this.to = to;
        this.type = type;
    }

    public DateTimeUnit getFrom()
    {
        return from;
    }

    public DateTimeUnit getTo()
    {
        return to;
    }

    public DateIntervalType getType()
    {
        return type;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        DateInterval that = (DateInterval) o;

        if ( from != null ? !from.equals( that.from ) : that.from != null ) return false;
        if ( to != null ? !to.equals( that.to ) : that.to != null ) return false;
        if ( type != that.type ) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString()
    {
        return "DateInterval{" +
            "from=" + from +
            ", to=" + to +
            ", type=" + type +
            '}';
    }
}
