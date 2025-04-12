import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

public class PollServer {
    static Map<String, Integer> votes = new LinkedHashMap<>();

    public static void main(String[] args) throws IOException {
        votes.put("Java", 0);
        votes.put("Python", 0);
        votes.put("JavaScript", 0);
        votes.put("C", 0);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/vote", new VoteHandler());
        server.setExecutor(null);
        System.out.println("Poll Server running at http://localhost:8080");
        server.start();
    }

    static class HomeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("index.html");
            byte[] response = java.nio.file.Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }

    static class VoteHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                String formData = reader.lines().collect(Collectors.joining());
                String[] parts = formData.split("=");

                if (parts.length == 2) {
                    String vote = java.net.URLDecoder.decode(parts[1], "UTF-8").replace("+", " ");
                    if (votes.containsKey(vote)) {
                        votes.put(vote, votes.get(vote) + 1);
                    }
                }

                int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();

                StringBuilder html = new StringBuilder();
                html.append("<!DOCTYPE html><html lang='en'><head>")
                        .append("<meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                        .append("<title>Poll Results</title>")
                        .append("<script src='https://cdn.tailwindcss.com'></script>")
                        .append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>")
                        .append("</head><body class='bg-gradient-to-br from-indigo-100 to-purple-100 min-h-screen p-6'>")

                        .append("<div class='max-w-5xl mx-auto'>")
                        .append("<h1 class='text-4xl font-bold text-center text-indigo-700 mb-10'>👨‍💻 Favorite Programming Language Poll</h1>")
                        .append("<div class='grid md:grid-cols-2 gap-8'>")

                        // Pie Chart Card
                        .append("<div class='bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition duration-300'>")
                        .append("<h2 class='text-xl font-semibold text-indigo-600 mb-4'>Pie Chart</h2>")
                        .append("<canvas id='voteChart' width='300' height='300'></canvas>")
                        .append("</div>")

                        // Vote Breakdown Card
                        .append("<div class='bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition duration-300'>")
                        .append("<h2 class='text-xl font-semibold text-indigo-600 mb-4'>Vote Breakdown</h2>")
                        .append("<p class='text-gray-700 mb-4'>Total Votes: <strong>").append(totalVotes)
                        .append("</strong></p>")
                        .append("<ul class='space-y-2'>");

                for (Map.Entry<String, Integer> entry : votes.entrySet()) {
                    int percent = (int) ((entry.getValue() * 100.0f) / Math.max(totalVotes, 1));
                    html.append("<li class='flex justify-between bg-gray-100 p-3 rounded'>")
                            .append("<span>").append(entry.getKey()).append("</span>")
                            .append("<span>").append(entry.getValue()).append(" votes (").append(percent)
                            .append("%)</span>")
                            .append("</li>");
                }

                html.append("</ul>")
                        .append("<a href='/' class='inline-block mt-6 bg-indigo-600 text-white px-5 py-2 rounded-3xl shadow hover:bg-indigo-700 transition font-medium'> Back to Poll</a>")
                        .append("</div></div></div>")

                        // Chart script
                        .append("<script>")
                        .append("const ctx = document.getElementById('voteChart').getContext('2d');")
                        .append("new Chart(ctx, { type: 'pie', data: { labels: ['Java', 'Python', 'JavaScript', 'C'], datasets: [{ data: [")
                        .append(votes.get("Java")).append(",")
                        .append(votes.get("Python")).append(",")
                        .append(votes.get("JavaScript")).append(",")
                        .append(votes.get("C"))
                        .append("], backgroundColor: ['#6366F1', '#8B5CF6', '#EC4899', '#FACC15'] }] }, options: { responsive: true } });")
                        .append("</script>")

                        .append("</body></html>");

                byte[] response = html.toString().getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
            exchange.close();
        }
    }
}
