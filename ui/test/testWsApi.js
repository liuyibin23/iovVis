/*
 * Copyright © 2016-2018 The ET-iLink Authors
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
 * 
 * usage:
 * > npm install axios ws
 * > node apitest.js
 */
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

// const path = require('path');
const axios = require('axios');
// tsTest();
// hsTest();
// attrTest();
async function postSync(url, data) {
    try {
        let res = await axios.post(url, (data));
        let res_data = res.data;
        return new Promise((resolve, reject) => {
            if (res.status === 200) {
                resolve(res_data)
            } else {
                reject(res)
            }
        })
    } catch (err) {
        console.log(err)
    }
}

async function tsTest() {
    var loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        { "username": "lvyu@beidouapp.com", "password": "12345" });
    var token = loginRes.token;
    /* begin websocket test session */
    const WebSocket = require('ws');
    const webSocket = new WebSocket('ws://cf.beidouapp.com:8080/api/ws/plugins/telemetry?token=' + token);

    webSocket.onopen = function open() {
        console.log('webSocket connected!');
    };
    webSocket.onmessage = function incoming(event) {
        var obj = JSON.parse(event.data);
        console.log('cmdChannel: %d',obj.subscriptionId, obj.data);
    };
    webSocket.onclose = function close(){
        console.log('webSocket closed!');
    };

    var cmd1 = {
        tsSubCmds: [
            {
                "entityType": "DEVICE",
                "entityId": "5074b200-e31a-11e8-be95-f3713e6700c3", //A监测点
                "keys": "crackWidth,crackDeepth",
                // "startTs":1545240944508,  //距离1970年1月1日零点的毫秒数
                // "timeWindow": 60000,		 //时间窗口为1分钟，60000毫秒
                // "interval": 6000,		 //分组间隔1000毫秒
                // "limit": 60,
                // "cmdId": 10,
                // "agg": "AVG"
            }
        ],
        historyCmds: [],
        attrSubCmds: []
    };
    var cmd2 = {
        tsSubCmds: [
            {
                "entityType": "DEVICE",
                "entityId": "056a2f60-e31a-11e8-be95-f3713e6700c3", //A巡检员
                "keys": "湿度",
                "startTs":1545240844508,  //距离1970年1月1日零点的毫秒数
                "timeWindow": 60000,		//时间窗口为1分钟，60000毫秒
                "interval": 6000,			//分组间隔1000毫秒
                "limit": 60,
                "cmdId": 10,
                "agg": "AVG"
            }
        ],
        historyCmds: [],
        attrSubCmds: []
    };
    var cmd3 = {
        tsSubCmds: [
            {
                "entityType": "DEVICE",
                "entityId": "056a2f60-e31a-11e8-be95-f3713e6700c3", //A巡检员
                cmdId: 10,
                unsubscribe: true
            }
        ],
        historyCmds: [],
        attrSubCmds: []
    };

    setTimeout(timerfun, 1000, cmd1);
    setTimeout(timerfun, 10000, cmd2);
    setTimeout(timerfun, 50000, cmd3);
    function timerfun(object) {
        var data = JSON.stringify(object);
        webSocket.send(data);
        console.log('>>>>>Message is sent: ' + data);
    }
}

async function hsTest() {
    var loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        { "username": "lvyu@beidouapp.com", "password": "12345" });
    var token = loginRes.token;
    /* begin websocket test session */
    const WebSocket = require('ws');
    const webSocket = new WebSocket('ws://cf.beidouapp.com:8080/api/ws/plugins/telemetry?token=' + token);

    webSocket.onopen = function open() {
        console.log('webSocket connected!');
    };
    webSocket.onmessage = function incoming(event) {
        var obj = JSON.parse(event.data);
        console.log(event.data);
        // console.log('cmdChannel: %d',obj.subscriptionId, obj.data);
    };
    webSocket.onclose = function close(){
        console.log('webSocket closed!');
    };

    var cmd1 = {
        tsSubCmds: [],
        historyCmds: [
            {
            "entityType": "DEVICE",
            "entityId": "5074b200-e31a-11e8-be95-f3713e6700c3", //A监测点
            "keys": "crackWidth,crackDeepth",
            "startTs": 1545268250243,  //距离1970年1月1日零点的毫秒数
            "endTs": 1545700250243,
            "interval": 2000000,			//分组间隔1000毫秒
            "limit": 500,
            "cmdId": 13,
            "agg": "AVG"
            }
        ],
        attrSubCmds: []
    };
    setTimeout(timerfun, 1000, cmd1);
    function timerfun(object) {
        var data = JSON.stringify(object);
        webSocket.send(data);
        console.log('>>>>>Message is sent: ' + data);
    }
}

async function attrTest() {
    var loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        { "username": "lvyu@beidouapp.com", "password": "12345" });
    var token = loginRes.token;
    /* begin websocket test session */
    const WebSocket = require('ws');
    const webSocket = new WebSocket('ws://cf.beidouapp.com:8080/api/ws/plugins/telemetry?token=' + token);

    webSocket.onopen = function open() {
        console.log('webSocket connected!');
    };
    webSocket.onmessage = function incoming(event) {
        var obj = JSON.parse(event.data);
        console.log(event.data);
        // console.log('cmdChannel: %d',obj.subscriptionId, obj.data);
    };
    webSocket.onclose = function close(){
        console.log('webSocket closed!');
    };

    var cmd1 = {
        tsSubCmds: [],
        historyCmds: [
            {
            "entityType": "DEVICE",
            "entityId": "5074b200-e31a-11e8-be95-f3713e6700c3", //A监测点
            "keys": "crackWidth,crackDeepth",
            "startTs": 1545268250243,  //距离1970年1月1日零点的毫秒数
            "endTs": 1545700250243,
            "interval": 2000000,			//分组间隔1000毫秒
            "limit": 500,
            "cmdId": 13,
            "agg": "AVG"
            }
        ],
        attrSubCmds: []
    };
    setTimeout(timerfun, 1000, cmd1);
    function timerfun(object) {
        var data = JSON.stringify(object);
        webSocket.send(data);
        console.log('>>>>>Message is sent: ' + data);
    }
}
