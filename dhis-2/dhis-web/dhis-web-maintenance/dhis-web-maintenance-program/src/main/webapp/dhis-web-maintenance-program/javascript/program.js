$(function() {
  dhis2.contextmenu.makeContextMenu({
    menuId: 'contextMenu',
    menuItemActiveClass: 'contextMenuItemActive'
  });
});

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function programIndicatorManagementForm( context ) {
  location.href = 'programIndicator.action?programId=' + context.id;
}

function showUpdateProgramForm( context ) {
  location.href = 'showUpdateProgramForm.action?id=' + context.id;
}

function programStageManagement( context ) {
  location.href = 'programStage.action?id=' + context.id;
}

function programValidationManagement( context ) {
  location.href = 'programValidation.action?programId=' + context.id;
}

function defineProgramAssociationsForm( context ) {
  location.href = 'defineProgramAssociationsForm.action?id=' + context.id;
}

function validationCriteria( context ) {
  location.href = 'validationCriteria.action?id=' + context.id;
}

function programReminder( context ){
  location.href = 'programReminder.action?id=' + context.id;
}

function viewProgramEntryForm( context ){
  location.href = 'viewProgramEntryForm.action?programId=' + context.id;
}

function showProgramDetails( context ) {
  jQuery.getJSON("getProgram.action", {
    id: context.id
  }, function( json ) {
    setInnerHTML('nameField', json.program.name);
    setInnerHTML('descriptionField', json.program.description);
    setInnerHTML('idField', json.program.uid);

    var type = i18n_with_registration;
    if( json.program.programType == "WITHOUT_REGISTRATION" ){
		type = i18n_without_registration;
	}
	setInnerHTML('typeField', type);

    var displayIncidentDate = ( json.program.displayIncidentDate == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('displayIncidentDateField', displayIncidentDate);

    var ignoreOverdueEvents = ( json.program.ignoreOverdueEvents == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('ignoreOverdueEventsField', ignoreOverdueEvents);

    var onlyEnrollOnce = ( json.program.onlyEnrollOnce == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('onlyEnrollOnceField', onlyEnrollOnce);

    var selectEnrollmentDatesInFuture = ( json.program.selectEnrollmentDatesInFuture == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('selectEnrollmentDatesInFutureField', selectEnrollmentDatesInFuture);

    var selectIncidentDatesInFuture = ( json.program.selectIncidentDatesInFuture == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('selectIncidentDatesInFutureField', selectIncidentDatesInFuture);

    var dataEntryMethod = ( json.program.dataEntryMethod == 'true') ? i18n_yes : i18n_no;
    setInnerHTML('dataEntryMethodField', dataEntryMethod);

    setInnerHTML('enrollmentDateLabelField', json.program.enrollmentDateLabel);
    setInnerHTML('incidentDateLabelField', json.program.incidentDateLabel);
    setInnerHTML('programStageCountField', json.program.programStageCount);
    setInnerHTML('noAttributesField', json.program.noAttributes);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Remove Program
// -----------------------------------------------------------------------------

function removeProgram( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'removeProgram.action');
}

function relationshipTypeOnchange() {
  
  clearListById('relationshipFromA');
  var relationshipSide = jQuery("#relationshipFromA");
  var relationshipType = jQuery('#relationshipTypeId option:selected');
  if( relationshipType.val() != "" ) {
  
	enable('relationshipFromA');
	enable('relatedProgramId');
	enable('relationshipText');
	
    var aIsToB = relationshipType.attr('aIsToB');
    var bIsToA = relationshipType.attr('bIsToA');

    relationshipSide.append('<option value="false">' + aIsToB + '</option>');
    relationshipSide.append('<option value="true">' + bIsToA + '</option>');
  }
  else
  {
	clearListById('relationshipFromA'); 
	jQuery('#relatedProgramId').val(""); 
	
	disable('relationshipFromA');
	disable('relatedProgramId');
	disable('relationshipText');
  }
}

function programOnChange() {
  var type = getFieldValue('programType');
  
  // anonymous
  if( type == "WITHOUT_REGISTRATION" ) {
    disable('onlyEnrollOnce');
    disable('enrollmentDateLabel');
    disable("displayIncidentDate");
    disable("incidentDateLabel");
    disable("generatedByEnrollmentDate");
    disable("availablePropertyIds");
    disable('ignoreOverdueEvents');
    disable('trackedEntityId');
    hideById('selectedList');
    hideById('programMessageTB');
	disable('compulsaryIdentifier');
	disable('displayFrontPageList');
	disable('useFirstStageDuringRegistration');

    jQuery("[name=displayed]").attr("disabled", true);
    jQuery("[name=displayed]").removeAttr("checked");

    jQuery("[name=nonAnonymous]").hide();
    jQuery('.multiEvents').hide();
  }
  else {
    enable('onlyEnrollOnce');
    jQuery("[name=displayed]").prop("disabled", false);
    enable("availablePropertyIds");
    enable("generatedByEnrollmentDate");
    enable('enrollmentDateLabel');
    enable("displayIncidentDate");
    enable('ignoreOverdueEvents');
    enable('trackedEntityId');
    showById('programMessageTB');
    showById("selectedList");
	enable('compulsaryIdentifier');
	enable('displayFrontPageList');
	enable('useFirstStageDuringRegistration');

    jQuery("[name=nonAnonymous]").show();
    
    if( byId('displayIncidentDate').checked ) {
      enable("incidentDateLabel");
    }
    else {
      disable("incidentDateLabel");
    }
	jQuery('.multiEvents').show();
  }
}

// -----------------------------------------------------------------------------
// select attributes
// -----------------------------------------------------------------------------

function selectProperties() {
  var selectedList = jQuery("#selectedList");
  jQuery("#availablePropertyIds").children().each(function( i, item ) {
    if( item.selected ) {
      html  = "<tr class='selected' id='" + item.value + "' ondblclick='unSelectProperties( this )'>";
	  html += "<td onmousedown='select(event,this)'>" + item.text + "</td>";
      html += "<td align='center'><input type='checkbox' name='displayed' value='" + item.value + "'></td>"
	  html += "<td align='center'><input type='checkbox' name='mandatory'></td>";
	  if( jQuery(item).attr('valuetype') =='DATE'){
		html += "<td align='center'><input type='checkbox' name='allowFutureDate'></td>";
	  }
	  else{
		html += "<td align='center'><input type='hidden' name='allowFutureDate'></td>";
	  }
	  html += "</tr>";
		
      selectedList.append(html);
      jQuery(item).remove();
    }
  });

  if( getFieldValue('type') == "3" ) {
    jQuery("[name=displayed]").attr("disabled", true);
  }
}

function selectAllProperties() {
  var selectedList = jQuery("#selectedList");
  jQuery("#availablePropertyIds").children().each(function( i, item ) {
    html = "<tr class='selected' id='" + item.value + "' ondblclick='unSelectDataElement( this )'>";
	html += "<td onmousedown='select(this)'>" + item.text + "</td>";
    html += "<td align='center'><input type='checkbox' name='displayed' value='" + item.value + "'></td>";
    html += "<td align='center'><input type='checkbox' name='mandatory'></td>";
    if( jQuery(item).attr('valuetype') =='DATE'){
		html += "<td align='center'><input type='checkbox' name='allowFutureDate'></td>";
	}
	else{
		html += "<td align='center'><input type='hidden' name='allowFutureDate'></td>";
	}
	html += "</tr>";
	
	selectedList.append(html);
    jQuery(item).remove();
  });
}

function unSelectProperties() {
  var availableList = jQuery("#availablePropertyIds");
  jQuery("#selectedList").find("tr").each(function( i, item ) {
    item = jQuery(item);
    if( item.hasClass("selected") ) {
      availableList.append("<option value='" + item.attr("id") + "' selected='true'>" + item.find("td:first").text() + "</option>");
      item.remove();
    }
  });
}

function unSelectAllProperties() {
  var availableList = jQuery("#availablePropertyIds");
  jQuery("#selectedList").find("tr").each(function( i, item ) {
    item = jQuery(item);
    availableList.append("<option value='" + item.attr("id") + "' selected='true'>" + item.find("td:first").text() + "</option>");
    item.remove();
  });
}

function select( event, element ) {
  if( !getKeyCode(event) )// Ctrl
  {
    jQuery("#selectedList .selected").removeClass('selected');
  }

  element = jQuery(element).parent();
  if( element.hasClass('selected') ) element.removeClass('selected');
  else element.addClass('selected');
}

function getKeyCode( e ) {
  var ctrlPressed = 0;

  if( parseInt(navigator.appVersion) > 3 ) {

    var evt = e ? e : window.event;

    if( document.layers && navigator.appName == "Netscape"
      && parseInt(navigator.appVersion) == 4 ) {
      // NETSCAPE 4 CODE
      var mString = (e.modifiers + 32).toString(2).substring(3, 6);
      ctrlPressed = (mString.charAt(1) == "1");
    }
    else {
      // NEWER BROWSERS [CROSS-PLATFORM]
      ctrlPressed = evt.ctrlKey;
    }
  }
  return ctrlPressed;
}

//-----------------------------------------------------------------------------
//Move Table Row Up and Down
//-----------------------------------------------------------------------------

function moveUpPropertyList() {
  var selectedList = jQuery("#selectedList");

  jQuery("#selectedList").find("tr").each(function( i, item ) {
    item = jQuery(item);
    if( item.hasClass("selected") ) {
      var prev = item.prev('#selectedList tr');
      if( prev.length == 1 ) {
        prev.before(item);
      }
    }
  });
}

function moveDownPropertyList() {
  var selectedList = jQuery("#selectedList");
  var items = new Array();
  jQuery("#selectedList").find("tr").each(function( i, item ) {
    items.push(jQuery(item));
  });

  for( var i = items.length - 1; i >= 0; i-- ) {
    var item = items[i];
    if( item.hasClass("selected") ) {
      var next = item.next('#selectedList tr');
      if( next.length == 1 ) {
        next.after(item);
      }
    }
  }
}

//-----------------------------------------------------------------------------
// Program Rule
//-----------------------------------------------------------------------------

function programRule( context )
{
	location.href = 'programRule.action?id=' + context.id;
}
