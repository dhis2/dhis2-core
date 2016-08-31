package org.hisp.dhis.trackedentity.action.trackedentityattributegroup;

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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.trackedentity.TrackedEntityAttributeGroup;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeGroupService;

import com.opensymphony.xwork2.Action;

/**
 * @author Chau Thu Tran
 * @version $ SaveAttributeGroupSortOrderAction.java Jul 5, 2011 11:07:38 AM $
 * 
 */
public class SaveAttributeGroupSortOrderAction
implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private TrackedEntityAttributeGroupService attributeGroupService;

    public void setAttributeGroupService( TrackedEntityAttributeGroupService attributeGroupService )
    {
        this.attributeGroupService = attributeGroupService;
    }

    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------

    private List<Integer> attributeGroupIds = new ArrayList<>();

    public void setAttributeGroupIds( List<Integer> attributeGroupIds )
    {
        this.attributeGroupIds = attributeGroupIds;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        int sortOrder = 1;

        List<TrackedEntityAttributeGroup> groups = new ArrayList<>( attributeGroupIds.size() );

        for ( Integer attributeGroupId : attributeGroupIds )
        {
            TrackedEntityAttributeGroup attributeGroup = attributeGroupService.getTrackedEntityAttributeGroup( attributeGroupId );

            groups.add( attributeGroup );

            attributeGroup.setSortOrder( sortOrder++ );

            attributeGroupService.updateTrackedEntityAttributeGroup( attributeGroup );
        }
        
        return SUCCESS;
    }

}
