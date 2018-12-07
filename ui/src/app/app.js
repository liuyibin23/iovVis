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
import './ie.support';

import 'event-source-polyfill';

import angular from 'angular';
import ngMaterial from 'angular-material';
import ngMdIcons from 'angular-material-icons';
import ngCookies from 'angular-cookies';
import angularSocialshare from 'angular-socialshare';
import 'angular-translate';
import 'angular-translate-loader-static-files';
import 'angular-translate-storage-local';
import 'angular-translate-storage-cookie';
import 'angular-translate-handler-log';
import 'angular-translate-interpolation-messageformat';
import 'md-color-picker';
import mdPickers from 'mdPickers';
import ngSanitize from 'angular-sanitize';
import FBAngular from 'angular-fullscreen';
import vAccordion from 'v-accordion';
import ngAnimate from 'angular-animate';
import 'angular-websocket';
import uiRouter from 'angular-ui-router';
import angularJwt from 'angular-jwt';
import 'angular-drag-and-drop-lists';
import mdDataTable from 'angular-material-data-table';
import fixedTableHeader from 'angular-fixed-table-header';
import 'angular-material-expansion-panel';
import ngTouch from 'angular-touch';
import 'angular-carousel';
import 'clipboard';
import 'ngclipboard';
import 'react';
import 'react-dom';
import 'material-ui';
import 'react-schema-form';
import react from 'ngreact';
import '@flowjs/ng-flow/dist/ng-flow-standalone.min';
import 'ngFlowchart/dist/ngFlowchart';

import 'typeface-roboto';
import 'font-awesome/css/font-awesome.min.css';
import 'angular-material/angular-material.min.css';
import 'angular-material-icons/angular-material-icons.css';
import 'angular-gridster/dist/angular-gridster.min.css';
import 'v-accordion/dist/v-accordion.min.css';
import 'md-color-picker/dist/mdColorPicker.min.css';
import 'mdPickers/dist/mdPickers.min.css';
import 'angular-hotkeys/build/hotkeys.min.css';
import 'angular-carousel/dist/angular-carousel.min.css';
import 'angular-material-expansion-panel/dist/md-expansion-panel.min.css';
import 'ngFlowchart/dist/flowchart.css';
import '../scss/main.scss';

import beidouappThirdpartyFix from './common/thirdparty-fix';
import beidouappTranslateHandler from './locale/translate-handler';
import beidouappLogin from './login';
import beidouappDialogs from './components/datakey-config-dialog.controller';
import beidouappMenu from './services/menu.service';
import beidouappRaf from './common/raf.provider';
import beidouappUtils from './common/utils.service';
import beidouappDashboardUtils from './common/dashboard-utils.service';
import beidouappTypes from './common/types.constant';
import beidouappApiTime from './api/time.service';
import beidouappKeyboardShortcut from './components/keyboard-shortcut.filter';
import beidouappHelp from './help/help.directive';
import beidouappToast from './services/toast';
import beidouappClipboard from './services/clipboard.service';
import beidouappHome from './layout';
import beidouappApiLogin from './api/login.service';
import beidouappApiDevice from './api/device.service';
import beidouappApiEntityView from './api/entity-view.service';
import beidouappApiUser from './api/user.service';
import beidouappApiEntityRelation from './api/entity-relation.service';
import beidouappApiAsset from './api/asset.service';
import beidouappApiAttribute from './api/attribute.service';
import beidouappApiEntity from './api/entity.service';
import beidouappApiAlarm from './api/alarm.service';
import beidouappApiAuditLog from './api/audit-log.service';
import beidouappApiComponentDescriptor from './api/component-descriptor.service';
import beidouappApiRuleChain from './api/rule-chain.service';

import AppConfig from './app.config';
import GlobalInterceptor from './global-interceptor.service';
import AppRun from './app.run';

angular.module('BeiDouApp', [
    ngMaterial,
    ngMdIcons,
    ngCookies,
    angularSocialshare,
    'pascalprecht.translate',
    'mdColorPicker',
    mdPickers,
    ngSanitize,
    FBAngular.name,
    vAccordion,
    ngAnimate,
    'ngWebSocket',
    angularJwt,
    'dndLists',
    mdDataTable,
    fixedTableHeader,
    'material.components.expansionPanels',
    ngTouch,
    'angular-carousel',
    'ngclipboard',
    react.name,
    'flow',
    'flowchart',
    beidouappThirdpartyFix,
    beidouappTranslateHandler,
    beidouappLogin,
    beidouappDialogs,
    beidouappMenu,
    beidouappRaf,
    beidouappUtils,
    beidouappDashboardUtils,
    beidouappTypes,
    beidouappApiTime,
    beidouappKeyboardShortcut,
    beidouappHelp,
    beidouappToast,
    beidouappClipboard,
    beidouappHome,
    beidouappApiLogin,
    beidouappApiDevice,
    beidouappApiEntityView,
    beidouappApiUser,
    beidouappApiEntityRelation,
    beidouappApiAsset,
    beidouappApiAttribute,
    beidouappApiEntity,
    beidouappApiAlarm,
    beidouappApiAuditLog,
    beidouappApiComponentDescriptor,
    beidouappApiRuleChain,
    uiRouter])
    .config(AppConfig)
    .factory('globalInterceptor', GlobalInterceptor)
    .run(AppRun);
