/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.validation.validator.trackedentity;

import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1006;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1076;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1090;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.getTrackedEntityAttributes;
import static org.hisp.dhis.tracker.imports.validation.validator.ValidationUtils.validateOptionSet;

import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Attribute;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.hisp.dhis.tracker.imports.validation.Validator;
import org.hisp.dhis.tracker.imports.validation.service.attribute.TrackedAttributeValidationService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component("org.hisp.dhis.tracker.imports.validation.validator.trackedentity.AttributeValidator")
class AttributeValidator
    extends org.hisp.dhis.tracker.imports.validation.validator.AttributeValidator
    implements Validator<org.hisp.dhis.tracker.imports.domain.TrackedEntity> {

  private final OptionService optionService;

  public AttributeValidator(
      TrackedAttributeValidationService teAttrService,
      DhisConfigurationProvider dhisConfigurationProvider,
      OptionService optionService) {
    super(teAttrService, dhisConfigurationProvider);
    this.optionService = optionService;
  }

  @Override
  public void validate(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity) {
    TrackedEntityType trackedEntityType =
        bundle.getPreheat().getTrackedEntityType(trackedEntity.getTrackedEntityType());

    TrackedEntity te = bundle.getPreheat().getTrackedEntity(trackedEntity.getTrackedEntity());
    OrganisationUnit organisationUnit =
        bundle.getPreheat().getOrganisationUnit(trackedEntity.getOrgUnit());

    validateMandatoryAttributes(reporter, bundle, trackedEntity, trackedEntityType);
    validateAttributes(reporter, bundle, trackedEntity, te, organisationUnit, trackedEntityType);
  }

  private void validateMandatoryAttributes(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntityType trackedEntityType) {

    Set<MetadataIdentifier> trackedEntityAttributes =
        getTrackedEntityAttributes(bundle, trackedEntity.getUid());

    TrackerIdSchemeParams idSchemes = bundle.getPreheat().getIdSchemes();
    trackedEntityType.getTrackedEntityTypeAttributes().stream()
        .filter(
            trackedEntityTypeAttribute ->
                Boolean.TRUE.equals(trackedEntityTypeAttribute.isMandatory()))
        .map(TrackedEntityTypeAttribute::getTrackedEntityAttribute)
        .map(idSchemes::toMetadataIdentifier)
        .filter(mandatoryAttribute -> !trackedEntityAttributes.contains(mandatoryAttribute))
        .forEach(
            attribute ->
                reporter.addError(
                    trackedEntity,
                    E1090,
                    attribute,
                    trackedEntityType.getUid(),
                    trackedEntity.getTrackedEntity()));
  }

  protected void validateAttributes(
      Reporter reporter,
      TrackerBundle bundle,
      org.hisp.dhis.tracker.imports.domain.TrackedEntity trackedEntity,
      TrackedEntity te,
      OrganisationUnit orgUnit,
      TrackedEntityType trackedEntityType) {
    TrackerPreheat preheat = bundle.getPreheat();

    for (Attribute attribute : trackedEntity.getAttributes()) {
      TrackedEntityAttribute tea = preheat.getTrackedEntityAttribute(attribute.getAttribute());

      if (tea == null) {
        reporter.addError(trackedEntity, E1006, attribute.getAttribute());
        continue;
      }

      if (attribute.getValue() == null) {
        Optional<TrackedEntityTypeAttribute> optionalTea =
            Optional.of(trackedEntityType)
                .map(tet -> tet.getTrackedEntityTypeAttributes().stream())
                .flatMap(
                    tetAtts ->
                        tetAtts
                            .filter(
                                teaAtt ->
                                    attribute
                                            .getAttribute()
                                            .isEqualTo(teaAtt.getTrackedEntityAttribute())
                                        && teaAtt.isMandatory() != null
                                        && teaAtt.isMandatory())
                            .findFirst());

        if (optionalTea.isPresent())
          reporter.addError(
              trackedEntity,
              E1076,
              TrackedEntityAttribute.class.getSimpleName(),
              attribute.getAttribute());

        continue;
      }

      validateAttributeValue(reporter, trackedEntity, tea, attribute.getValue());
      validateAttrValueType(reporter, bundle, trackedEntity, attribute, tea);
      validateOptionSet(reporter, trackedEntity, tea, attribute.getValue(), optionService);

      validateAttributeUniqueness(
          reporter, preheat, trackedEntity, attribute.getValue(), tea, te, orgUnit);
    }
  }
}
