package styx.explorer;

import static styx.data.Values.complex;
import static styx.data.Values.generate;
import static styx.data.Values.parse;
import static styx.data.Values.root;
import static styx.explorer.MustacheUtils.scope;
import static styx.explorer.MustacheUtils.tag;
import static styx.http.server.Server.route;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import styx.data.GeneratorOption;
import styx.data.InvalidAccessException;
import styx.data.Pair;
import styx.data.ParserException;
import styx.data.Reference;
import styx.data.Store;
import styx.data.Value;
import styx.explorer.MustacheUtils.Flag;
import styx.http.server.Request;
import styx.http.server.Response;
import styx.http.server.Server;

public class Explorer {

    private final Logger logger = Logger.getLogger(getClass().getName());

    private final int serverPort;
    private final String datastoreUrl;
    private final boolean templateCache;
    private final MustacheFactory templateFactory = new DefaultMustacheFactory();

    public static void main(String[] args) {
        new Explorer(new Arguments(args)).run();
    }

    private Explorer(Arguments args) {
        this.serverPort = args.getInt("server-port").orElse(8080);
        this.datastoreUrl = args.getString("datastore-url").orElse("lmdb:data/datastore.lmdb");
        this.templateCache = args.getBoolean("template-cache").orElse(true);
    }

    public void run() {
        logger.info("Starting (serverPort: " + serverPort + ", datastoreUrl: " + datastoreUrl + ", templateCache: " + templateCache + ").");
        try(Server server = new Server()) {
            server.
                port(serverPort).
                routes(
                    route().path("/").toResource("index.html"),
                    route().path("/favicon.ico").toResource("favicon.ico"),
                    route().path("/time").to((req, res) -> res.write(LocalDateTime.now().toString() + "\n")),
                    route().path("/static/**").toResource("."),
                    route().path("/browse/**").to(this::browse),
                    route().path("/view/**").to(this::view),
                    route().path("/edit/**").to(this::edit),
                    route().path("/content/**").to(this::getContent)).
                run();
        }
        logger.info("Stopping.");
    }

    private void browse(Request req, Response res) {
        Reference reference = parseReference(req);
        Optional<String> addButton = req.param("addButton");
        Optional<String> addKey = req.param("addKey");
        Optional<String> addVal = req.param("addVal");
        Optional<String> delKey = req.param("delKey");
        logger.info((addButton.isPresent() ? "Add " : delKey.isPresent() ? "Del " : "Browse ") + reference);
        try(Store store = Store.open(datastoreUrl)) {
            String error = null;
            if(addButton.isPresent()) {
                try {
                    store.write(reference.child(parse(addKey.get())), parse(addVal.get()));
                    addKey = Optional.empty();
                    addVal = Optional.empty();
                } catch(ParserException e) {
                    error = "Failed to add: " + e.getMessage();
                }
            } else if(delKey.isPresent()) {
                try {
                    store.write(reference.child(parse(delKey.get())), null);
                } catch(ParserException e) {
                    error = "Failed to delete: " + e.getMessage();
                }
            }
            List<Pair> children;
            try {
                children = store.browse(reference).collect(Collectors.toList());
            } catch(InvalidAccessException e) {
                if(!reference.parent().isPresent()) {
                    store.write(root(), complex());
                }
                res.status(303);
                res.header("location", req.path() + "/..");
                return;
            }
            Map<String, Object> scope = scope(
                    tag("selfText", generate(reference)),
                    tag("selfRef", toRef(reference)),
                    tag("parentRef", reference.parent().isPresent() ? toRef(reference.parent().get()) : null),
                    tag("error", error),
                    tag("entries", children.stream().
                            map(pair -> pair.value().isComplex() ?
                                    scope(  tag("keyText", pair.key().toString(), Flag.JS),
                                            tag("valueText", "{ ... }"),
                                            tag("keyRef", toRef(reference.child(pair.key())))) :
                                    scope(  tag("keyText", pair.key().toString(), Flag.JS),
                                            tag("valueText", pair.value().toString()),
                                            tag("valueRef", toRef(reference.child(pair.key()))))).
                            collect(Collectors.toList())),
                    tag("addKey", addKey.orElse(null)),
                    tag("addVal", addVal.orElse(null)));
            res.contentType("text/html");
            applyTemplate(res, "browse.html", scope);
        }
    }

    private void view(Request req, Response res) {
        Reference reference = parseReference(req);
        logger.info("View " + reference);
        try(Store store = Store.open(datastoreUrl)) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
                return;
            }
            Map<String, Object> scope = scope(
                    tag("selfText", generate(reference)),
                    tag("selfRef", toRef(reference)),
                    tag("parentRef", reference.parent().isPresent() ? toRef(reference.parent().get()) : null),
                    tag("content", generate(value.get(), GeneratorOption.INDENT)));
            res.contentType("text/html");
            applyTemplate(res, "view.html", scope);
        }
    }

    private void edit(Request req, Response res) {
        Reference reference = parseReference(req);
        Optional<String> content = req.param("content");
        logger.info((content.isPresent() ? "Store " : "Edit ") + reference);
        try(Store store = Store.open(datastoreUrl)) {
            String error = null;
            if(content.isPresent()) {
                try {
                    store.write(reference, parse(content.get()));
                } catch(ParserException e) {
                    error = "Failed to store: " + e.getMessage();
                }
            }
            if(error == null) {
                Optional<Value> value = store.read(reference);
                if(!value.isPresent()) {
                    res.status(404);
                    return;
                }
                content = Optional.of(generate(value.get(), GeneratorOption.INDENT));
            }
            Map<String, Object> scope = scope(
                    tag("selfText", generate(reference)),
                    tag("selfRef", toRef(reference)),
                    tag("parentRef", reference.parent().isPresent() ? toRef(reference.parent().get()) : null),
                    tag("error", error),
                    tag("content", content.orElse(null)));
            res.contentType("text/html");
            applyTemplate(res, "edit.html", scope);
        }
    }

    private void getContent(Request req, Response res) {
        Reference reference = parseReference(req);
        logger.info("Get Content " + reference);
        try(Store store = Store.open(datastoreUrl)) {
            Optional<Value> value = store.read(reference);
            if(!value.isPresent()) {
                res.status(404);
                return;
            }
            res.contentType("text/plain");
            res.write(generate(value.get(), GeneratorOption.INDENT));
        }
    }

    private void applyTemplate(Response res, String name, Map<String, Object> scope) {
        Mustache template = (templateCache ? templateFactory : new DefaultMustacheFactory()).
                compile("META-INF/resources/" + name); // strange: does not work with leading slash!
        StringWriter writer = new StringWriter();
        template.execute(writer, scope);
        res.write(writer.toString());
    }

    private Reference parseReference(Request req) {
        return parse("<" + Paths.get("/").resolve(req.param("**").get()).normalize() + ">").asReference();
    }

    private String toRef(Reference reference) {
        String string = generate(reference);
        return string.subSequence(1, string.length() - 1).toString();
    }
}
