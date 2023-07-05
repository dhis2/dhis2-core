/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.validation;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * This class represents the most fundamental parameters to run a validation rule analysis. The
 * class is immutable and is meant to work as a gap-filler for the different use-cases of validation
 * rule analysis (Data set validation, "manual" validation and scheduled validation).
 *
 * @author Stian Sandvold
 */
public final class ValidationAnalysisParams {
  /*
   * Required properties: Although required, they can be empty collections. If
   * any of the collections are empty, there would be nothing to analyse. This
   * is still a valid state for the params to have. The attribute option combo
   * can also be null, in that case the default attribute option combo will be
   * used. The organisation unit can be null, in that case all organisation
   * units will be used.
   */
  private ImmutableSet<ValidationRule> validationRules;

  private OrganisationUnit orgUnit;

  private ImmutableSet<Period> periods;

  private CategoryOptionCombo attributeOptionCombo;

  /*
   * Optional properties: These have default values, which disables the
   * behaviour represented by them.
   */
  private boolean includeOrgUnitDescendants = false;

  private int maxResults = ValidationService.MAX_INTERACTIVE_ALERTS;

  private boolean sendNotifications = false;

  private boolean persistResults = false;

  private int dayInPeriod = -1;

  /**
   * Gets the rules selected for analysis
   *
   * @return a collection of validation rules to be analysed
   */
  public ImmutableSet<ValidationRule> getValidationRules() {
    return validationRules;
  }

  /**
   * Gets the organisation unit selected for analysis
   *
   * @return the organisation unit to be analysed
   */
  public OrganisationUnit getOrgUnit() {
    return orgUnit;
  }

  /**
   * Gets the periods selected for analysis
   *
   * @return a collection of periods to be analysed
   */
  public ImmutableSet<Period> getPeriods() {
    return periods;
  }

  /**
   * Gets the attribute option combo if defined
   *
   * @return an attribute option combo to be analysed
   */
  public CategoryOptionCombo getAttributeOptionCombo() {
    return attributeOptionCombo;
  }

  /**
   * Gets whether or not organisation unit descendants are included
   *
   * @return true if organisation unit descendants are included, false if not.
   */
  public boolean isIncludeOrgUnitDescendants() {
    return includeOrgUnitDescendants;
  }

  /**
   * Gets whether or not notifications should be sent for this analysis
   *
   * @return true if notifications should be sent, false if not.
   */
  public boolean isSendNotifications() {
    return sendNotifications;
  }

  /**
   * Gets whether or not the results of the analysis should be persisted in the database after the
   * analysis
   *
   * @return true if results should be persisted, false if not.
   */
  public boolean isPersistResults() {
    return persistResults;
  }

  /**
   * Gets which day of a period the analysis should be run for. If a validation rule is utilizing
   * sliding windows, this property will decide the positioning of the window relative to the period
   * checked. In cases where the dayInPeriod is larger than the length of a period, the last day of
   * the period will be used. If dayInPeriod is -1, it will not be used to position the window, and
   * the window will be positioned according to the current date.
   *
   * @return -1 if disabled, or a positive integer if enabled.
   */
  public int getDayInPeriod() {
    return dayInPeriod;
  }

  /**
   * Limits the number of results we should look for. This can help prevent the analysis running too
   * long by stopping after a set number of results, as well as limit any payload trough api.
   *
   * @return number of results we should look for
   */
  public int getMaxResults() {
    return maxResults;
  }

  public static class Builder {
    private ValidationAnalysisParams params;

    public Builder(
        Collection<ValidationRule> validationRules,
        OrganisationUnit orgUnit,
        Collection<Period> periods) {
      this.params = new ValidationAnalysisParams();
      this.params.validationRules = ImmutableSet.copyOf(validationRules);
      this.params.orgUnit = orgUnit;
      this.params.periods = ImmutableSet.copyOf(periods);
    }

    /**
     * Sets the attributeOptionCombo to use.
     *
     * @param attributeOptionCombo the attributeOptionCombo to use
     * @return the updated builder object
     */
    public Builder withAttributeOptionCombo(CategoryOptionCombo attributeOptionCombo) {
      this.params.attributeOptionCombo = attributeOptionCombo;
      return this;
    }

    /**
     * If set to true, organisation unit descendants will be included
     *
     * @param includeOrgUnitDescendants true if organisation unit descendants will be included,
     *     false if not.
     * @return the updated builder object
     */
    public Builder withIncludeOrgUnitDescendants(boolean includeOrgUnitDescendants) {
      this.params.includeOrgUnitDescendants = includeOrgUnitDescendants;
      return this;
    }

    /**
     * If set to true, results will be persisted in the database
     *
     * @param persistResults true if results should be persisted, false if not.
     * @return the updated builder object
     */
    public Builder withPersistResults(boolean persistResults) {
      this.params.persistResults = persistResults;
      return this;
    }

    /**
     * If set to true, notifications will be sent after the analysis is completed if any results
     * where found
     *
     * @param sendNotifications true if notifications should be sent, false if not.
     * @return the updated builder object
     */
    public Builder withSendNotifications(boolean sendNotifications) {
      this.params.sendNotifications = sendNotifications;
      return this;
    }

    /**
     * Decides the position of the sliding window, for rules that utilizes this feature. -1 means
     * disabled, and integers bigger than the period length will position the window to the end of
     * the period. If -1 the analysis will use todays date to position the window
     *
     * @param dayInPeriod -1 if disabled, any positive integer if enabled
     * @return the updated builder object
     */
    public Builder withDayInPeriod(int dayInPeriod) {
      this.params.dayInPeriod = dayInPeriod;
      return this;
    }

    /**
     * The max number of results we want from the analysis.
     *
     * @param maxResults the number of results
     * @return the updated builder object
     */
    public Builder withMaxResults(int maxResults) {
      this.params.maxResults = maxResults;
      return this;
    }

    /**
     * Returns the params object
     *
     * @return the final ValidationAnalysisParams object.
     */
    public ValidationAnalysisParams build() {
      return params;
    }
  }
}
