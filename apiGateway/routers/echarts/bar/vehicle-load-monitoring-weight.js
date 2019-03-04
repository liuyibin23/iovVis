const axios = require('axios');
const util = require('../../../util/utils');

let option = {
    title : {
         text: '车辆荷载自动监测数据车重',
         x: 'center',
         y:'bottom'
     },
     legend: {
         data:['车重 > 44T', '24T < 车重 < 44T', '7.5T < 车重 < 24T', '2.5T < 车重 < 7.5T', '车重 < 2.5T'],
         orient: 'vertical',
         x:'80%',
         y:'top',
         borderWidth: 1
     },
     toolbox: {
         show : false
     },
     xAxis : [
         {
             type : 'category',
             data : []
         }
     ],
     yAxis : [
         {
                type : 'value',
           axisLabel : {
                 formatter: '{value} 辆'
             }
         }
     ],
     series : [
         {
             name:'车重 < 2.5T',
             type:'bar',
             stack: '总量',
            // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[]
         },
         {
            name:'2.5T < 车重 < 7.5T',
            type:'bar',
            stack: '总量',
             data:[]
         },
         {
            name:'7.5T < 车重 < 24T',
            type:'bar',
            stack: '总量',
          
             //itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
             data:[]
         },
         {
            name:'24T < 车重 < 44T',
            type:'bar',
            stack: '总量',
           // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data:[]
        },
        {
            name:'车重 > 44T',
            type:'bar',
            stack: '总量',
            barCateGoryGap:20,
           // itemStyle : { normal: {label : {show: true, position: 'insideRight'}}},
            data:[]
        }
     ]
 };
                     
 var allData = [];
 var MAX_DATA = 1;
 var retCnt = 0;
 var respHasSend = 0;
 
 function processData(res, params, callback){ 
     callback(option, params, res);
 }
 
 async function getData(idx, dataType, params, token, res, callback){
     let keyValue = 'Type1,Type2,Type3,Type4,Type5';   //
     let limit    = 100000;
     let interval = 60 * 1000;
     let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
      + `&startTs=${params.startTime}&endTs=${params.endTime}&interval=${interval}&limit=${limit}&agg=COUNT`;
     api = encodeURI(api);
     //console.log(api);
 
     axios.get(api, {
         headers: { "X-Authorization": token }
       }).then(response => {
         retCnt++;
         allData[idx] = response.data;
 
         if (retCnt == MAX_DATA){
             console.log('all data receive');
            
             //
             let type1 = response.data.Type1;
             let type2 = response.data.Type2;
             let type3 = response.data.Type3;

             let typeList = [response.data.Type1, response.data.Type2, response.data.Type3, response.data.Type4, response.data.Type5];

             let max_idx = 0;
             for (let i = 0; i < typeList.length; i++){
                let type = typeList[i];
                if (type){
                    let idx = 0;
                    type.forEach(data => {
                        if (idx > 10){                       
                            option.series[i].barWidth = 20;
                        }
                        option.series[i].data[idx++] = Number.parseFloat(data.value);
                    });
                    
                    if (idx > max_idx)
                        max_idx = idx;
                }   
             }

             for (let i = 0; i < max_idx; i++){
                option.xAxis[0].data.push(i);
             }

             processData(res, params, callback);
         }
       }).catch(err => {
         if (!respHasSend) {
             respHasSend = true;
             //util.responErrorMsg(err, res);
             processData(res, params, callback);
         }
       });   
 }
 
 function resetPreData(){
     respHasSend = false;
     retCnt = 0;
     option.xAxis[0].data = [];
     for (let idx = 0; idx < MAX_DATA; idx++) {
         option.series[idx].data = [];
     }
 }
 
 var chart_area = {
     name: 'chart_data',
     version: '1.0.0',
 
     fillData: async function (params, token, res, callback) {
         resetPreData();
         for (var i = 0; i < MAX_DATA; i++){
             dataType = '震动';
             getData(i, dataType, params, token, res, callback);
         }
     }
 }
 module.exports = chart_area;
