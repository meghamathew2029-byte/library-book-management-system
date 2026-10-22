import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LibraryServer {
    private static final List<Book> books = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        books.add(new Book(1, "Java Programming", "James Gosling"));
        books.add(new Book(2, "Data Structures", "Mark Allen Weiss"));
        books.add(new Book(3, "Computer Networks", "Andrew S. Tanenbaum"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/books", LibraryServer::showBooks);
        server.createContext("/api/issue", e -> changeAvailability(e, false));
        server.createContext("/api/return", e -> changeAvailability(e, true));
        server.createContext("/", LibraryServer::showWebsite);
        server.start();
        System.out.println("Library system running at http://localhost:8080");
    }

    private static synchronized void showBooks(HttpExchange exchange) throws IOException {
        String query = getParams(exchange).getOrDefault("q", "").toLowerCase();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (Book book : books) {
            if (book.getTitle().toLowerCase().contains(query)
                    || book.getAuthor().toLowerCase().contains(query)) {
                if (!first) json.append(",");
                json.append(book.toJson());
                first = false;
            }
        }
        json.append("]");
        send(exchange, 200, json.toString(), "application/json");
    }

    private static synchronized void changeAvailability(HttpExchange exchange, boolean returning)
            throws IOException {
        try {
            int id = Integer.parseInt(getParams(exchange).getOrDefault("id", "0"));
            for (Book book : books) {
                if (book.getId() == id) {
                    if (returning) {
                        book.setAvailable(true);
                        send(exchange, 200, "{\"message\":\"Book returned successfully.\"}", "application/json");
                    } else if (book.isAvailable()) {
                        book.setAvailable(false);
                        send(exchange, 200, "{\"message\":\"Book issued successfully.\"}", "application/json");
                    } else {
                        send(exchange, 200, "{\"message\":\"Book is already issued.\"}", "application/json");
                    }
                    return;
                }
            }
            send(exchange, 404, "{\"message\":\"Book not found.\"}", "application/json");
        } catch (NumberFormatException ex) {
            send(exchange, 400, "{\"message\":\"Enter a valid book ID.\"}", "application/json");
        }
    }

    private static void showWebsite(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.equals("/")) requestPath = "/index.html";
        Path root = Paths.get("public").toAbsolutePath().normalize();
        Path file = root.resolve(requestPath.substring(1)).normalize();
        if (!file.startsWith(root) || !Files.exists(file) || Files.isDirectory(file)) {
            send(exchange, 404, "File not found.", "text/plain");
            return;
        }
        String type = requestPath.endsWith(".css") ? "text/css" : "text/html";
        byte[] content = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }

    private static Map<String, String> getParams(HttpExchange exchange) throws IOException {
        Map<String, String> params = new HashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] part = pair.split("=", 2);
                String key = URLDecoder.decode(part[0], StandardCharsets.UTF_8);
                String value = part.length > 1
                        ? URLDecoder.decode(part[1], StandardCharsets.UTF_8) : "";
                params.put(key, value);
            }
        }
        return params;
    }

    private static void send(HttpExchange exchange, int status, String body, String type)
            throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }
}
