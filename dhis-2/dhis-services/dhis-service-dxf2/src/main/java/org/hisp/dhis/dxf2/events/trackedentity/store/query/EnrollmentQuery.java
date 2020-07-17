package org.hisp.dhis.dxf2.events.trackedentity.store.query;

import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.trackedentity.store.TableColumn;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class EnrollmentQuery
{
    public enum COLUMNS
    {
        TEI_UID,
        ID,
        UID,
        CREATED,
        CREATEDCLIENT,
        UPDATED,
        UPDATEDCLIENT,
        STATUS,
        GEOMETRY,
        ENROLLMENTDATE,
        INCIDENTDATE,
        FOLLOWUP,
        COMPLETED,
        COMPLETEDBY,
        STOREDBY,
        DELETED,
        PROGRAM_UID,
        PROGRAM_FEATURE_TYPE,
        TEI_TYPE_UID,
        ORGUNIT_UID,
        ORGUNIT_NAME
    }

    public static Map<COLUMNS, TableColumn> columnMap = ImmutableMap.<COLUMNS, TableColumn> builder()
        .put( COLUMNS.TEI_UID, new TableColumn( "tei", "uid", "tei_uid" ) )
        .put( COLUMNS.GEOMETRY, new TableColumn( "pi", "geometry" ) )
        .put( COLUMNS.ID, new TableColumn( "pi", "programinstanceid" ) )
        .put( COLUMNS.UID, new TableColumn( "pi", "uid" ) )
        .put( COLUMNS.CREATED, new TableColumn( "pi", "created" ) )
        .put( COLUMNS.CREATEDCLIENT, new TableColumn( "pi", "createdatclient" ) )
        .put( COLUMNS.UPDATED, new TableColumn( "pi", "lastupdated" ) )
        .put( COLUMNS.UPDATEDCLIENT, new TableColumn( "pi", "lastupdatedatclient" ) )
        .put( COLUMNS.STATUS, new TableColumn( "pi", "status" ) )
        .put( COLUMNS.ENROLLMENTDATE, new TableColumn( "pi", "enrollmentdate" ) )
        .put( COLUMNS.INCIDENTDATE, new TableColumn( "pi", "incidentdate" ) )
        .put( COLUMNS.FOLLOWUP, new TableColumn( "pi", "followup" ) )
        .put( COLUMNS.COMPLETED, new TableColumn( "pi", "enddate" ) )
        .put( COLUMNS.COMPLETEDBY, new TableColumn( "pi", "completedby" ) )
        .put( COLUMNS.STOREDBY, new TableColumn( "pi", "storedby" ) )
        .put( COLUMNS.DELETED, new TableColumn( "pi", "deleted" ) )
        .put( COLUMNS.PROGRAM_UID, new TableColumn( "p", "uid", "program_uid" ) )
        .put( COLUMNS.PROGRAM_FEATURE_TYPE, new TableColumn( "p", "featuretype", "program_feature_type" ) )
        .put( COLUMNS.TEI_TYPE_UID, new TableColumn( "tet", "uid", "type_uid" ) )
        .put( COLUMNS.ORGUNIT_UID, new TableColumn( "o", "uid", "ou_uid" ) )
        .put( COLUMNS.ORGUNIT_NAME, new TableColumn( "o", "name", "ou_name" ) )
        .build();

    public static String getQuery()
    {
        return getSelect() +
            "from programinstance pi " +
            "join program p on pi.programid = p.programid " +
            "join trackedentityinstance tei on pi.trackedentityinstanceid = tei.trackedentityinstanceid " +
            "join trackedentitytype tet on tei.trackedentitytypeid = tet.trackedentitytypeid " +
            "join organisationunit o on tei.organisationunitid = o.organisationunitid " +
            "where pi.trackedentityinstanceid in (:ids) ";
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
