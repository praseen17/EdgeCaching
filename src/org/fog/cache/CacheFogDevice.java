package org.fog.cache;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogUtils;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;
import org.fog.utils.FogEvents;
import org.fog.utils.FogLinearPowerModel;

public class CacheFogDevice extends FogDevice {
    public enum Role { EDGE, RELAY, ORIGIN }

    private final Role role;
    private final CachePolicy policy;
    private final CacheMetrics metrics;
    private final double userLatency;
    private final long contentSize;
    private final double originMs;
    private long counter;

    private static FogDeviceCharacteristics characteristics(long mips, int ram) {
        List<Pe> pes = new ArrayList<>();
        pes.add(new Pe(0, new PeProvisionerOverbooking(mips)));
        PowerHost host = new PowerHost(FogUtils.generateEntityId(), new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(10000), 1000000, pes, new StreamOperatorScheduler(pes),
                new FogLinearPowerModel(107.339, 83.4333));
        return new FogDeviceCharacteristics("x86", "Linux", "Xen", host, 10.0, 3.0, 0.05, 0.001, 0.0);
    }

    public CacheFogDevice(String name, long mips, int ram, double up, double down, int level, Role role,
                          CachePolicy policy, CacheMetrics metrics, double userLatency, long contentSize,
                          double originMs) throws Exception {
        this(name, characteristics(mips, ram), up, down, level, role, policy, metrics, userLatency, contentSize, originMs);
    }

    private CacheFogDevice(String name, FogDeviceCharacteristics ch, double up, double down, int level, Role role,
                           CachePolicy policy, CacheMetrics metrics, double userLatency, long contentSize,
                           double originMs) throws Exception {
        super(name, ch, new AppModuleAllocationPolicy(ch.getHostList()), new LinkedList<Storage>(), 10, up, down, 0, 0.01);
        setLevel(level);
        this.role = role;
        this.policy = policy;
        this.metrics = metrics;
        this.userLatency = userLatency;
        this.contentSize = contentSize;
        this.originMs = originMs;
    }

    public CachePolicy getPolicy() { return policy; }
    public CacheMetrics getMetrics() { return metrics; }
    public Role getRole() { return role; }

    @Override
    protected void processTupleArrival(SimEvent ev) {
        if (!(ev.getData() instanceof ContentTuple)) {
            super.processTupleArrival(ev);
            return;
        }
        ContentTuple ct = (ContentTuple) ev.getData();
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
        double now = CloudSim.clock();

        if (ct.getDirection() == Tuple.UP) {
            if (role == Role.EDGE) {
                counter++;
                if (policy.lookup(ct.contentId, counter)) {
                    metrics.record(true, now - ct.emitTime + userLatency, 0);
                } else {
                    sendUp(ct);
                }
            } else if (role == Role.RELAY) {
                ct.route.push(ev.getSource());
                sendUp(ct);
            } else {
                ContentTuple resp = new ContentTuple(ct.getAppId(), Tuple.DOWN, contentSize, ct.contentId, ct.emitTime);
                resp.extraDelay = ct.extraDelay + originMs;
                resp.route.addAll(ct.route);
                resp.setUserId(ct.getUserId());
                resp.setTupleType("CONTENT_RESP");
                sendDown(resp, ev.getSource());
            }
        } else {
            if (role == Role.EDGE) {
                double lat = now - ct.emitTime + ct.extraDelay + userLatency;
                metrics.record(false, lat, ct.getCloudletFileSize());
                policy.admit(ct.contentId, counter);
            } else {
                sendDown(ct, ct.route.pop());
            }
        }
    }
}
