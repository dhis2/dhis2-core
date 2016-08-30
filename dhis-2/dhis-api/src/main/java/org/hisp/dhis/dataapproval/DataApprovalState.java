package org.hisp.dhis.dataapproval;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

/**
 * Current state of data approval for a selected combination of data set, period,
 * organisation unit, and category options or category group options.
 * 
 * ACCEPTED_HERE refers the approval being accepted by the level above, implying
 * that the approval has an accepted state at this level.
 * 
 * APPROVED_HERE refers to the data being approved by this level.
 *
 * @author Jim Grace
 */
public enum DataApprovalState
{
    /**
     * Data approval does not apply to this selection. (Data is neither
     * "approved" nor "unapproved".)
     */
    UNAPPROVABLE ( /* approved */ false, /* approvable */ false, /* unapprovable */ false,
                   /* accepted */ false, /* acceptable */ false, /* unacceptable */ false ),

    /**
     * At least some data within the selection is unapproved and waiting for
     * approval at a higher organisation unit level (not approvable here.)
     */
    UNAPPROVED_ABOVE ( /* approved */ false, /* approvable */ false, /* unapprovable */ false,
                       /* accepted */ false, /* acceptable */ false, /* unacceptable */ false ),

    /**
     * At least some data within the selection is unapproved and waiting for
     * lower-level approval (not ready for approving here.)
     */
    UNAPPROVED_WAITING ( /* approved */ false, /* approvable */ false, /* unapprovable */ false,
                         /* accepted */ false, /* acceptable */ false, /* unacceptable */ false ),

    /**
     * Data is unapproved, and is ready to be approved for this selection.
     */
    UNAPPROVED_READY ( /* approved */ false, /* approvable */ true, /* unapprovable */ false,
                       /* accepted */ false, /* acceptable */ false, /* unacceptable */ false ),
    /**
     * Data is approved, but at a higher organisation unit level
     * (so cannot be unapproved here.)
     */
    APPROVED_ABOVE ( /* approved */ true, /* approvable */ false, /* unapprovable */ false,
                     /* accepted */ false, /* acceptable */ false, /* unacceptable */ false ),

    /**
     * Data is approved, and was approved here (so could be unapproved here.)
     */
    APPROVED_HERE ( /* approved */ true, /* approvable */ false, /* unapprovable */ true,
                    /* accepted */ false, /* acceptable */ true, /* unacceptable */ false ),

    /**
     * Data is approved and accepted here (so could be unapproved here.)
     */
    ACCEPTED_HERE ( /* approved */ true, /* approvable */ false, /* unapprovable */ true,
                    /* accepted */ true, /* acceptable */ false, /* unacceptable */ true );

    /**
     * Is this (entire) data selection approved (and therefore locked)?
     */
    private final boolean approved;

    /**
     * Is this data selection able to be approved?
     */
    private final boolean approvable;

    /**
     * Is this data selection able to be unapproved?
     */
    private final boolean unapprovable;

    /**
     * Is this (entire) data selection accepted?
     */
    private final boolean accepted;

    /**
     * Is this data selection able to be accepted?
     */
    private final boolean acceptable;

    /**
     * Is this data selection able to be unaccepted?
     */
    private final boolean unacceptable;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    DataApprovalState( boolean approved, boolean approvable, boolean unapprovable,
                       boolean accepted, boolean acceptable, boolean unacceptable )
    {
        this.approved = approved;
        this.approvable = approvable;
        this.unapprovable = unapprovable;
        this.accepted = accepted;
        this.acceptable = acceptable;
        this.unacceptable = unacceptable;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public boolean isApproved()
    {
        return approved;
    }

    public boolean isApprovable()
    {
        return approvable;
    }

    public boolean isUnapprovable()
    {
        return unapprovable;
    }

    public boolean isAccepted()
    {
        return accepted;
    }

    public boolean isAcceptable()
    {
        return acceptable;
    }

    public boolean isUnacceptable()
    {
        return unacceptable;
    }

}
