package crawlercommons.urlfrontier.service.elasticsearch;

import crawlercommons.urlfrontier.service.QueueInterface;

public class Queue implements QueueInterface {

	private long blockedUntil = -1;

	private int delay = -1;

	private long lastProduced = 0;

	@Override
	public void setBlockedUntil(long until) {
		blockedUntil = until;
	}

	@Override
	public long getBlockedUntil() {
		return blockedUntil;
	}

	@Override
	public void setDelay(int delayRequestable) {
		delay = delayRequestable;
	}

	@Override
	public long getLastProduced() {
		return lastProduced;
	}

	@Override
	public int getDelay() {
		return delay;
	}

	@Override
	public void setLastProduced(long lastProduced) {
		this.lastProduced = lastProduced;
	}

}
