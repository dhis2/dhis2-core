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
 */
public class MergeQueryProcessor<T extends BaseIdentifiableObject> {

  public MergeQueryProcessor(MergeService<T> mergeService) {
    this.mergeService = mergeService;
  }

  private final MergeService<T> mergeService;

  public MergeReport processMergeRequest(MergeQuery query, MergeType mergeType) {
    MergeReport mergeReport = new MergeReport(mergeType);
    return processMerge(query, mergeReport);
  }

  /**
   * Processes a merge query in full, using the implemented {@link MergeService} provided.
   *
   * @param mergeQuery {@link MergeQuery} to process
   * @param mergeReport {@link MergeReport} to update with any errors
   * @return updated {@link MergeReport} with any errors
   */
  public MergeReport processMerge(MergeQuery mergeQuery, MergeReport mergeReport) {
    MergeRequest<T> mergeRequest = mergeService.transform(mergeQuery, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    MergeRequest<T> validatedRequest = mergeService.validate(mergeRequest, mergeReport);
    if (mergeReport.hasErrorMessages()) return mergeReport;

    return mergeService.merge(validatedRequest, mergeReport);
  }
}
