package org.hisp.dhis.dxf2.events.trackedentity.store.query;

import java.util.Map;

import org.hisp.dhis.dxf2.events.trackedentity.store.Function;
import org.hisp.dhis.dxf2.events.trackedentity.store.QueryElement;
import org.hisp.dhis.dxf2.events.trackedentity.store.Subselect;
import org.hisp.dhis.dxf2.events.trackedentity.store.TableColumn;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
public class EventQuery
{
    public enum COLUMNS
    {
        ID,
        UID,
        STATUS,
        EXECUTION_DATE,
        DUE_DATE,
        STOREDBY,
        COMPLETEDBY,
        COMPLETEDDATE,
        CREATED,
        CREATEDCLIENT,
        UPDATED,
        UPDATEDCLIENT,
        DELETED,
        GEOMETRY,
        TEI_UID,
        ENROLLMENT_UID,
        ENROLLMENT_FOLLOWUP,
        ENROLLMENT_STATUS,
        PROGRAM_UID,
        PROGRAM_STAGE_UID,
        ORGUNIT_UID,
        ORGUNIT_NAME,
        COC_UID,
        CAT_OPTIONS
    }

    public static Map<COLUMNS, ? extends QueryElement> columnMap = ImmutableMap.<COLUMNS, QueryElement> builder()
        .put( COLUMNS.ID, new TableColumn( "psi", "programstageinstanceid" ) )
        .put( COLUMNS.UID, new TableColumn( "psi", "uid" ) )
        .put( COLUMNS.STATUS, new TableColumn( "psi", "status" ) )
        .put( COLUMNS.EXECUTION_DATE, new TableColumn( "psi", "executiondate" ) )
        .put( COLUMNS.DUE_DATE, new TableColumn( "psi", "duedate" ) )
        .put( COLUMNS.STOREDBY, new TableColumn( "psi", "storedby" ) )
        .put( COLUMNS.COMPLETEDBY, new TableColumn( "psi", "completedby" ) )
        .put( COLUMNS.COMPLETEDDATE, new TableColumn( "psi", "completeddate" ) )
        .put( COLUMNS.CREATED, new TableColumn( "psi", "created" ) )
        .put( COLUMNS.CREATEDCLIENT, new TableColumn( "psi", "createdatclient" ) )
        .put( COLUMNS.UPDATED, new TableColumn( "psi", "lastupdated" ) )
        .put( COLUMNS.UPDATEDCLIENT, new TableColumn( "psi", "lastupdatedatclient" ) )
        .put( COLUMNS.DELETED, new TableColumn( "psi", "deleted" ) )
        .put( COLUMNS.GEOMETRY, new Function( "ST_AsBinary", "psi", "geometry", "geometry")  )
        .put( COLUMNS.TEI_UID, new TableColumn( "tei", "uid", "tei_uid" ) )
        .put( COLUMNS.ENROLLMENT_UID, new TableColumn( "pi", "uid", "enruid" ) )
        .put( COLUMNS.ENROLLMENT_FOLLOWUP, new TableColumn( "pi", "followup", "enrfollowup" ) )
        .put( COLUMNS.ENROLLMENT_STATUS, new TableColumn( "pi", "status", "enrstatus" ) )
        .put( COLUMNS.PROGRAM_UID, new TableColumn( "p", "uid", "prguid" ) )
        .put( COLUMNS.PROGRAM_STAGE_UID, new TableColumn( "ps", "uid", "prgstguid" ) )
        .put( COLUMNS.ORGUNIT_UID, new TableColumn( "o", "uid", "ou_uid" ) )
        .put( COLUMNS.ORGUNIT_NAME, new TableColumn( "o", "name", "ou_name" ) )
        .put( COLUMNS.COC_UID, new TableColumn( "coc", "uid", "cocuid" ) )
        .put( COLUMNS.CAT_OPTIONS, new Subselect( "( " +
            "SELECT string_agg(opt.uid::text, ',') " +
            "FROM dataelementcategoryoption opt " +
            "join categoryoptioncombos_categoryoptions ccc " +
            "on opt.categoryoptionid = ccc.categoryoptionid " +
            "WHERE coc.categoryoptioncomboid = ccc.categoryoptioncomboid )", "catoptions" ) )
        .build();

    public static String getQuery()
    {
        return getSelect() +
            "from programstageinstance psi " +
            "join programinstance pi on psi.programinstanceid = pi.programinstanceid " +
            "join trackedentityinstance tei on pi.trackedentityinstanceid = tei.trackedentityinstanceid " +
            "join program p on pi.programid = p.programid " +
            "join programstage ps on psi.programstageid = ps.programstageid " +
            "join organisationunit o on psi.organisationunitid = o.organisationunitid " +
            "join categoryoptioncombo coc on psi.attributeoptioncomboid = coc.categoryoptioncomboid " +
            "where pi.programinstanceid in (:ids)";
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
