package eu.enexa.service;

import java.io.File;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

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
    Map<String,String> resolveContainerEndpoint(String containerId, Integer port);

    /**
     * Combines two path components to create a valid path.
     * the directory will create if not exist
     *
     * @param partOneOfPath   The first part of the path.
     * @param partTwoOfPath   The second part of the path.
     * @return                The combined path.
     */
    default String makeTheDirectoryInThisPath(String partOneOfPath, String partTwoOfPath) {
        if(partOneOfPath ==null && partTwoOfPath == null) return "";
        assert partOneOfPath != null;
        String path = combinePaths(partOneOfPath, partTwoOfPath);
        File appPathDirectory = new File(path);
        if(!appPathDirectory.exists()){
            appPathDirectory.mkdirs();
        }
        return path;
    }

    /**
     * Combines two path components to create a valid path, taking into account trailing separators.
     *
     * @param partOne   The first part of the path.
     * @param partTwo   The second part of the path.
     * @return          The combined path.
     */
    default String combinePaths(String partOne, String partTwo) {
        String path = partOne + File.separator + partTwo;
        if (partOne.endsWith(File.separator)) {
            path = partOne + partTwo;
        }
        return path;
    }
}
