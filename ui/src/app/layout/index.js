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
import './home.scss';

import uiRouter from 'angular-ui-router';
import ngSanitize from 'angular-sanitize';
import 'angular-breadcrumb';

import beidouappMenu from '../services/menu.service';
import beidouappApiDevice from '../api/device.service';
import beidouappApiLogin from '../api/login.service';
import beidouappApiUser from '../api/user.service';

import beidouappNoAnimate from '../components/no-animate.directive';
import beidouappOnFinishRender from '../components/finish-render.directive';
import beidouappSideMenu from '../components/side-menu.directive';
import beidouappDashboardAutocomplete from '../components/dashboard-autocomplete.directive';
import beidouappKvMap from '../components/kv-map.directive';
import beidouappJsonObjectEdit from '../components/json-object-edit.directive';
import beidouappJsonContent from '../components/json-content.directive';

import beidouappUserMenu from './user-menu.directive';

import beidouappEntity from '../entity';
import beidouappEvent from '../event';
import beidouappAlarm from '../alarm';
import beidouappAuditLog from '../audit';
import beidouappExtension from '../extension';
import beidouappTenant from '../tenant';
import beidouappCustomer from '../customer';
import beidouappUser from '../user';
import beidouappHomeLinks from '../home';
import beidouappAdmin from '../admin';
import beidouappProfile from '../profile';
import beidouappAsset from '../asset';
import beidouappDevice from '../device';
import beidouappEntityView from '../entity-view';
import beidouappWidgetLibrary from '../widget';
import beidouappDashboard from '../dashboard';
import beidouappRuleChain from '../rulechain';

import beidouappJsonForm from '../jsonform';

import HomeRoutes from './home.routes';
import HomeController from './home.controller';
import BreadcrumbLabel from './breadcrumb-label.filter';
import BreadcrumbIcon from './breadcrumb-icon.filter';

export default angular.module('beidouapp.home', [
    uiRouter,
    ngSanitize,
    'ncy-angular-breadcrumb',
    beidouappMenu,
    beidouappHomeLinks,
    beidouappUserMenu,
    beidouappEntity,
    beidouappEvent,
    beidouappAlarm,
    beidouappAuditLog,
    beidouappExtension,
    beidouappTenant,
    beidouappCustomer,
    beidouappUser,
    beidouappAdmin,
    beidouappProfile,
    beidouappAsset,
    beidouappDevice,
    beidouappEntityView,
    beidouappWidgetLibrary,
    beidouappDashboard,
    beidouappRuleChain,
    beidouappJsonForm,
    beidouappApiDevice,
    beidouappApiLogin,
    beidouappApiUser,
    beidouappNoAnimate,
    beidouappOnFinishRender,
    beidouappSideMenu,
    beidouappDashboardAutocomplete,
    beidouappKvMap,
    beidouappJsonObjectEdit,
    beidouappJsonContent
])
    .config(HomeRoutes)
    .controller('HomeController', HomeController)
    .filter('breadcrumbLabel', BreadcrumbLabel)
    .filter('breadcrumbIcon', BreadcrumbIcon)
    .name;
