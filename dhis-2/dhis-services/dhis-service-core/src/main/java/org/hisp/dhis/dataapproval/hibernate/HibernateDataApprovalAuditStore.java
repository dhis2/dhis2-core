package org.hisp.dhis.dataapproval.hibernate;

import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.commons.util.TextUtils.getQuotedCommaDelimitedString;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.hisp.dhis.commons.util.SqlHelper;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataapproval.DataApprovalAuditQueryParams;
import org.hisp.dhis.dataapproval.DataApprovalAuditStore;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;

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
            setParameter( "unit", organisationUnit ).executeUpdate();
    }

    @Override
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
