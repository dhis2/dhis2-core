package org.hisp.dhis.gist;

import lombok.Data;
import org.hisp.dhis.common.OpenApi;

@Data
@OpenApi.Shared
@OpenApi.Property // all fields are public properties
public class GistObjectParams {

  @OpenApi.Description(
      """
      Switch translation language of display names. If not specified the translation language is the one configured in the users account settings.
      See [Gist locale parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_locale).""")
  String locale = "";

  @OpenApi.Description(
      """
      Fields like _name_ or _shortName_ can be translated (internationalised).
      By default, any translatable field that has a translation is returned translated given that the user requesting the gist has an interface language configured.
      To return the plain non-translated field use `translate=false`.
      See [Gist translate parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_translate).""")
  boolean translate = true;

  @OpenApi.Description(
      """
      The extent of fields to include when no specific list of fields is provided using `fields` so that  that listed fields are automatically determined.
      See [Gist auto parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#the-auto-parameter).""")
  GistAutoType auto;

  @OpenApi.Description(
      """
      Use absolute (`true`) or relative URLs (`false`, default) when linking to other objects in `apiEndpoints`.
      See [Gist absoluteUrls parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_absoluteUrls).""")
  boolean absoluteUrls = false;

  @OpenApi.Description(
      """
      By default, the Gist API includes links to referenced objects. This can be disabled by using `references=false`.""")
  boolean references = true;

  @OpenApi.Description(
      """
      A comma seperated list of fields to include in the response. `*` includes all `auto` detected fields.
      See [Gist fields parameter](https://docs.dhis2.org/en/develop/using-the-api/dhis-core-version-master/metadata-gist.html#gist_parameters_fields).""")
  @OpenApi.Shared.Inline
  @OpenApi.Property(OpenApi.PropertyNames[].class)
  String fields;

  public GistAutoType getAuto(GistAutoType defaultValue) {
    return auto == null ? defaultValue : auto;
  }
}
