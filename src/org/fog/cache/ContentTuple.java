package org.fog.cache;

import java.util.ArrayDeque;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.entities.Tuple;
import org.fog.utils.FogUtils;

public class ContentTuple extends Tuple {
    public final int contentId;
    public final double emitTime;
    public double extraDelay;
    public final ArrayDeque<Integer> route = new ArrayDeque<>();

    public ContentTuple(String appId, int direction, long fileSize, int contentId, double emitTime) {
        super(appId, FogUtils.generateTupleId(), direction, 10, 1, fileSize, 1,
                new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
        this.contentId = contentId;
        this.emitTime = emitTime;
    }
}
