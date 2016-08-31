function addReport() {
  if( $("#reportForm #name").val().trim().length == 0 ) {
    setHeaderDelayMessage(i18n_specify_name);
    return false;
  }

  if( $("#reportForm #id").val().trim().length == 0 && !hasText("upload") ) {
    setHeaderDelayMessage(i18n_please_specify_file);
    return false;
  }

  $("#reportForm").submit();
}

function setReportType() {
  var type = $("#type :selected").val();

  if( "JASPER_REPORT_TABLE" == type ) {
    $(".jasperJdbcDataSource").hide();
    $(".htmlDataSource").hide();
    $(".jasperReportTableDataSource").show();
  }
  else if( "JASPER_JDBC" == type ) {
    $(".jasperReportTableDataSource").hide();
    $(".htmlDataSource").hide();
    $(".jasperJdbcDataSource").show();
  }
  else if( "HTML" == type ) {
    $(".jasperReportTableDataSource").hide();
    $(".jasperJdbcDataSource").hide();
    $(".htmlDataSource").show();
  }
}

// -----------------------------------------------------------------------------
// View details
// -----------------------------------------------------------------------------

function createReportParams( context ) {
  location.href = 'getReportParams.action?uid=' + context.uid + '&mode=report&type=' + context['report-type'];
}

function displayAddReportForm( context ) {
  location.href = 'displayAddReportForm.action?id=' + context.id;
}

function removeReport( context ) {
  removeItem(context.id, context.name, i18n_confirm_remove_report, "removeReport.action");
}

function showReportDetails( context ) {
  jQuery.get('getReport.action', { "id": context.id }, function( json ) {
    setInnerHTML('nameField', json.report.name);
	setInnerHTML('idField', json.report.uid);

    var reportTableName = json.report.reportTableName;
    setInnerHTML('reportTableNameField', reportTableName ? reportTableName : '[' + i18n_none + ']');

    var orgGroupSets = json.report.orgGroupSets;
    setInnerHTML('orgGroupSetsField', orgGroupSets == 'true' ? i18n_yes : i18n_no);

    showDetails();
  });
}

