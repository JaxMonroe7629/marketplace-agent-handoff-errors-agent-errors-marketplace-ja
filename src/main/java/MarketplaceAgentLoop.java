import java.util.Objects;

/** A compact, Spring-style application service for a course marketplace. */
public final class MarketplaceAgentLoop {
    private final InfraiErrorClient errors;

    public MarketplaceAgentLoop(InfraiErrorClient errors) {
        this.errors = Objects.requireNonNull(errors);
    }

    public OrderHandoff applyBuyerUpdate(SellerAsset asset, BuyerUpdate update) {
        if (asset.courseTitle().isBlank() || update.deliveryAddress().isBlank()) {
            return OrderHandoff.pending(update.orderId());
        }
        return OrderHandoff.ready(update.orderId());
    }

    public OrderHandoff runAgentStep(SellerAsset asset, BuyerUpdate update, AgentStep step) {
        try {
            step.run();
            return applyBuyerUpdate(asset, update);
        } catch (RuntimeException exception) {
            errors.capture(exception);
            return OrderHandoff.pending(update.orderId());
        }
    }

    public static void main(String[] args) {
        MarketplaceConfiguration configuration = MarketplaceConfiguration.fromEnvironment();
        MarketplaceAgentLoop service = new MarketplaceAgentLoop(InfraiErrorClient.from(configuration));
        SellerAsset asset = new SellerAsset("course-asset-7", "Practical Algebra");
        BuyerUpdate update = new BuyerUpdate("course-order-42", "14 Study Lane");
        OrderHandoff handoff = service.runAgentStep(asset, update, () -> { });
        System.out.println(handoff.ready()
                ? "Handoff ready for order " + handoff.orderId()
                : "Handoff pending for order " + handoff.orderId());
        OrderHandoff failedHandoff = service.runAgentStep(asset, update, () -> {
            throw new IllegalStateException("Marketplace agent step failed");
        });
        System.out.println(failedHandoff.ready()
                ? "Handoff ready for order " + failedHandoff.orderId()
                : "Handoff pending for order " + failedHandoff.orderId());
    }

    @FunctionalInterface
    public interface AgentStep {
        void run();
    }

    public record SellerAsset(String assetId, String courseTitle) { }
    public record BuyerUpdate(String orderId, String deliveryAddress) { }
    public record OrderHandoff(String orderId, boolean ready) {
        static OrderHandoff pending(String orderId) { return new OrderHandoff(orderId, false); }
        static OrderHandoff ready(String orderId) { return new OrderHandoff(orderId, true); }
    }
}
