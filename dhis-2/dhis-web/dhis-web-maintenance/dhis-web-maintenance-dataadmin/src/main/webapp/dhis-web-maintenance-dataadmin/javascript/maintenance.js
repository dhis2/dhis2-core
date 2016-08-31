function performMaintenance() {
    var clearAnalytics = $("#clearAnalytics").is(":checked");
    var zeroValues = $("#zeroValues").is(":checked");
    var prunePeriods = $("#prunePeriods").is(":checked");
    var removeExpiredInvitations = $("#removeExpiredInvitations").is(":checked");
    var dropSqlViews = $("#dropSqlViews").is(":checked");
    var createSqlViews = $("#createSqlViews").is(":checked");
    var updateCategoryOptionCombos = $("#updateCategoryOptionCombos").is(":checked");
    var updateOrganisationUnitPaths = $("#updateOrganisationUnitPaths").is(":checked");
    var clearApplicationCache = $("#clearApplicationCache").is(":checked");
    var reloadApps = $("#reloadApps").is(":checked");

    if (clearAnalytics || zeroValues || prunePeriods || removeExpiredInvitations ||
        dropSqlViews || createSqlViews || updateCategoryOptionCombos || updateOrganisationUnitPaths || clearApplicationCache || reloadApps) {

        setHeaderWaitMessage(i18n_performing_maintenance);

        var params = "analyticsTableClear=" + clearAnalytics +
            "&zeroDataValueRemoval=" + zeroValues +
            "&periodPruning=" + prunePeriods +
            "&expiredInvitationsClear=" + removeExpiredInvitations +
            "&sqlViewsDrop=" + dropSqlViews +
            "&sqlViewsCreate=" + createSqlViews +
            "&categoryOptionComboUpdate=" + updateCategoryOptionCombos +
            "&ouPathsUpdate=" + updateOrganisationUnitPaths +
            "&cacheClear=" + clearApplicationCache +
            "&appReload=" + reloadApps;

        $.ajax({
            type: "post",
            url: "../api/maintenance",
            data: params,
            success: function (result) {
                setHeaderDelayMessage(i18n_maintenance_performed);
            }
        });
    }
    else {
        setHeaderDelayMessage(i18n_select_options);
    }
}
