package org.hisp.dhis.dxf2.events.enrollment;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hisp.dhis.program.ProgramStatus;

/**
 * FIXME we should probably remove this, and replace it with program status
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public enum EnrollmentStatus
{
    ACTIVE( 0, ProgramStatus.ACTIVE ),
    COMPLETED( 1, ProgramStatus.COMPLETED ),
    CANCELLED( 2, ProgramStatus.CANCELLED );

    private final int value;
    private final ProgramStatus programStatus;

    EnrollmentStatus( int value, ProgramStatus programStatus )
    {
        this.value = value;
        this.programStatus = programStatus;
    }

    public int getValue()
    {
        return value;
    }

    public ProgramStatus getProgramStatus()
    {
        return programStatus;
    }

    public static EnrollmentStatus fromProgramStatus( ProgramStatus programStatus )
    {
        switch ( programStatus )
        {
            case ACTIVE:
                return ACTIVE;
            case CANCELLED:
                return CANCELLED;
            case COMPLETED:
                return COMPLETED;
        }

        throw new IllegalArgumentException( "Enum value not found: " + programStatus );
    }

    public static EnrollmentStatus fromStatusString( String status )
    {
        switch ( status )
        {
        case "ACTIVE":
            return ACTIVE;
        case "CANCELLED":
            return CANCELLED;
        case "COMPLETED":
            return COMPLETED;
        default:
            // Do nothing and fail
        }
        throw new IllegalArgumentException( "Enum value not found for string: " + status );
    }
}
