package contenedores;

import static spark.Spark.*;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.json.*;

import redis.clients.jedis.Jedis;

public class Ej1 {
	
	static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST","localhost");	
    public static int TAM_VENTANA_MAX = 5;
    public static int tam = 0;
    public static ArrayList<Medicion> mediciones = new ArrayList<>();

	public static String mostrarLista() {
		try (Jedis jedis = new Jedis(REDIS_HOST)) {
			StringBuffer res = new StringBuffer();
			res.append("Mediciones:<br>");
			long ult = jedis.llen("queue#mediciones");
			int i;
			for(i = 0; i<ult-1; i++) {
				res.append("Valor: " + jedis.lindex("queue#mediciones",i));
				res.append(", timestamp: " + jedis.lindex("queue#timestamp",i) + "<br>");
			}
			res.append("Valor: " + jedis.lindex("queue#mediciones",i++));
			res.append(", timestamp: " + jedis.lindex("queue#timestamp",jedis.llen("queue#timestamp")-1));
			
			jedis.close();
			return res.toString();
		}
	}
	
	public static double calcProbVentana(int[] ventana, Map<Integer, Map<Integer, Integer>> matriz){
        double res = 1;
        Map<Integer, Integer> valor;
        int total = 0;

        for(int i = 0; i<TAM_VENTANA_MAX-1; i++){
            // Vemos si tenemos ese estado en la matriz entrenada
            if((valor = matriz.get(ventana[i])) != null){
                // Vemos si existe la transicion de el estado ant al de esta linea
                if ((valor.get(ventana[i+1])) != null){
                    for (int e : valor.values()) total += e;
                    res *= (double) valor.get(ventana[i+1])/total;
                    total = 0;
                }else {
                    if(ventana[i+1] < 60 && ventana[i+1] > 199){
                        res *= 0.000199; // valor transicion que estado sig no se encuentra entre los valores validos
                    }else{
                       res *= 0.00033; // valor transicion que estado sig no se encuentra entre los valores de la matriz
                    }
                }
            }else {
                res *= 0.000199; // valor transicion que estado ant no se encuentra entre los valores validos
            }
        }
        return res;
    }
	
	// Método que crea la ventana con las mediciones correspondientes y devuelve si tiene anomalías
	public static boolean encontrarAnomalia(Training t, int[] ventana) throws IOException {
		boolean res = false;
		double probMin = t.calcProbMin();
		if(calcProbVentana(ventana, t.matriz) < probMin) {
			res = true;
		}
		return res;
	}
	
    public static void main(String[] args) {
    	try {
			System.out.println("Conectando con "+ REDIS_HOST);
			final Jedis jedis = new Jedis(REDIS_HOST);
			DateFormat df = new SimpleDateFormat("M dd yyyy HH:mm:ss");
			
			// Introduce las mediciones
			get("/nuevo/:dato", (req, res) -> {
				Date dt = new Date();
				Medicion med = new Medicion();
				String valor = req.params(":dato");
	        	jedis.rpush("queue#mediciones", valor);
				jedis.rpush("queue#timestamp", df.format(dt));
				med.valor = valor;
				med.cuando = dt;
				mediciones.add(med);
	        	tam++;
	        	return "Medicion: " + req.params(":dato") + " Tamaño Ventana = " + tam;
	        });
			
			// Lista las mediciones guardadas en redis
	        get("/listar", (req, res) -> {
	        	return mostrarLista();
	        });
	        
	        // Nos muestra si se encuentra anomalia en todas las ventanas posibles de tamaño TAM_VENTANA_MAX
	        get("/detectaAnomalia", (req, res) ->{
	        	String res1 = new String();
	        	if(tam < TAM_VENTANA_MAX) res1 = "Error, faltan mediciones";
	        	else {
	        		Training t = new Training();
	        		t.leerFich("SortedTraining.txt");
	        		int x = 0;
	        		int[] ventana = new int[TAM_VENTANA_MAX];
	        		while(x + TAM_VENTANA_MAX <= tam) {
		        		JSONObject myObject = new JSONObject();
		        		JSONObject[] med = new JSONObject[TAM_VENTANA_MAX];
		        		for(int i = 0; i<TAM_VENTANA_MAX; i++) {
		        			JSONObject medi = new JSONObject();
		        			ventana[i] = Integer.parseInt(jedis.lindex("queue#mediciones",i+x));
		    				medi.put("time", jedis.lindex("queue#timestamp",i+x));
		    				medi.put("valor", jedis.lindex("queue#mediciones",i+x));
		        			med[(int) i] = medi;
		        		}
		        		myObject.put("Mediciones", med);
		        		if(encontrarAnomalia(t, ventana)) myObject.put("anomalia", "si"); // llamo al metodo para comprobar si hay o no anomalia
		        		else myObject.put("anomalia", "no");
		        		res1 += myObject.toString() + "<br>";
		        		x++;
	        		}
	        	}
	        	return res1;
	        });
	        
	        // Muestra una grafica con las 10 últimas mediciones/timestamp
	        get("/grafica", (req, res) -> {
	        	return GraficaChart.crea_grafica();
	        });
	        
	        // Lista con las 10 últimas mediciones
	        get("/listajson", (req, res) ->{
	        	JSONObject myObject = new JSONObject();
	        	JSONObject[] med = new JSONObject[10];
        		long len = jedis.llen("queue#timestamp");
    			for(long i = len - 10 - 1; i<len; i++) {
    				JSONObject medi = new JSONObject();
    				medi.put("time", jedis.lindex("queue#timestamp",i));
    				medi.put("valor", jedis.lindex("queue#mediciones",i));
        			med[(int) i] = medi;
        		}
        		myObject.put("Mediciones", med);
        		return myObject.toString();
	        });
        	jedis.flushAll();
	        jedis.close();
		} catch (Exception ex) {
			System.err.println(ex);
		}
    }
}
