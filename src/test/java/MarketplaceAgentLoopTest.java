import java.net.http.HttpClient;

public final class MarketplaceAgentLoopTest {
    public static void main(String[] args) {
        MarketplaceAgentLoop loop = new MarketplaceAgentLoop(
                new InfraiErrorClient(HttpClient.newHttpClient(), System.getenv("INFRAI_API_KEY")));
        MarketplaceAgentLoop.SellerAsset asset = new MarketplaceAgentLoop.SellerAsset("course-asset-7", "Practical Algebra");
        MarketplaceAgentLoop.OrderHandoff pending = loop.applyBuyerUpdate(asset,
                new MarketplaceAgentLoop.BuyerUpdate("course-order-42", ""));
        MarketplaceAgentLoop.OrderHandoff ready = loop.applyBuyerUpdate(asset,
                new MarketplaceAgentLoop.BuyerUpdate("course-order-42", "14 Study Lane"));
        if (pending.ready() || !ready.ready()) {
            throw new AssertionError("A delivery address must control the handoff decision");
        }
        System.out.println("Marketplace handoff decision verified");
    }
}
