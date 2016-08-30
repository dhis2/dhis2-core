dhis2.util.namespace( 'dhis2.pi' );

dhis2.pi.aggregatableValueTypes = [
  'BOOLEAN', 'TRUE_ONLY', 'NUMBER', 'UNIT_INTERVAL', 'PERCENTAGE',
  'INTEGER', 'INTEGER_POSITIVE', 'INTEGER_NEGATIVE', 'INTEGER_ZERO_OR_POSITIVE',
  'DATE', 'DATETIME' ];  

$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function showUpdateProgramIndicator( context ) {
  location.href = 'showUpdateProgramIndicator.action?id=' + context.id;
}

function removeIndicator( context ) {
  removeItem( context.id, context.name, i18n_confirm_delete , 'removeProgramIndicator.action' );
}

function showProgramIndicatorDetails( context ) {
  jQuery.getJSON('getProgramIndicator.action', { id: context.id }, function( json ) {
    setInnerHTML('nameField', json.programIndicator.name);
    setInnerHTML('codeField', json.programIndicator.code);
    setInnerHTML('descriptionField', json.programIndicator.description);
    setInnerHTML('valueTypeField', json.programIndicator.valueType);
    setInnerHTML('rootDateField', json.programIndicator.rootDate);
    setInnerHTML('expressionField', json.programIndicator.expression);
    setInnerHTML('filterField', json.programIndicator.filter);
    setInnerHTML('idField', json.programIndicator.uid);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Remove Program Indicator
// -----------------------------------------------------------------------------

function removeProgramIndicator( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeProgramIndicator.action');
}

function filterExpressionSelect( event, value, fieldName ) {
	var field = byId(fieldName);
	
	for ( var index = 0; index < field.options.length; index++ )
    {
		var option = field.options[index];
		
		if ( value.length == 0 || option.text.toLowerCase().indexOf( value.toLowerCase() ) != -1 )
		{
			option.style.display = "block";
		}
		else
		{
			option.style.display = "none";
		}
    }
}

function getTrackedEntityDataElements( type ) {
	var fieldId = type + '-data-elements';
	clearListById(fieldId);

	var psSelectId = type + '-program-stage';
	var programStageId = getFieldValue(psSelectId);

	if(programStageId) {
		jQuery.getJSON('../api/programStages/' + programStageId + '.json?fields=programStageDataElements[dataElement[id,displayName|rename(name),valueType]',
		{
			programId: getFieldValue('programId'),
			programStageUid: programStageId
		}, 
		function( json ) {
			var dataElements = jQuery('#' + fieldId);
			$.each( json.programStageDataElements, function(inx, val) {
				var de = val.dataElement;
				if ( !('expression' == type && de.valueType && dhis2.pi.aggregatableValueTypes.indexOf(de.valueType) == -1)) {
					dataElements.append("<option value='" + de.id + "'>" + de.name + "</option>");
				}
			} );
		});
	}
}

function insertDataElement( type ) {
  var psFieldId = type + '-program-stage',
      deFieldId = type + '-data-elements',
      areaId = type,
      programStageId = getFieldValue(psFieldId),
      dataElementId = getFieldValue(deFieldId);

  insertTextCommon(areaId, "#{" + programStageId + "." + dataElementId + "}");
  getExpressionDescription( type );
}

function insertAttribute( type ){
  var atFieldId = type + '-attributes',
      areaId = type,
      attributeId = getFieldValue(atFieldId);

  insertTextCommon(areaId, "A{" + attributeId + "}");
  getExpressionDescription( type );
}

function insertVariable( type ){
  var varFieldId = type + '-variables',
      areaId = type,
      variableId = getFieldValue(varFieldId);

  insertTextCommon(areaId, "V{" + variableId + "}");
  getExpressionDescription( type );
}

function insertConstant( type ){
  var coFieldId = type + '-constants',
      areaId = type,
      constantId = getFieldValue(coFieldId);

  insertTextCommon(areaId, "C{" + constantId + "}");
  getExpressionDescription( type );
}

function insertOperator( type, value ) {
  insertTextCommon(type, ' ' + value + ' ');
  getExpressionDescription( type );
}

function getExpressionDescription( type ) {
	var expression = getFieldValue( type );
	
	if( !expression || expression == '' )
	{
		setInnerHTML(type + '-description', '');
	}
	else
	{
		$.ajax({
			url: '../api/programIndicators/' + type + '/description',
			type: 'post',
			data: expression,
			contentType: 'text/plain',
			success: function( json ) {
				if ('OK' == json.status) {
					setInnerHTML(type + '-description', json.description);
				}
				else {
					setInnerHTML(type + '-description', json.message);
				}
			}
		});
	}
}

function programIndicatorOnChange() {
  var valueType = getFieldValue('valueType');
  if( valueType == 'int' ) {
    hideById('rootDateTR');
    disable('rootDate');
  }
  else {
    showById('rootDateTR');
    enable('rootDate');
  }
}

function setExpressionCount(type) {
	$('#aggregationType').val('COUNT');
	
	if ('psi' == type) {
		$('#expression').val('V{event_count}');
	}
	else if ('pi' == type) {
		$('#expression').val('V{enrollment_count}');
	}
	else if ('tei' == type) {
		$('#expression').val('V{tei_count}');
	}
}


