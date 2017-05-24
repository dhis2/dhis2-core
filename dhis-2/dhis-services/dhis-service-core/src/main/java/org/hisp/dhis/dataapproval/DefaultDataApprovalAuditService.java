package org.hisp.dhis.dataapproval;

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

import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.hisp.dhis.dataelement.CategoryOptionGroup;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Jim Grace
 */
@Transactional
public class DefaultDataApprovalAuditService
    implements DataApprovalAuditService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataApprovalAuditStore dataApprovalAuditStore;

    public void setDataApprovalAuditStore( DataApprovalAuditStore dataApprovalAuditStore )
    {
        this.dataApprovalAuditStore = dataApprovalAuditStore;
    }

    private DataApprovalLevelService dataApprovalLevelService;

    public void setDataApprovalLevelService( DataApprovalLevelService dataApprovalLevelService )
    {
        this.dataApprovalLevelService = dataApprovalLevelService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private AclService aclService;

    public void setAclService( AclService aclService )
    {
        this.aclService = aclService;
    }

    // -------------------------------------------------------------------------
    // DataValueAuditService implementation
    // -------------------------------------------------------------------------

    @Override
    public void deleteDataApprovalAudits( OrganisationUnit organisationUnit )
    {
        dataApprovalAuditStore.deleteDataApprovalAudits( organisationUnit );
    }

    @Override
    public List<DataApprovalAudit> getDataApprovalAudits( DataApprovalAuditQueryParams params )
    {
        if ( !currentUserService.currentUserIsSuper() )
        {
            Set<DataApprovalLevel> userLevels = new HashSet<>( dataApprovalLevelService.getUserDataApprovalLevels() );

            if ( params.hasLevels() )
            {
                params.setLevels( Sets.intersection( params.getLevels(), userLevels ) );
            }
            else
            {
                params.setLevels( userLevels );
            }
        }

        List<DataApprovalAudit> audits = dataApprovalAuditStore.getDataApprovalAudits( params );

        retainFromDimensionConstraints( audits );

        return audits;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Retain the DataApprovalAudits that the user can read
     * despite any dimension constraints that the user my have.
     *
     * @param audits the list of audit records.
     */
    private void retainFromDimensionConstraints( List<DataApprovalAudit> audits )
    {
        User user = currentUserService.getCurrentUser();

        Set<CategoryOptionGroupSet> cogDimensionConstraints = user.getUserCredentials().getCogsDimensionConstraints();
        Set<DataElementCategory> catDimensionConstraints = user.getUserCredentials().getCatDimensionConstraints();

        if ( currentUserService.currentUserIsSuper() ||
            ( CollectionUtils.isEmpty( cogDimensionConstraints ) && CollectionUtils.isEmpty( catDimensionConstraints ) ) )
        {
            return;
        }

        Map<DataElementCategoryOptionCombo, Boolean> readableOptionCombos = new HashMap<>(); // Local cached results

        for (Iterator<DataApprovalAudit> i = audits.iterator(); i.hasNext(); )
        {
            DataElementCategoryOptionCombo optionCombo = i.next().getAttributeOptionCombo();

            Boolean canRead = readableOptionCombos.get( optionCombo );

            if ( canRead == null )
            {
                canRead = canReadOptionCombo( user, optionCombo,
                    cogDimensionConstraints, catDimensionConstraints );

                readableOptionCombos.put( optionCombo, canRead );
            }

            if ( !canRead )
            {
                i.remove();
            }
        }
    }

    /**
     * Returns whether a user can read a data element attribute option combo
     * given the user's dimension constraints.
     * <p>
     * In order to read an option combo, the user must be able to read *every*
     * option in the option combo.
     *
     * @param user the user.
     * @param optionCombo the record to test.
     * @param cogDimensionConstraints category option combo group constraints, if any.
     * @param catDimensionConstraints category constraints, if any.
     * @return whether the user can read the DataApprovalAudit.
     */
    private boolean canReadOptionCombo( User user, DataElementCategoryOptionCombo optionCombo,
        Set<CategoryOptionGroupSet> cogDimensionConstraints, Set<DataElementCategory> catDimensionConstraints )
    {
        for ( DataElementCategoryOption option : optionCombo.getCategoryOptions() )
        {
            if ( !isOptionCogConstraintReadable( user, option, cogDimensionConstraints ) ||
                !isOptionCatConstraintReadable( user, option, catDimensionConstraints ) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether a user can read a data element category option
     * given the user's category option group constraints, if any.
     * <p>
     * If the option belongs to *any* option group that is readable by the user
     * which belongs to a constrained option group set, then the user may see
     * the option.
     *
     * @param user the user.
     * @param option the data element category option to test.
     * @param cogDimensionConstraints category option combo group constraints, if any.
     * @return whether the user can read the data element category option.
     */
    private boolean isOptionCogConstraintReadable( User user, DataElementCategoryOption option,
        Set<CategoryOptionGroupSet> cogDimensionConstraints )
    {
        if ( CollectionUtils.isEmpty( cogDimensionConstraints ) )
        {
            return true; // No category option group dimension constraints.
        }

        for ( CategoryOptionGroupSet groupSet : cogDimensionConstraints )
        {
            for ( CategoryOptionGroup group : groupSet.getMembers() )
            {
                if ( group.getMembers().contains( option ) && aclService.canRead( user, group ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether a user can read a data element category option
     * given the user's category constraints, if any.
     * <p>
     * If the option belongs to *any* category that is constrained for the user,
     * and the option is readable by the user, return true.
     *
     * @param user the user.
     * @param option the data element category option to test.
     * @param catDimensionConstraints category constraints, if any.
     * @return whether the user can read the data element category option.
     */
    private boolean isOptionCatConstraintReadable( User user, DataElementCategoryOption option,
        Set<DataElementCategory> catDimensionConstraints )
    {
        if ( CollectionUtils.isEmpty( catDimensionConstraints ) )
        {
            return true; // No category dimension constraints.
        }

        return !CollectionUtils.intersection ( catDimensionConstraints, option.getCategories() ).isEmpty()
            && aclService.canRead( user, option );
    }
}
