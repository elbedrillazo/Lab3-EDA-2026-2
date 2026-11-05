import edu.princeton.cs.algs4.Graph;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generador docente para el Laboratorio 3 de EDA, semestre 2026-2.
 * Construye una malla rectangular aumentada con contactos aleatorios.
 *
 * <p>Es una variante no dirigida del modelo de Kleinberg: cada vertice
 * selecciona un contacto con probabilidad proporcional al inverso del
 * cuadrado de su distancia Manhattan en la malla ORIGINAL. Se agrega
 * la arista no dirigida correspondiente si aun no existe. No hay
 * emparejamiento, conexiones periodicas ni nuevos sorteos por duplicados.
 * No se afirma que los teoremas de navegacion descentralizada del modelo
 * original se apliquen a esta variante, que se recorrera mediante BFS.
 *
 * <p>Los nombres son opacos, unicos y de longitud fija. Dependen solo de n
 * y del indice: no cambian con la semilla del grafo, entre objetos ni con
 * distintas factorizaciones del mismo n. No se usan como indices numericos.
 * El catalogo inverso es infraestructura docente, ajena al mapa experimental.
 *
 * <p>El objeto no cambia despues de construirse. Cada generar(semilla)
 * utiliza su propio Random y retorna un Graph nuevo y modificable. Dos
 * llamadas con iguales dimensiones y semilla producen las mismas listas
 * de adyacencia. No se usa ni se modifica el estado global de StdRandom.
 *
 * <p>Constructor: O(n) tiempo y espacio (costos esperados para HashMap).
 * Generacion directa: O(n^2) tiempo, O(n) espacio auxiliar, mas el Graph.
 * Dependencia: algs4.jar; compatible con Java 8. No modifica GraphGenerator.
 * Codigo docente original; utiliza Graph de Sedgewick y Wayne por composicion.
 *
 * @see <a href="https://algs4.cs.princeton.edu/code/">Codigo de Algorithms</a>
 * @see <a href="https://www.cs.cornell.edu/home/kleinber/swn.d/swn.html">
 *      Kleinberg, The Small-World Phenomenon: An Algorithmic Perspective</a>
 */
public final class GeneradorTopologia {
    private final int filas;
    private final int columnas;
    private final int n;
    private final String[] identificadorPorIndice;
    private final Map<String, Integer> indicePorIdentificador;

    /**
     * Prepara dimensiones y catalogo; NO construye ni conserva un grafo.
     * @param filas cantidad positiva de filas
     * @param columnas cantidad positiva de columnas
     * @throws IllegalArgumentException si alguna dimension no es positiva
     *         o su producto excede Integer.MAX_VALUE
     */
    public GeneradorTopologia(int filas, int columnas) {
        long cantidad = (long) filas * columnas;
        if (filas <= 0 || columnas <= 0 || cantidad > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Dimensiones invalidas");
        }
        this.filas = filas;
        this.columnas = columnas;
        this.n = (int) cantidad;
        this.identificadorPorIndice = new String[n];
        this.indicePorIdentificador = new HashMap<String, Integer>();
        long base = mezclar64(0x4544413230323632L ^ (long) n);
        for (int v = 0; v < n; v++) {
            // mezclar64 es una biyeccion de los patrones de 64 bits:
            // los nombres son distintos sin depender de una prueba azarosa.
            String codigo = Long.toHexString(mezclar64(base + v));
            StringBuilder nombre = new StringBuilder("estacion-");
            for (int i = codigo.length(); i < 16; i++) {
                nombre.append('0');
            }
            nombre.append(codigo).append(".red-simulada");
            String id = nombre.toString();
            identificadorPorIndice[v] = id;
            indicePorIdentificador.put(id, v);
        }
    }

    /** @return cantidad de filas */
    public int filas() { return filas; }

    /** @return cantidad de columnas */
    public int columnas() { return columnas; }

    /** @return orden n del grafo */
    public int cantidadVertices() { return n; }

    /**
     * Traduce coordenadas, numeradas desde cero, a indice: f * C + c.
     * @param fila coordenada vertical en [0, filas)
     * @param columna coordenada horizontal en [0, columnas)
     * @throws IllegalArgumentException si las coordenadas estan fuera de rango
     * @return indice en [0,n)
     */
    public int indice(int fila, int columna) {
        if (fila < 0 || fila >= filas || columna < 0 || columna >= columnas) {
            throw new IllegalArgumentException("Coordenadas fuera de rango");
        }
        return fila * columnas + columna;
    }

    /**
     * Traduce un indice a un NUEVO arreglo {fila, columna}; cuesta O(1).
     * Modificar el arreglo retornado no modifica este objeto.
     * @param vertice indice interno en [0,n)
     * @throws IllegalArgumentException si vertice no pertenece a [0,n)
     * @return coordenadas numeradas desde cero
     */
    public int[] coordenadas(int vertice) {
        validarVertice(vertice);
        return new int[] {vertice / columnas, vertice % columnas};
    }

    /**
     * Consulta directa O(1) del nombre inmutable de una estacion.
     * @param vertice indice interno en [0,n)
     * @throws IllegalArgumentException si vertice no pertenece a [0,n)
     * @return identificador logico; no es una direccion DNS real
     */
    public String identificador(int vertice) {
        validarVertice(vertice);
        return identificadorPorIndice[vertice];
    }

    /**
     * Consulta inversa esperada O(1), para PREPARACION de datos y pruebas.
     * No debe llamarse durante el ruteo medido para convertir destinos.
     * @param identificador nombre logico de una estacion del catalogo
     * @throws IllegalArgumentException si el nombre es nulo o desconocido
     * @return indice interno asociado al nombre
     */
    public int indice(String identificador) {
        Integer v = indicePorIdentificador.get(identificador);
        if (v == null) {
            throw new IllegalArgumentException("Identificador desconocido");
        }
        return v;
    }

    /**
     * Construye un grafo simple, no dirigido y conexo de orden n.
     * Agrega primero la malla (derecha y abajo) y luego un contacto sorteado
     * por vertice, en orden de indices. El destino puede ser un vecino local.
     * Las elecciones duplicadas se descartan SIN volver a sortear.
     * Para n=1 retorna el grafo de un vertice, sin aristas.
     *
     * @param semilla semilla de la realizacion de contactos
     * @return Graph nuevo con {@code 2*n-filas-columnas <= E <= 3*n-filas-columnas}
     */
    public Graph generar(long semilla) {
        Graph grafo = new Graph(n);
        for (int f = 0; f < filas; f++) {
            for (int c = 0; c < columnas; c++) {
                int u = indice(f, c);
                if (c + 1 < columnas) { grafo.addEdge(u, indice(f, c + 1)); }
                if (f + 1 < filas) { grafo.addEdge(u, indice(f + 1, c)); }
            }
        }
        if (n == 1) { return grafo; }
        Random azar = new Random(semilla);
        int[] contacto = new int[n];
        Arrays.fill(contacto, -1);
        for (int u = 0; u < n; u++) {
            int v = seleccionarContacto(u, azar);
            // La unica arista aleatoria previa que podria repetir {u,v}
            // es la eleccion inversa, v -> u. Las otras son de la malla.
            if (distanciaManhattan(u, v) != 1 && contacto[v] != u) {
                grafo.addEdge(u, v);
            }
            contacto[u] = v;
        }
        return grafo;
    }

    private int seleccionarContacto(int u, Random azar) {
        double total = 0.0;
        for (int v = 0; v < n; v++) {
            if (v != u) { total += peso(u, v); }
        }
        double objetivo = azar.nextDouble() * total;
        double acumulado = 0.0;
        int ultimo = -1;
        for (int v = 0; v < n; v++) {
            if (v != u) {
                ultimo = v;
                acumulado += peso(u, v);
                if (objetivo < acumulado) { return v; }
            }
        }
        return ultimo; // resguardo frente al redondeo de punto flotante
    }

    private double peso(int u, int v) {
        double distancia = distanciaManhattan(u, v);
        return 1.0 / (distancia * distancia);
    }

    private int distanciaManhattan(int u, int v) {
        return Math.abs(u / columnas - v / columnas)
                + Math.abs(u % columnas - v % columnas);
    }

    private void validarVertice(int v) {
        if (v < 0 || v >= n) {
            throw new IllegalArgumentException("Vertice fuera de rango");
        }
    }

    private static long mezclar64(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }
}
