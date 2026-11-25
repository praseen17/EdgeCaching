package org.fog.cache;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementMapping;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.distribution.DeterministicDistribution;

public class EdgeCacheMain {
    public static int USERS_PER_EDGE = 10;
    public static double REQUEST_INTERVAL_MS = 20;
    public static int SIM_TIME_MS = 120000;
    public static int CATALOG = 1000;
    public static int CACHE_SIZE = 50;
    public static double ZIPF_ALPHA = 0.9;
    public static double SHIFT_EVERY_MS = 15000;
    public static long CONTENT_SIZE = 500;
    public static double ORIGIN_MS = 20;
    public static long WINDOW = 2000;

    static class CacheController extends Controller {
        CacheController(String n, List<FogDevice> f, List<Sensor> s, List<Actuator> a) { super(n, f, s, a); }
        @Override
        public void processEvent(SimEvent ev) {
            if (ev.getTag() == FogEvents.STOP_SIMULATION) { CloudSim.stopSimulation(); return; }
            super.processEvent(ev);
        }
    }

    public static List<CacheMetrics> run() throws Exception {
        Log.disable();
        Config.MAX_SIMULATION_TIME = SIM_TIME_MS;
        Config.ENABLE_DYNAMIC_CLUSTERING = false;
        CloudSim.init(1, Calendar.getInstance(), false);

        String appId = "edge_cache";
        FogBroker broker = new FogBroker("broker");
        Application app = Application.createApplication(appId, broker.getId());
        app.setUserId(broker.getId());
        app.addAppModule("edge_cache_service", 10);
        app.addAppModule("origin_service", 10);
        app.addAppEdge("CONTENT_REQ", "edge_cache_service", 100, 50, "CONTENT_REQ", Tuple.UP, AppEdge.SENSOR);
        app.addAppEdge("edge_cache_service", "origin_service", 100, 50, "FETCH", Tuple.UP, AppEdge.MODULE);
        app.addTupleMapping("edge_cache_service", "CONTENT_REQ", "FETCH", new FractionalSelectivity(1.0));

        List<FogDevice> devices = new ArrayList<>();
        List<Sensor> sensors = new ArrayList<>();
        List<Actuator> actuators = new ArrayList<>();
        List<CacheMetrics> all = new ArrayList<>();
        ModuleMapping mapping = ModuleMapping.createModuleMapping();

        CacheFogDevice cloud = new CacheFogDevice("cloud", 44800, 40000, 1000, 1000, 0,
                CacheFogDevice.Role.ORIGIN, null, null, 0, CONTENT_SIZE, ORIGIN_MS);
        cloud.setParentId(-1);
        CacheFogDevice proxy = new CacheFogDevice("regional-proxy", 2800, 4000, 1000, 1000, 1,
                CacheFogDevice.Role.RELAY, null, null, 0, CONTENT_SIZE, 0);
        proxy.setParentId(cloud.getId());
        proxy.setUplinkLatency(60);
        devices.add(cloud);
        devices.add(proxy);
        mapping.addModuleToDevice("origin_service", "cloud");

        ZipfCatalog catalog = new ZipfCatalog(CATALOG, ZIPF_ALPHA, SHIFT_EVERY_MS);
        String[] names = {"LRU", "LFU", "ML-Predict"};
        for (String n : names) {
            CachePolicy p = n.equals("LRU") ? new LruPolicy(CACHE_SIZE)
                    : n.equals("LFU") ? new LfuPolicy(CACHE_SIZE) : new MlPolicy(CACHE_SIZE, CATALOG);
            CacheMetrics m = new CacheMetrics(n, WINDOW);
            all.add(m);
            CacheFogDevice edge = new CacheFogDevice("edge-" + n, 2800, 4000, 1000, 1000, 2,
                    CacheFogDevice.Role.EDGE, p, m, 2, CONTENT_SIZE, 0);
            edge.setParentId(proxy.getId());
            edge.setUplinkLatency(10);
            devices.add(edge);
            mapping.addModuleToDevice("edge_cache_service", edge.getName());
            for (int u = 0; u < USERS_PER_EDGE; u++) {
                ContentSensor s = new ContentSensor("user-" + n + "-" + u, broker.getId(), appId,
                        new DeterministicDistribution(REQUEST_INTERVAL_MS), catalog, 5000 + u);
                s.setGatewayDeviceId(edge.getId());
                s.setLatency(2.0);
                sensors.add(s);
            }
        }

        Controller controller = new CacheController("controller", devices, sensors, actuators);
        controller.submitApplication(app, 0, new ModulePlacementMapping(devices, app, mapping));
        CloudSim.terminateSimulation(SIM_TIME_MS);
        CloudSim.startSimulation();
        CloudSim.stopSimulation();
        return all;
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) SIM_TIME_MS = Integer.parseInt(args[0]);
        System.out.println("iFogSim2 Edge Caching | cloud -> regional-proxy -> 3 edge nodes x " + USERS_PER_EDGE
                + " users | cache=" + CACHE_SIZE + " catalog=" + CATALOG + " sim=" + SIM_TIME_MS / 1000 + "s");
        List<CacheMetrics> res = run();
        System.out.println("---------------------------------------------------------------------------");
        System.out.printf("%-12s %10s %10s %14s %16s%n", "Edge policy", "Requests", "Hit %", "Avg latency", "Backhaul (MB)");
        try (PrintWriter pw = new PrintWriter("results.csv")) {
            pw.println("policy,requests,hit_ratio,avg_latency_ms,backhaul_mb");
            for (CacheMetrics m : res) {
                double mb = m.backhaulBytes / 1024.0;
                System.out.printf("%-12s %10d %9.2f%% %11.2f ms %16.1f%n", m.node, m.requests, m.hitRatio() * 100, m.avgLatency(), mb);
                pw.printf("%s,%d,%.4f,%.2f,%.1f%n", m.node, m.requests, m.hitRatio(), m.avgLatency(), mb);
            }
        }
        System.out.println("Saved results.csv");
        if (!java.awt.GraphicsEnvironment.isHeadless()) ResultChart.show(res);
        System.exit(0);
    }
}
