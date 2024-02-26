package com.example.runtime;

import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.Method;

/**
 * Clase que representa un servidor HTTP básico que maneja solicitudes GET para archivos locales.
 */
public class HttpServer {


    private static String staticFilesDirectory="/public";
    private static final Map<String, Function> getHandlers = new HashMap();
    private static final Map<String, Function> postHandlers = new HashMap();


    public static Map<String, Method> componentes = new HashMap<>();

    /**
     * Método principal que inicia el servidor HTTP.
     *
     * @param args Argumentos de línea de comandos (no se utilizan).
     * @throws ClassNotFoundException Si no se encuentra la clase especificada.
     */
    public static void main(String[] args) throws ClassNotFoundException {
        loadComponents("com.example.components");
        try (ServerSocket serverSocket = new ServerSocket(35000)) {
            System.out.println("Servidor listo para recibir conexiones...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleRequest(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }

    }

    /**
     * Carga dinámicamente las clases anotadas con @Component desde el paquete especificado.
     *
     * @param packageName El nombre del paquete donde se buscarán las clases anotadas.
     * @throws URISyntaxException Si ocurre un error al convertir la URL del paquete en URI.
     * @throws ClassNotFoundException Si no se encuentra alguna de las clases especificadas.
     */
    private static void loadComponents(String packageName) {
        try {
            ClassLoader classLoader = HttpServer.class.getClassLoader();
            String path = packageName.replace('.', '/');
            File[] files = new File(classLoader.getResource(path).toURI()).listFiles();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = packageName + "." + file.getName().replace(".class", "");
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        for (Method m : clazz.getMethods()) {
                            if (m.isAnnotationPresent(GetMapping.class)) {
                                componentes.put(m.getAnnotation(GetMapping.class).value(), m);
                            }
                        }
                    }
                }
            }
        } catch (URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Maneja una solicitud HTTP recibida desde un cliente.
     *
     * @param clientSocket El socket del cliente que realizó la solicitud.
     */
    private static void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream()) {

            String inputLine;
            boolean firstLine = true;
            String method = "";
            String uriStr = "";
            String requestBody = "";

            while ((inputLine = in.readLine()) != null) {
                if (firstLine) {
                    String[] requestParts = inputLine.split(" ");
                    uriStr = requestParts[1];
                    method = requestParts[0];
                    firstLine = false;
                } else if (!inputLine.isEmpty()) { // Si no es la primera línea y no está vacía, es una cabecera
                    // procesar las cabeceras si es necesario
                } else {
                    // Si la línea está vacía, significa se alcanzó el final de las cabeceras y el inicio del cuerpo del mensaje
                    break;
                }
            }

            if ("POST".equalsIgnoreCase(method)) {
                StringBuilder requestBodyBuilder = new StringBuilder();
                while (in.ready()) {
                    requestBodyBuilder.append((char) in.read());
                }
                requestBody = requestBodyBuilder.toString();
            }

            URI requestUri = new URI(uriStr);
            String response = handleHttpRequest(requestBody, requestUri, method);
            outputStream.write(response.getBytes(StandardCharsets.ISO_8859_1));

        } catch (IOException | URISyntaxException e) {
            System.err.println("Error al manejar la solicitud: " + e.getMessage());
        }
    }

    /**
     * Maneja una solicitud HTTP recibida desde un cliente, determina si se trata de una solicitud para un archivo estático o para un componente dinámico,
     * y genera una respuesta HTTP en consecuencia.
     *
     * @param requestBody El cuerpo de la solicitud HTTP.
     * @param requestUri  La URI de la solicitud HTTP.
     * @param method      El método HTTP utilizado en la solicitud.
     * @return La respuesta HTTP generada.
     */
    private static String handleHttpRequest(String requestBody, URI requestUri, String method) {
        String response = "";
        try {
            String extension = getFileExtension(requestUri.getPath());
            if (!extension.isEmpty()) {
                response = serveStaticFile(requestUri);
            } else {
                Method componentMethod = componentes.get(requestUri.getPath());
                if (componentMethod != null) {
                    Object result = componentMethod.invoke(null, requestBody, requestUri, method);
                    response = createHttpResponse(200, "text/html", result.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    response = createHttpResponse(404, "text/html", "<h1>Error 404: Not Found</h1>".getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException | IllegalAccessException | InvocationTargetException e) {
            response = createHttpResponse(500, "text/html", httpError().getBytes(StandardCharsets.UTF_8));
        }
        return response;
    }

    /**
     * Sirve un archivo estático.
     *
     * @param requestedURI La URI solicitada por el cliente.
     * @return El contenido del archivo estático.
     * @throws IOException Si ocurre un error al leer el archivo.
     */
    private static String serveStaticFile(URI requestedURI) throws IOException {
        String outputline = "";
        Path file = Paths.get("target/classes" + staticFilesDirectory + requestedURI.getPath());
        if (Files.exists(file) && !Files.isDirectory(file)) {
            byte[] fileContent = Files.readAllBytes(file);
            String contentType = getContentType(file.getFileName().toString());
            outputline = createHttpResponse(200, contentType, fileContent);
        } else {
            throw new IOException();
        }
        return outputline;
    }

    /**
     * Obtiene la respuesta HTML para la solicitud de la película.
     *
     * @param method El método HTTP utilizado en la solicitud.
     * @param uri    La URI solicitada por el cliente.
     * @return La respuesta HTML generada.
     */
    public static String obtainHtmlRequest(String method, URI uri) {
        JSONObject jsonResponse = HttpRequestHandler.handleRequest(method, uri);

        String title = jsonResponse.optString("Title", "");
        String year = jsonResponse.optString("Year", "");
        String rated = jsonResponse.optString("Rated", "");
        String released = jsonResponse.optString("Released", "");
        String runtime = jsonResponse.optString("Runtime", "");
        String genre = jsonResponse.optString("Genre", "");
        String director = jsonResponse.optString("Director", "");
        String plot = jsonResponse.optString("Plot", "");
        String imdbRating = jsonResponse.optString("imdbRating", "");
        String poster = jsonResponse.optString("Poster", "");

        String outputLine = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n"
                + "<!DOCTYPE html>\r\n"
                + "<html>\r\n"
                + "<head>\r\n"
                + "<title>Movies</title>\r\n"
                + "</head>\r\n"
                + "<body>\r\n"
                + "<h1>" + title + " (" + year + ")</h1>\r\n"
                + "<div class=\"movie-details\">\r\n"
                + "<img src=\"" + poster + "\" alt=\"" + title + "\"> <br>\r\n"
                + "<strong>Rated:</strong> " + rated + "<br>\r\n"
                + "<strong>Released:</strong> " + released + "<br>\r\n"
                + "<strong>Runtime:</strong> " + runtime + "<br>\r\n"
                + "<strong>Genre:</strong> " + genre + "<br>\r\n"
                + "<strong>Director:</strong> " + director + "<br>\r\n"
                + "<strong>IMDb Rating:</strong> " + imdbRating + "<br>\r\n"
                + "<strong>Plot:</strong><br>\r\n"
                + "<p>" + plot + "</p>\r\n"
                + "</div>\r\n"
                + "</body>\r\n"
                + "</html>";
        return outputLine;
    }

    /**
     * Genera una página de error HTTP básica.
     *
     * @return La página de error generada.
     */
    private static String httpError() {
        String errorPage = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "    <head>\n" +
                "        <title>Error Not found</title>\n" +
                "        <meta charset=\"UTF-8\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        <h1>Error</h1>\n" +
                "    </body>\n" +
                "</html>";
        return errorPage;
    }

    /**
     * Genera una página de saludo HTTP básica.
     *
     * @param query Los parámetros de la consulta.
     * @return La página de saludo generada.
     */
    public static String httpHello(String query) {
        String helloPage = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html\r\n"
                + "\r\n"
                + "<!DOCTYPE html>\r\n"
                + "<html>\r\n" +
                "    <head>\n" +
                "        <title>Error Not found</title>\n" +
                "        <meta charset=\"UTF-8\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    </head>\n" +
                "    <body>\n" +
                "        <p>Hola, El Query es: " + query + ".</p>\n" +
                "    </body>\n" +
                "</html>";
        return helloPage;
    }

    /**
     * Crea una respuesta HTTP con el código de estado, tipo de contenido y contenido especificados.
     *
     * @param statusCode  El código de estado HTTP.
     * @param contentType El tipo de contenido HTTP.
     * @param content     El contenido de la respuesta.
     * @return La respuesta HTTP generada.
     */
    private static String createHttpResponse(int statusCode, String contentType, byte[] content) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" OK\r\n");
        response.append("Content-Type: ").append(contentType).append("\r\n");
        response.append("Content-Length: ").append(content.length).append("\r\n");
        response.append("\r\n");
        response.append(new String(content, StandardCharsets.ISO_8859_1));
        return response.toString();
    }

    /**
     * Obtiene el tipo de contenido MIME basado en la extensión del archivo.
     *
     * @param fileName El nombre del archivo.
     * @return El tipo de contenido MIME.
     */
    private static String getContentType(String fileName) {
        switch (getFileExtension(fileName)) {
            case "html":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }

    /**
     * Obtiene la extensión de un archivo a partir de su nombre.
     *
     * @param fileName El nombre del archivo.
     * @return La extensión del archivo.
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
