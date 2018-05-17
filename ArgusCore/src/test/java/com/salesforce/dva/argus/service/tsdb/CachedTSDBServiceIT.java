package com.salesforce.dva.argus.service.tsdb;

import com.salesforce.dva.argus.entity.Metric;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class CachedTSDBServiceIT extends DefaultTSDBServiceIT {

    @Override
    public void setUp() {
        Properties props = new Properties();
        props.put("service.binding.tsdb", "com.salesforce.dva.argus.service.tsdb.DefaultTSDBService");
        props.put("service.property.tsdb.endpoint.read", "http://shared1-argustsdbr2-0-prd.data.sfdc.net:4466");
        props.put("service.property.tsdb.endpoint.write", "http://shared1-argustsdbw1-0-prd.data.sfdc.net:4477");
        props.put("service.property.cache.redis.cluster", "shared2-arguscachedev1-1-prd.eng.sfdc.net:6379,shared2-arguscachedev1-1-prd.eng.sfdc.net:6389,shared2-arguscachedev1-1-prd.eng.sfdc.net:6399");
        props.put("service.binding.cache", "com.salesforce.dva.argus.service.cache.RedisCacheService");

        system = newInstance(props);
        service = new CachedTSDBService(system.getConfiguration(), system.getServiceFactory().getMonitorService(), system.getServiceFactory().getCacheService(), system.getServiceFactory().getTSDBService());
    }

    @Override
    public void tearDown() {
    }

    @Override
    public void testPutAndGetMetrics() throws InterruptedException {
        super.testPutAndGetMetrics();
    }

    @Test
    public void testPutAndGetCacheHit() throws InterruptedException {
        List<Metric> expected = createRandomMetrics(null, null, 10);

        try {
            service.putMetrics(expected);
            Thread.sleep(2000);

            List<MetricQuery> queries = toQueries(expected);
            List<Metric> actual = _coalesceMetrics(service.getMetrics(queries));

            Assert.assertEquals(new HashSet<>(expected), new HashSet<>(actual));
            for (Metric metric : expected) {
                Metric actualMetric = actual.remove(actual.indexOf(metric));

                assertEquals(metric.getDisplayName(), actualMetric.getDisplayName());
                assertEquals(metric.getUnits(), actualMetric.getUnits());
                assertEquals(metric.getDatapoints(), actualMetric.getDatapoints());
            }
        } finally {
            service.dispose();
        }
    }

    protected static MetricQuery toQuery(Metric metric) {
        TreeMap<Long, Double> datapoints = new TreeMap<>(metric.getDatapoints());
        Long start = datapoints.firstKey() - 86400000L;
        Long end = System.currentTimeMillis();

        return new MetricQuery(metric.getScope(), metric.getMetric(), metric.getTags(), start, end);
    }

    private List<MetricQuery> toQueries(List<Metric> expected) {
        List<MetricQuery> queries = new LinkedList<>();

        for (Metric metric : expected) {
            queries.add(toQuery(metric));
        }
        return queries;
    }

    @Override
    public void testFractureMetrics() {
    }
}
