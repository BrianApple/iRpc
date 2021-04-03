/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iRpc.vote;

import java.io.File;

/**
 * @Description:    描述
 * @author: https://github.com/openmessaging/openmessaging-storage-dledger
 * @date:   2020年6月29日  
 * @version V1.0
 */
public class DLedgerConfig {

    public static final String MEMORY = "MEMORY";
    public static final String FILE = "FILE";
    /**
     * 组名字
     */
    private String group = "default";

    private String selfId = "n0";//根据服务端口自动识别服务当前节点名称

    private StringBuffer peers = new StringBuffer();//当前rpc集群所有节点信息，用分号分割,thread-safe

    private String storeBaseDir = File.separator + "tmp" + File.separator + "dledgerstore";

    private int peerPushThrottlePoint = 300 * 1024 * 1024;

    private int peerPushQuota = 20 * 1024 * 1024;

    private String storeType = MEMORY;//memory 或者file 两种类型
    private String dataStorePath;

    private int maxPendingRequestsNum = 10000;

    private int maxWaitAckTimeMs = 2500;

    private int maxPushTimeOutMs = 1000;

    private boolean enableLeaderElector = true;

    private int heartBeatTimeIntervalMs = 2000;

    private int maxHeartBeatLeak = 3;

    private int minVoteIntervalMs = 300;
    private int maxVoteIntervalMs = 1000;

    private int fileReservedHours = 72;
    private String deleteWhen = "04";

    private float diskSpaceRatioToCheckExpired = Float.parseFloat(System.getProperty("dledger.disk.ratio.check", "0.70"));
    private float diskSpaceRatioToForceClean = Float.parseFloat(System.getProperty("dledger.disk.ratio.clean", "0.85"));

    private boolean enableDiskForceClean = true;

    private long flushFileInterval = 10;

    private long checkPointInterval = 3000;

    private boolean enablePushToFollower = true;

    public String getDefaultPath() {
        return storeBaseDir + File.separator + "dledger-" + selfId;
    }

    public String getDataStorePath() {
        if (dataStorePath == null) {
            return getDefaultPath() + File.separator + "data";
        }
        return dataStorePath;
    }

    public void setDataStorePath(String dataStorePath) {
        this.dataStorePath = dataStorePath;
    }

    public String getIndexStorePath() {
        return getDefaultPath() + File.separator + "index";
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getSelfId() {
        return selfId;
    }

    public void setSelfId(String selfId) {
        this.selfId = selfId;
    }

    public String getPeers() {
        return peers.toString();
    }

    /**
     * 节点动态扩容时，会存在并发
     * @param peers
     */
    public synchronized void setPeers(String peers) {
        this.peers = new StringBuffer(peers);
    }

    public String getStoreBaseDir() {
        return storeBaseDir;
    }

    public void setStoreBaseDir(String storeBaseDir) {
        this.storeBaseDir = storeBaseDir;
    }

    public String getStoreType() {
        return storeType;
    }

    public void setStoreType(String storeType) {
        this.storeType = storeType;
    }

    public boolean isEnableLeaderElector() {
        return enableLeaderElector;
    }

    public void setEnableLeaderElector(boolean enableLeaderElector) {
        this.enableLeaderElector = enableLeaderElector;
    }

    //for builder semantic
    public DLedgerConfig group(String group) {
        this.group = group;
        return this;
    }

    public DLedgerConfig selfId(String selfId) {
        this.selfId = selfId;
        return this;
    }

    public DLedgerConfig peers(String peers) {
        this.peers = new StringBuffer(peers);
        return this;
    }

    public DLedgerConfig storeBaseDir(String dir) {
        this.storeBaseDir = dir;
        return this;
    }

    public boolean isEnablePushToFollower() {
        return enablePushToFollower;
    }

    public void setEnablePushToFollower(boolean enablePushToFollower) {
        this.enablePushToFollower = enablePushToFollower;
    }

    public int getMaxPendingRequestsNum() {
        return maxPendingRequestsNum;
    }

    public void setMaxPendingRequestsNum(int maxPendingRequestsNum) {
        this.maxPendingRequestsNum = maxPendingRequestsNum;
    }

    public int getMaxWaitAckTimeMs() {
        return maxWaitAckTimeMs;
    }

    public void setMaxWaitAckTimeMs(int maxWaitAckTimeMs) {
        this.maxWaitAckTimeMs = maxWaitAckTimeMs;
    }

    public int getMaxPushTimeOutMs() {
        return maxPushTimeOutMs;
    }

    public void setMaxPushTimeOutMs(int maxPushTimeOutMs) {
        this.maxPushTimeOutMs = maxPushTimeOutMs;
    }

    public int getHeartBeatTimeIntervalMs() {
        return heartBeatTimeIntervalMs;
    }

    public void setHeartBeatTimeIntervalMs(int heartBeatTimeIntervalMs) {
        this.heartBeatTimeIntervalMs = heartBeatTimeIntervalMs;
    }

    public int getMinVoteIntervalMs() {
        return minVoteIntervalMs;
    }

    public void setMinVoteIntervalMs(int minVoteIntervalMs) {
        this.minVoteIntervalMs = minVoteIntervalMs;
    }

    public int getMaxVoteIntervalMs() {
        return maxVoteIntervalMs;
    }

    public void setMaxVoteIntervalMs(int maxVoteIntervalMs) {
        this.maxVoteIntervalMs = maxVoteIntervalMs;
    }

    public String getDeleteWhen() {
        return deleteWhen;
    }

    public void setDeleteWhen(String deleteWhen) {
        this.deleteWhen = deleteWhen;
    }

    public float getDiskSpaceRatioToCheckExpired() {
        return diskSpaceRatioToCheckExpired;
    }

    public void setDiskSpaceRatioToCheckExpired(float diskSpaceRatioToCheckExpired) {
        this.diskSpaceRatioToCheckExpired = diskSpaceRatioToCheckExpired;
    }

    public float getDiskSpaceRatioToForceClean() {
        if (diskSpaceRatioToForceClean < 0.50f) {
            return 0.50f;
        } else {
            return diskSpaceRatioToForceClean;
        }
    }

    public void setDiskSpaceRatioToForceClean(float diskSpaceRatioToForceClean) {
        this.diskSpaceRatioToForceClean = diskSpaceRatioToForceClean;
    }

    public float getDiskFullRatio() {
        float ratio = diskSpaceRatioToForceClean + 0.05f;
        if (ratio > 0.95f) {
            return 0.95f;
        }
        return ratio;
    }

    public int getFileReservedHours() {
        return fileReservedHours;
    }

    public void setFileReservedHours(int fileReservedHours) {
        this.fileReservedHours = fileReservedHours;
    }

    public long getFlushFileInterval() {
        return flushFileInterval;
    }

    public void setFlushFileInterval(long flushFileInterval) {
        this.flushFileInterval = flushFileInterval;
    }

    public boolean isEnableDiskForceClean() {
        return enableDiskForceClean;
    }

    public void setEnableDiskForceClean(boolean enableDiskForceClean) {
        this.enableDiskForceClean = enableDiskForceClean;
    }

    public int getMaxHeartBeatLeak() {
        return maxHeartBeatLeak;
    }

    public void setMaxHeartBeatLeak(int maxHeartBeatLeak) {
        this.maxHeartBeatLeak = maxHeartBeatLeak;
    }

    public int getPeerPushThrottlePoint() {
        return peerPushThrottlePoint;
    }

    public void setPeerPushThrottlePoint(int peerPushThrottlePoint) {
        this.peerPushThrottlePoint = peerPushThrottlePoint;
    }

    public int getPeerPushQuota() {
        return peerPushQuota;
    }

    public void setPeerPushQuota(int peerPushQuota) {
        this.peerPushQuota = peerPushQuota;
    }

    public long getCheckPointInterval() {
        return checkPointInterval;
    }

    public void setCheckPointInterval(long checkPointInterval) {
        this.checkPointInterval = checkPointInterval;
    }
}
