package eu.enexa.service;

import java.util.AbstractMap;
import java.util.List;

/**
 * The interface of a manager for containers that can be started and stopped by
 * the ENEXA service.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public interface ContainerManager {

    // containerName is the podName for kubernetes
    String startContainer(String image, String containerName, List<AbstractMap.SimpleEntry<String, String>> variables, String hostSharedDirectory, String appName);

    /**
     * Stop the container with the given ID.
     *
     * @param containerIdOrName
     * @return TODO what should be returned, here?
     */
    String stopContainer(String containerIdOrName);

    /**
     * Get the status of the container with the given ID as String.
     *
     * @param containerId the ID of the container for which the status should be
     *                    returned.
     * @return The status as String or {@code null} if an error occurs or the
     *         container does not exist
     */
    String getContainerStatus(String containerId);

    /**
     *
     * @param containerId
     * @param port
     * @return url as callable endpoint from outside of the container
     */
    String resolveContainerEndpoint(String containerId, Integer port);
}
