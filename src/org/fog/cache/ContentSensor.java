package org.fog.cache;

import java.util.Random;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.FogEvents;
import org.cloudbus.cloudsim.core.CloudSim;

public class ContentSensor extends Sensor {
    private final ZipfCatalog catalog;
    private final Random rnd;

    public ContentSensor(String name, int userId, String appId, Distribution dist, ZipfCatalog catalog, long seed) {
        super(name, "CONTENT_REQ", userId, appId, dist);
        this.catalog = catalog;
        this.rnd = new Random(seed);
    }

    @Override
    public void transmit() {
        double now = CloudSim.clock();
        ContentTuple t = new ContentTuple(getAppId(), Tuple.UP, 1, catalog.sample(rnd, now), now);
        t.setUserId(getUserId());
        t.setTupleType("CONTENT_REQ");
        t.setSrcModuleName(getSensorName());
        t.setDestModuleName("edge_cache_service");
        t.setDestinationDeviceId(getGatewayDeviceId());
        send(getGatewayDeviceId(), getLatency(), FogEvents.TUPLE_ARRIVAL, t);
    }
}
