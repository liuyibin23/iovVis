
const axios = require('axios');
const util = require('../util/utils');

const {
  GraphQLObjectType,
  GraphQLSchema,
  GraphQLInt,
  GraphQLID,
  GraphQLString,
  GraphQLList,
  GraphQLNonNull,
} = require('graphql');

function findExist(itemkey, dataList){
  for (var i = 0; i < dataList.length; i++){
    if (dataList[i].monitorItem == itemkey){
      return i;
    }
  }

  return -1;
}

// 获取检测项目及测点数据
async function getMonitorData(req, assetId){

  //  let data = [
  //   {
  //     monitorItem: '桥面',
  //     sensorType: '位移传感器',
  //     unit: '米',
  //     measurePointCnt: 10
  //   },
  //   {
  //     monitorItem: '桥墩',
  //     sensorType: '温度传感器',
  //     unit: '度',
  //     measurePointCnt: 6
  //   },
  //   {
  //     monitorItem: assetId.assetId,
  //     sensorType: '温度传感器',
  //     unit: '度',
  //     measurePointCnt: 20
  //   }
  // ];
  // return data;

  let data = [];

  let token = req.headers['x-authorization'];
  let getDeviceByAssetIdAPI = util.getAPI() + `currentUser/getDeviceByAssetId?assetId=${assetId.assetId}`;

  await axios.get(getDeviceByAssetIdAPI, {
    headers: {
      "X-Authorization": token
    }
  })
    .then((resp) => {
      let find = false;
      for (var i = 0; i < resp.data.length; i++) {
        let info = resp.data[i];
        // 遍历属性 查找moniteritem
        for (var j = 0; j < info.attributeKvList.length; j++){
          let attr = info.attributeKvList[j];
          if (attr.key === 'moniteritem' && attr.value != '') {
            // console.log('监测项:' + attr.value + ' ' + info.device.type);
            let idx = findExist(attr.value, data);
            if (-1 != idx) {
              data[idx].measurePointCnt += 1;
            } else {
              let _dt = {monitorItem:attr.value, sensorType:info.device.type, unit:'度', measurePointCnt:1};
              data.push(_dt);
            }
          }
        }
      }
    })
    .catch((err) => {
      util.responErrorMsg(err, res);
    });

    return data;
}

 // 监测项目及测点布置
 const MonitorItemAndItem = new GraphQLObjectType({
  name: 'monitorItemAndMesurePoint',
  fields: {
    // 监测项目
    monitorItem: {
      type: GraphQLString
    },
    // 传感器类型
    sensorType: {
      type: GraphQLString
    },
    // 单位
    unit: {
      type: GraphQLString
    },
    // 测点数量
    measurePointCnt:{
      type: GraphQLInt
    }
  }
});

const queryObj = new GraphQLObjectType({
name: 'MonitorItemAndItemQuery',
description: 'a hello world demo',
fields: {
    // hello: {
    //     name: 'a hello world query',
    //     description: 'a hello world demo',
    //     type: GraphQLString,
    //     resolve(parentValue, args, request) {
    //         return '测试数据接口Demo';
    //     }
    // },
    monitorData: {
      type: new GraphQLList(MonitorItemAndItem),
      args: {
        // 资产ID
        assetId: { type: GraphQLString }
      },
      resolve: (req, assetId) => {
        return getMonitorData(req, assetId);
      }
    }
  }
});

module.exports = new GraphQLSchema({query: queryObj});
