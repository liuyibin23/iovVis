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
/* eslint-disable */

const HtmlWebpackPlugin = require('html-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CompressionPlugin = require('compression-webpack-plugin');

const webpack = require('webpack');
const path = require('path');
const dirTree = require('directory-tree');
const jsonminify = require("jsonminify");

const PUBLIC_RESOURCE_PATH = '/static/';
// The path to the cesium source code
const cesiumSource = 'node_modules/cesium/Source';
const cesiumWorkers = '../Build/Cesium/Workers';

var langs = [];
dirTree('./src/app/locale/', {extensions:/\.json$/}, (item) => {
    /* It is expected what the name of a locale file has the following format: */
    /* 'locale.constant-LANG_CODE[_REGION_CODE].json', e.g. locale.constant-es.json or locale.constant-zh_CN.json*/
    langs.push(item.name.slice(item.name.lastIndexOf('-') + 1, -5));
});

module.exports = {
    entry: [
        './src/app/app.js',
        'webpack-material-design-icons'
    ],
    output: {
        path: path.resolve(__dirname, 'target/generated-resources/public/static'),
        publicPath: PUBLIC_RESOURCE_PATH,
        filename: 'bundle.[hash].js',
        // Needed by Cesium for multiline strings
        sourcePrefix: ''
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: "jquery",
            jQuery: "jquery",
            "window.jQuery": "jquery",
            tinycolor: "tinycolor2",
            tv4: "tv4",
            moment: "moment"
        }),
        new CopyWebpackPlugin([
            {
                from: './src/BeiDouApp.ico',
                to: 'BeiDouApp.ico'
            },
            {
                from: './src/dist',
                to: 'dist'
            },
            {
                from: './src/app/locale',
                to: 'locale',
                ignore: [ '*.js' ],
                transform: function(content, path) {
                    return Buffer.from(jsonminify(content.toString()));
                }
            }
        ]),
        // Copy Cesium Assets, Widgets, and Workers to a static directory
        new CopyWebpackPlugin([{from: path.join(cesiumSource, cesiumWorkers), to: 'Workers'}]),
        new CopyWebpackPlugin([{from: path.join(cesiumSource, 'Assets'), to: 'Assets'}]),
        new CopyWebpackPlugin([{from: path.join(cesiumSource, 'Widgets'), to: 'Widgets'}]),
        new webpack.DefinePlugin({
            // Define relative base path in cesium for loading assets
            CESIUM_BASE_URL: JSON.stringify('')
        }),
        new HtmlWebpackPlugin({
            template: './src/index.html',
            filename: '../index.html',
            title: 'BeiDouApp',
            inject: 'body',
        }),
        new webpack.optimize.OccurrenceOrderPlugin(),
        new webpack.NoErrorsPlugin(),
        new webpack.optimize.DedupePlugin(),
        new webpack.optimize.CommonsChunkPlugin('vendor', 'vendor.[hash].js'),
        new ExtractTextPlugin('style.[contentHash].css', {
            allChunks: true,
        }),
        new webpack.DefinePlugin({
            THINGSBOARD_VERSION: JSON.stringify(require('./package.json').version),
            '__DEVTOOLS__': false,
            'process.env': {
                NODE_ENV: JSON.stringify('production'),
            },
            PUBLIC_PATH: PUBLIC_RESOURCE_PATH,
            SUPPORTED_LANGS: JSON.stringify(langs)
        }),
        
        new CompressionPlugin({
            asset: "[path].gz[query]",
            algorithm: "gzip",
            test: /\.js$|\.css$|\.svg$|\.ttf$|\.woff$|\.woff2|\.eot$\.json$/,
            threshold: 10240,
            minRatio: 0.8
        })
    ],
    node: {
        tls: "empty",
        fs: "empty"
    },
    resolve: {
        alias: {
            // Cesium module name
            cesium: path.resolve(__dirname, cesiumSource)
        }
    },
    module: {
        loaders: [
            {
                test: /\.jsx$/,
                loader: 'babel',
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loaders: ['ng-annotate', 'babel'],
                exclude: /node_modules/,
                include: __dirname,
            },
            {
                test: /\.js$/,
                loader: "eslint-loader?{parser: 'babel-eslint'}",
                exclude: /node_modules|vendor/,
                include: __dirname,
            },
            {
                test: /\.css$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader'),
            },
            {
                test: /\.scss$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!sass-loader'),
            },
            {
                test: /\.less$/,
                loader: ExtractTextPlugin.extract('style-loader', 'css-loader!postcss-loader!less-loader'),
            },
            {
                test: /\.tpl\.html$/,
                loader: 'ngtemplate?relativeTo=' + (path.resolve(__dirname, './src/app')) + '/!html!html-minifier-loader'
            },
            {
                test: /\.(svg)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loader: 'url?limit=8192'
            },
            {
                test: /\.(png|jpe?g|gif|woff|woff2|ttf|otf|eot|ico)(\?v=[0-9]+\.[0-9]+\.[0-9]+)?$/,
                loaders: [
                    'url?limit=8192',
                    'img?minimize'
                ]
            }
        ],
    },
    'html-minifier-loader': {
        caseSensitive: true,
        removeComments: true,
        collapseWhitespace: false,
        preventAttributesEscaping: true,
        removeEmptyAttributes: false
    }
};
