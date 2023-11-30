package org.hisp.dhis.merge;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.feedback.MergeReport;

public class MergeQueryProcessor<T extends BaseIdentifiableObject, U extends MergeService<T>> {

  public MergeQueryProcessor(U mergeService) {
    this.mergeService = mergeService;
  }

  private final U mergeService;

  public MergeReport processMerge(MergeQuery mergeQuery, MergeReport mergeReport) {
    MergeRequest<T> mergeRequest = mergeService.getFromQuery(mergeQuery, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    MergeReport validate = mergeService.validate(mergeRequest, mergeReport);
    if (validate.hasErrorMessages()) return mergeReport;

    return mergeService.merge(mergeRequest, mergeReport);
  }
}
