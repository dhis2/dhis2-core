package org.hisp.dhis.dxf2.webmessage.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.dxf2.common.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.sync.MetadataSyncSummary;
import org.hisp.dhis.dxf2.webmessage.AbstractWebMessageResponse;
import org.hisp.dhis.feedback.Stats;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.metadata.version.MetadataVersion;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Created by vanyas on 4/22/17.
 */
public class MetadataSyncWebMessageResponse
    extends AbstractWebMessageResponse
{
    private final MetadataSyncSummary syncSummary;

    public MetadataSyncWebMessageResponse( MetadataSyncSummary syncSummary )
    {
        Assert.notNull( syncSummary, "SyncSummary is require to be non-null." );
        this.syncSummary = syncSummary;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ImportReport getImportReport()
    {
        return syncSummary.getImportReport();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MetadataVersion getMetadataVersion()
    {
        return syncSummary.getMetadataVersion();
    }

}
