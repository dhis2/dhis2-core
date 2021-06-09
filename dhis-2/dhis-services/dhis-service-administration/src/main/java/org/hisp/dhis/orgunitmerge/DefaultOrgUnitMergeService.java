package org.hisp.dhis.orgunitmerge;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class DefaultOrgUnitMergeService
    implements OrgUnitMergeService
{
    private final IdentifiableObjectManager idObjectManager;

    private final ImmutableList<OrgUnitMergeHandler> mergeHandlers;

    public DefaultOrgUnitMergeService( IdentifiableObjectManager idObjectManager )
    {
        Preconditions.checkNotNull( idObjectManager );

        this.idObjectManager = idObjectManager;

        this.mergeHandlers = ImmutableList.<OrgUnitMergeHandler>builder()
            .add( ( s, t ) -> mergeDataSets( s, t ) )
            .add( ( s, t ) -> mergePrograms( s, t ) )
            .add( ( s, t ) -> mergeOrgUnitGroups( s, t ) )
            .add( ( s, t ) -> mergeCategoryOptions( s, t ) )
            .add( ( s, t ) -> mergeUsers( s, t ) )
            .build();
    }

    @Override
    public void merge( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        mergeHandlers.forEach( merge -> merge.apply( sources, target ) );

        idObjectManager.update( target );
    }

    private void mergeDataSets( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        Set<DataSet> objects = sources.stream()
            .map( OrganisationUnit::getDataSets )
            .flatMap( Collection::stream )
            .collect( Collectors.toSet() );

        objects.forEach( o -> o.addOrganisationUnit( target ) );
    }

    private void mergePrograms( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getPrograms )
            .flatMap( Collection::stream )
            .forEach( o -> o.addOrganisationUnit( target ) );
    }

    private void mergeOrgUnitGroups( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getGroups )
            .flatMap( Collection::stream )
            .forEach( o -> o.addOrganisationUnit( target ) );
    }

    private void mergeCategoryOptions( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getCategoryOptions )
            .flatMap( Collection::stream )
            .forEach( o -> o.addOrganisationUnit( target ) );
    }

    private void mergeUsers( Set<OrganisationUnit> sources, OrganisationUnit target )
    {
        sources.stream()
            .map( OrganisationUnit::getUsers )
            .flatMap( Collection::stream )
            .forEach( o -> o.addOrganisationUnit( target ) );

        // TODO data view, TEI search
    }
}
