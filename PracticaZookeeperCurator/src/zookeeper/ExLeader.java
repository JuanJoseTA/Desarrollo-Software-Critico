package zookeeper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

public class ExLeader {
	
	private static String NODO = "";
	static final String ZOOKEEPER_HOST = System.getenv().getOrDefault("ZOOKEEPER_HOST","127.0.0.1:2181");	
	
	private static int generarMedicion() {
		Random r = new Random();
		return r.nextInt(140)+60;
	}
	
	private static double getMedia(int[] med, int tam) {
		double res = 0;
		for(int i = 0; i<tam; i++) {
			res += med[i];
		}
		return res/tam;
	}
	
	private static void enviarMedia(double media) {
		try {
			// Url de la practica anterior
            URL url = new URL("http://localhost:4000/nuevo/" + media);
 
            // Conectamos con la url y hacemos peticion get
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                                                conn.getInputStream())))
            {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
		System.out.println(media);
	}


	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ElectionNode nd;
		Random r=new Random();
		try {
			// Creamos el nodo
			nd = new ElectionNode("node"+r.nextInt());
			nd.start();
			CuratorFramework client;
			String zkConnString = ZOOKEEPER_HOST;
			client = CuratorFrameworkFactory.newClient(zkConnString,
					new ExponentialBackoffRetry(1000, 3));
			client.start();
			String node = NODO;
			try {
				node = args[0];
			}catch(Exception e) {
				
			}
			while (true) {	
			// Escritura 
				String datos= String.valueOf(generarMedicion());
				client.create().orSetData().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/mediciones/" + node, datos.getBytes());
				// Comprobacion de si es líder
				if(nd.getImLeading()) {
					Thread.sleep(2000);
					// Miramos hijos para poder consultar los datos de cada nodo
					GetChildrenBuilder childrenBuilder = client.getChildren();
					List<String> children = childrenBuilder.forPath("/mediciones");
					int [] mediciones = new int[10000];
					int tam = 0;
					double media;
					String s;
					byte[] datos2;
					for(int i = 0; i < children.size(); i++) {
						s = children.get(i);
						datos2 = client.getData().forPath("/mediciones/"+s);
						String num = new String(datos2);
						mediciones[tam] = Integer.parseInt(num);
						tam++;
					}
					// Obtenemos la media
					media = getMedia(mediciones, tam);
					// Escribimos la media en /media
					client.create().orSetData().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/media", String.valueOf(media).getBytes());
					// Enviamos la media al localhost que reside en la practica anterior
					enviarMedia(media);
				}else {
					Thread.sleep(1500);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
