package com.jsmatos.timesheets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

public class BookmarkletsHandler {

    void start() {

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(3000), 10);
            HttpHandler handler = new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    Headers requestHeaders = exchange.getRequestHeaders();
                    System.out.println("Request headers:");
                    requestHeaders.forEach((name,value)->{
                        System.out.printf("\t%s -> %s%n", name, value);
                    });
                    System.out.println();
                    Headers responseHeaders = exchange.getResponseHeaders();
                    final String origin = requestHeaders.getFirst("Origin");
                    if(origin != null) responseHeaders.add("Access-Control-Allow-Origin", origin);
                    try {
                        URI requestURI = exchange.getRequestURI();
                        String query = requestURI.toString();
                        query = query.substring(query.indexOf("?") + 1);
                        System.out.println("query: " + query);
                        Map<String, List<String>> parameters = splitQuery(query);
                        String id = parameters.get("id").get(0);
                    String response = String.format("document.body.removeChild(document.getElementById('%s'));window.alert('Ok');", id);
//                        String response = String.format("(function(){document.body.removeChild(document.getElementById('%s'));var div = document.createElement('div');div.innerHTML = \"%s\";document.body.appendChild(div)})();", id, createDivLabel("OK"));

                        exchange.sendResponseHeaders(200, response.length());
                        OutputStream responseBody = exchange.getResponseBody();
                        responseBody.write(response.getBytes());
                        responseBody.close();
                        System.out.println("parameters: " + parameters);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            server.createContext("/timeshiit", handler);
            server.setExecutor(null);
            server.start();
            System.out.println("serving requests on " + server.getAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createDivLabel(String content) {
        return "<div style='position:fixed;z-index:99999999;padding-top:0px;right:0;top:0;overflow:none;'><div style='background-color:#fefefe;margin:auto;border:1px solid blue;width:100%;'><p>"+content+"</p></div></div>1  VVVVVvvvv'v";

    }

    public static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
//        Base64.getDecoder().decode()
        final Map<String, List<String>> queryPairs = new LinkedHashMap<>();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
            if (!queryPairs.containsKey(key)) {
                queryPairs.put(key, new LinkedList<>());
            }
            final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
            queryPairs.get(key).add(value);
        }
        return queryPairs;
    }

    public static void main(String[] args) {
        new BookmarkletsHandler().start();
    }

}
