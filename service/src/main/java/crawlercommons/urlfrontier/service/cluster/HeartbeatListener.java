// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import java.util.List;

/** Representation of the Frontier instances so that the heartbeats can communicate with them * */
public interface HeartbeatListener {
    /** Used by heartbeat to identify a Frontier * */
    String getAddress();

    /** Report back to the assigner with the list of nodes in the cluster * */
    public void setNodes(List<String> n);
}
