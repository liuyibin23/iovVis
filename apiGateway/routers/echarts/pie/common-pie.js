const axios = require('axios');
const node_echarts = require('node-echarts');
const util  = require('../../../util/utils');
const charCfg = require('../../echarts/chartConfig');

function SendPngResponse(option, params, res){
    if (!option)
    {
        util.responData(404, '访问资源不存在。', res);
        return;  
    }

    let config = {
        width: params.chartWidth   ? params.chartWidth  * 100 : 500, // Image width, type is number.
        height: params.chartHeight ? params.chartHeight * 100 : 400, // Image height, type is number.
        option: option, // Echarts configuration, type is Object.
        //If the path  is not set, return the Buffer of image.
        // path:  '', // Path is filepath of the image which will be created.
        enableAutoDispose: true  //Enable auto-dispose echarts after the image is created.
    }
    let bytes = node_echarts(config);
    if (bytes) {
        let data = 'data:image/png;base64,' + bytes.toString('base64');
        util.responData(200, data, res);
    }
}

function getData(plotCfg, option, params, token, res){    
    let grounCnt = plotCfg.groupInfo.length;
    let loopCnt =  Math.ceil((params.endTime - params.startTime) / plotCfg.interval);
    /*
    // 阈值分组参数
    let ySeries = [
        [0,   2.5],
        [2.5, 7.5],
        [7.5, 24],
        [24,  44],
        [44,  999]
    ];*/
    let ySeries = [];
    for (let i = 0; i < grounCnt; i++) {
        let cfg = plotCfg.groupInfo[i];
        ySeries.push([cfg.min, cfg.max]);

        // let name = "";
        // // 特殊处理
        // if (cfg.max == 99999){
        //     name = `${plotCfg.keys} > ${cfg.min}`;
        // }
        // else
        // {
        //     name = `${cfg.min} < ${plotCfg.keys} < ${cfg.max}`;
        // }

        // option.series[i].name = name;
        // option.legend.data[i] = name;
    }
    option.title.text = plotCfg.title;
    /*
    // 时间分组参数
    let timeSeries =[
        [1548950556000, 1548950756000],
        [1548950756000, 1548950956000],
        [1548950956000, 1548951156000],
        [1548951156000, 1548951356000],
        [1548951356000, 1548951556000]
    ];
    */
    let timeSeries = [];
    // 按时间段拆分成多个组 多少个柱状
    let startTimeFloat = Number.parseFloat(params.startTime);
    for (let j = 0; j < loopCnt; j++) {        
        let startTime = startTimeFloat + j * plotCfg.interval;
        let endTime   = startTime + plotCfg.interval;
        timeSeries.push([startTime, endTime]);
    }


    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/counts/timeseries?key=${plotCfg.keys}&tsIntervals=${JSON.stringify(timeSeries)}&valueIntervals=${JSON.stringify(ySeries)}&dataType=LONG`;
    api = encodeURI(api);

    axios.get(api, {
        headers: { "X-Authorization": token }
    }).then(resp => {
        let data = resp.data;

        // 统计每个区间的总数
        if (data[0]) {
            let len = data[0].length;
            for (let i = 0; i < len; i++){
                let sum = 0;
                for (let j = 0; j < loopCnt; j++){
                    sum += data[j][i];
                }

                if (sum != 0) {
                    let name = [];
                    if (plotCfg.groupInfo[i].max == 99999){
                        name = `${plotCfg.groupInfo[i].min} ${plotCfg.unit}以上`;
                    }else if (plotCfg.groupInfo[i].min == 0) {
                        name = `${plotCfg.groupInfo[i].max} ${plotCfg.unit}以下`;
                    }
                    else{
                        name = `${plotCfg.groupInfo[i].min}-${plotCfg.groupInfo[i].max} ${plotCfg.unit}`;
                    }
                    option.series[0].data[i] = { value:sum, name:name};
                }            
            }
        }        
        
        SendPngResponse(option, params, res);
    }).catch(err =>{
        console.log(err);
        SendPngResponse(option, params, res);
    });
}

function resetPreData(option, maxCnt){
    let len = option.series[0].data.length;
    option.series[0].data = [];
}

exports.resetPreData = resetPreData;
exports.getData = getData;