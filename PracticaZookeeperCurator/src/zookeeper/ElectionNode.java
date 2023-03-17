package zookeeper;
import java.io.Closeable;
import java.io.IOException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ElectionNode  extends LeaderSelectorListenerAdapter implements Closeable {
	String name;
	CuratorFramework client;
	LeaderSelector leaderSelector;
	private static final String PATH = "/exelection";

	
	public ElectionNode(String name) {
		this.name=name;
		
		String zkConnString = "127.0.0.1:2181";
		client = CuratorFrameworkFactory.newClient(zkConnString,
				new ExponentialBackoffRetry(1000, 3));
		client.start();

		leaderSelector = new LeaderSelector(client, PATH, this);
		leaderSelector.autoRequeue();		
	}

	public void start() throws IOException {
		leaderSelector.start();
	}

	public void close() throws IOException {
		leaderSelector.close();
	}

	public void takeLeadership(CuratorFramework client) throws Exception {
		System.out.println("I'm leading: "+name);
		
        while (true) {
        	//ver mediciones de los demás nodos, hacer media y enviar
        	try {
        		Thread.sleep(100);
        	} catch (InterruptedException e) {}
        }
	}
	

	public boolean getImLeading() throws Exception {
		return leaderSelector.hasLeadership();
	}
	
}
