/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.gist;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PropertyPath;
import org.hisp.dhis.common.input.Fields;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.RelativePropertyContext;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.user.sharing.Sharing;
import org.springframework.stereotype.Component;

/**
 * The Gist bridge converts metadata parameters into Gist parameters to allow using the Gist backend
 * to respond to a metadata request in case the Gist backend can supply an equivalent response.
 *
 * <p>The major function of this component is to decide if the metadata request does have an
 * equivalent Gist response.
 *
 * <p>An equivalent response in this context might still differ from the metadata response in
 * several ways. For example, when no order is requested the list order might be different. The list
 * of object fields will always reflect the user's requested sequence whereas the sequence of fields
 * in the metadata API is not aligned with the fields parameter (but undefined and depends on
 * implementation details).
 *
 * @author Jan Bernitt
 * @since 2.44
 */
@Component
@RequiredArgsConstructor
public class GistBridge {

  /**
   * Information needed for the Gist bridge to convert a metadata object list parameters to Gist
   * object list parameters (and to make a decision on if that is possible).
   *
   * @param entityType same as controller
   * @param metadata the original decoded metadata API object list params
   * @param requestParametersNames all URL request params present
   * @param fields URL fields parameter (comma joined)
   * @param filter URL filter parameter(s) (comma joined)
   * @param order URL order parameter(s) (comma joined)
   * @param locale URL locale parameter
   */
  public record GistBridgeParams(
      Class<?> entityType,
      GetObjectListParams metadata,
      Set<String> requestParametersNames,
      String fields,
      String filter,
      String order,
      String locale) {}

  private final SchemaService schemaService;

  /**
   * Create Gist params from bridge params (which was created from metadata params and further
   * request information)
   *
   * @param params bridge params as extracted earlier to make a decision if the bridge can be used
   * @return the Gist object list params to use to (hopefully) create a comparable response to the
   *     original metadata request.
   */
  public static GistObjectListParams toObjectListParams(GistBridgeParams params) {
    GistObjectListParams res = new GistObjectListParams();
    res.setFields(params.fields());
    res.setFilter(params.filter());
    res.setPage(params.metadata().getPage());
    res.setPageSize(params.metadata().getPageSize());
    res.setOrder(params.order());
    res.setRootJunction(params.metadata().getRootJunction());
    res.setTotalPages(true);
    res.setLocale(params.locale());
    res.setReferences(false);
    res.setUnwrap(false);
    return res;
  }

  public boolean isSupportedObjectList(GistBridgeParams params) {
    try {
      return isSupportedObjectListInternal(params);
    } catch (RuntimeException ex) {
      return false; // we have to assume this might be fine in metadata API
    }
  }

  private boolean isSupportedObjectListInternal(GistBridgeParams params) {
    GetObjectListParams mdp = params.metadata();
    // check is there is explicit opt-in or opt-out, then short circuit
    Boolean gist = mdp.getGist();
    if (gist != null) return gist;
    // ATM bridge does only support paged results
    if (!mdp.isPaging()) return false;
    // Attributes have nasty persistence mapping so we give up
    if (params.entityType() == Attribute.class) return false;
    // check request parameters in general
    for (String name : params.requestParametersNames())
      if (!isSupportedRequestParameter(name)) return false;
    RelativePropertyContext context =
        new RelativePropertyContext(params.entityType(), schemaService::getSchema);
    String fields = params.fields();
    // default fields logic differs, give up
    if (fields.isEmpty()) return false;
    // check each field
    for (Fields.Field f : Fields.of(fields)) if (!isSupportedField(f, context)) return false;

    String filter = params.filter();
    if (filter.contains("token") || filter.contains("display")) return false;
    for (GistQuery.Filter f : GistQuery.Filter.ofList(filter))
      if (!isSupportedFilter(f, context)) return false;

    String order = params.order();
    if (order.contains("iasc")
        || order.contains("idesc")
        || order.contains("IASC")
        || order.contains("IDESC")) return false;

    for (GistQuery.Order o : GistQuery.Order.ofList(order))
      if (!isSupportedOrder(o, context)) return false;
    return true;
  }

  private static List<Property> getPropertyPath(
      PropertyPath path, RelativePropertyContext context) {
    Property leaf = context.resolve(path);
    if (leaf == null) return List.of(); // unknown property, give up
    return context.resolvePath(path);
  }

  private static boolean isSupportedField(Fields.Field field, RelativePropertyContext context) {
    if (field.isPreset()) return false;
    List<Property> path = getPropertyPath(field.propertyPath(), context);
    if (path.isEmpty() || path.size() > 2) return false;
    // any embedded is not supported
    if (path.stream().anyMatch(Property::isEmbeddedObject)) return false;
    // only identifiable objects are allowed as parent for a nested field
    if (path.size() == 2) {
      Property ref = path.get(0);
      Class<?> refType = ref.isCollection() ? ref.getItemKlass() : ref.getKlass();
      if (!IdentifiableObject.class.isAssignableFrom(refType)) return false;
      // continue to check leaf
    }
    Property leaf = path.get(path.size() - 1);
    if (leaf.isCollection()) return false;
    Class<?> type = leaf.getKlass();
    // non-persisted properties only allows (non-nested) translated properties
    if (!leaf.isPersisted()) return path.size() == 1 && isTranslatedProperty(leaf, context);
    return leaf.isSimple() || type == Sharing.class;
  }

  private static boolean isTranslatedProperty(Property property, RelativePropertyContext context) {
    if (!property.getName().startsWith("display")) return false;
    Property base =
        context.resolve(Property.resolveTranslationBasePropertyName(property.getName()));
    return base != null && base.isTranslatable() && base.isPersisted();
  }

  private static boolean isSupportedFilter(
      GistQuery.Filter filter, RelativePropertyContext context) {
    List<Property> path = getPropertyPath(filter.getPropertyPath(), context);
    if (path.size() != 1) return false;
    Property p = path.get(0);
    return p.isPersisted() && !p.isEmbeddedObject();
  }

  private static boolean isSupportedOrder(GistQuery.Order order, RelativePropertyContext context) {
    List<Property> path = getPropertyPath(order.getPropertyPath(), context);
    if (path.size() != 1) return false;
    Property p = path.get(0);
    return !p.isEmbeddedObject()
        && p.isSimple()
        && p.isSortable()
        && (p.isPersisted() || isTranslatedProperty(p, context));
  }

  /**
   * These URL parameters can generally be handled by the Gist API, but anything not in this list is
   * likely to have special meaning in the metadata API so the bridge should not be used to be safe
   */
  private static final Set<String> GIST_BRIDE_PARAMS =
      Set.of(
          "fields",
          "filter",
          "order",
          "page",
          "pageSize",
          "paging",
          "locale",
          "rootJunction",
          "gist");

  private static boolean isSupportedRequestParameter(String name) {
    return GIST_BRIDE_PARAMS.contains(name);
  }
}
