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
//GET
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
    let url = util.getAPI() + `beidouapp/ruleChains`;
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
      if (ruleChain) {
        ruleID = ruleChain[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        //获取告警规则链的meta数据
        axios.get(url, {
          headers: {
            "X-Authorization": token
          }
        }).then(resp => {
          let nodes = resp.data.nodes;
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
          if (js1 !== '') {
            let jsScript = js1;
            var index = jsScript.indexOf('/* alarm rule tables */');
            eval(jsScript.substr(0, index));
            if (typeof ruleTables !== 'undefined') {
              let cfg = ruleTables[devID];
              if (cfg) {
                var start_idx = cfg.indexOf('/*S*/');
                var end_idx = cfg.indexOf('/*E*/');
                var cfg_rule = cfg.substr(start_idx, end_idx - start_idx);
                eval(cfg_rule);
                if (thd) {
                  retCfg.IndeterminateRules.min = thd.min;
                  retCfg.IndeterminateRules.max = thd.max;
                  find = true;
                }
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
                var start_idx = cfg.indexOf('/*S*/');
                var end_idx = cfg.indexOf('/*E*/');
                var cfg_rule = cfg.substr(start_idx, end_idx - start_idx);
                eval(cfg_rule);
                if (thd) {
                  retCfg.WarningRules.min = thd.min;
                  retCfg.WarningRules.max = thd.max;
                  find = true;
                }
              }
              delete ruleTables; // clear the enviroment variable, only for eval case.
            }
          }

          if (find) {
            util.responData(util.CST.OK200, retCfg, res);
          }
          else {
            util.responData(util.CST.ERR404, util.CST.MSG404, res);
          }
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
    util.responErrorMsg(err, res);
  });
})
//POST
router.post('/:id', async function (req, res) {
  let devID = req.params.id;
  let IndeterminateRules = req.body.IndeterminateRules;
  let WarningRules = req.body.WarningRules;
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
    let url = util.getAPI() + `beidouapp/ruleChains`;
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
            // 一级告警配置
            if (js1 !== '') {
              let jsScript = js1;
              let index = jsScript.indexOf('/* alarm rule tables */');
              eval(jsScript.substr(0, index));
              if (typeof ruleTables !== 'undefined') {
                let newJs = `JSON.parse(msg.waves).some(function(it,ind){/*S*/var thd={min:${IndeterminateRules.min}, max:${IndeterminateRules.max}};/*E*/if(it < thd.min || it > thd.max) return 1;});`;
                ruleTables[devID] = newJs;
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
                let newJs = `JSON.parse(msg.waves).some(function(it,ind){/*S*/var thd={min:${WarningRules.min}, max:${WarningRules.max}};/*E*/if(it < thd.min || it > thd.max) return 1;});`;
                ruleTables[devID] = newJs;
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
                util.responData(util.CST.OK200, util.CST.MSG200, res);
              }
              else {
                //
                logger.log('error', 'FIXME: why should I go here?')
                util.responData(util.CST.ERR404, util.CST.MSG404, res);
              }
            }).catch(err => {
              util.responErrorMsg(err, res);
            })
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
              url = util.getAPI() + `device`;
              let data = resp.data;
              data.additionalInfo = {
                WarningRules,
                IndeterminateRules
              };
              axios.post(url, (data), {
                headers: { "X-Authorization": token },
                params: { "tenantIdStr": TID }
              }).then(resp => {
                //添加addinfo后更新device信息
                util.responData(util.CST.OK200, util.CST.MSG200, res);
              }).catch(err => {
                //通过设备ID获取设备信息失败
                util.responErrorMsg(err, res);
              });
            }).catch(err => {
              //通过设备ID获取设备信息失败
              util.responErrorMsg(err, res);
            });
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