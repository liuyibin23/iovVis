const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');


// define the about route
router.get('/about', function (req, res) {
    res.send('About wall result');
})


function configureRPC(deviceId, rpcCfg, onoff, token, res) {
    console.log(`CFG: ${deviceId} ${rpcCfg.ip} ${onoff}`);

    let rpcCmd = { 
        "method": "214",
        "param":{
            "cmd":214,    
            "ignoreip":false,    
            "ip": rpcCfg.ip,    
            "type":rpcCfg.type,    
            "onoff":onoff
        }
    };    

    let rpcApi = util.getAPI() + `plugins/rpc/twoway/${deviceId}`;
    axios.post(rpcApi, rpcCmd, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        //util.responErrorMsg(err, res);
        console.log(resp.data);
      }).catch(err => {
        util.responErrorMsg(err, res);
      });
}


function rpcOperator(onoff, clientsCnt, toekn, res)
{
    // 关流 
    if (onoff == 0) {
        if (clientsCnt >= 2) {
            util.responData(util.CST.OK200, "还有其他用户在观看视频流,停流操作被拒绝", res);
        } else {
            configureRPC(deviceId, rpcCfg, 0, token, res);            
        }     
    } else if (onoff == 1) {
        // 开流
        if (clientsCnt == 0) {
            configureRPC(deviceId, rpcCfg, 1, token, res);
        } else {
            util.responData(util.CST.OK200, "视频流处于打开状态,无需重复打开", res);
        }     
    } else {
        // 参数错误
        util.responData(util.CST.ERR400, util.CST.MSG400, res);
    }
}

function process(deviceId, rpcCfg, clientsInfo, streamsInfo, token, res){
    let ip = rpcCfg.ip;

    // 找到clientToken
    let clientToken = null;
    let clients = clientsInfo.clients;
    for (let i = 0 ; i < clients.length; i++) {
        if (clients[i].ip === ip) {
            clientToken = clients[i].url;      
            break;
        }
    }

    if (clientToken) {
        //clientToken = "/live/4KmwVQxPoNxph1dhQ27I";
        let tokenList = clientToken.split('/');

        if (tokenList[2]) {
            clientToken = tokenList[2];

            // 根据clientToken 获取当前在线的客户端数量
            let streams = streamsInfo.streams;
            if (streams) {
                for (let i = 0; i < streams.length; i++) {
                    if (streams[i].name == clientToken) {
                        let onoff = Number.parseInt(rpcCfg.onoff);
                        let clientsCnt = Number.parseInt(streams[i].clients);
                         // 根据服务端的客户端数量，决定是否关流
                        rpcOperator(onoff, clientsCnt, token, res);
                        break;
                    }
                }
            } else {
                util.responData(util.CST.ERR400, "流服务器查不到此客户端的token信息", res);
            }            
        }
        else {
            util.responData(util.CST.ERR400, "设备token格式错误", res);
        } 
    }
    else {
        util.responData(util.CST.ERR400, "未找到此设备的信息", res);
    }    
}

router.post('/:id', async function (req, res) {
    let deviceId = req.params.id;
    let rpcCfg = req.body;
    let token = req.headers['x-authorization'];

    // 获取clients信息
    let srsApi = util.getSrsAPI() + 'v1/clients/';
    axios.get(srsApi, {
        headers: {
          "X-Authorization": token
        }
      }).then(resp => {
        let clientsInfo = resp.data;
        // 获取服务器信息
        let srsApiStreams = util.getSrsAPI() + 'v1/streams/';
        axios.get(srsApiStreams, {
            headers: {
                Accept: 'application/json',
                "X-Authorization": token,
                'Content-Type': 'application/json',
            }
          }).then(resp => {
            let streamsInfo = resp.data;

            let testCode = 0; //1;
            if (testCode) {
                streamsInfo = {
                    "code": 0,
                    "server": 18732,
                    "streams": [{
                        "id": 18734,
                        "name": "B9INx5NPObulJUL1M1Th",
                        "vhost": 18733,
                        "app": "live",
                        "live_ms": 1558669309276,
                        "clients": 2,
                        "frames": 11784619,
                        "send_bytes": 43368409107,
                        "recv_bytes": 83750823400,
                        "kbps": {
                            "recv_30s": 844,
                            "send_30s": 853
                        },
                        "publish": {
                            "active": true,
                            "cid": 12723
                        },
                        "video": {
                            "codec": "H264",
                            "profile": "High",
                            "level": "5.1"
                        },
                        "audio": {
                            "codec": "AAC",
                            "sample_rate": 44100,
                            "channel": 2,
                            "profile": "LC"
                        }
                    }, {
                        "id": 18735,
                        "name": "4KmwVQxPoNxph1dhQ27I",
                        "vhost": 18733,
                        "app": "live",
                        "live_ms": 1558669309276,
                        "clients": 2,
                        "frames": 3294109,
                        "send_bytes": 3535501867,
                        "recv_bytes": 3694988013,
                        "kbps": {
                            "recv_30s": 87,
                            "send_30s": 89
                        },
                        "publish": {
                            "active": true,
                            "cid": 12720
                        },
                        "video": {
                            "codec": "H264",
                            "profile": "Main",
                            "level": "3"
                        },
                        "audio": {
                            "codec": "AAC",
                            "sample_rate": 44100,
                            "channel": 2,
                            "profile": "LC"
                        }
                    }
                ]
                };
            }
           
            // 处理
            process(deviceId, rpcCfg, clientsInfo, streamsInfo, token, res);
          }).catch(err => {
              util.responErrorMsg(err, res);
          });
      }).catch(err => {
          util.responErrorMsg(err, res);
      });
})

module.exports = router