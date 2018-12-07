/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import injectTapEventPlugin from 'react-tap-event-plugin';
import UrlHandler from './url.handler';

/* eslint-disable import/no-unresolved, import/default */

import mdiIconSet from '../svg/mdi.svg';

/* eslint-enable import/no-unresolved, import/default */

const PRIMARY_BACKGROUND_COLOR = "#305680";//#2856b6";//"#3f51b5";
const SECONDARY_BACKGROUND_COLOR = "#527dad";
const HUE3_COLOR = "#a7c1de";

/*@ngInject*/
export default function AppConfig($provide,
                                  $urlRouterProvider,
                                  $locationProvider,
                                  $mdIconProvider,
                                  $mdThemingProvider,
                                  $httpProvider,
                                  $translateProvider,
                                  storeProvider) {

    injectTapEventPlugin();
    $locationProvider.html5Mode(true);
    $urlRouterProvider.otherwise(UrlHandler);
    storeProvider.setCaching(false);
    
    $translateProvider.useSanitizeValueStrategy(null)
                      .useMissingTranslationHandler('tbMissingTranslationHandler')
                      .addInterpolation('$translateMessageFormatInterpolation')
                      .useStaticFilesLoader({
                          files: [
                              {
                                  prefix: PUBLIC_PATH + 'locale/locale.constant-', //eslint-disable-line
                                  suffix: '.json'
                              }
                          ]
                      })
                      .registerAvailableLanguageKeys(SUPPORTED_LANGS, getLanguageAliases(SUPPORTED_LANGS)) //eslint-disable-line
                      .fallbackLanguage('en_US') // must be before determinePreferredLanguage   
                      .uniformLanguageTag('java')  // must be before determinePreferredLanguage
                      .determinePreferredLanguage();                

    $httpProvider.interceptors.push('globalInterceptor');

    $provide.decorator("$exceptionHandler", ['$delegate', '$injector', function ($delegate/*, $injector*/) {
        return function (exception, cause) {
/*            var rootScope = $injector.get("$rootScope");
            var $window = $injector.get("$window");
            var utils = $injector.get("utils");
            if (rootScope.widgetEditMode) {
                var parentScope = $window.parent.angular.element($window.frameElement).scope();
                var data = utils.parseException(exception);
                parentScope.$emit('widgetException', data);
                parentScope.$apply();
            }*/
            $delegate(exception, cause);
        };
    }]);

    $mdIconProvider.iconSet('mdi', mdiIconSet);

    configureTheme();

    function blueGrayTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('blue-grey');
        var tbAccentPalette = $mdThemingProvider.extendPalette('orange', {
            'contrastDefaultColor': 'light'
        });

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-primary')
            .dark();
    }

    function indigoTheme() {
        var tbPrimaryPalette = $mdThemingProvider.extendPalette('indigo', {
            '500': PRIMARY_BACKGROUND_COLOR,
            '600': SECONDARY_BACKGROUND_COLOR,
            'A100': HUE3_COLOR
        });

        var tbAccentPalette = $mdThemingProvider.extendPalette('deep-orange');

        $mdThemingProvider.definePalette('tb-primary', tbPrimaryPalette);
        $mdThemingProvider.definePalette('tb-accent', tbAccentPalette);

        var tbDarkPrimaryPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '500': '#9fa8da'
        });

        var tbDarkPrimaryBackgroundPalette = $mdThemingProvider.extendPalette('tb-primary', {
            '800': PRIMARY_BACKGROUND_COLOR
        });

        $mdThemingProvider.definePalette('tb-dark-primary', tbDarkPrimaryPalette);
        $mdThemingProvider.definePalette('tb-dark-primary-background', tbDarkPrimaryBackgroundPalette);

        $mdThemingProvider.theme('default')
            .primaryPalette('tb-primary')
            .accentPalette('tb-accent');

        $mdThemingProvider.theme('tb-dark')
            .primaryPalette('tb-dark-primary')
            .accentPalette('tb-accent')
            .backgroundPalette('tb-dark-primary-background')
            .dark();
    }
    
    function customerTheme() {
        // $mdThemingProvider.definePalette('amazingPaletteName', {
        //     '50': 'ffebee',
        //     '100': 'ffcdd2',
        //     '200': 'ef9a9a',
        //     '300': 'e57373',
        //     '400': 'ef5350',
        //     '500': 'f44336',
        //     '600': 'e53935',
        //     '700': 'd32f2f',
        //     '800': 'c62828',
        //     '900': 'b71c1c',
        //     'A100': 'ff8a80',
        //     'A200': 'ff5252',
        //     'A400': 'ff1744',
        //     'A700': 'd50000',
        //     'contrastDefaultColor': 'dark',    // whether, by default, text (contrast)
        //                                         // on this palette should be dark or light
        
        //     'contrastDarkColors': ['50', '100', //hues which contrast should be 'dark' by default
        //      '200', '300', '400', 'A100'],
        //     'contrastLightColors': undefined    // could also specify this if default was 'dark'
        //   });
        
        //   $mdThemingProvider.theme('default')
        //     .primaryPalette('amazingPaletteName')
         /*引入自定义调色板*/
         $mdThemingProvider.definePalette('beidouapp_black', {
            '50': '000000',
            '100': '000000',
            '200': '000000',
            '300': '000000',
            '400': '000000',
            '500': '000000',
            '600': '000000',
            '700': '000000',
            '800': '000000',
            '900': '000000',
            'A100': '000000',
            'A200': '000000',
            'A400': '000000',
            'A700': '000000',
            'contrastDefaultColor': 'light'
          });
          $mdThemingProvider.definePalette('beidouapp_white', {
            '50': 'ffffff',
            '100': 'ffffff',
            '200': 'ffffff',
            '300': 'ffffff',
            '400': 'ffffff',
            '500': 'ffffff',
            '600': 'ffffff',
            '700': 'ffffff',
            '800': 'ffffff',
            '900': 'ffffff',
            'A100': 'ffffff',
            'A200': 'ffffff',
            'A400': 'ffffff',
            'A700': 'ffffff',
            'contrastDefaultColor': 'dark'
          });
        //   var tbAccentPalette = $mdThemingProvider.extendPalette('deep-orange');
        //   $mdThemingProvider.definePalette('orange', tbAccentPalette);
        $mdThemingProvider.theme('default')
        .primaryPalette('beidouapp_black')
        .accentPalette('beidouapp_white');

        $mdThemingProvider.theme('bd-dark')
            .primaryPalette('beidouapp_black')
            .accentPalette('beidouapp_white')
            .backgroundPalette('beidouapp_black')
            .dark();
    }

    function configureTheme() {

        var theme = 'customer';
        // var theme = 'indigo';
        // var theme = 'blueGray';

        if (theme === 'blueGray') {
            blueGrayTheme();
        } else if (theme === 'indigo') {
            indigoTheme();
        } else {
            customerTheme();
        }

        $mdThemingProvider.setDefaultTheme('default');
        //$mdThemingProvider.alwaysWatchTheme(true);
    }

    function getLanguageAliases(supportedLangs) {
        var aliases = {};

        supportedLangs.sort().forEach(function(item, index, array) {
            if (item.length === 2) { 
                aliases[item] = item;
                aliases[item + '_*'] = item;
            } else {
                var key = item.slice(0, 2);
                if (index === 0 || key !== array[index - 1].slice(0, 2)) {
                    aliases[key] = item;
                    aliases[key + '_*'] = item;
                } else {
                    aliases[item] = item;
                }
            }
        });
        
        return aliases;
    }
}