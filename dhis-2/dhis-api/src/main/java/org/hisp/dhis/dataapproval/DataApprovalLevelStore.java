package org.hisp.dhis.dataapproval;

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

import java.util.List;

import org.hisp.dhis.common.IdentifiableObjectStore;

/**
 * Defines the functionality for persisting DataApproval objects.
 *
 * @author Jim Grace
 */
public interface DataApprovalLevelStore
    extends IdentifiableObjectStore<DataApprovalLevel>
{
    String ID = DataApprovalLevelStore.class.getName();

    // -------------------------------------------------------------------------
    // Basic DataApprovalLevel
    // -------------------------------------------------------------------------

    /**
     * Gets a list of all data approval levels, ordered by level in ascending order,
     * i.e. from 1 to n.
     *
     * @return List of all data approval levels, ordered from 1 to n.
     */
    List<DataApprovalLevel> getAllDataApprovalLevels();

    /**
     * Gets data approval levels by org unit level.
     * 
     * @param orgUnitLevel the org unit level.
     * @return a list of data approval levels, ordered by level number.
     */
    List<DataApprovalLevel> getDataApprovalLevelsByOrgUnitLevel( int orgUnitLevel );
}
