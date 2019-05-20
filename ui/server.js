/*
 * Copyright © 2016-2018 The BeidouApp Authors
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
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

const path = require('path');
const webpack = require('webpack');
const historyApiFallback = require("connect-history-api-fallback");
const webpackDevMiddleware = require('webpack-dev-middleware');
const webpackHotMiddleware = require('webpack-hot-middleware');
const config = require('./webpack.config');

const express = require('express');
const http = require('http');
const httpProxy = require('http-proxy');
// const forwardHost = 'cf.beidouapp.com';
const forwardHost = 'localhost';//'192.168.1.138';
const forwardPort = 8080;

const ruleNodeUiforwardHost = 'localhost';
const ruleNodeUiforwardPort = 8080;//5000;

const app = express();
const server = http.createServer(app);

const PORT = 3000;

const compiler = webpack(config);

app.use(historyApiFallback());
app.use(webpackDevMiddleware(compiler, {noInfo: true, publicPath: config.output.publicPath}));
app.use(webpackHotMiddleware(compiler));

const root = path.join(__dirname, '/src');

const models = path.join(__dirname, '/3d-tiles-samples');
const Workers = path.join(__dirname, '/Workers');
const Assets = path.join(__dirname, '/Assets');
const Widgets = path.join(__dirname, '/Widgets');
const ThirdParty = path.join(__dirname, '/ThirdParty');

app.use('/static', express.static(root));
app.use('/Workers', express.static(Workers));
app.use('/Assets', express.static(Assets));
app.use('/Widgets', express.static(Widgets));
app.use('/ThirdParty',express.static(ThirdParty));
app.use('/3dtiles', express.static(models));

const apiProxy = httpProxy.createProxyServer({
    target: {
        host: forwardHost,
        port: forwardPort
    }
});

const ruleNodeUiApiProxy = httpProxy.createProxyServer({
    target: {
        host: ruleNodeUiforwardHost,
        port: ruleNodeUiforwardPort
    }
});

apiProxy.on('error', function (err, req, res) {
    console.warn('API proxy error: ' + err);
    res.end('Error.');
});

ruleNodeUiApiProxy.on('error', function (err, req, res) {
    console.warn('RuleNode UI API proxy error: ' + err);
    res.end('Error.');
});

console.info(`Forwarding API requests to http://${forwardHost}:${forwardPort}`);
console.info(`Forwarding Rule Node UI requests to http://${ruleNodeUiforwardHost}:${ruleNodeUiforwardPort}`);

app.all('/api/*', (req, res) => {
    // console.info(req);
    apiProxy.web(req, res);
});

app.all('/static/rulenode/*', (req, res) => {
    ruleNodeUiApiProxy.web(req, res);
});

app.get('*', function(req, res) {
    res.sendFile(path.join(__dirname, 'src/index.html'));
});

server.on('upgrade', (req, socket, head) => {
    apiProxy.ws(req, socket, head);
});

server.listen(PORT, '0.0.0.0', (error) => {
    if (error) {
        console.error(error);
    } else {
        console.info(`==>   Listening on port ${PORT}. Open up http://localhost:${PORT}/ in your browser.`);
    }
});
