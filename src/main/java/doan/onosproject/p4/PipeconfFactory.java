/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package doan.onosproject.p4;

import org.onosproject.driver.pipeline.DefaultSingleTablePipeline;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.pi.model.DefaultPiPipeconf;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.service.PiPipeconfService;
import org.onosproject.p4runtime.model.P4InfoParser;
import org.onosproject.p4runtime.model.P4InfoParserException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.BMV2_JSON;
import static org.onosproject.net.pi.model.PiPipeconf.ExtensionType.P4_INFO_TEXT;


import org.onosproject.net.topology.TopologyService;
// import org.onosproject.net.topology.Topology;
// import org.onosproject.net.topology.TopologyEvent;
// import org.onosproject.net.topology.TopologyListener;
// import org.onosproject.net.DeviceId;
// import org.onosproject.net.Path;
// import java.util.Set;

/**
 * Component that produces and registers a pipeconf when loaded.
 */
@Component(immediate = true)
public final class PipeconfFactory {
    // private ApplicationId appId;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final PiPipeconfId PIPECONF_ID = new PiPipeconfId("p4");
    private static final URL P4INFO_URL = PipeconfFactory.class.getResource("/info.txt");

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PiPipeconfService piPipeconfService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private TopologyService topologyService;

    @Activate
    public void activate() {
        // appId = coreService.registerApplication("doan.onosproject.p4");
        // Registers the pipeconf at component activation.
        try {
            piPipeconfService.register(buildPipeconf());
        } catch (P4InfoParserException e) {
            log.error("Fail to register {} - Exception: {} - Cause: {}",
                    PIPECONF_ID, e.getMessage(), e.getCause().getMessage());
        }
    }

    @Deactivate
    public void deactivate() {
        // Unregisters the pipeconf at component deactivation.
        try {
            piPipeconfService.unregister(PIPECONF_ID);
        } catch (IllegalStateException e) {
            log.warn("{} haven't been registered", PIPECONF_ID);
        }
    }

    private PiPipeconf buildPipeconf() throws P4InfoParserException {

        final PiPipelineModel pipelineModel = P4InfoParser.parse(P4INFO_URL);

        return DefaultPiPipeconf.builder()
                .withId(PIPECONF_ID)
                .withPipelineModel(pipelineModel)
                .addBehaviour(PiPipelineInterpreter.class, PipelineInterpreterImpl.class)
                .addBehaviour(PortStatisticsDiscovery.class, PortStatisticsDiscoveryImpl.class)
                // Since mytunnel.p4 defines only 1 table, we re-use the existing single-table pipeliner.
                .addBehaviour(Pipeliner.class, DefaultSingleTablePipeline.class)
                .addExtension(P4_INFO_TEXT, P4INFO_URL)
                .addExtension(BMV2_JSON, BMV2_JSON_URL)
                .build();
    }

}
