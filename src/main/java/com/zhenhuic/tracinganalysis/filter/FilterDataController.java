package com.zhenhuic.tracinganalysis.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * 与汇总节点通信接口
 */
@RestController
public class FilterDataController {

    @RequestMapping("/getBadTrace")
    public String getBadTrace(@RequestParam String badTraceIdJson, @RequestParam Integer cachePos) {
        HashSet<String> badTraceIds = JSON.parseObject(badTraceIdJson, new TypeReference<HashSet<String>>() {
        });
        if (badTraceIds == null) {
            return null;
        }
        int index = cachePos % FilterData.CACHE_SIZE;
        int prevIndex = (index - 1 + FilterData.CACHE_SIZE) % FilterData.CACHE_SIZE;
        int nextIndex = (index + 1) % FilterData.CACHE_SIZE;

        HashMap<String, ArrayList<String>> badTraceMap = new HashMap<>();

        PullData.getBadTrace(prevIndex, badTraceIds, badTraceMap);
        PullData.getBadTrace(index, badTraceIds, badTraceMap);
        PullData.getBadTrace(nextIndex, badTraceIds, badTraceMap);

        //前一个缓冲区中的数据已经超时
        HashMap<String, ArrayList<String>> prevCache = FilterData.TRACE_CACHE.get(prevIndex);
        prevCache.clear();
        return JSON.toJSONString(badTraceMap);
    }
}
