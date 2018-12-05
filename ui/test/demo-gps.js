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
const fs = require('fs');
const readline = require('readline');


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

function postGps(ind) {
    axios.post('http://localhost:3000/api/v1/3B06sOePQ4RF9C8NUsUm/telemetry',
        // arrPTS_[ind]
        {"latitude": arrPTS_[ind].latitude, "longitude": arrPTS_[ind].longitude, 
         "speed": arrPTS_[ind].speed,
         "energy": arrPTS_[ind].energy,
         "subject":"测试数据TEST"+arrPTS_[ind].latitude
        } 
    ).then(res => {
        console.info(res)
    }).catch(e => {
        console.info(e)
    })
}


let count_ = 0,
    timer_ = setInterval((str, num) => {
        count_++;
        console.log(str, count_);
        postGps(count_++);
        //objReadline.resume();
        if (count_ >= num)
            clearInterval(timer_);
    }, 1000, 'total times', 150);
