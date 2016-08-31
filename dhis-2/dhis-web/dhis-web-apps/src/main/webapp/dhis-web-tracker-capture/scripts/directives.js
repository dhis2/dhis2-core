/* global directive, selection, dhis2, angular */

'use strict';

/* Directives */

var trackerCaptureDirectives = angular.module('trackerCaptureDirectives', [])

.directive('selectedOrgUnit', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {

            selection.load();
            $("#orgUnitTree").one("ouwtLoaded", function (event, ids, names) {
                if (dhis2.tc && dhis2.tc.metaDataCached) {
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    selection.responseReceived();
                }
                else {
                    console.log('Finished loading orgunit tree');
                    $("#orgUnitTree").addClass("disable-clicks"); //Disable ou selection until meta-data has downloaded
                    $timeout(function () {
                        scope.treeLoaded = true;
                        scope.$apply();
                    });
                    downloadMetaData();
                }
            });

            //listen to user selection, and inform angular         
            selection.setListenerFunction(setSelectedOu, true);
            function setSelectedOu(ids, names) {
                var ou = {id: ids[0], name: names[0]};
                $timeout(function () {
                    scope.selectedOrgUnit = ou;
                    scope.$apply();
                });
            }
        }
    };
})

.directive('eventPaginator', function factory() {

    return {
        restrict: 'E',
        controller: function ($scope, Paginator) {
            $scope.paginator = Paginator;
        },
        templateUrl: 'components/dataentry/event-paging.html'
    };
})

.directive('stringToNumber', function () {
    return {
        require: 'ngModel',
        link: function (scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function (value) {
                return '' + value;
            });
            ngModel.$formatters.push(function (value) {
                return parseFloat(value, 10);
            });
        }
    };
});