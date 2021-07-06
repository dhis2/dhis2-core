package org.hisp.dhis.event;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.program.ProgramStage;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Zubair Asghar
 */

@JacksonXmlRootElement( localName = "programStageInstanceAudit", namespace = DxfNamespaces.DXF_2_0 )
@Data
public class ProgramStageInstanceAudit  implements Serializable
{
    private long id;

    private ProgramStage programStage;

    private String programStageInstance;

    private String modifiedBy;

    private Date created;

    private AuditType auditType;
}
