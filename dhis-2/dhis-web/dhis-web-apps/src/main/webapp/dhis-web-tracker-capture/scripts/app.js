'use strict';

/* App Module */

var trackerCapture = angular.module('trackerCapture',
        ['ui.bootstrap', 
         'ngRoute', 
         'ngCookies',
         'ngSanitize',
         'ngMessages',
         'trackerCaptureServices',
         'trackerCaptureFilters',
         'trackerCaptureDirectives', 
         'trackerCaptureControllers',
         'd2Directives',
         'd2Filters',
         'd2Services',
         'd2Controllers',
         'angularLocalStorage',
         'ui.select',
         'ui.select2',
         'infinite-scroll',
         'd2HeaderBar',
         'ngCsv',
         'sticky',
         'nvd3ChartDirectives',
         'pascalprecht.translate'])
              
.value('DHIS2URL', '..')

.config(function($httpProvider, $routeProvider, $translateProvider) {    
            
    $httpProvider.defaults.useXDomain = true;
    delete $httpProvider.defaults.headers.common['X-Requested-With'];
    
    $routeProvider.when('/', {
        templateUrl:'views/home.html',
        controller: 'SelectionController'
    }).when('/dashboard',{
        templateUrl:'components/dashboard/dashboard.html',
        controller: 'DashboardController'
    }).when('/report-types',{
        templateUrl:'views/report-types.html',
        controller: 'ReportTypesController'
    }).when('/program-summary',{
        templateUrl:'components/report/program-summary.html',
        controller: 'ProgramSummaryController'
    }).when('/program-statistics',{
        templateUrl:'components/report/program-statistics.html',
        controller: 'ProgramStatisticsController'
    }).when('/overdue-events',{
        templateUrl:'components/report/overdue-events.html',
        controller: 'OverdueEventsController'
    }).when('/upcoming-events',{
        templateUrl:'components/report/upcoming-events.html',
        controller: 'UpcomingEventsController'
    }).otherwise({
        redirectTo : '/'
    });  
    
    $translateProvider.preferredLanguage('en');
    $translateProvider.useSanitizeValueStrategy('escaped');
    $translateProvider.useLoader('i18nLoader');
    
});