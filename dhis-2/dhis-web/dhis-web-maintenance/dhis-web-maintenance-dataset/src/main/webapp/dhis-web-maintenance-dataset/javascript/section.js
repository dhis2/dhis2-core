// -----------------------------------------------------------------------------
// Section details form
// -----------------------------------------------------------------------------

function editSectionShow( context ) {
    location.href = 'editSection.action?sectionId=' + context.id;
}

function greySectionShow( context ) {
    location.href = 'greySection.action?sectionId=' + context.id;
}

function showSectionDetails( context )
{
	jQuery.get( 'getSection.action', { sectionId: context.id }, function ( json ) {
		setInnerHTML( 'nameField', json.section.name );
		setInnerHTML( 'dataSetField', json.section.dataSet );
		setInnerHTML( 'categoryComboField', json.section.categoryCombo );
		setInnerHTML( 'dataElementCountField', json.section.dataElementCount );  
		setInnerHTML( 'idField', json.section.uid ); 
		
		showDetails();
	} );
}

function sortOrderSubmit() 
{
	var dataSetId = $( "#dataSetId" ).val();

	if ( dataSetId && dataSetId != -1 ) 
	{
		window.location.href = "showSortSectionForm.action?dataSetId=" + dataSetId;
	} 
	else 
	{
		window.alert( i18n_please_select_dataset );
	}
}

function getSectionByDataSet( dataSetId ) 
{
	window.location.href = "section.action?dataSetId=" + dataSetId;
}

function removeSection( context ) 
{
	removeItem( context.id, context.name, i18n_confirm_delete, "removeSection.action" );
}

function addSectionSubmit() 
{
	var dataSetId = $( '#dataSetId' ).val();
	var categoryComboId = $( '#categoryComboId' ).val();

	if ( dataSetId && dataSetId != "-1" && categoryComboId && categoryComboId != "-1" ) 
	{
		window.location.href = "getSectionOptions.action?dataSetId=" + dataSetId + "&categoryComboId=" + categoryComboId;
	} 
	else 
	{
		setHeaderDelayMessage( i18n_please_select_dataset_categorycombo );		
	}
}

function toggle( dataElementId, optionComboId ) 
{
	var elementId = '[' + dataElementId;

	if (optionComboId != '') 
	{
		elementId = elementId + ']_[' + optionComboId;
	}

	elementId = elementId + ']';

	if (byId(elementId + '.text').disabled == true) 
	{
		byId(elementId + '.text').disabled = false;
		byId(elementId + '.button').value = i18n_disable;
	} 
	else 
	{
		byId(elementId + '.text').disabled = true;
		byId(elementId + '.button').value = i18n_enable;
	}
}

// -----------------------------------------------------------------------------
// Grey/Ungrey Fields
// -----------------------------------------------------------------------------

function saveGreyStatus( sectionId_, dataElementId_, optionComboId_ ) 
{
	var sectionId = sectionId_;
	var dataElementId = dataElementId_;
	var optionComboId = optionComboId_;
	var isGreyed;

	var elementId = '[' + dataElementId;	

	if ( optionComboId != '') {
		elementId = elementId + ']_[' + optionComboId;
	}

	elementId = elementId + ']';
	
	var txtElementId = elementId + '.txt';
	var btnElementId = elementId + '.btn';

	if ( document.getElementById( txtElementId ).disabled == true ) 
	{
		document.getElementById( txtElementId ).disabled = false;
		document.getElementById( btnElementId ).value = i18n_disable;

		isGreyed = false;		
	} 
	else 
	{
		document.getElementById( txtElementId ).disabled = true;
		document.getElementById( btnElementId ).value = i18n_enable;

		isGreyed = true;
	}
	
	// TODO check result
	
	$.post( 'saveSectionGreyStatus.action',
		{ sectionId:sectionId, dataElementId:dataElementId, optionComboId:optionComboId, isGreyed:isGreyed }, 
		function() {} );
}
