package gnomeshopping;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Properties;


public class wowapi {
    private InputStream input = gnome.class.getClassLoader().getResourceAsStream("config.properties");
    private Properties prop = PropertiesController.readPropertiesFile(input);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private String credentials = prop.getProperty("bcid") + ":" + prop.getProperty("btoken");
    private String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

    private HttpRequest request = HttpRequest.newBuilder()
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
    private final String die = token.replaceAll("[\\s\\r\\n]", "");

    private String cachedCommodityBody = null;
    private Instant commodityFetchTime = Instant.MIN;

    private String cachedNeutralCommodityBody = null;
    private Instant neutralCommodityFetchTime = Instant.MIN;

    private String cachedBody = null;
    private Instant fetchTime = Instant.MIN;

    private String cachedNeutralBody = null;
    private Instant neutralFetchTime = Instant.MIN;


    public String temp() throws IOException, InterruptedException { //returns same ids for every ruleset :O.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://us.api.blizzard.com/data/wow/ruleset/index"))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    private String auctionGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Battlenet-Namespace", "dynamic-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String staticGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Battlenet-Namespace", "static-forever-us")
                .header("Authorization", "Bearer " + die)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    //Auction House IDs are unknown NOT 111 or 117. They are just what showed up in the API.
    private synchronized String getAuctions() throws IOException, InterruptedException {
        if (cachedBody == null || Duration.between(fetchTime, Instant.now()).compareTo(CACHE_TTL) > 0) {
            cachedBody = auctionGet("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/111");
        }
        return cachedBody;
    }
    private synchronized String getCommodityAuctions() throws IOException, InterruptedException {
        if (cachedCommodityBody == null || Duration.between(commodityFetchTime, Instant.now()).compareTo(CACHE_TTL) > 0) {
            cachedCommodityBody = auctionGet("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/111/commodities");
        }
        return cachedCommodityBody;
    }
    public String getNeutralAuctions() throws IOException, InterruptedException {
        if (cachedNeutralBody == null || Duration.between(neutralFetchTime, Instant.now()).compareTo(CACHE_TTL) > 0) {
            cachedNeutralBody = auctionGet("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/117");
        }
        return cachedNeutralBody;
    }
    public String getNeutralCommodityAuctions() throws IOException, InterruptedException {
        if (cachedNeutralCommodityBody == null || Duration.between(neutralCommodityFetchTime, Instant.now()).compareTo(CACHE_TTL) > 0) {
            cachedNeutralCommodityBody = auctionGet("https://us.api.blizzard.com/data/wow/ruleset/2/auctions/111/commodities");
        }
        return cachedNeutralCommodityBody;
    }

    private JsonNode ItemSearch(String item) throws IOException, InterruptedException {
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
        if (results != null && results.isArray() && !results.isEmpty()) {
            return results.get(0).get("data");
        }
        return null;
    }
    public int getItemID(String item) throws IOException, InterruptedException {
        JsonNode results = ItemSearch(item);
        int id = -1;
        return (results != null && results.has("id")) ? results.get("id").asInt() : -1;
    }
    record itemData(String name, String icon, String quality) {}
    public itemData getItemName(int itemID) throws IOException, InterruptedException {
        String encodedName = URLEncoder.encode(String.valueOf(itemID), StandardCharsets.UTF_8);
        JsonNode rootNode = objectMapper.readTree(staticGet(String.format(
                "https://us.api.blizzard.com/data/wow/item/%s?locale=en_US", encodedName)));
        JsonNode results = rootNode;
        String name = (results != null && results.has("name") && !results.isEmpty()) ? results.get("name").asText() : "";
        String quality = (results != null && results.has("quality") && !results.isEmpty()) ? results.get("quality").get("name").asText() : "";
        rootNode = objectMapper.readTree(staticGet(String.format(
                "https://us.api.blizzard.com/data/wow/media/item/%s?locale=en_US", encodedName)));
        results = rootNode;
        String icon = (results != null && results.has("assets") && !results.isEmpty()) ? results.get("assets").get(0).get("value").asText() : "";
        return new itemData(name, icon, quality);
    }


    public record Prices(Double allyPrice, int allyquant, Double nPrice, int nquant, boolean commodity, int itemID) {}

    private record AuctionData(long lowestPrice, int bestQuantity) {}

    private AuctionData getLowestPrice(String myJson, int itemID, boolean commodity) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(myJson);
        JsonNode auctions = rootNode.get("auctions");
        long lowestPrice = Long.MAX_VALUE;
        String priceField = commodity ? "unit_price" : "buyout";
        int bestQuantity = 0;
        for (JsonNode auction : auctions) {
            JsonNode itemNode = auction.get("item");
            if (itemNode != null && itemNode.get("id").asInt() == itemID) {
                long price = auction.get(priceField).asLong();
                if (price < lowestPrice) {
                    lowestPrice = price;
                    bestQuantity = auction.get("quantity").asInt();
                }
            }
        }
        return new AuctionData(lowestPrice, bestQuantity);
    }

    public Prices getPrice(String name) throws IOException, InterruptedException {
        int itemID = getItemID(name);
        if (itemID == -1) return new Prices(-1.0,-1,-1.0,-1, true, itemID);

        AuctionData commodityAuctions = getLowestPrice(getCommodityAuctions(), itemID, true);
        if (commodityAuctions.lowestPrice != Long.MAX_VALUE) { //Will probably be a commodity if it's on the city AH
            AuctionData NeutralCommodityAuctions = getLowestPrice(getNeutralCommodityAuctions(), itemID, true);
            return new Prices(commodityAuctions.lowestPrice / 10000.0,
                    commodityAuctions.bestQuantity,
                    NeutralCommodityAuctions.lowestPrice / 10000.0,
                    NeutralCommodityAuctions.bestQuantity,
                    true,
                    itemID);
        }
        AuctionData auctions = getLowestPrice(getAuctions(), itemID, false);
        AuctionData neutralAuctions = getLowestPrice(getNeutralAuctions(), itemID, false);

        return new Prices(auctions.lowestPrice / 10000.0,
                auctions.bestQuantity,
                neutralAuctions.lowestPrice / 10000.0,
                neutralAuctions.bestQuantity,
                false,
                itemID);
    }

    public wowapi() throws IOException, InterruptedException {
    }
}
