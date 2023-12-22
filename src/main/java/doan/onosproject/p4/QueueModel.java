package doan.onosproject.p4;


import org.onosproject.net.DeviceId;
import static org.slf4j.LoggerFactory.getLogger;
import org.slf4j.Logger;
import org.onosproject.net.Path;
import java.util.Set;
// import javax.ws.rs.GET;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyService;
// import org.onosproject.net.topology.TopologyEvent;
// import org.onosproject.net.topology.TopologyListener;
// import org.osgi.service.component.annotations.Activate;
// import org.osgi.service.component.annotations.Component;
// import org.osgi.service.component.annotations.Deactivate;
// import org.osgi.service.component.annotations.Reference;
// import org.osgi.service.component.annotations.ReferenceCardinality;
// import org.onlab.osgi.ServiceDirectory;



// @Component(immediate = true)
public class QueueModel {


    private static final Logger log = getLogger(QueueModel.class);

    // @Reference(cardinality = ReferenceCardinality.MANDATORY)
    public TopologyService topologyService;
    // private ServiceDirectory directory;

    private DeviceId switchStart;
    private DeviceId switchEnd;

    public QueueModel(DeviceId switchStartMethod, DeviceId switchEndMethod)
    {
        // this.topology = topologyMethod;
        this.switchStart = switchStartMethod;
        this.switchEnd = switchEndMethod;
    }
    
    public String findPath() {
        Topology topology = topologyService.currentTopology();
        // Topology topology = topologyService.currentTopology();
        Set<Path> paths = topologyService.getPaths(topology,
                this.switchStart,
                this.switchEnd);
        if (paths.isEmpty()) {
            log.warn("Khong biet duong di {} -> {}", switchStart, switchEnd);
            
        }
        Path path = pickForwardPathIfPossible(paths);
        if (path == null) {
            log.warn("Khong biet duong di {} -> {}", switchStart, switchEnd);

            // cong dau ra: path.src().port()
        }
        return path.src().port().toString();
    }

    public Path pickForwardPathIfPossible(Set<Path> paths) {
        for (Path path : paths) {
                return path;
        }
        return null;
    }
}
