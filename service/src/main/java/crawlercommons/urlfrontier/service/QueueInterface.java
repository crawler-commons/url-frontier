package crawlercommons.urlfrontier.service;

/** Defines the behaviour of a queue **/

public interface QueueInterface {

	public void setBlockedUntil(long until);

	public long getBlockedUntil();

	public void setDelay(int delayRequestable);

	public long getLastProduced();

	public int getDelay();

	public void setLastProduced(long lastProduced);
}
