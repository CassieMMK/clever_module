package com.avalon.jpcap.worker;

import com.avalon.jpcap.common.queue.DefaultAbstractBlockingQueue;
import com.avalon.jpcap.domain.VisitorRecordDTO;
import com.avalon.jpcap.service.RecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用户访问记录worker
 *
 * @author DingHaoLun
 * @since 2022-12-15 17:55
 **/
@Slf4j
@Component
public class VisitorRecordWorker {

    @Resource(name = "blockingStringQueue")
    private DefaultAbstractBlockingQueue blockingStringQueue;

    @Resource
    private RecordService recordService;

    /**各省访问计数Map*/
    private final Map<String, AtomicInteger> visitCountMap = new ConcurrentHashMap<>(64, 0.75f, 32);
    //ConcurrentHashMap线程安全，在设置worker多线程并发下避免脏读, 34个省级行政区+国家，默认初始化容量赋值64

    @Scheduled(cron = "${visitorRecord.worker.cron}")
    public void run() {
        log.info("从队列中消费一批数据，用户请求省份记录落库 run start!");
        List<String> provinceVisitorList= blockingStringQueue.consumerByBatch();

        if(!CollectionUtils.isEmpty(provinceVisitorList)){
            provinceVisitorList.stream().map(p -> {
                AtomicInteger a1 = visitCountMap.computeIfAbsent(p, (key) ->new AtomicInteger());//ConcurrentHashMap原子操作
                a1.incrementAndGet();//内存可见，原子操作+1
                return null;
            }).collect(Collectors.toList());

            //落库数据库，每个省份或地区"插入或更新"一条数据（内部也做了并发处理）
            visitCountMap.forEach((key, value) -> {
                VisitorRecordDTO visitorRecordDTO = new VisitorRecordDTO();
                visitorRecordDTO.setProvince(key);
                visitorRecordDTO.setProvinceVisitCountEachDay(value.intValue());
                visitorRecordDTO.setVisitDay(new Date());
                recordService.addRecord(visitorRecordDTO);
            });
            log.info("用户请求省份记录落库 run end");
        }
    }
}

//错误代码
//                if (visitCountMap.containsKey(p)) {
//                    //原子方式增加数量1没卵用，66-71行代码块并不是原子性的，并发时就脏读了
//                    visitCountMap.put(p,new AtomicInteger(visitCountMap.get(p).incrementAndGet()));
//                } else {
//                    visitCountMap.put(p,new AtomicInteger(1));
//                }
//https://zhuanlan.zhihu.com/p/113379816 错误例子