package gnomeshopping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;


public class wowapi {
    private InputStream input = gnome.class.getClassLoader().getResourceAsStream("config.properties");
    private Properties prop = PropertiesController.readPropertiesFile(input);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private String credentials = prop.getProperty("bcid") + ":" + prop.getProperty("btoken");
    private String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://oauth.battle.net/token"))
            .header("Authorization", "Basic " + encodedAuth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build();
    private final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    private final String json = response.body();
    private final String tokenKey = "\"access_token\":\"";
    private final int start = json.indexOf(tokenKey) + tokenKey.length();
    private final int end = json.indexOf("\"", start);
    private final String token = json.substring(start, end);
    private final String die = (token != null) ? token.replaceAll("[\\s\\r\\n]", "") : "";

    public String temp() throws IOException, InterruptedException { //returns same ids for every ruleset :O.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/index"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    //Auction House IDs are unknown NOT 111 or 117. They are just what showed up in the API.
    public String getAuctions() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/111"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public String getCommodityAuctions() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/111/commodities"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public String getNeutralAuctions() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/117"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public String getNeutralCommodityAuctions() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/117/commodities"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
    public int getItemID(String item) throws IOException, InterruptedException {
        String encodedName = URLEncoder.encode(item, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://us.api.blizzard.com/data/wow/search/item?name.en_US=%s&locale=en_US"
                , encodedName)))
                .header("Battlenet-Namespace", "static-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode results = rootNode.get("results");
        int id = -1;
        if (results != null && results.isArray() && !results.isEmpty()) {
            // Extracts the ID from the first search result matching the name
            id = results.get(0).get("data").get("id").asInt();
        }
        return id;
    }
    public String getItemName(String item) throws IOException, InterruptedException {
        String encodedName = URLEncoder.encode(item, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("https://us.api.blizzard.com/data/wow/search/item?name.en_US=%s&locale=en_US"
                        , encodedName)))
                .header("Battlenet-Namespace", "static-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode rootNode = objectMapper.readTree(response.body());
        JsonNode results = rootNode.get("results");
        String id = "";
        if (results != null && results.isArray() && !results.isEmpty()) {
            // Extracts the ID from the first search result matching the name
            id = results.get(0).get("data").get("name").get("en_US").asText();
        }
        return id;
    }


    public record Prices(Double allyPrice, int allyquant, Double nPrice, int nquant, boolean commodity) {}
    public Prices getPrice(String name) throws IOException, InterruptedException {
        int itemID = getItemID(name);
        if (itemID == -1) return new Prices(-1.0,-1,-1.0,-1, true);

        String commodityAuctions = getCommodityAuctions();
        JsonNode rootNode = objectMapper.readTree(commodityAuctions);
        JsonNode commodityAuctionsJson = rootNode.get("auctions");
        long lowestPrice = Long.MAX_VALUE;
        int bestQuantity = 0;
        for (JsonNode auction : commodityAuctionsJson) {
            JsonNode itemNode = auction.get("item");
            if (itemNode != null && itemNode.get("id").asInt() == itemID) {
                long price = auction.get("unit_price").asLong();
                if (price < lowestPrice) {
                    lowestPrice = price;
                    bestQuantity = auction.get("quantity").asInt();
                }
            }
        }
        if (lowestPrice != Long.MAX_VALUE) {
            String NeutralCommodityAuctions = getNeutralCommodityAuctions();
            rootNode = objectMapper.readTree(NeutralCommodityAuctions);
            commodityAuctionsJson = rootNode.get("auctions");
            long lowestNeutralPrice = Long.MAX_VALUE;
            int bestNeutralQuantity = 0;
            for (JsonNode auction : commodityAuctionsJson) {
                JsonNode itemNode = auction.get("item");
                if (itemNode != null && itemNode.get("id").asInt() == itemID) {
                    long price = auction.get("unit_price").asLong();
                    if (price < lowestNeutralPrice) {
                        lowestNeutralPrice = price;
                        bestNeutralQuantity = auction.get("quantity").asInt();
                    }
                }
            }
            double lowestPriceGold = lowestPrice / 10000.0;
            double lowestNeutralPriceGold = lowestNeutralPrice / 10000.0;
            return new Prices(lowestPriceGold,bestQuantity,lowestNeutralPriceGold,bestNeutralQuantity, true);
        }

        String auctions = getAuctions();
        String neutralAuctions = getNeutralAuctions();
        return new Prices(-1.0,-1,-1.0,-1, true);

    }

    public wowapi() throws IOException, InterruptedException {
    }
}
