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
                    route().path("/").toResource("/index.html"),
                    route().path("/favicon.ico").toResource(),
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
            } else {
                res.contentType("text/html");
                res.write("<!DOCTYPE html>\n");
                res.write("<html>\n");
                res.write("  <head><title>Data Explorer: " + escapeHtml(reference) + "</title></head>\n");
                res.write("  <body>\n");
                res.write("    <a href='/'>Home</a>\n");
                res.write("    <a href='" + getPath("/view", reference) +"'>View</a><br>\n");
                res.write("    <hr>\n");
                res.write("    <b>" + escapeHtml(reference) +"</b><br>\n");
                if(reference.parent().isPresent()) {
                    res.write("    <a href='" + getPath("/browse", reference.parent().get()) +"'>..</a><br>\n");
                }
                for(Pair pair : value.get().asComplex().allEntries()) {
                    if(pair.value().isComplex()) {
                        res.write("    <a href='" + getPath("/browse", reference.child(pair.key())) +"'>" + escapeHtml(pair.key()) +"</a> : <a href='" + getPath("/view", reference.child(pair.key())) + "'>{ ... }</a><br>\n");
                    } else {
                        res.write("    " + escapeHtml(pair.key()) + " : <a href='" + getPath("/view", reference.child(pair.key())) + "'>" + escapeHtml(pair.value()) + "</><br>\n");
                    }
                }
                res.write("  </body>\n");
                res.write("</html>\n");
            }
        }
    }

    private void view(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("View " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
            } else {
                Reference parentOrSelf = reference.parent().isPresent() ? reference.parent().get() : reference;
                res.contentType("text/html");
                res.write("<!DOCTYPE html>\n");
                res.write("<html>\n");
                res.write("  <head><title>Data Explorer: " + escapeHtml(reference) + "</title></head>\n");
                res.write("  <body>\n");
                res.write("    <a href='" + getPath("/browse", parentOrSelf) +"'>Browse</a>");
                res.write("    <a href='" + getPath("/edit", reference) +"'>Edit</a>\n");
                res.write("    <a href='" + getPath("/content", reference) +"'>Download</a><br>\n");
                res.write("    <hr>\n");
                res.write("    <b>" + escapeHtml(reference) +"</b><br>\n");
                res.write("    <pre>\n" + escapeHtml(generate(value.get(), GeneratorOption.INDENT)) +"\n</pre>\n");
                res.write("  </body>\n");
                res.write("</html>\n");
            }
        }
    }

    private void edit(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("Edit " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            if(req.param("content").isPresent()) {
                logger.info("Writing " + reference);
                Value content = parse(req.param("content").get());
                store.write(reference, content);
            }
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
            } else {
                res.contentType("text/html");
                res.write("<!DOCTYPE html>\n");
                res.write("<html>\n");
                res.write("  <head><title>Data Explorer: " + escapeHtml(reference) + "</title></head>\n");
                res.write("  <body>\n");
                res.write("    <a href='" + getPath("/view", reference) +"'>View</a><br>\n");
                res.write("    <hr>\n");
                res.write("    <b>" + escapeHtml(reference) +"</b><br>\n");
                res.write("    <form method='post'>\n");
                res.write("      <input type='submit' value='Store'><br>\n");
                res.write("      <textarea cols='150' rows='50' name='content'>\n" + escapeHtml(generate(value.get(), GeneratorOption.INDENT)) +"\n</textarea>\n");
                res.write("    </form>\n");
                res.write("  </body>\n");
                res.write("</html>\n");
            }
        }
    }

    private void getContent(Request req, Response res) {
        Reference reference = getReference(req);
        logger.info("Get Content " + reference);
        try(Store store = Store.file(Paths.get(dataStoreUrl))) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
            } else {
                res.contentType("text/plain");
                res.write(generate(value.get(), GeneratorOption.INDENT));
            }
        }
    }

    private Reference getReference(Request req) {
        return parse("<" + req.path().substring(req.path().indexOf('/', 1)) + ">").asReference();
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
