package com.zhenhuic.tracinganalysis.gather;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.zhenhuic.tracinganalysis.CommonController;
import com.zhenhuic.tracinganalysis.NodePort;
import com.zhenhuic.tracinganalysis.Utils;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据排序
 */
public class MergeData {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeData.class);
    private static final String[] filterPorts = new String[]{NodePort.FILTER_PORT1, NodePort.FILTER_PORT2};
    private static final String LOCALHOST = "http://localhost:";

    public static void getBadTrace(HashSet<String> badTraceIds, int cachePos) {
        try {
            HashMap<String, HashSet<String>> badTrace = new HashMap<>();
            String badTraceIdJson = JSON.toJSONString(badTraceIds);
            for (String port : filterPorts) {
                RequestBody body = new FormBody.Builder().add("badTraceIdJson", badTraceIdJson)
                        .add("cachePos", cachePos + "").build();
                Request request = new Request.Builder().url(LOCALHOST + port + "/getBadTrace").post(body).build();

                Response response = Utils.callHttp(request);
                assert response.body() != null;
                HashMap<String, ArrayList<String>> oneBadTrace = JSON.parseObject(response.body().string(),
                        new TypeReference<HashMap<String, ArrayList<String>>>() {
                        });

                if (oneBadTrace != null) {
                    for (Map.Entry<String, ArrayList<String>> entry : oneBadTrace.entrySet()) {
                        String traceId = entry.getKey();
                        HashSet<String> spans = badTrace.computeIfAbsent(traceId, k -> new HashSet<>());
                        spans.addAll(entry.getValue());
                    }
                }
                response.close();
            }

            for (Map.Entry<String, HashSet<String>> entry : badTrace.entrySet()) {
                String traceId = entry.getKey();
                HashSet<String> spans = entry.getValue();

                //按照startTime排序
                String spanStr = spans.stream().sorted(Comparator.comparing(Utils::parseStartTime))
                        .collect(Collectors.joining("\n"));
                spanStr = spanStr + '\n';

                //生成md5
                String result = Utils.MD5(spanStr);
                GatherData.CHECKSUM.put(traceId, result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void finish() {
        //处理完剩余的数据
        for (int i = 0; i < GatherData.CACHE_SIZE; i++) {
            TraceIdSet traceIdSet = GatherData.TRACE_ID_CACHE.get(i);
            if (traceIdSet.getTraceIds() != null) {
                MergeData.getBadTrace(traceIdSet.getTraceIds(), traceIdSet.getCachePos());
            }
        }

        //上报结果
        try {
            String result = JSON.toJSONString(GatherData.CHECKSUM);
            RequestBody body = new FormBody.Builder()
                    .add("result", result).build();
            String url = LOCALHOST + CommonController.getDataSourcePort() + "/api/finished";
            Request request = new Request.Builder().url(url).post(body).build();
            Response response = Utils.callHttp(request);
            response.close();
            LOGGER.info("checksum finish");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
