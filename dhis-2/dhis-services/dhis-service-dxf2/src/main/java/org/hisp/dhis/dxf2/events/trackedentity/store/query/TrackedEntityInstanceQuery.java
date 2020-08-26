package org.hisp.dhis.dxf2.events.trackedentity.store.query;

import java.util.Map;

import org.hisp.dhis.dxf2.events.trackedentity.store.Function;
import org.hisp.dhis.dxf2.events.trackedentity.store.QueryElement;
import org.hisp.dhis.dxf2.events.trackedentity.store.TableColumn;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceQuery
{
    public enum COLUMNS
    {
        UID,
        CREATED,
        CREATEDCLIENT,
        UPDATED,
        UPDATEDCLIENT,
        INACTIVE,
        DELETED,
        GEOMETRY,
        TYPE_UID,
        ORGUNIT_UID,
        TRACKEDENTITYINSTANCEID
    }

    public static Map<COLUMNS,  ? extends QueryElement> columnMap = ImmutableMap.<COLUMNS, QueryElement> builder()
        .put( COLUMNS.UID, new TableColumn( "tei", "uid", "tei_uid" ) )
        .put( COLUMNS.CREATED, new TableColumn( "tei", "created" ) )
        .put( COLUMNS.CREATEDCLIENT, new TableColumn( "tei", "createdatclient" ) )
        .put( COLUMNS.UPDATED, new TableColumn( "tei", "lastupdated" ) )
        .put( COLUMNS.UPDATEDCLIENT, new TableColumn( "tei", "lastupdatedatclient" ) )
        .put( COLUMNS.INACTIVE, new TableColumn( "tei", "inactive" ) )
        .put( COLUMNS.DELETED, new TableColumn( "tei", "deleted" ) )
        .put( COLUMNS.GEOMETRY, new Function( "ST_AsBinary", "tei", "geometry", "geometry")  )
        .put( COLUMNS.TYPE_UID, new TableColumn( "tet", "uid", "type_uid" ) )
        .put( COLUMNS.ORGUNIT_UID, new TableColumn( "o", "uid", "ou_uid" ) )
        .put( COLUMNS.TRACKEDENTITYINSTANCEID, new TableColumn( "tei", "trackedentityinstanceid", "trackedentityinstanceid" ) )
        .build();

    public static String getQuery()
    {
        return getSelect() +
            "FROM trackedentityinstance tei " +
            "join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid " +
            "join organisationunit o on tei.organisationunitid = o.organisationunitid " +
            "where tei.trackedentityinstanceid in (:ids)";
    }

    private static String getSelect()
    {
        return QueryUtils.getSelect( columnMap.values() );
    }

    public static String getColumnName( COLUMNS columns )
    {
        return columnMap.get( columns ).getResultsetValue();
    }
}
