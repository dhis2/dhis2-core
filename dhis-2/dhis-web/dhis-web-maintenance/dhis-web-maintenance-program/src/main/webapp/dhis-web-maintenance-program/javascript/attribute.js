$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });

  typeOnChange();
  optionSetChanged();
  applyConfidentialEffect();
  
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateAttributeForm(context) {
  location.href = 'showUpdateAttributeForm.action?id=' + context.id;
}

function showAttributeDetails(context) {
  jQuery.getJSON('getAttribute.action', { id: context.id },
    function(json) {
      setInnerHTML('nameField', json.attribute.name);
      setInnerHTML('descriptionField', json.attribute.description);
      setInnerHTML('optionSetField', json.attribute.optionSet);
      setInnerHTML('idField', json.attribute.uid);

      var unique = ( json.attribute.unique == 'true') ? i18n_yes : i18n_no;
      setInnerHTML('uniqueField', unique);
      
      var generated = ( json.attribute.generated == 'true') ? i18n_yes : i18n_no;
      setInnerHTML('generatedField', generated);
      
      setInnerHTML('patternField', json.attribute.pattern);

      var inherit = ( json.attribute.inherit == 'true') ? i18n_yes : i18n_no;
      setInnerHTML('inheritField', inherit);

      var confidential = ( json.attribute.confidential == 'true') ? i18n_yes : i18n_no;
      setInnerHTML('confidentialField', confidential);

      var valueType = json.attribute.valueType;
      var typeMap = attributeTypeMap();
      setInnerHTML('valueTypeField', typeMap[valueType]);

      if( json.attribute.unique == 'true' ) {
        var orgunitScope = json.attribute.orgunitScope;
        var programScope = json.attribute.programScope;
        if( orgunitScope == 'false' && programScope == 'false' ) {
          setInnerHTML('scopeField', i18n_whole_system);
        }
        else if( orgunitScope == 'true' && programScope == 'false' ) {
          setInnerHTML('scopeField', i18n_orgunit);
        }
        else if( orgunitScope == 'false' && programScope == 'true' ) {
          setInnerHTML('scopeField', i18n_program);
        }
        else {
          setInnerHTML('scopeField', i18n_program_within_orgunit);
        }
      }

      showDetails();
    });
}

function attributeTypeMap() {
  var typeMap = [];
  typeMap['NUMBER'] = i18n_number;
  typeMap['TEXT'] = i18n_text;
  typeMap['BOOLEAN'] = i18n_yes_no;
  typeMap['TRUE_ONLY'] = i18n_yes_only;
  typeMap['DATE'] = i18n_date;
  typeMap['PHONE_NUMBER'] = i18n_phone_number;
  typeMap['TRACKER_ASSOCIATE'] = i18n_tracker_associate;
  return typeMap;
}

// -----------------------------------------------------------------------------
// Remove Attribute
// -----------------------------------------------------------------------------

function removeAttribute(context) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeAttribute.action');
}


function typeOnChange() {
  var type = getFieldValue('valueType');

  hideById("trackedEntityRow");
  disable("trackedEntityId");

  if( type == "TRACKER_ASSOCIATE" ) {
    showById("trackedEntityRow");
    enable("trackedEntityId");
  }

  if( type == "INTEGER" || type == "INTEGER_POSITIVE" || type == "INTEGER_NEGATIVE" || type == "INTEGER_ZERO_OR_POSITIVE" ||
	type == "NUMBER" || type == 'TEXT' || type == 'LETTER' || type == 'PHONE_NUMBER' ) {
    enable("unique");
  }
  else {
    disable("unique");
  }
}

function optionSetChanged() {
  var optionSetId = $('#optionSetId').val();
  var valueType = $('#optionSetId').find(':selected').data('valuetype');
  if ( optionSetId != 0 && valueType ) {
	  $('#valueType').val(valueType);
	  $('#valueType').prop('disabled', true);
  }
  else {
	  $('#valueType').prop('disabled', false);
  }
}

function uniqueOnChange() {
  if( $('#unique').attr('checked') == "checked" ) {
    jQuery('[name=uniqueTR]').show();
    jQuery('#valueType [value=BOOLEAN]').hide();
    jQuery('#valueType [value=TRUE_ONLY]').hide();
    jQuery('#valueType [value=DATE]').hide();
    jQuery('#valueType [value=TRACKER_ASSOCIATE]').hide();
    jQuery('#valueType [value=USERNAME]').hide();
    
    if( $('#scope').find(":selected").val() == "" ) {
        jQuery('[name=generatedTR]').show();
    }
    else {
        jQuery('[name=generatedTR]').hide();
    }
    
    generatedOnChange();
  }
  else {
    jQuery('[name=uniqueTR]').hide();
    jQuery('#valueType [value=BOOLEAN]').show();
    jQuery('#valueType [value=TRUE_ONLY]').show();
    jQuery('#valueType [value=DATE]').show();
    jQuery('#valueType [value=TRACKER_ASSOCIATE]').show();
    jQuery('#valueType [value=USERNAME]').show();
    
    jQuery('[name=generatedTR]').hide();
    generatedOnChange();
  }
}

function generatedOnChange() {
  if( $('#generated').attr('checked') == "checked" &&
          $('#unique').attr('checked') == "checked" ) {
    jQuery('[name=generatedPatternTR]').show();
  }
  else {
    jQuery('[name=generatedPatternTR]').hide();   
  }
}

function applyConfidentialEffect() {
	if( $('#confidential').attr('checked') == "checked" ) {
		$('#searchScope').find('option[value="NOT_SEARCHABLE"]').prop('selected', true);
		$('#searchScope').prop('disabled', true);
	}
	else {
		$('#searchScope').prop('disabled', false);
	}
}
