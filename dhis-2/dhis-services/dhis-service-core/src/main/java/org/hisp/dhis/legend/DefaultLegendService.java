package org.hisp.dhis.legend;

import java.util.List;

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultLegendService
    implements LegendService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericIdentifiableObjectStore<Legend> legendStore;

    public void setLegendStore( GenericIdentifiableObjectStore<Legend> legendStore )
    {
        this.legendStore = legendStore;
    }

    private GenericIdentifiableObjectStore<LegendSet> legendSetStore;

    public void setLegendSetStore( GenericIdentifiableObjectStore<LegendSet> legendSetStore )
    {
        this.legendSetStore = legendSetStore;
    }

    // -------------------------------------------------------------------------
    // Legend
    // -------------------------------------------------------------------------

    @Override
    public int addLegend( Legend legend )
    {
        return legendStore.save( legend );
    }

    @Override
    public void updateLegend( Legend legend )
    {
        legendStore.update( legend );        
    }

    @Override
    public Legend getLegend( int id )
    {
        return legendStore.get( id );
    }

    @Override
    public Legend getLegend( String uid )
    {
        return legendStore.getByUid( uid );
    }
    
    @Override
    public void deleteLegend( Legend legend )
    {
        legendStore.delete( legend );
    }

    @Override
    public List<Legend> getAllLegends()
    {
        return legendStore.getAll();
    }

    // -------------------------------------------------------------------------
    // LegendSet
    // -------------------------------------------------------------------------

    @Override
    public int addLegendSet( LegendSet legend )
    {
        return legendSetStore.save( legend );
    }

    @Override
    public void updateLegendSet( LegendSet legend )
    {
        legendSetStore.update( legend );
    }

    @Override
    public LegendSet getLegendSet( int id )
    {
        return legendSetStore.get( id );
    }

    @Override
    public LegendSet getLegendSet( String uid )
    {
        return legendSetStore.getByUid( uid );
    }
    
    @Override
    public void deleteLegendSet( LegendSet legendSet )
    {
        legendSetStore.delete( legendSet );
    }

    @Override
    public List<LegendSet> getAllLegendSets()
    {
        return legendSetStore.getAll();
    }
}
