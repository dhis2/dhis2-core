package org.hisp.dhis.program.notification;

import com.sun.imageio.plugins.common.I18N;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Zubair Asghar
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramNotificationInstanceParam
{
    public static final int DEFAULT_PAGE_SIZE = 50;

    public static final int DEFAULT_PAGE = 1;

    private String programInstance;

    private String programStageInstance;

    private Date scheduledAt;

    private Integer page;

    private Integer pageSize;

    public boolean hasProgramInstance()
    {
        return programInstance != null;
    }

    public boolean hasProgramStageInstance()
    {
        return programStageInstance != null;
    }

    public boolean hasPaging()
    {
        return page != null && pageSize != null;
    }

    public boolean hasScheduledAt()
    {
        return scheduledAt != null;
    }
}
