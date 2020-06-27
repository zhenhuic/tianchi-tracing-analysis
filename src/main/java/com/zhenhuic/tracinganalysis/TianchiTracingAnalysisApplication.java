package com.zhenhuic.tracinganalysis;

import com.zhenhuic.tracinganalysis.filter.FilterData;
import com.zhenhuic.tracinganalysis.gather.GatherData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TianchiTracingAnalysisApplication {

    public static void main(String[] args) {
        String port = System.getProperty("server.port", "8080");
        if (NodePort.isFilter()) {
            FilterData.initCache();
        }
        if (NodePort.isGather()) {
            GatherData.initCache();
        }
        SpringApplication.run(TianchiTracingAnalysisApplication.class,
                "--server.port=" + port
        );
    }

}
