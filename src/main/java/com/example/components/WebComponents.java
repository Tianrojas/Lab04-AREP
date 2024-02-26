package com.example.components;

import com.example.runtime.Component;
import com.example.runtime.GetMapping;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.example.runtime.HttpServer.*;

/**
 * Clase que contiene los componentes web para el servidor HTTP.
 */
@Component
public class WebComponents {

    /**
     * Maneja la solicitud para la raíz del servidor web.
     *
     * @param requestBody El cuerpo de la solicitud HTTP.
     * @param requestUri  La URI de la solicitud HTTP.
     * @param method      El método HTTP utilizado en la solicitud.
     * @return Una página HTML de bienvenida.
     */
    @GetMapping("/")
    public static String index(String requestBody, URI requestUri, String method) {
        return "<h1>Welcome to My Web Application</h1>";
    }

    /**
     * Maneja la solicitud relacionada con las películas.
     *
     * @param requestBody El cuerpo de la solicitud HTTP.
     * @param requestUri  La URI de la solicitud HTTP.
     * @param method      El método HTTP utilizado en la solicitud.
     * @return Una página HTML que muestra detalles de la película.
     * @throws URISyntaxException          Si se produce un error al analizar la URI.
     * @throws UnsupportedEncodingException Si se produce un error al codificar la URI.
     */
    @GetMapping("/movie")
    public static String movieHandler(String requestBody, URI requestUri, String method) throws URISyntaxException, UnsupportedEncodingException {
        String output = "";
        if (method.equals("POST")){
            String encodedUriIN = URLEncoder.encode(requestBody, StandardCharsets.UTF_8.toString());
            output = obtainHtmlRequest("POST", new URI("/movie?" + encodedUriIN));
        } else {
            output = obtainHtmlRequest("GET", new URI(requestUri.toString()));
        }
        return output;
    }

    /**
     * Maneja la solicitud para la ruta "/hello".
     *
     * @param requestBody El cuerpo de la solicitud HTTP.
     * @param requestUri  La URI de la solicitud HTTP.
     * @param method      El método HTTP utilizado en la solicitud.
     * @return Una página HTML de saludo.
     */
    @GetMapping("/hello")
    public static String hello(String requestBody, URI requestUri, String method){
        return httpHello("Buddy");
    }
}
