package org.apache.nifi.diagnostics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.nifi.controller.FlowController;
import org.apache.nifi.controller.service.ControllerServiceProvider;
import org.apache.nifi.groups.ProcessGroup;
import org.apache.nifi.nar.ExtensionManager;
import org.apache.nifi.registry.flow.mapping.InstantiatedVersionedProcessGroup;
import org.apache.nifi.registry.flow.mapping.NiFiRegistryFlowMapper;

import java.io.File;
import java.io.IOException;

public class BundleDataFlowWriter implements DataFlowWriter {

    private ExtensionManager extensionManager;
    private FlowController flowController;

    @Override
    public String createVersionedProcessGroup() {
        ObjectMapper mapper = new ObjectMapper();
        NiFiRegistryFlowMapper flowMapper = new NiFiRegistryFlowMapper(extensionManager);
        final ProcessGroup rootProcessGroup = flowController.getFlowManager().getRootGroup();
        final ControllerServiceProvider controllerServiceProvider = flowController.getControllerServiceProvider();
        InstantiatedVersionedProcessGroup nonVersionedProcessGroup = flowMapper.mapNonVersionedProcessGroup(
                rootProcessGroup,
                controllerServiceProvider
        );
        try {
            mapper.writeValue(new File("VPG.json"), nonVersionedProcessGroup);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String json = null;
        try {
            json = mapper.writeValueAsString(nonVersionedProcessGroup);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return json;
    }

    public void setFlowController(final FlowController flowController) {
        this.flowController = flowController;
    }

    public void setExtensionManager(final ExtensionManager extensionManager) {
        this.extensionManager = extensionManager;
    }


}
