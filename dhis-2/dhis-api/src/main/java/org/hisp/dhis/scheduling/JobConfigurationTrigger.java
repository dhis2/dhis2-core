package org.hisp.dhis.scheduling;

import java.util.Date;
import lombok.Value;

@Value
public class JobConfigurationTrigger {
  String uid;
  // TODO SchedulingType type;
  Date lastExecuted;
  String cronExpression;
  Integer delay;
}
