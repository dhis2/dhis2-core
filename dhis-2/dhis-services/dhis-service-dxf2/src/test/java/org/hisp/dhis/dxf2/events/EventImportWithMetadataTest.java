package org.hisp.dhis.dxf2.events;

import com.vividsolutions.jts.io.ParseException;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventImportWithMetadataTest extends DhisSpringTest
{

    @Autowired
    private RenderService _renderService;

    @Autowired
    private CsvEventService csvEventService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService _userService;

    @Autowired
    private MetadataImportService importService;

    private static IdSchemes idSchemes = new IdSchemes();

    private static Events events;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/import/create_program_stages.json" ).getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        params.setSkipSharing( true );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        idSchemes.setDataElementIdScheme( "UID" );
        idSchemes.setOrgUnitIdScheme( "CODE" );
        idSchemes.setProgramStageInstanceIdScheme( "UID" );

        events = csvEventService
            .readEvents( new ClassPathResource( "dxf2/import/csv/events_import_code.csv" ).getInputStream(), true );
    }

    @Test
    public void shouldSuccessCsvLookUpByCode()
    {
        idSchemes.setIdScheme( "CODE" );
        ImportOptions importOptions = new ImportOptions();
        importOptions.setIdSchemes( idSchemes );

        ImportSummaries importSummaries = eventService.addEvents( events.getEvents(), importOptions, null );

        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }

    @Test
    public void shouldFailImportCsvLookUpByUid()
    {
        idSchemes.setIdScheme( "UID" );
        ImportOptions importOptions = new ImportOptions();
        importOptions.setIdSchemes( idSchemes );

        ImportSummaries importSummaries = eventService.addEvents( events.getEvents(), importOptions, null );

        assertEquals( ImportStatus.ERROR, importSummaries.getStatus() );
    }
}
