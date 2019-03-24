const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');


// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Time: ', Date.now());
//   next();
// })
// define the home page route
router.get('/', function (req, res) {
  res.send('Alarms Api home page');
})
// define the about route
router.get('/about', function (req, res) {
  res.send('About alarms');
})

function getRuleChain(devID, nodes, res){
  let retCfg = {
    "IndeterminateRules": {
      "min": 0,
      "max": 0
    },
    "WarningRules": {
      "min": 0,
      "max": 0
    }
  };
  let find = false;

  //一级告警规则配置和二级告警配置的获取
  let js1 = '', js2 = '';
  for (let node of nodes) {
    if (node.name === util.CFG.ALARM_NODE_NAME_1)
      js1 = node.configuration.jsScript;
    else if (node.name === util.CFG.ALARM_NODE_NAME_2)
      js2 = node.configuration.jsScript;
  }

  var allCfg = [];
  if (js1 !== '') {
    let jsScript = js1;
    var index = jsScript.indexOf('/* alarm rule tables */');
    eval(jsScript.substr(0, index));
    if (typeof ruleTables !== 'undefined') {
      let cfg = ruleTables[devID];
      if (cfg) {
        find = true;
        allCfg = JSON.parse(cfg);
      }
      delete ruleTables; // clear the enviroment variable, only for eval case.
    }
  }
  if (js2 !== '') {
    let jsScript = js2;
    index = jsScript.indexOf('/* alarm rule tables */');
    eval(jsScript.substr(0, index));
    if (typeof ruleTables !== 'undefined') {
      let cfg = ruleTables[devID];
      if (cfg) {
        find = true;
        cfg = JSON.parse(cfg);
        let i = 0;
        let j = 0;
        for (i = 0; i < allCfg.length; i++){
          for (j = 0; j < cfg.length; j++){
            if (allCfg[i].Key == cfg[j].Key){
               allCfg[i].WarningRules = cfg[j].WarningRules;
            }
          }
        }
      }
      delete ruleTables; // clear the enviroment variable, only for eval case.
    }
  }

  if (find) {
    util.responData(util.CST.OK200, allCfg, res);
  }
  else {
    util.responData(util.CST.ERR404, util.CST.MSG404, res);
  }
}

//GET获取指定设备号上绑定的告警规则
router.get('/:id', async function (req, res) {
  var devID = req.params.id;
  let token = req.headers['x-authorization'];

  //接口因权限问题修改，需要根据devID查询tenantId
  let urlGetTenant = util.getAPI() + `device/${devID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    let url = util.getAPI() + `currentUser/ruleChains`;
    var tenantId = resp.data.tenantId.id;
    //获取规则链
    axios.get(url, {
      headers: {
        "X-Authorization": token
      },
      params: {
        textSearch: "CONFIG_ALARM_RULE",
        limit: 1,
        tenantIdStr: tenantId
      }
    }).then(resp => {
      //res.status(200).json({ code: 200, message: 'ok' });
      var ruleChain = resp.data;
      if (ruleChain && ruleChain.length > 0) {
        ruleID = ruleChain[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        //获取告警规则链的meta数据
        axios.get(url, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          let nodes = resp.data.nodes;
          getRuleChain(devID, nodes, res);
        }).catch(err => {
          util.responErrorMsg(err, res);
        });
      } else {
        logger.log('error', util.CST.MSG510);
        util.responData(util.ERR510, util.MSG510, res);
      }
    }).catch(err => {
      util.responErrorMsg(err, res);
    });
  }).catch(err => {
    // 由devID查询tenantId出现问题
    // util.responData(util.CST.ERR512, util.CST.MSG512, res);
    util.responErrorMsg(err, res);
  });
})

function updateDeviceInfo(tenantId, data, WarningRules, IndeterminateRules, token, res){
  let url = util.getAPI() + `device`;
  data.additionalInfo = {
    WarningRules,
    IndeterminateRules
  };
  axios.post(url, (data), {
    headers: { "X-Authorization": token },
    params: { "tenantIdStr": tenantId }
  }).then(resp => {
    //添加addinfo后更新device信息
    util.responData(util.CST.OK200, util.CST.MSG200, res);
  }).catch(err => {
    //通过设备ID获取设备信息失败
    util.responErrorMsg(err, res);
  });
}

function configRuleChain(devID, nodes, params, ruleMeta, tenantId, token, res){
  //一级告警规则配置和二级告警配置的获取
  let js1 = '', js2 = '', index1 = -1, index2 = -1;
  for (let index in nodes) {
    if (nodes[index].name === util.CFG.ALARM_NODE_NAME_1) {
      js1 = nodes[index].configuration.jsScript;
      index1 = index;
    } else if (nodes[index].name === util.CFG.ALARM_NODE_NAME_2) {
      js2 = nodes[index].configuration.jsScript;
      index2 = index;
    }
  }

  let IndeterminateRules = [];
  let WarningRules       = [];
  if (params.length > 0){
    let i = 0;    
    // [{"Key":"温度","thrd": {"min": 1,"max": 2}},{"Key":"湿度","thrd": {"min": 1,"max": 2}}]
    for (i = 0; i < params.length; i++){
      let _dt = {
        "Key":`${params[i].Key}`, 
        "IndeterminateRules":{
          "min":`${params[i].IndeterminateRules.min}`, 
          "max":`${params[i].IndeterminateRules.max}`
        }
      };
      IndeterminateRules.push(_dt);

      let _dt2 = {
        "Key":`${params[i].Key}`, 
        "WarningRules":{
          "min":`${params[i].WarningRules.min}`, 
          "max":`${params[i].WarningRules.max}`
        }
      };
      WarningRules.push(_dt2);
    }
  }

  // 一级告警配置
  if (js1 !== '') { 
    let jsScript = js1;
    let index = jsScript.indexOf('/* alarm rule tables */');
    eval(jsScript.substr(0, index));
    if (typeof ruleTables !== 'undefined') {
      if (ruleTables[devID]) {
        let oldCfg = JSON.parse(ruleTables[devID]);

        // 覆盖
        let newCfg = IndeterminateRules;

        /*
        // 使用最新的值去匹配旧值，不存在就新加一条
        for (let i = 0; i < IndeterminateRules.length; i++){
          let find = 0;
          if (oldCfg[i]){
            for (let j = 0; j < oldCfg.length; j++){
              // 找到 修改阈值
              if (IndeterminateRules[j] && (oldCfg[i].Key === IndeterminateRules[j].Key)) {
                find = 1;
                newCfg[i].IndeterminateRules =  IndeterminateRules[j].IndeterminateRules;
              }
            } 
          }          

          // 没找到 就新加一条
          if (!find) {
            newCfg.push(IndeterminateRules[i]);
          }
        }
        */
        ruleTables[devID] = JSON.stringify(newCfg);;
      }
      else{
        // 不存在添加
        ruleTables[devID] = JSON.stringify(IndeterminateRules);
      }
          
      nodes[index1].configuration.jsScript = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + jsScript.substr(index);
      delete ruleTables;
    }
  }
  // 二级告警配置
  if (js2 !== '') {
    let jsScript = js2;
    let index = jsScript.indexOf('/* alarm rule tables */');
    eval(jsScript.substr(0, index));
    if (typeof ruleTables !== 'undefined') {
      if (ruleTables[devID]) {
        let oldCfg = JSON.parse(ruleTables[devID]);
        // 覆盖
        let newCfg = WarningRules;

        /*
        // 使用最新的值去匹配旧值，不存在就新加一条
        for (let i = 0; i < WarningRules.length; i++){
          let find = 0;
          if (oldCfg[i]){
            for (let j = 0; j < oldCfg.length; j++){
              // 找到 修改阈值
              if ((WarningRules[j]) && oldCfg[i].Key === WarningRules[j].Key) {
                find = 1;
                newCfg[i].WarningRules =  WarningRules[j].WarningRules;
              }
            }   
          }
         
          // 没找到 就新加一条
          if (!find) {
            newCfg.push(WarningRules[i]);
          }
        }
        */
        ruleTables[devID] = JSON.stringify(newCfg);;
      }
      else {
        // 不存在添加
        ruleTables[devID] = JSON.stringify(WarningRules);
      }

      
      nodes[index2].configuration.jsScript = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + jsScript.substr(index);
      delete ruleTables;
    }
  }
  //post写库
  let url = util.getAPI() + 'ruleChain/metadata';
  //1.写入规则链
  axios.post(url, (ruleMeta), {
    headers: { "X-Authorization": token }
  }).then(response => {
    if (response.status == 200) {
      //2.写入设备的addinfo（失败的情况不考虑）
      //IndeterminateRules, WarningRules, devID, TID
      //通过设备ID获取设备信息
      url = util.getAPI() + `device/${devID}`;
      axios.get(url, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        //添加addinfo后更新device信息
        updateDeviceInfo(tenantId, resp.data, WarningRules, IndeterminateRules, token, res);
      }).catch(err => {
        //通过设备ID获取设备信息失败
        util.responErrorMsg(err, res);
      });
      //util.responData(util.CST.OK200, util.CST.MSG200, res);
    }
    else {
      //
      logger.log('error', 'FIXME: why should I go here?')
      util.responData(util.CST.ERR404, util.CST.MSG404, res);
    }
  }).catch(err => {
    util.responErrorMsg(err, res);
  })
}

//POST更新设备的告警规则，并且更新设备表的addtionalInfo，记录该告警规则
router.post('/:id', async function (req, res) {
  let devID = req.params.id;
  let token = req.headers['x-authorization'];
  let TID = '';

  //接口因权限问题修改，需要根据devID查询tenantId
  let urlGetTenant = util.getAPI() + `device/${devID}`;
  axios.get(urlGetTenant, {
    headers: {
      "X-Authorization": token
    }
  }).then(resp => {
    //根据tenantId和规则链名称获取规则链
    let url = util.getAPI() + `currentUser/ruleChains`;
    let tenantId = resp.data.tenantId.id;
    TID = tenantId; // save it for later usage.
    axios.get(url, {
      headers: {
        "X-Authorization": token
      },
      params: {
        textSearch: "CONFIG_ALARM_RULE",
        limit: 1,
        tenantIdStr: tenantId
      }
    }).then(resp => {
      //根据规则链编号获取规则链中存储的meta数据
      let ruleChain = resp;
      if (ruleChain) {
        ruleID = ruleChain.data[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        axios.get(url, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          let ruleMeta = resp.data;
          if (ruleMeta) {
            let nodes = ruleMeta.nodes;
            configRuleChain(devID, nodes, req.body, ruleMeta, TID, token, res);
          }
          // util.responData(511, 'CONFIG_ALARM_RULE规则链MetaData获取失败。' , res);
        }).catch(err => {
          //无法通过规则链编号获取规则链中存储的meta数据
          util.responErrorMsg(err, res);
        });
      } else {
        logger.log('error', util.CST.MSG510);
        util.responData(util.ERR510, util.MSG510, res);
      }
    }).catch(err => {
      //无法通过tenantId和规则链名称获取规则链
      util.responErrorMsg(err, res);
    });
  }).catch(err => {
    //无法通过设备号devID获取tenantId
    util.responErrorMsg(err, res);
  });
})

module.exports = router