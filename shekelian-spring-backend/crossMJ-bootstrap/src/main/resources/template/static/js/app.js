//地图容器
var chart = echarts.init(document.getElementById('main'));
//34个省、市、自治区的名字拼音映射数组
var provinces = {
    //23个省
    "台湾": "taiwan",
    "河北": "hebei",
    "山西": "shanxi",
    "辽宁": "liaoning",
    "吉林": "jilin",
    "黑龙江": "heilongjiang",
    "江苏": "jiangsu",
    "浙江": "zhejiang",
    "安徽": "anhui",
    "福建": "fujian",
    "江西": "jiangxi",
    "山东": "shandong",
    "河南": "henan",
    "湖北": "hubei",
    "湖南": "hunan",
    "广东": "guangdong",
    "海南": "hainan",
    "四川": "sichuan",
    "贵州": "guizhou",
    "云南": "yunnan",
    "陕西": "shanxi1",
    "甘肃": "gansu",
    "青海": "qinghai",
    //5个自治区
    "新疆": "xinjiang",
    "广西": "guangxi",
    "内蒙古": "neimenggu",
    "宁夏": "ningxia",
    "西藏": "xizang",
    //4个直辖市
    "北京": "beijing",
    "天津": "tianjin",
    "上海": "shanghai",
    "重庆": "chongqing",
    //2个特别行政区
    "香港": "xianggang",
    "澳门": "aomen"
};

//直辖市和特别行政区-只有二级地图，没有三级地图
var special = ["北京","天津","上海","重庆","香港","澳门"];
var mapdata = [];
//绘制全国地图
$.getJSON('static/map/china.json', function(data){
    mapdata = [0,1,2];
	for( var i=0;i<data.features.length;i++ ){
        mapdata.push({
			name:data.features[i].properties.name
		})
	}
    //映射组件最大值
    maxValue=getMaxVlaue(mapdata)
    option.visualMap.max=maxValue
	//注册地图
	echarts.registerMap('china', data);
	//渲染地图
	renderMap('china',mapdata);
});

// //地图点击事件，下钻到更下一级地图
// chart.on('click', function (params) {
// 	console.log( params );
// 	if( params.name in provinces ){
// 		//如果点击的是34个省、市、自治区，绘制选中地区的二级地图
// 		$.getJSON('static/map/province/'+ provinces[params.name] +'.json', function(data){
// 			echarts.registerMap( params.name, data);
// 			var d = [];
// 			for( var i=0;i<data.features.length;i++ ){
// 				d.push({
// 					name:data.features[i].properties.name
// 				})
// 			}
// 			renderMap(params.name,d);
// 		});
// 	}else if( params.seriesName in provinces ){
// 		//如果是【直辖市/特别行政区】只有二级下钻
// 		if(  special.indexOf( params.seriesName ) >=0  ){
// 			renderMap('china',mapdata);
// 		}else{
// 			//显示县级地图
// 			$.getJSON('static/map/city/'+ cityMap[params.name] +'.json', function(data){
// 				echarts.registerMap( params.name, data);
// 				var d = [];
// 				for( var i=0;i<data.features.length;i++ ){
// 					d.push({
// 						name:data.features[i].properties.name
// 					})
// 				}
// 				renderMap(params.name,d);
// 			});
// 		}
// 	}else{
// 		renderMap('china',mapdata);
// 	}
// });

//地图点击事件，点击查看这个省份的7天数据变化
chart.on('click', function (params) {
	console.log( params );
	//展示省份的访问数据

});


//初始化绘制全国地图配置
var option = {
	backgroundColor: '#000',
    title : {
        text: '访问量地图监控大屏',
        subtext: '三级下钻',
        // link:'http://www.ldsun.com',
        left: 'center',
        textStyle:{
            color: '#fff',
            fontSize:16,
            fontWeight:'normal',
            fontFamily:"Microsoft YaHei"
        },
        subtextStyle:{
        	color: '#ccc',
            fontSize:13,
            fontWeight:'normal',
            fontFamily:"Microsoft YaHei"
        }
    },
    //配置tooltip自定义提示框组件,提示数量
    tooltip: {
        show: 'true',
        trigger: "item",
        formatter: function (params) {
            let str = ''
            if (isNaN(params.value)) {
                str = params.seriesName + ' : 0'
            } else {
                str = params.seriesName + ' : ' + params.value
            }
            return str
        }
    },
    //配置visualMap视觉映射组件,根据传入的data显示不同的颜色
    visualMap: {
        min: 0,
        max: 20,
        left: 'left',
        top: 'bottom',
        text: ['高', '低'],
        calculable: true
    },
    toolbox: {
        show: true,
        orient: 'vertical',
        left: 'right',
        top: 'center',
        feature: {
            dataView: {readOnly: false},
            restore: {},
            saveAsImage: {}
        },
        iconStyle:{
        	normal:{
        		color:'#fff'
        	}
        }
    },
    animationDuration:1000,
	animationEasing:'cubicOut',
	animationDurationUpdate:1000
     
};

//渲染地图
function renderMap(map,data){
	option.title.subtext = map;
    // option.series = [
	// 	{
    //         name: map,
    //         type: 'map',
    //         mapType: map,
    //         roam: false,
    //         nameMap:{
	// 		    'china':'中国'
	// 		},
    //         label: {
	//             normal:{
	// 				show:true,
	// 				textStyle:{
	// 					color:'#999',
	// 					fontSize:13
	// 				}
	//             },
	//             emphasis: {
	//                 show: true,
	//                 textStyle:{
	// 					color:'#fff',
	// 					fontSize:13
	// 				}
	//             }
	//         },
	//         itemStyle: {
	//             normal: {
	//                 areaColor: '#323c48',
	//                 borderColor: 'dodgerblue'
	//             },
	//             emphasis: {
	//                 areaColor: 'darkorange'
	//             }
	//         },
    //         data:data
    //     }
    // ];
    option.series = [
        {
            name: '访问量',
            type: "map",
            mapType: 'china',
            roam: true,
            label: {
                normal: {
                    show: true,
                    textStyle: {
                        color: "#fff",
                    },
                },
            },
            itemStyle: {
                normal: {
                    borderColor: "#e88787",
                    borderWidth: "2",
                    areaColor: "#373d4f",
                },
                emphasis: {
                    areaColor: '#F2D5AD',
                }
            },
            data: data //数据来源
        }
    ];
    chart.setOption(option);
}

//获取最大值
function getMaxVlaue(arr){
    let result=-Infinity
    for(let i in arr){
        if(arr[i].value>result)result=arr[i].value
    }
    return result
}