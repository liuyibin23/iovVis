const express = require('express');
const axios = require('axios');
//const fs = require('fs');
//const request = require('request');
const util = require('../util/utils');
const logger = require('../util/logger');
var keyMapArr = new Array();
var userArr = new Array();

const router = express.Router();

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Reports Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Statistics Api home page')
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About reports')
})

function keysConvert(keys){
     let klist = keys.split(',');
     var arr = new Array();
     //console.log(klist)
     for (let i = 0; i < klist.length; i++){
         let key = klist[i];
         let key_count = key+'_count';
         let keyMap = new Map()
         keyMap.set('key',key);
         keyMap.set('key_count',key_count);
         keyMap.set('value',0);
         //console.log(keyMap);
         arr.push(keyMap);
     }
     return arr 
}

function respKeyCounts(keyMapArr){
  let respData = new Array()
    for (keyMap of keyMapArr) {
        let tmp = {'key':keyMap.get('key'), 'value':keyMap.get('value')};
        respData.push(tmp);
        //console.log(tmp)
    }
  return respData
}

function getKeyCounts(keyMapArr){
    var keyCountstr = '';
    for (keyMap of keyMapArr) {
        keyCountstr = keyCountstr.concat(keyMap.get('key_count'))
        keyCountstr = keyCountstr.concat(',')
    }
    return keyCountstr.substr(0, keyCountstr.lastIndexOf(','))
}

function saveKeyValue(keyMapArr, data){
  for (keyMap of keyMapArr){
    for(var item in data){
      if(item === keyMap.get('key')) {
        let value = 0
        if(data[item]){
          for(dat of data[item]){
            value += Number.parseFloat(dat.value)
          }
        }
        keyMap.set('value',value)
      }else if (item === keyMap.get('key_count')) {
        let oldValue = keyMap.get('value')
        let value = 1
        if(data[item]){
          for(dat of data[item]){
            value += Number.parseFloat(dat.value)
          }
          value = value / data[item].length
        }
        keyMap.set('value',Math.ceil(Number.parseFloat(oldValue)* value))
      }
    }
    //console.log(keyMap)
  }
}
/*
*/
function userFlowControl(token, isInput){
  let userObj = util.parseToken(token)    
  if(isInput){
    for (user of userArr) {
      if(user === userObj.sub)
      {
          logger.log('info',`${userObj.sub} is contorled by user flow control!`)
          return true
      }
    }
    userArr.push(userObj.sub)
    return false
  }else{
    for (user of userArr) {
      if(user === userObj.sub)
      {
        userArr.splice(userArr.indexOf(user),1);
      }
    }
  }
}

//GET STAT
router.get('/:id', function (req, res) {
    let id = req.params.id;
    let token = req.headers['x-authorization'];
    let interval = (req.query.endTime - req.query.startTime)/10;
    let keys = req.query.keys;
    if(userFlowControl(token,1))
    {
      util.responData(403, "请求过于频繁，请您稍后再进行操作！！", res);
      return 
    }
    keyMapArr = keysConvert(keys)
    let key_counts = getKeyCounts(keyMapArr)
    //console.log(key_counts)
    let api = util.getAPI() + `plugins/telemetry/DEVICE/${id}/values/timeseries?&keys=${keys}&startTs=${req.query.startTime}&endTs=${req.query.endTime}&agg=COUNT&interval=${interval}`;
    api = encodeURI(api)
    //console.log(api)
    axios.get(api, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        saveKeyValue(keyMapArr, resp.data)
        let api_sr = util.getAPI() + `plugins/telemetry/DEVICE/${id}/values/timeseries?&keys=${key_counts}&startTs=${req.query.startTime}&endTs=${req.query.endTime}&agg=AVG&interval=${interval}`;
        api_sr = encodeURI(api_sr)
        //console.log(api_sr)
        axios.get(api_sr, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          saveKeyValue(keyMapArr, resp.data)
          userFlowControl(token,0)
          util.responData(util.CST.OK200, respKeyCounts(keyMapArr), res);
        }).catch(err =>{
          userFlowControl(token,0)
          util.responErrorMsg(err, res);
        });
      }).catch(err =>{
        userFlowControl(token,0)
        util.responErrorMsg(err, res);
      });
})

module.exports = router