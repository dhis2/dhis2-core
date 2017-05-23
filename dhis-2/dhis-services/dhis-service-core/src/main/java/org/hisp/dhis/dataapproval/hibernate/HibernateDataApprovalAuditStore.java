package org.hisp.dhis.dataapproval.hibernate;

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

import org.apache.commons.collections.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;

import java.util.List;
import java.util.Set;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

/**
 * @author Jim Grace
 */
public class HibernateDataApprovalAuditStore
    extends HibernateGenericStore<DataApprovalAudit>
    implements DataApprovalAuditStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // DataValueAuditStore implementation
    // -------------------------------------------------------------------------

    @Override
    public void deleteDataApprovalAudits( OrganisationUnit organisationUnit )
    {
        String hql = "delete from DataApprovalAudit d where d.organisationUnit = :unit";

        getSession().createQuery( hql ).
            setEntity( "unit", organisationUnit ).executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DataApprovalAudit> getDataApprovalAudits( DataApprovalAuditQueryParams params )
    {
        SqlHelper hlp = new SqlHelper();

        String hql = "select a from DataApprovalAudit a ";

        if ( params.hasWorkflows() )
        {
            hql += hlp.whereAnd() + " a.workflow.uid in (" + getQuotedCommaDelimitedString( getUids( params.getWorkflows() ) ) + ") ";
        }

        if ( params.hasLevels() )
        {
            hql += hlp.whereAnd() + " a.level.uid in (" + getQuotedCommaDelimitedString( getUids( params.getLevels() ) ) + ") ";
        }

        if ( params.hasOrganisationUnits() )
        {
            hql += hlp.whereAnd() + " a.organisationUnit.uid in (" + getQuotedCommaDelimitedString( getUids( params.getOrganisationUnits() ) ) + ") ";
        }

        if ( params.hasAttributeOptionCombos() )
        {
            hql += hlp.whereAnd() + " a.attributeOptionCombo.uid in (" + getQuotedCommaDelimitedString( getUids( params.getAttributeOptionCombos() ) ) + ") ";
        }

        if ( params.hasStartDate() )
        {
            hql += hlp.whereAnd() + " a.period.startDate >= '" + getMediumDateString( params.getStartDate() ) + "' ";
        }

        if ( params.hasEndDate() )
        {
            hql += hlp.whereAnd() + " a.period.endDate <= '" + getMediumDateString( params.getEndDate() ) + "' ";
        }

        Set<OrganisationUnit> userOrgUnits = currentUserService.getCurrentUserOrganisationUnits();

        if ( !CollectionUtils.isEmpty( userOrgUnits ) )
        {
            hql += hlp.whereAnd() + " (";

            for ( OrganisationUnit userOrgUnit : userOrgUnits )
            {
                hql += "a.organisationUnit.path like '%" + userOrgUnit.getUid() + "%' or ";
            }

            hql = TextUtils.removeLastOr( hql ) + ") ";
        }

        hql += "order by a.workflow.name, a.organisationUnit.name, a.attributeOptionCombo.name, a.period.startDate, a.period.endDate, a.created";

        return getQuery( hql ).list();
    }
}
