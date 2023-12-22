package rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
// import com.google.logging.type.HttpRequest;

import org.slf4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
// import java.util.HashMap;
// import java.util.Map;

import org.onosproject.rest.AbstractWebResource;

import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.flow.DefaultFlowRule;

import org.onosproject.net.DeviceId;
import static org.onosproject.net.DeviceId.deviceId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.core.ApplicationId;
// import static com.google.common.base.Preconditions.checkNotNull;
// import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
// import static doan.onosproject.p4.PipeconfFactory.appId;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onosproject.core.DefaultApplicationId;

@Component(immediate = true)
@Path("")
public class P4PipelineWebResource extends AbstractWebResource {

    public long ipToDecimal(String ipAddress) {
        String[] ipAddressInArray = ipAddress.split("\\.");
        long result = 0;
        for (int i = 0; i < ipAddressInArray.length; i++) {
            int power = 3 - i;
            int ip = Integer.parseInt(ipAddressInArray[i]);
            result += ip * Math.pow(256, power);
        }
        return result;
    }

    private static final Logger log = getLogger(P4PipelineWebResource.class);

    public static String senddata(String url) throws IOException, InterruptedException {

        var client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                "onos",
                                "rocks".toCharArray());
                    }

                })
                .build();

        var uri = URI.create(url);

        var request = HttpRequest.newBuilder(uri).GET()
                .setHeader("User-Agent", "Java 11 HttpClient Bot").build();
        var response = client.send(request, BodyHandlers.ofString());
        String result = response.body();

        String[] cutResult = result.split(",");
        String ketqua = cutResult[1].replace("\"links\":[{\"src\":{\"port\":", "");

        ketqua = ketqua.replace("\"", "");

        return ketqua;
    }

    @POST
    @Path("setroute")
    public Response postQueue(InputStream stream) {
        final FlowRuleService flowRuleService = get(FlowRuleService.class);
        final CoreService coreService = get(CoreService.class);
        ApplicationId appId;
        appId = coreService.getAppId("org.onosproject.fwd");
        // appId = new DefaultApplicationId(1, "1");
        String previous = " ";
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            log.warn(jsonTree.toString());
            String ip_src = jsonTree.path("ip_src").toString();
            String ip_dst = jsonTree.path("ip_dst").toString();
            long port_dst = jsonTree.path("port_dst").asLong();
            long port_src = jsonTree.path("port_src").asLong();
            String route = jsonTree.path("route").toString();
            route = route.replace("\"", "");
            String[] routeArray = route.split(",");
            for (int i = 0; i < routeArray.length; i++) {

                PiTableId traffic_inside = PiTableId.of("c_ingress.table_inside_vxlan");
                PiMatchFieldId divao = PiMatchFieldId.of("standard_metadata.ingress_port");
                PiMatchFieldId ipdest = PiMatchFieldId.of("hdr.inner_ipv4.dst_addr");
                PiMatchFieldId ipsrc = PiMatchFieldId.of("hdr.inner_ipv4.src_addr");
                PiMatchFieldId ipprotocol = PiMatchFieldId.of("hdr.inner_ipv4.protocol");
                PiMatchFieldId tcpdst = PiMatchFieldId.of("hdr.inner_tcp.dst_port");
                PiMatchFieldId tcpsrc = PiMatchFieldId.of("hdr.inner_tcp.src_port");

                if (i == 0) {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt("3"), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpdst, port_dst, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);

                    String url = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i] + "/" + routeArray[i + 1];
                    String urlNguoc = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i + 1] + "/" + routeArray[i];
                    String portNumber = "";
                    try {
                        try {
                            portNumber = senddata(url);
                            previous = senddata(urlNguoc);

                        } catch (InterruptedException oe) {
                            System.out.println("Exception!!! InterruptedException");

                        }
                    } catch (IOException ioe) {
                        System.out.println("Exception!!! IOException");

                    }
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt(portNumber));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                } else if (i == routeArray.length - 1) {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt(previous), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpdst, port_dst, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt("3"));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                } else {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt(previous), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpdst, port_dst, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);

                    String url = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i] + "/" + routeArray[i + 1];
                    String urlNguoc = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i + 1] + "/" + routeArray[i];
                    String portNumber = "";
                    try {
                        try {
                            portNumber = senddata(url);
                            previous = senddata(urlNguoc);

                        } catch (InterruptedException oe) {
                            System.out.println("Exception!!! InterruptedException");

                        }
                    } catch (IOException ioe) {
                        System.out.println("Exception!!! IOException");

                    }
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt(portNumber));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                }
            }
            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @POST
    @Path("setroute1")
    public Response postQueueresponse(InputStream stream) {
        final FlowRuleService flowRuleService = get(FlowRuleService.class);
        final CoreService coreService = get(CoreService.class);
        ApplicationId appId;
        appId = coreService.getAppId("org.onosproject.fwd");
        // appId = new DefaultApplicationId(1, "1");
        String previous = " ";
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            log.warn(jsonTree.toString());
            String ip_src = jsonTree.path("ip_src").toString();
            String ip_dst = jsonTree.path("ip_dst").toString();
            long port_dst = jsonTree.path("port_dst").asLong();
            long port_src = jsonTree.path("port_src").asLong();
            String route = jsonTree.path("route").toString();
            route = route.replace("\"", "");
            String[] routeArray = route.split(",");
            for (int i = 0; i < routeArray.length; i++) {

                PiTableId traffic_inside = PiTableId.of("c_ingress.table_inside_vxlan");
                PiMatchFieldId divao = PiMatchFieldId.of("standard_metadata.ingress_port");
                PiMatchFieldId ipdest = PiMatchFieldId.of("hdr.inner_ipv4.dst_addr");
                PiMatchFieldId ipsrc = PiMatchFieldId.of("hdr.inner_ipv4.src_addr");
                PiMatchFieldId ipprotocol = PiMatchFieldId.of("hdr.inner_ipv4.protocol");
                PiMatchFieldId tcpdst = PiMatchFieldId.of("hdr.inner_tcp.dst_port");
                PiMatchFieldId tcpsrc = PiMatchFieldId.of("hdr.inner_tcp.src_port");

                if (i == 0) {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt("3"), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpsrc, port_src, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);

                    String url = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i] + "/" + routeArray[i + 1];
                    String urlNguoc = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i + 1] + "/" + routeArray[i];
                    String portNumber = "";
                    try {
                        try {
                            portNumber = senddata(url);
                            previous = senddata(urlNguoc);

                        } catch (InterruptedException oe) {
                            System.out.println("Exception!!! InterruptedException");

                        }
                    } catch (IOException ioe) {
                        System.out.println("Exception!!! IOException");

                    }
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt(portNumber));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                } else if (i == routeArray.length - 1) {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt(previous), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpsrc, port_src, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt("3"));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                } else {
                    PiCriterion match = PiCriterion.builder()
                            .matchTernary(divao, Integer.parseInt(previous), 511)
                            .matchTernary(ipdest, ipToDecimal(ip_dst.replace("\"", "")), 4294967295L)
                            .matchTernary(ipsrc, ipToDecimal(ip_src.replace("\"", "")), 4294967295L)
                            .matchTernary(ipprotocol, 6, 255)
                            .matchTernary(tcpsrc, port_src, 65535)
                            .build();

                    DeviceId switchStart = deviceId(routeArray[i]);

                    String url = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i] + "/" + routeArray[i + 1];
                    String urlNguoc = "http://127.0.0.1:8181/onos/v1/paths/" + routeArray[i + 1] + "/" + routeArray[i];
                    String portNumber = "";
                    try {
                        try {
                            portNumber = senddata(url);
                            previous = senddata(urlNguoc);

                        } catch (InterruptedException oe) {
                            System.out.println("Exception!!! InterruptedException");

                        }
                    } catch (IOException ioe) {
                        System.out.println("Exception!!! IOException");

                    }
                    PiActionParam daura = new PiActionParam(PiActionParamId.of("port"), Integer.parseInt(portNumber));
                    PiActionId actiondaura = PiActionId.of("c_ingress.set_out_port");
                    PiAction action = PiAction.builder()
                            .withId(actiondaura)
                            .withParameter(daura)
                            .build();
                    FlowRule rule = DefaultFlowRule.builder()
                            .forDevice(switchStart)
                            .forTable(traffic_inside)
                            .fromApp(appId)
                            .withPriority(41000)
                            .makePermanent()
                            .withSelector(DefaultTrafficSelector.builder()
                                    .matchPi(match).build())
                            .withTreatment(DefaultTrafficTreatment.builder()
                                    .piTableAction(action).build())
                            .build();
                    flowRuleService.applyFlowRules(rule);
                }
            }
            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}