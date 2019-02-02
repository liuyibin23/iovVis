var express = require('express');
var router = express.Router();
const axios = require('axios');
var util = require('./utils');

// middleware that is specific to this router
router.use(function timeLog(req, res, next) {
  console.log('Time: ', Date.now());
  next();
})
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
    // 获取规则链
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
      // res.status(200).json({ code: 200, message: 'ok' });
      var ruleChain = resp.data;
      if (ruleChain) {
        ruleID = ruleChain[0].id;
        let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
        // 获取告警规则链的meta数据
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
          // 一级告警配置
          let jsScript = nodes[1].configuration.jsScript;
          var index = jsScript.indexOf('/* alarm rule tables */');
          eval(jsScript.substr(0, index));
          if (devID) {
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
          }
          // 二级告警配置
          jsScript = nodes[8].configuration.jsScript;
          index = jsScript.indexOf('/* alarm rule tables */');
          eval(jsScript.substr(0, index));
          if (devID) {
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
          }
          if (find) {
            let resMsg = {
              "code": '200',
              "message:": retCfg
            };
            util.responData('200', retCfg, res);
          }
          else {
            util.responData('404', '访问资源不存在。', res);
          }
        }).catch(err => {
          util.responErrorMsg(err, res);
        });

      }
    }).catch(err => {
      util.responErrorMsg(err, res);
    });
  }).catch(err => {
    // 由资产号查询tenantId出现问题
    util.responErrorMsg(err, res);
  });
})
//POST
router.post('/:id', async function (req, res) {
  var devID = req.params.id;
  var IndeterminateRules = req.body.IndeterminateRules;
  var WarningRules = req.body.WarningRules;
  let token = req.headers['x-authorization'];

  // 获取规则链
  let ruleChain = await util.getSync('http://cf.beidouapp.com:8080/api/ruleChains',
    {
      headers: {
        "X-Authorization": token
      },
      params: {
        textSearch: "CONFIG_ALARM_RULE",
        limit: 1
      }
    }
  );

  if (ruleChain) {
    ruleID = ruleChain.data[0].id;
    let url = util.getAPI() + `ruleChain/${ruleID.id}/metadata`;
    // 获取告警规则链的meta数据
    let ruleMeta = await util.getSync(url,
      {
        headers: {
          "X-Authorization": token
        }
      }
    );

    if (ruleMeta) {
      let nodes = ruleMeta.nodes;

      // 一级告警配置
      let jsScript = nodes[1].configuration.jsScript;
      var index = jsScript.indexOf('/* alarm rule tables */');
      eval(jsScript.substr(0, index));
      if (devID) {
        let newJs = `JSON.parse(msg.waves).some(function(it,ind){/*S*/var thd={min:${IndeterminateRules.min}, max:${IndeterminateRules.max}};/*E*/if(it < thd.min || it > thd.max) return 1;});`;
        ruleTables[devID] = newJs;
        nodes[1].configuration.jsScript = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + jsScript.substr(index);
      }

      // 二级告警配置
      jsScript = nodes[8].configuration.jsScript;
      index = jsScript.indexOf('/* alarm rule tables */');
      eval(jsScript.substr(0, index));
      if (devID) {
        let newJs = `JSON.parse(msg.waves).some(function(it,ind){/*S*/var thd={min:${WarningRules.min}, max:${WarningRules.max}};/*E*/if(it < thd.min || it > thd.max) return 1;});`;
        ruleTables[devID] = newJs;
        nodes[8].configuration.jsScript = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + jsScript.substr(index);
      }

      // post
      url = util.getAPI() + 'ruleChain/metadata';
      axios.post(url, (ruleMeta), { headers: { "X-Authorization": token } })
        .then(response => {
          if (response.status == 200) {
            let resMsg = {
              "code": '200',
              "message:": '设置告警规则成功。'
            };
            res.status(200).json(resMsg);
          }
          else {
            let resMsg = {
              "code": `${response.status}`,
              "message:": '访问资源不存在。'
            };
            res.status(404).json(resMsg);
          }
        })
        .catch(err => {
          util.responErrorMsg(err, res);
        })
    }
  }
})

module.exports = router