function performMaintenance() {
    var clearAnalytics = $("#clearAnalytics").is(":checked");
    var analyzeAnalytics = $("#analyzeAnalytics").is(":checked");
    var zeroValues = $("#zeroValues").is(":checked");
    var softDeletedValues = $("#softDeletedValues").is(":checked");
    var softDeletedEvents = $("#softDeletedEvents").is(":checked");
    var softDeletedEnrollments = $("#softDeletedEnrollments").is(":checked");
    var softDeletedTrackedEntityInstances = $("#softDeletedTrackedEntityInstances").is(":checked");
    var prunePeriods = $("#prunePeriods").is(":checked");
    var removeExpiredInvitations = $("#removeExpiredInvitations").is(":checked");
    var dropSqlViews = $("#dropSqlViews").is(":checked");
    var createSqlViews = $("#createSqlViews").is(":checked");
    var updateCategoryOptionCombos = $("#updateCategoryOptionCombos").is(":checked");
    var updateOrganisationUnitPaths = $("#updateOrganisationUnitPaths").is(":checked");
    var clearApplicationCache = $("#clearApplicationCache").is(":checked");
    var reloadApps = $("#reloadApps").is(":checked");

    if ( clearAnalytics || analyzeAnalytics || zeroValues || softDeletedValues || softDeletedEvents || softDeletedEnrollments || softDeletedTrackedEntityInstances || prunePeriods || removeExpiredInvitations ||
        dropSqlViews || createSqlViews || updateCategoryOptionCombos || updateOrganisationUnitPaths || clearApplicationCache || reloadApps ) {

        setHeaderWaitMessage(i18n_performing_maintenance);

        var params = "analyticsTableClear=" + clearAnalytics +
        	"&analyticsTableAnalyze=" + analyzeAnalytics +
            "&zeroDataValueRemoval=" + zeroValues +
            "&softDeletedDataValueRemoval=" + softDeletedValues +
            "&softDeletedEventRemoval=" + softDeletedEvents +
            "&softDeletedEnrollmentRemoval=" + softDeletedEnrollments +
            "&softDeletedTrackedEntityInstanceRemoval=" + softDeletedTrackedEntityInstances +
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
