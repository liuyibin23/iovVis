const axios = require('axios');
const node_echarts = require('node-echarts');
const util  = require('../../../util/utils');
const charCfg = require('../../echarts/chartConfig');

var sTime = [];

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
    let interval = Number.parseFloat(params.interval) * 1000;
    let loopCnt =  Math.ceil((params.endTime - params.startTime) / interval);
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
        let startTime = startTimeFloat + j * interval;
        let endTime   = startTime + interval;
        timeSeries.push([startTime, endTime]);
    }


    let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/counts/timeseries?key=${plotCfg.keys}&tsIntervals=${JSON.stringify(timeSeries)}&valueIntervals=${JSON.stringify(ySeries)}&dataType=LONG`;
    api = encodeURI(api);
    sTime = timeSeries;
    
    axios.get(api, {
        headers: { "X-Authorization": token }
    }).then(resp => {
        let data = resp.data;       
        // 计算百分比
        if (plotCfg.groupType == "PERCENT"){
            //loopCnt = loopCnt > 7 ? 7 : loopCnt;

            loopCnt = resp.data.length > loopCnt ? loopCnt : resp.data.length;

            let idx = 0;
            // 分组处理
            for (let i = 0; i < loopCnt; i++) {
                // 跳过全为0的数据
                if (resp.data[i][0] == 0 && resp.data[i][1] == 0){
                    continue;
                }        

                // 时间轴
                var dat = util.getDateString(sTime[i][0], params.interval);
                option.xAxis[0].data.push(dat);  

                let cnt_over = 0;
                let cnt_sum = 0;
                for (let j = 0; j < resp.data[0].length; j++){
                    let value =  resp.data[i][j];
                    cnt_sum += value;
                    if (j >= 1) {
                        cnt_over += value;
                    }
                }

                // 计算百分比  超重车占一天车总数的百分比
                let percent = cnt_over / cnt_sum * 100;
                option.series[0].data[idx++] = percent.toFixed(2);
            }
        }
        else {
            loopCnt = loopCnt > 7 ? 7 : loopCnt;

            // 分组处理
            for (let i = 0; i < loopCnt; i++) {                
                // 时间轴
                var dat = util.getDateString(sTime[i][0], params.interval);
                option.xAxis[0].data.push(dat);  
            }
            
            for (let i = 0; i < loopCnt; i++){
                let col_data = data[i];

                for (let j = 0; j < col_data.length; j++){
                    option.series[j].data[i] = col_data[j];
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
    option.xAxis[0].data = [];
    let len = option.series.length;
    for (let idx = 0; idx < len; idx++) {
        option.series[idx].data = [];
    }
}

exports.resetPreData = resetPreData;
exports.getData = getData;