/*
 * Copyright 2019-present Open Networking Foundation
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

package org.p4.p4d2.tutorial;

import org.onlab.packet.MacAddress;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.p4.p4d2.tutorial.common.Srv6DeviceConfig;
import org.p4.p4d2.tutorial.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.p4.p4d2.tutorial.AppConstants.APP_PREFIX;
import static org.p4.p4d2.tutorial.AppConstants.CPU_CLONE_SESSION_ID;
import static org.p4.p4d2.tutorial.AppConstants.INITIAL_SETUP_DELAY;

/**
 * App component that configures devices to provide L2 bridging capabilities.
 */
@Component(immediate = true)
public class L2BridgingComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_NAME = APP_PREFIX + ".l2bridging";
    private static final int DEFAULT_BROADCAST_GROUP_ID = 255;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();

    private ApplicationId appId;

    //--------------------------------------------------------------------------
    // ONOS CORE SERVICE BINDING
    //
    // These variables are set by the Karaf runtime environment before calling
    // the activate() method.
    //--------------------------------------------------------------------------

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    //--------------------------------------------------------------------------
    // COMPONENT ACTIVATION.
    //
    // When loading/unloading the app the Karaf runtime environment will call
    // activate()/deactivate().
    //--------------------------------------------------------------------------

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);
        Utils.waitPreviousCleanup(appId, deviceService, flowRuleService, groupService);
        // Register listeners to be informed about device and host events.
        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);
        // Schedule set up of existing devices. Needed when reloading the app.
        SharedScheduledExecutors.newTimeout(
                this::setUpAllDevices, INITIAL_SETUP_DELAY, TimeUnit.SECONDS);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        // Remove flows and groups installed by this app.
        cleanUpAllDevices();
        log.info("Stopped");
    }

    /**
     * Sets up everything necessary to support L2 bridging on the given device.
     *
     * @param deviceId the device to set up
     */
    private void setUpDevice(DeviceId deviceId) {
        // We need a clone group on all switches to clone LLDP packets for link
        // discovery as well as ARP/NDP ones for host discovery.
        insertCpuCloneGroup(deviceId);

        if (isSpine(deviceId)) {
            // Stop here. We support bridging only on leaf/tor switches.
            return;
        }
        insertMulticastGroup(deviceId);
        insertMulticastFlowRules(deviceId);
    }

    //--------------------------------------------------------------------------
    // METHODS TO COMPLETE.
    //
    // Complete the implementation wherever you see TODO.
    //--------------------------------------------------------------------------

    /**
     * Inserts a CLONE group in the ONOS core to clone packets to the CPU (i.e.
     * to ONOS via packet-in). CLONE groups in ONOS are equivalent to P4Runtime
     * Packet Replication Engine (PRE) clone sessions.
     *
     * @param deviceId device where to install the clone session
     */
    private void insertCpuCloneGroup(DeviceId deviceId) {
        log.info("Inserting CPU clone session on {}", deviceId);

        // Ports where to clone the packet. Just controller in this case.
        Set<PortNumber> clonePorts = Collections.singleton(PortNumber.CONTROLLER);

        // TODO: Create the clone group to send packets to the controller
        // HINT: at the top of the file, we define a group ID to be used
        //       for this purpose: *CPU_CLONE_SESSION_ID*
        // HINT: The Utils class contains a helper method for building the
        //       group, called *buildMulticastGroup*
        // ---- START SOLUTION ----
        final GroupDescription cloneGroup = Utils.buildCloneGroup(
                appId, deviceId, CPU_CLONE_SESSION_ID, clonePorts);
        // ---- END SOLUTION ----

        // Install the group to the device
        groupService.addGroup(cloneGroup);
    }

    /**
     * Inserts an ALL group in the ONOS core to replicate packets on all host
     * facing ports. This group will be used to broadcast all ARP/NDP requests.
     * <p>
     * ALL groups in ONOS are equivalent to P4Runtime Packet Replication Engine
     * (PRE) Multicast groups.
     *
     * @param deviceId the device where to install the group
     */
    private void insertMulticastGroup(DeviceId deviceId) {
        // Replicate packets where we know hosts are attached.
        Set<PortNumber> ports = getHostFacingPorts(deviceId);
        if (ports.isEmpty()) {
            // Stop here.
            log.warn("Device {} has 0 host facing ports", deviceId);
            return;
        }

        log.info("Creating multicast group with {} ports on {}",
                 ports.size(), deviceId);

        // TODO: Create the multicast group to send packets to the host-facing ports
        // HINT: at the top of the file, we define a group ID to be used
        //       for this purpose: *DEFAULT_BROADCAST_GROUP_ID*
        // HINT: The Utils class contains a helper method for building the
        //       group, called *buildMulticastGroup*
        // ---- START SOLUTION ----
        final GroupDescription multicastGroup = Utils.buildMulticastGroup(
                appId, deviceId, DEFAULT_BROADCAST_GROUP_ID, ports);
        // ---- END SOLUTION ----

        // TODO: Install the group to the device using the *groupService*
        // ---- START SOLUTION ----
        groupService.addGroup(multicastGroup);
        // ---- END SOLUTION ----
    }

    /**
     * Insert flow rules matching matching ethernet destination
     * broadcast/multicast addresses (e.g. NDP Neighbor Solicitation).
     * Such packets should be processed by the multicast group created above.
     *
     * @param deviceId device ID where to install the rules
     */
    private void insertMulticastFlowRules(DeviceId deviceId) {
        log.info("Inserting L2 multicast flow rules on {}...", deviceId);

        // TODO: Fill in table name.
        // HINT: this should match the table name from your P4 program
        // ---- START SOLUTION ----
        String tableId = "FabricIngress.l2_ternary_table";
        // ---- END SOLUTION ----

        // TODO: Create a criterion that matches NDP neighbor solicitation (NS) packets
        // HINT: the destination MAC address is a ternary value: 33:33:**:**:**:**
        final PiCriterion ipv6MulticastCriterion = PiCriterion.builder()
                .matchTernary(
                        // ---- START SOLUTION ----
                        PiMatchFieldId.of("hdr.ethernet.dst_addr"), // omit the match key name
                        MacAddress.valueOf("33:33:00:00:00:00").toBytes(),
                        MacAddress.valueOf("FF:FF:00:00:00:00").toBytes()
                        // ---- END SOLUTION ----
                )
                .build();

        // TODO: Create an action that sets the multicast group id
        // HINT: use the same multicast group that you inserted above
        final PiAction setMcastGroupAction = PiAction.builder()
                // ---- START SOLUTION ----
                //.withId(PiActionId.of(""))
                .withId(PiActionId.of("FabricIngress.set_multicast_group"))
                // ---- END SOLUTION ----
                .withParameter(new PiActionParam(
                        // ---- START SOLUTION ----
                        //PiActionParamId.of(""), 0L))
                        PiActionParamId.of("gid"), DEFAULT_BROADCAST_GROUP_ID))
                        // ---- END SOLUTION ----
                .build();


        //  Build the flow rule for the given table.
        final FlowRule rule = Utils.buildFlowRule(
                deviceId, appId, tableId,
                ipv6MulticastCriterion, setMcastGroupAction);

        // Install flow rule onto the device.
        flowRuleService.applyFlowRules(rule);
    }

    /**
     * Insert flow rules to forward packets to a given host located at the given
     * device and port.
     *
     * @param host     host object
     * @param deviceId device where the host is located
     * @param port     port where the host is attached to
     */
    private void learnHost(Host host, DeviceId deviceId, PortNumber port) {
        log.info("Adding L2 bridging rule on {} for host {} (port {})...",
                 deviceId, host.id(), port);

        // Match exactly on the host MAC address.
        final MacAddress hostMac = host.mac();

        // TODO create a criterion that matches on the destination MAC of the host
        // HINT: use the *toBytes* method to convert a MacAddress to a byte array
        final PiCriterion hostMacCriterion = PiCriterion.builder()
                // ---- START SOLUTION ----
                //.matchExact(PiMatchFieldId.of(""), new byte[]{})
                .matchExact(PiMatchFieldId.of("hdr.ethernet.dst_addr"),
                            hostMac.toBytes())
                // ---- END SOLUTION ----
                .build();

        // TODO create an action to sets to the correct output port
        final PiAction l2UnicastAction = PiAction.builder()
                // ---- START SOLUTION ----
                //.withId(PiActionId.of(""))
                .withId(PiActionId.of("FabricIngress.set_output_port"))
                .withParameter(new PiActionParam(
                        //PiActionParamId.of(""), 0L
                        PiActionParamId.of("port_num"), port.toLong()
                        ))
                // ---- END SOLUTION ----
                .build();

        // Forge flow rule.
        final FlowRule rule = Utils.buildFlowRule(
                deviceId, appId, "FabricIngress.l2_exact_table",
                hostMacCriterion, l2UnicastAction);

        // Insert.
        flowRuleService.applyFlowRules(rule);
    }

    //--------------------------------------------------------------------------
    // EVENT LISTENERS
    //
    // Events are processed only if isRelevant() returns true.
    //--------------------------------------------------------------------------

    /**
     * Listener of device events.
     */
    public class InternalDeviceListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            switch (event.type()) {
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    break;
                default:
                    // Ignore other events.
                    return false;
            }
            // Process only if this controller instance is the master.
            final DeviceId deviceId = event.subject().id();
            return mastershipService.isLocalMaster(deviceId);
        }

        @Override
        public void event(DeviceEvent event) {
            final DeviceId deviceId = event.subject().id();
            log.info("{} event! deviceId={}", event.type(), deviceId);
            if (deviceService.isAvailable(deviceId)) {
                // A P4Runtime device is considered available in ONOS when there
                // is a StreamChannel session open and the pipeline
                // configuration has been set.
                setUpDevice(deviceId);
            }
        }
    }

    /**
     * Listener of host events.
     */
    public class InternalHostListener implements HostListener {

        @Override
        public boolean isRelevant(HostEvent event) {
            switch (event.type()) {
                case HOST_ADDED:
                    // Host added events will be generated by the
                    // HostLocationProvider by intercepting ARP/NDP packets.
                    break;
                case HOST_REMOVED:
                case HOST_UPDATED:
                case HOST_MOVED:
                default:
                    // Ignore other events.
                    // Food for thoughts: how to support host moved/removed?
                    return false;
            }
            // Process host event only if this controller instance is the master
            // for the device where this host is attached to.
            final Host host = event.subject();
            final DeviceId deviceId = host.location().deviceId();
            return mastershipService.isLocalMaster(deviceId);
        }

        @Override
        public void event(HostEvent event) {
            final Host host = event.subject();
            // Device and port where the host is located.
            final DeviceId deviceId = host.location().deviceId();
            final PortNumber port = host.location().port();

            log.info("{} event! host={}, deviceId={}, port={}",
                     event.type(), host.id(), deviceId, port);

            learnHost(host, deviceId, port);
        }
    }

    //--------------------------------------------------------------------------
    // UTILITY METHODS
    //--------------------------------------------------------------------------

    /**
     * Returns a set of ports for the given device that are used to connect
     * hosts to the fabric.
     *
     * @param deviceId device ID
     * @return set of host facing ports
     */
    private Set<PortNumber> getHostFacingPorts(DeviceId deviceId) {
        // Get all interfaces configured via netcfg for the given device ID and
        // return the corresponding device port number.
        return interfaceService.getInterfaces().stream()
                .map(Interface::connectPoint)
                .filter(cp -> cp.deviceId().equals(deviceId))
                .map(ConnectPoint::port)
                .collect(Collectors.toSet());
    }

    /**
     * Returns true if the given device is defined as a spine in the netcfg.
     *
     * @param deviceId device ID
     * @return true if spine, false otherwise
     */
    private boolean isSpine(DeviceId deviceId) {
        final Srv6DeviceConfig cfg = configService.getConfig(deviceId, Srv6DeviceConfig.class);
        return cfg != null && cfg.isSpine();
    }

    /**
     * Sets up L2 bridging on all devices known by ONOS and for which this ONOS
     * node instance is currently master.
     */
    private void setUpAllDevices() {
        deviceService.getAvailableDevices().forEach(device -> {
            if (mastershipService.isLocalMaster(device.id())) {
                setUpDevice(device.id());
                // For all hosts connected to this device...
                hostService.getConnectedHosts(device.id()).forEach(
                        host -> learnHost(host, host.location().deviceId(),
                                          host.location().port()));
            }
        });
    }

    /**
     * Cleans up the L2 bridging runtime configuration from the given device.
     *
     * @param deviceId the device to clean up
     */
    private void cleanUpDevice(DeviceId deviceId) {
        log.info("Cleaning up L2 bridging on {}...", deviceId);
        // Remove all runtime entities installed by this app.
        flowRuleService.removeFlowRulesById(appId);
        groupService.getGroups(deviceId, appId).forEach(
                group -> groupService.removeGroup(deviceId, group.appCookie(), appId));
    }

    /**
     * Cleans up L2 bridging runtime configuration from all devices known by
     * ONOS and for which this ONOS node instance is currently master.
     */
    private void cleanUpAllDevices() {
        deviceService.getDevices().forEach(device -> {
            if (mastershipService.isLocalMaster(device.id())) {
                cleanUpDevice(device.id());
            }
        });
    }
}
