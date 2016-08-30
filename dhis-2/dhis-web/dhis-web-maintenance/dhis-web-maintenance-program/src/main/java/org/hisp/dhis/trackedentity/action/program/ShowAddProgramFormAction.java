package org.hisp.dhis.trackedentity.action.program;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.comparator.IdentifiableObjectNameComparator;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserGroupService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Abyot Asalefew Gizaw
 */
public class ShowAddProgramFormAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    private ProgramService programService;

    public void setProgramService( ProgramService programService )
    {
        this.programService = programService;
    }

    private TrackedEntityAttributeService trackedEntityAttributeService;

    public void setTrackedEntityAttributeService( TrackedEntityAttributeService trackedEntityAttributeService )
    {
        this.trackedEntityAttributeService = trackedEntityAttributeService;
    }

    private UserGroupService userGroupService;

    public void setUserGroupService( UserGroupService userGroupService )
    {
        this.userGroupService = userGroupService;
    }

    private RelationshipTypeService relationshipTypeService;

    public void setRelationshipTypeService( RelationshipTypeService relationshipTypeService )
    {
        this.relationshipTypeService = relationshipTypeService;
    }

    @Autowired
    private TrackedEntityService trackedEntityService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private DataApprovalService dataApprovalService;

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<TrackedEntityAttribute> availableAttributes;

    public List<TrackedEntityAttribute> getAvailableAttributes()
    {
        return availableAttributes;
    }

    private List<UserGroup> userGroups;

    public List<UserGroup> getUserGroups()
    {
        return userGroups;
    }

    private List<RelationshipType> relationshipTypes;

    public List<RelationshipType> getRelationshipTypes()
    {
        return relationshipTypes;
    }

    private List<Program> programs;

    public List<Program> getPrograms()
    {
        return programs;
    }

    private List<TrackedEntity> trackedEntities;

    public List<TrackedEntity> getTrackedEntities()
    {
        return trackedEntities;
    }

    private List<Attribute> attributes;

    public List<Attribute> getAttributes()
    {
        return attributes;
    }

    private Map<Integer, String> attributeValues = new HashMap<>();

    public Map<Integer, String> getAttributeValues()
    {
        return attributeValues;
    }

    private List<DataElementCategoryCombo> categoryCombos = new ArrayList<>();

    public List<DataElementCategoryCombo> getCategoryCombos()
    {
        return categoryCombos;
    }

    private List<DataApprovalWorkflow> workflows = new ArrayList<>();

    public List<DataApprovalWorkflow> getWorkflows()
    {
        return workflows;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        programs = programService.getAllPrograms();

        programs.removeAll( programService.getPrograms( ProgramType.WITHOUT_REGISTRATION ) );
        Collections.sort( programs, IdentifiableObjectNameComparator.INSTANCE );

        availableAttributes = trackedEntityAttributeService.getAllTrackedEntityAttributes();
        Collections.sort( availableAttributes, IdentifiableObjectNameComparator.INSTANCE );

        userGroups = userGroupService.getAllUserGroups();
        Collections.sort( userGroups, IdentifiableObjectNameComparator.INSTANCE );

        relationshipTypes = relationshipTypeService.getAllRelationshipTypes();
        Collections.sort( relationshipTypes, IdentifiableObjectNameComparator.INSTANCE );

        trackedEntities = trackedEntityService.getAllTrackedEntity();
        Collections.sort( trackedEntities, IdentifiableObjectNameComparator.INSTANCE );

        attributes = attributeService.getAttributes( Program.class );

        categoryCombos = new ArrayList<>( categoryService.getAttributeCategoryCombos() );

        workflows = new ArrayList<>( dataApprovalService.getAllWorkflows() );
        Collections.sort( workflows, IdentifiableObjectNameComparator.INSTANCE );

        return SUCCESS;
    }
}
