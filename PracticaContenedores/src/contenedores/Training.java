package contenedores;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Training {

    protected Map<Integer, Map<Integer, Integer>> matriz;

    public Training() {
        matriz = new HashMap<Integer, Map<Integer, Integer>>();
    }
    
    /* Funcion para desplazar los elementos del array double una pos a la izquierda */
    public static double[] reordenarArray(double[] a) {
        double[] res = new double[Ej1.TAM_VENTANA_MAX];

        for (int i = 1; i < Ej1.TAM_VENTANA_MAX; i++) {
            res[i - 1] = a[i];
        }

        return res;
    }

    /* Funcion para desplazar los elementos del array String una pos a la izquierda */
    public static String[] reordenarArray(String[] a) {
        String[] res = new String[Ej1.TAM_VENTANA_MAX];

        for (int i = 1; i < Ej1.TAM_VENTANA_MAX; i++) {
            res[i - 1] = a[i];
        }

        return res;
    }
    
    /* Funcion que ordena un fichero por lineas */
    public void ordenar() throws IOException {
        FileInputStream entrada = new FileInputStream("training.txt");
        FileOutputStream salida = new FileOutputStream("SortedTraining.txt");

        BufferedReader in = new BufferedReader(new InputStreamReader(entrada));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(salida));

        String linea;
        ArrayList<String> al = new ArrayList<String>();

        while ((linea = in.readLine()) != null) {
            if (!linea.trim().startsWith("-") && linea.trim().length() > 0) {
                al.add(linea);
            }
        }

        Collections.sort(al);
        for (String s : al) {
            out.write(s);
            out.newLine();
        }

        in.close();
        out.close();

    }

    /* Funcion que devuelve la probabilidad minima de las transiciones */
    public double calcProbMin() throws IOException {
        double min, aux;
        min = 1.0;
        aux = 1.0;
        FileInputStream fichero = new FileInputStream("SortedTraining.txt");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(fichero))) {
            String split[];
            double[] ventana = new double[Ej1.TAM_VENTANA_MAX];
            int tamVentana = 0;
            String linea = in.readLine();
            split = linea.split(",");
            String disp = split[0];
            int ant = Integer.parseInt(split[2]);
            int total = 0;
            int sig;

            /*
             * Tenemos el fichero previamente ordenado.
             * Vamos leyendo cada TAM_VENTANA_MAX transiciones por dispositivo
             * Reiniciamos cuando cambiamos de dispositivo
             */
            while ((linea = in.readLine()) != null) {
                split = linea.split(",");
                sig = Integer.parseInt(split[2]);
                if (split[0].equals(disp)) {
                    // Calculamos la probabilidad de que suceda esta transicion y la añadimos a la ventana
                    for (int e : matriz.get(ant).values()) {
                        total += e;
                    }
                    ventana[tamVentana] = (double) matriz.get(ant).get(Integer.parseInt(split[2])) / total;
                    tamVentana++;
                    if (tamVentana == Ej1.TAM_VENTANA_MAX) {
                        for (double d : ventana)
                            aux *= d;
                        ventana = reordenarArray(ventana);
                        tamVentana--;
                    }
                } else {
                    disp = split[0];
                }
                // Comparamos con la minima actual por si la probabilidad es menor
                if (aux < min)
                    min = aux;
                total = 0;
                aux = 1.0;
                ant = sig;
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return min;
    }


    /* Funcion que devuelve el String que corresponde a la matriz */
    public String toString() {
        StringBuffer s = new StringBuffer();
        for (int e : matriz.keySet()) {
            for (int x : matriz.get(e).keySet()) {
                s.append(e + "," + x + "," + matriz.get(e).get(x)+"\n");
            }
        }
        return s.toString();
    }

    /* Función que lee el fichero ordenado y va añadiendo a la matriz las transiciones */
    public void leerFich(String file) throws IOException {
        FileInputStream fichero = new FileInputStream(file);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(fichero))) {
            String split[];
            String linea = in.readLine();
            split = linea.split(",");
            String disp = split[0];
            int ant = Integer.parseInt(split[2]);

            /* 
             * Lee por linea
             * Tiene en cuenta que las transiciones sean del mismo dispositivo
             */
            while ((linea = in.readLine()) != null) {
                split = linea.split(",");
                if (split[0].equals(disp)) {
                    addEstado(ant, Integer.parseInt(split[2]));
                } else {
                    disp = split[0];
                }
                ant = Integer.parseInt(split[2]);
            }
        } catch (NumberFormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* Funcion que añade a la matriz el estado o lo modifica añadiendo uno al valor */
    public void addEstado(int ini, int sig) {
        Map<Integer, Integer> add = matriz.getOrDefault(ini, new HashMap<Integer, Integer>());
        int value = add.getOrDefault(sig, 0);
        value++;
        add.put(sig, value);
        matriz.put(ini, add);
    }
}
