package com.github.phoswald.data.explorer;

import static styx.data.Values.generate;
import static styx.data.Values.parse;
import static styx.http.server.Server.route;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Logger;

import styx.data.GeneratorOption;
import styx.data.Pair;
import styx.data.Reference;
import styx.data.Store;
import styx.data.Value;
import styx.data.exception.ParserException;
import styx.http.server.Request;
import styx.http.server.Response;
import styx.http.server.Server;

public class Main {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final int serverPort = Integer.parseInt(System.getProperty("serverPort", "8080"));
    private final String dataStoreUrl = System.getProperty("dataStoreUrl", "./datastore.styx");

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        logger.info("Starting (serverPort: " + serverPort + ", dataStoreUrl: " + dataStoreUrl + ").");
        try(Server server = new Server()) {
            server.
                port(serverPort).
                routes(
                    route().path("/").toResource("index.html"),
                    route().path("/data-explorer.css").toResource("data-explorer.css"),
                    route().path("/favicon.ico").toResource("favicon.ico"),
                    route().path("/browse/**").to(this::browse),
                    route().path("/view/**").to(this::view),
                    route().path("/edit/**").to(this::edit),
                    route().path("/content/**").to(this::getContent)).
                run();
        }
        logger.info("Stopping.");
    }

    private void browse(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("Browse " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent() || !value.get().isComplex()) {
                res.status(404);
                return;
            }
            res.contentType("text/html");
            res.write("<!DOCTYPE html>\n");
            res.write("<html>\n");
            res.write("  <head>\n");
            res.write("    <title>Data Explorer: " + escapeHtml(reference) + "</title>\n");
            res.write("    <link rel='stylesheet' href='/data-explorer.css'>\n");
            res.write("  </head>\n");
            res.write("  <body>\n");
            res.write("    <p class='nav'><a href='/'>Home</a>\n");
            res.write("    <a href='" + getPath("/view", reference) +"'>View</a></p>\n");
            res.write("    <hr>\n");
            res.write("    <p class='reference'>" + escapeHtml(reference) +"</p>\n");
            if(reference.parent().isPresent()) {
                res.write("    <a href='" + getPath("/browse", reference.parent().get()) +"'>..</a><br>\n");
            }
            res.write("    <table>\n");
            res.write("      <tr><th>Key</th><th>Value</th></tr>\n");
            for(Pair pair : value.get().asComplex().allEntries()) {
                res.write("      <tr>\n");
                if(pair.value().isComplex()) {
                    res.write("        <td><a href='" + getPath("/browse", reference.child(pair.key())) +"'>" + escapeHtml(pair.key()) +"</a></td><td><a href='" + getPath("/view", reference.child(pair.key())) + "'>{ ... }</a></td>\n");
                } else {
                    res.write("        <td>" + escapeHtml(pair.key()) + "</td><td><a href='" + getPath("/view", reference.child(pair.key())) + "'>" + escapeHtml(pair.value()) + "</a></td>\n");
                }
                res.write("      </tr>\n");
            }
            res.write("    </table>\n");
            res.write("  </body>\n");
            res.write("</html>\n");
        }
    }

    private void view(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("View " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
                return;
            }
            String content = generate(value.get(), GeneratorOption.INDENT);
            Reference parentOrSelf = reference.parent().isPresent() ? reference.parent().get() : reference;
            res.contentType("text/html");
            res.write("<!DOCTYPE html>\n");
            res.write("<html>\n");
            res.write("  <head>\n");
            res.write("    <title>Data Explorer: " + escapeHtml(reference) + "</title>\n");
            res.write("    <link rel='stylesheet' href='/data-explorer.css'>\n");
            res.write("  </head>\n");
            res.write("  <body>\n");
            res.write("    <p class='nav'><a href='" + getPath("/browse", parentOrSelf) +"'>Browse</a>");
            res.write("    <a href='" + getPath("/edit", reference) +"'>Edit</a>\n");
            res.write("    <a href='" + getPath("/content", reference) +"'>Download</a></p>\n");
            res.write("    <hr>\n");
            res.write("    <p class='reference'>" + escapeHtml(reference) +"</p>\n");
            res.write("    <pre>\n" + escapeHtml(content) +"\n</pre>\n");
            res.write("  </body>\n");
            res.write("</html>\n");
        }
    }

    private void edit(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("Edit " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            String error = null;
            String content = null;
            if(req.param("content").isPresent()) {
                logger.info("Writing " + reference);
                content = req.param("content").get();
                try {
                    store.write(reference, parse(content));
                } catch(ParserException e) {
                    error = e.getMessage();
                }
            }
            if(error == null) {
                Optional<Value> value = store.read(reference);
                if(!value.isPresent()) {
                    res.status(404);
                    return;
                }
                content = generate(value.get(), GeneratorOption.INDENT);
            }
            res.contentType("text/html");
            res.write("<!DOCTYPE html>\n");
            res.write("<html>\n");
            res.write("  <head>\n");
            res.write("    <title>Data Explorer: " + escapeHtml(reference) + "</title>\n");
            res.write("    <link rel='stylesheet' href='/data-explorer.css'>\n");
            res.write("  </head>\n");
            res.write("  <body>\n");
            res.write("    <p class='nav'><a href='" + getPath("/view", reference) +"'>View</a></p>\n");
            res.write("    <hr>\n");
            res.write("    <p class='reference'>" + escapeHtml(reference) +"</p>\n");
            if(error != null) {
                res.write("    <p class='error'>Failed to store: " + escapeHtml(error) +"</p>\n");
            }
            res.write("    <form method='post'>\n");
            res.write("      <input type='submit' value='Store'><br>\n");
            res.write("      <textarea name='content'>\n" + escapeHtml(content) +"\n</textarea>\n");
            res.write("    </form>\n");
            res.write("  </body>\n");
            res.write("</html>\n");
        }
    }

    private void getContent(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("Get Content " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
                return;
            }
            res.contentType("text/plain");
            res.write(generate(value.get(), GeneratorOption.INDENT));
        }
    }

    private Reference getReference(Request req) { // TODO: improve conversion of param("**") to reference
        return parse("<" + Paths.get("/").resolve(req.param("**").get()).normalize() + ">").asReference();
    }

    private String getPath(String prefix, Reference reference) {
        String string = generate(reference);
        return prefix + string.subSequence(1, string.length() - 1);
    }

    private String escapeHtml(Value value) {
        return escapeHtml(generate(value));
    }

    private String escapeHtml(String string) {
        return string.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&apos;");
    }
}
