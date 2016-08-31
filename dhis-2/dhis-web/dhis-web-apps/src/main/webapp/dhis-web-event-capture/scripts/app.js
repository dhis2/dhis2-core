'use strict';

/* App Module */

var eventCapture = angular.module('eventCapture',
                    ['ui.bootstrap',
                    'ngRoute',
                    'ngCookies',
                    'ngMessages',
                    'ngSanitize',
                    'eventCaptureDirectives',
                    'eventCaptureControllers',
                    'eventCaptureServices',
                    'eventCaptureFilters',
                    'd2Filters',
                    'd2Directives',
                    'd2Services',
                    'd2Controllers',
                    'ui.select',
                    'angularLocalStorage',
                    'pascalprecht.translate',
                    'd2HeaderBar'])

.value('DHIS2URL', '..')

.config(function ($routeProvider, $translateProvider) {

    $routeProvider.when('/', {
        templateUrl: 'views/home.html',
        controller: 'MainController'
    }).otherwise({
        redirectTo: '/'
    });

    $translateProvider.preferredLanguage('en');
    $translateProvider.useSanitizeValueStrategy('escaped');
    $translateProvider.useLoader('i18nLoader');
});