/** Configuration layer: deployment values enter the service here. */
public record MarketplaceConfiguration(String infraiApiKey) {
    public static MarketplaceConfiguration fromEnvironment() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running the marketplace loop");
        }
        return new MarketplaceConfiguration(apiKey);
    }
}
