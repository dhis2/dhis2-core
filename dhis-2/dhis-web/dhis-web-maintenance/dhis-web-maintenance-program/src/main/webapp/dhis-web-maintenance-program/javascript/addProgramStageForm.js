var duplicate = false;
jQuery(document).ready(function() {
  jQuery("#availableList").dhisAjaxSelect({
    source: "../dhis-web-commons-ajax-json/getDataElements.action?domain=patient",
    iterator: "dataElements",
    connectedTo: 'selectedDataElementsValidator',
    handler: function( item ) {
      var option = jQuery("<option />");
      option.text(item.name);
      option.attr("value", item.id);

      if( item.optionSet == "true" ) {
        option.attr("valuetype", "optionset");
      }
      else {
        option.attr("valuetype", item.valueType);
      }
      return option;
    }
  });

  checkValueIsExist("name", "validateProgramStage.action", {id: getFieldValue('programId')});
});
