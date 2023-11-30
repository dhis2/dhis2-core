package org.hisp.dhis.merge;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.feedback.MergeReport;

/**
 * This class is used to process merging of {@link BaseIdentifiableObject}s uniformly, in a
 * standardised fashion. It requires an implementation of {@link MergeService}, which will process
 * the merging for its required use case. <br>
 * It essentially calls each method of the {@link MergeService} in the following order:<br>
 *
 * <ol>
 *   <li>transform
 *   <li>validate
 *   <li>merge
 * </ol>
 *
 * @param <T> type extending BaseIdentifiableObject
 * @param <U> type extending MergeService
 */
public class MergeQueryProcessor<T extends BaseIdentifiableObject, U extends MergeService<T>> {

  public MergeQueryProcessor(U mergeService) {
    this.mergeService = mergeService;
  }

  private final U mergeService;

  /**
   * @param mergeQuery {@link MergeQuery} to process
   * @param mergeReport {@link MergeReport} to update with any errors and return
   * @return updated {@link MergeReport} with any errors
   */
  public MergeReport processMerge(MergeQuery mergeQuery, MergeReport mergeReport) {
    MergeRequest<T> mergeRequest = mergeService.transform(mergeQuery, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    MergeReport updatedReport = mergeService.validate(mergeRequest, mergeReport);
    if (updatedReport.hasErrorMessages()) return mergeReport;

    return mergeService.merge(mergeRequest, mergeReport);
  }
}
