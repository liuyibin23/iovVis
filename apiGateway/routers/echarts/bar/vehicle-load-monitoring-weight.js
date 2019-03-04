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

 /**
 * 普通数组快速排序
 *
 * @param arr Array 数字数组
 * @param dir asc升序、desc降序
 *
 * @example:
 * sort([1,4,2,5])
 * sort([1,4,2,5],'asc')
 * sort([1,4,2,5],'desc')
 */
function sort(arr, dir){
    dir=dir||'asc';
    if (arr.length == 0) return [];

    var left = new Array();
    var right = new Array();
    var pivot = arr[0];

    if(dir==='asc'){//升序
        for (var i = 1; i < arr.length; i++) {
            arr[i] < pivot ? left.push(arr[i]): right.push(arr[i]);
        }
    }else{//降序
        for (var i = 1; i < arr.length; i++) {
            arr[i] > pivot ? left.push(arr[i]): right.push(arr[i]);
        }
    }
    return sort(left,dir).concat(pivot, sort(right,dir));
}
 
 async function getData(idx, dataType, params, token, res, callback){
     let keyValue = 'Type1,Type2,Type3,Type4,Type5';   //
     let interval = 10 * 60 * 1000;
     let limit = (params.endTime - params.startTime) / 1000.0 + 2;
     let filter = `&interval=${interval}&agg=COUNT`;
     let api = util.getAPI() + `plugins/telemetry/DEVICE/${params.devid}/values/timeseries?keys=${keyValue}`
      + `&startTs=${params.startTime}&endTs=${params.endTime}&limit=${limit}${filter}`;
     api = encodeURI(api);
     //console.log(api);
 
     axios.get(api, {
         headers: { "X-Authorization": token }
       }).then(response => {
         retCnt++;
         allData[idx] = response.data;
 
         if (retCnt == MAX_DATA){
             console.log('all data receive');

            let typeList = [response.data.Type1, response.data.Type2, response.data.Type3, response.data.Type4, response.data.Type5];
            var timeMap = new Map();
            for (let i = 0; i < typeList.length; i++){
                let type = typeList[i];
                if (type){
                    for (let i = 0; i < type.length; i++){
                        let data = type[i];
                        timeMap.set(data.ts, 1);
                    }
                }
            }

            let tsArry = [];
            let idx = 0;
            timeMap.forEach(function(value, key) {  
               tsArry[idx++] = key;  
            });

            let newArray = sort(tsArry, 'asc');

            let max_idx = timeMap.size;
            console.log('max idx=' + max_idx);

            // sort and remap
            timeMap.clear();
            for (var i = 0; i < newArray.length; i++){
                timeMap.set(newArray[i], i);
            }

             for (let i = 0; i < typeList.length; i++){
                let type = typeList[i];
                if (type){
                    // init
                    for (let j = 0; j < max_idx; j++){
                        option.series[i].data[j] = 0;
                    }
                    for (let j = 0; j < max_idx; j++){
                        if (type[j] && type[j].ts) {
                            let val = timeMap.get(type[j].ts);
                            if (val != -1){
                                option.series[i].data[val] = Number.parseFloat(type[j].value);
                            } 
                        }
                    }
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
