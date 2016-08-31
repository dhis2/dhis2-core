var tmpDataSetId;
var tmpSource;

// -----------------------------------------------------------------------------
// DataSet details form
// -----------------------------------------------------------------------------

function showDataSetDetails( context ) {
  jQuery.get('../dhis-web-commons-ajax-json/getDataSet.action', {
    id: context.id
  }, function( json ) {
    setInnerHTML('nameField', json.dataSet.name);
    setInnerHTML('descriptionField', json.dataSet.description);
    setInnerHTML('frequencyField', json.dataSet.frequency);
    setInnerHTML('dataElementCountField', json.dataSet.dataElementCount);
    setInnerHTML('dataEntryFormField', json.dataSet.dataentryform);
    setInnerHTML('idField', json.dataSet.uid);

    showDetails();
  });
}

// -----------------------------------------------------------------------------
// Delete DataSet
// -----------------------------------------------------------------------------

function removeDataSet( context ) {
  removeItem(context.id, context.name, i18n_confirm_delete, 'delDataSet.action');
}

// ----------------------------------------------------------------------
// DataEntryForm
// ----------------------------------------------------------------------

function viewDataEntryForm( context ) {
  window.location.href = 'viewDataEntryForm.action?dataSetId=' + context.id;
}

function displayCompulsoryDataElementsForm( context ) {
  location.href = 'displayCompulsoryDataElementsForm.action?id=' + context.id;
}

function exportPdfDataSet( context ) {
  location.href = '../api/pdfForm/dataSet/' + context.uid;
}

function editDataSetForm( context ) {
  location.href = 'editDataSetForm.action?dataSetId=' + context.id;
}

function defineDataSetAssociationsForm( context ) {
  location.href = 'defineDataSetAssociationsForm.action?dataSetId=' + context.id;
}

function viewSectionList( context ) {
  location.href = 'section.action?dataSetId=' + context.id;
}

function frequencyChanged() {
  $('#workflowId option').each(function() {
    this.disabled = this.value > 0 && !this.text.endsWith(" (" + $('#frequencySelect').val() + ")");
  })
}
