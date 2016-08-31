'use strict';

/* Directives */

var eventCaptureDirectives = angular.module('eventCaptureDirectives', [])

.directive('selectedOrgUnit', function ($timeout) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            $("#orgUnitTree").one("ouwtLoaded", function (event, ids, names) {                                   
                console.log('Finished loading orgunit tree');                        
                $("#orgUnitTree").addClass("disable-clicks"); //Disable ou selection until meta-data has downloaded
                $timeout(function () {
                    scope.treeLoaded = true;
                    scope.$apply();
                });
                downloadMetaData();                
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
});
