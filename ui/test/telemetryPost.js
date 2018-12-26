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
 */
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

// const path = require('path');
const axios = require('axios');

function postCrackWidth(point) {
    axios.post('http://cf.beidouapp.com:8080/api/v1/GbGuHQkbgeQcAoFd3GLF/telemetry', //A监测点 
        point).then(res => {
            console.log(res.status);
        }).catch(e => {
            // console.info(e);
        })
}

function postCrackDeepth(pt) {
    axios.post('http://cf.beidouapp.com:8080/api/v1/mvjPD7zvq7CqkzmyPOSi/telemetry', //A巡视员 
        ptJson).then(res => {
            console.info(res)
        }).catch(e => {
            console.info(e)
        })
}

let pt = { "裂缝宽度-----": 0.16, idType: "CRACK-ID", alarmCnt: 10.79 };
postCrackWidth(pt);

let pt2 = { "裂缝深度": 0.4, idType: "CRACK", alarmCnt: 1 };
//postCrackDeepth(pt2);

/*
var fRead = fs.createReadStream('./taxi/Taxi_175');
var objReadline = readline.createInterface({
    input: fRead,
    crlfDelay: Infinity
});
var arrPTS_ = new Array();
objReadline.on('line', function (line) {
    console.log('line:' + line);
    let arr = line.split(',');
    let pt = {latitude:0,longitude:0,speed:0,energy:0};
    pt.longitude = Number(arr[2]);
    pt.latitude = Number(arr[3]);
    pt.speed = Number(arr[4]);
    pt.energy = Number(arr[5]);
    arrPTS_.push(pt);
    //objReadline.pause();
});
objReadline.on('close', function () {
    console.log('closed!');
    // callback(arr);
});




let count_ = 0,
    timer_ = setInterval((str, num) => {
        count_++;
        console.log(str, count_);
        postGps(count_++);
        //objReadline.resume();
        if (count_ >= num)
            clearInterval(timer_);
    }, 1000, 'total times', 3);

*/