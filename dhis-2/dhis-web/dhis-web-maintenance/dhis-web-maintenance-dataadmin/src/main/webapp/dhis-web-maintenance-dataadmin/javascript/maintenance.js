function performMaintenance() {
  var clearAnalytics = $("#clearAnalytics").is(":checked");
  var zeroValues = $("#zeroValues").is(":checked");
  var prunePeriods = $("#prunePeriods").is(":checked");
  var removeExpiredInvitations = $("#removeExpiredInvitations").is(":checked");
  var dropSqlViews = $("#dropSqlViews").is(":checked");
  var createSqlViews = $("#createSqlViews").is(":checked");
  var updateCategoryOptionCombos = $("#updateCategoryOptionCombos").is(":checked");
  var updateOrganisationUnitPaths = $("#updateOrganisationUnitPaths").is(":checked");

  if( clearAnalytics || zeroValues || 
    prunePeriods || removeExpiredInvitations || dropSqlViews || createSqlViews || updateCategoryOptionCombos || updateOrganisationUnitPaths ) {

    setHeaderWaitMessage(i18n_performing_maintenance);

    var params = "clearAnalytics=" + clearAnalytics +
      "&zeroValues=" + zeroValues +
      "&prunePeriods=" + prunePeriods +
      "&removeExpiredInvitations=" + removeExpiredInvitations +
      "&dropSqlViews=" + dropSqlViews +
      "&createSqlViews=" + createSqlViews +
      "&updateCategoryOptionCombos=" + updateCategoryOptionCombos +
      "&updateOrganisationUnitPaths=" + updateOrganisationUnitPaths;

    $.ajax({
      type: "POST",
      url: "performMaintenance.action",
      data: params,
      dataType: "xml",
      success: function(result) {
        setHeaderDelayMessage(i18n_maintenance_performed);
      }
    });
  }
  else {
    setHeaderDelayMessage(i18n_select_options);
  }
}