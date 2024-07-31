// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

/** Defines the behaviour of a queue * */
public interface QueueInterface {

    public void setBlockedUntil(long until);

    public long getBlockedUntil();

    public void setDelay(int delayRequestable);

    public int getDelay();

    public void setLastProduced(long lastProduced);

    public long getLastProduced();

    public int getInProcess(long now);

    public int getCountCompleted();

    public int countActive();

    public void setCrawlLimit(int crawlLimit);

    public Boolean IsLimitReached();
}
