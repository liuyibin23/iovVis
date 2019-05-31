const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');

// 关流后台定时任务 配置参数
const MaxRetryCount = 3;          // 重试多少次
const TaskInterVal  = 3600000;    // 任务执行周期 ms
var   taskMap = new Map();

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
        if (res) {
            util.responData(resp.data, res);
        }        
      }).catch(err => {
        if (res) {
            util.responErrorMsg(err, res);
        } 
      });
    }

function closeVideoStreamTask(streamToken) {
    let taskInfo = taskMap[streamToken];
    if (taskInfo) {
        // 调度多少次之后 退出定时器
        if (taskMap[streamToken].taskCnt >= MaxRetryCount) {
            clearInterval(taskInfo.taskId);
            taskInfo.taskId = null;
            clearTask(streamToken)
            return
        }
        processTask(taskInfo)
    }
}

// 启动一个任务去处理关流操作
function saveTask(enableInterval,taskInfo) {
    if(enableInterval)
    {
        if(taskInfo.taskId !== null){
            clearInterval(taskInfo.taskId)
        }
        taskInfo.taskCnt++
        taskInfo.res = null
        taskInfo.taskId = setInterval(closeVideoStreamTask, TaskInterVal, taskInfo.streamToken);
    }
    taskMap[taskInfo.streamToken] = taskInfo;
}

function clearTask(streamToken) {
    taskMap[streamToken] = null;
}

function updateTask(streamToken,token,rpcCfg){
    taskMap[streamToken].taskCnt = 0;
    taskMap[streamToken].token = token;
    taskMap[streamToken].rpcCfg = rpcCfg;
}


function processTask(taskInfo){
    logger.log('info', `token:${taskInfo.streamToken},cnt:${taskInfo.taskCnt}`)
    let rpcCfg = taskInfo.rpcCfg;
    let srsApiStreams = util.getSrsAPI() + 'v1/streams/';
    axios.get(srsApiStreams, {
        headers: {
            Accept: 'application/json',
            //"X-Authorization": taskInfo.token,
            'Content-Type': 'application/json',
        }
    }).then(resp => {
        let streamsInfo = resp.data;
        if (streamsInfo) {
            let streams = streamsInfo.streams;
            if (streams) {
                for (var i = 0; i < streams.length; i++) {
                    if (streams[i].name == taskInfo.streamToken) {
                        let clientsCnt = Number.parseInt(streams[i].clients);
                        // 根据服务端的客户端数量，决定是否关流
                        if (clientsCnt >= 1) {
                            if(taskInfo.res){
                                util.responData(util.CST.OK200, "还有其他用户在观看视频流,已启动后台任务去做关流处理。", taskInfo.res);
                            }
                            // 重新启动定时器来触发任务
                            saveTask(1,taskInfo);
                        } else {
                            clearTask(taskInfo.streamToken)
                            configureRPC(deviceId, rpcCfg, 0, token, taskInfo.res);
                        }
                        return
                    }
                }
                if(i === streams.length && taskInfo.res){
                    clearTask(taskInfo.streamToken)
                    util.responData(util.CST.ERR400, "流服务器查不到此客户端的token信息", taskInfo.res);
                }
            }else{
                clearTask(taskInfo.streamToken)
                util.responData(util.CST.ERR400, "流服务器查不到任何流信息", taskInfo.res);
            }
        } else {
            clearTask(taskInfo.streamToken)
            util.responData(util.CST.ERR400, "流服务器信息错误", taskInfo.res);
        }            
    }).catch(err =>{
        logger.log('error', err)
        if(taskInfo.res){
            util.responErrorMsg(err, taskInfo.res);
        }
    });
}

router.post('/:id', async function (req, res) {
    let deviceId = req.params.id;
    let rpcCfg = req.body;
    let token = req.headers['x-authorization'];
    let streamToken = rpcCfg.token


    //on
    if (rpcCfg.onoff === "1") {
        //configure and response
        configureRPC(deviceId, rpcCfg, 1, token, res);
        return
    }
    if(rpcCfg.onoff !== "0"){
        util.responData(util.CST.ERR400, util.CST.MSG400, res);
        return
    }

    //check task map
    if (taskMap[streamToken]) {
        updateTask(streamToken,token,rpcCfg)
        util.responData(util.CST.OK200, "后台任务正在关流，更新任务", res);
        return
    }

    //save task map
    var taskInfo = {
        "taskId":null,
        "taskCnt":0,
        "streamToken":streamToken,
        "deviceId":deviceId,
        "rpcCfg": rpcCfg,
        "token": token,
        "res":res
    }
    saveTask(0,taskInfo)
    processTask(taskInfo)
})

module.exports = router