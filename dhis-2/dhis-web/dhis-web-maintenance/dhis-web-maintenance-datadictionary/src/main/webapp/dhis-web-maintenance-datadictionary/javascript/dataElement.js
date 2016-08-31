$(function() {
  $('#aggregationTypeSelect').change(updateZeroIsSignificant);
  $('#aggregationTypeSelect').change();

  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

function updateZeroIsSignificant() {
  var $this = $('#aggregationTypeSelect');

  if( $this.val() == 'SUM' ) {
    $('#zeroIsSignificant').removeAttr('disabled');
  }
  else if( $this.val() == 'AVERAGE' ) {
    $('#zeroIsSignificant').attr('disabled', true);
  }
}

function exportPDF( type ) {
  var params = "type=" + type;

  exportPdfByType(type, params);
}

function isValueTypeNumeric(value) {
  return value === 'INTEGER' ||
      value === 'INTEGER_POSITIVE' ||
      value === 'INTEGER_NEGATIVE' ||
      value === 'INTEGER_ZERO_OR_POSITIVE' ||
      value === 'NUMBER' ||
      value === 'UNIT_INTERVAL' ||
      value === 'PERCENTAGE';
}

function isValueTypeText(value) {
  return value === 'TEXT' || value === 'LONG_TEXT';
}

function changeValueType(value) {
  showById('aggregationOperatorSelect');
  if( isValueTypeNumeric(value) ) {
    showById('zeroIsSignificant');
  } else {
    hideById('zeroIsSignificant');
    hideById('aggregationOperatorSelect');

    if( value == 'BOOLEAN' ) {
      showById('aggregationOperatorSelect');
    }
  }

  updateAggreationOperation(value);
}

function dataValueOptionSetChanged() {
  var optionSetId = $('#selectedOptionSetId').val();
  var valueType = $('#selectedOptionSetId').find(':selected').data('valuetype');
  if ( optionSetId != 0 && valueType ) {
	  $('#valueType').val(valueType);
	  $('#valueType').prop('disabled', true);
  }
  else {
	  $('#valueType').prop('disabled', false);
  }
}

function updateAggreationOperation( value ) {
  if( isValueTypeText(value) || value == 'DATE' || value == 'TRUE_ONLY' ) {
    hideById("aggregationType");
  } else {
    showById("aggregationType");
  }
}

// -----------------------------------------------------------------------------
// Change data element group and data dictionary
// -----------------------------------------------------------------------------

function criteriaChanged() {
  var domainType = getListValue("domainTypeList");

  var url = "dataElement.action?domainType=" + domainType;

  window.location.href = url;
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showDataElementDetails( context ) {
  jQuery.get('../dhis-web-commons-ajax-json/getDataElement.action',
    { "id": context.id }, function( json ) {
      setInnerHTML('nameField', json.dataElement.name);
      setInnerHTML('shortNameField', json.dataElement.shortName);

      var description = json.dataElement.description;
      setInnerHTML('descriptionField', description ? description : '[' + i18n_none + ']');

      var typeMap = {
        'int': i18n_number,
        'bool': i18n_yes_no,
        'trueOnly': i18n_yes_only,
        'string': i18n_text,
        'date': i18n_date,
        'username': i18n_user_name
      };
      var type = json.dataElement.valueType;
      setInnerHTML('typeField', typeMap[type]);

      var domainTypeMap = {
        'aggregate': i18n_aggregate,
        'tracker': i18n_tracker
      };
      var domainType = json.dataElement.domainType;
      setInnerHTML('domainTypeField', domainTypeMap[domainType]);

      var aggregationOperator = json.dataElement.aggregationType;
      var aggregationOperatorText = i18n_none;
      if( aggregationOperator == 'SUM' ) {
        aggregationOperatorText = i18n_sum;
      } else if( aggregationOperator == 'AVERAGE' ) {
        aggregationOperatorText = i18n_average;
      }
      setInnerHTML('aggregationOperatorField', aggregationOperatorText);

      setInnerHTML('categoryComboField', json.dataElement.categoryCombo);

      var url = json.dataElement.url;
      setInnerHTML('urlField', url ? '<a href="' + url + '">' + url + '</a>' : '[' + i18n_none + ']');

      var lastUpdated = json.dataElement.lastUpdated;
      setInnerHTML('lastUpdatedField', lastUpdated ? lastUpdated : '[' + i18n_none + ']');

      var approveData = json.dataElement.approveData;
      setInnerHTML('approveDataField', approveData == "true" ? i18n_yes : i18n_no );
      
      var dataSets = joinNameableObjects(json.dataElement.dataSets);
      setInnerHTML('dataSetsField', dataSets ? dataSets : '[' + i18n_none + ']');

	  setInnerHTML('idField', json.dataElement.uid);

      showDetails();
    });
}

function removeDataElement( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeDataElement.action');
}

function domainTypeChange( domainType ) {
  if( domainType == 'AGGREGATE' ) {
    enable('selectedCategoryComboId');
  }
  else {
    setFieldValue('selectedCategoryComboId', getFieldValue('defaultCategoryCombo'));
    disable('selectedCategoryComboId');
  }
}

function showUpdateDataElementForm( context ) {
  location.href = 'showUpdateDataElementForm.action?id=' + context.id + '&update=true';
}

function showCloneDataElementForm( context ) {
  location.href = 'showAddDataElementForm.action?id=' + context.id;
}
