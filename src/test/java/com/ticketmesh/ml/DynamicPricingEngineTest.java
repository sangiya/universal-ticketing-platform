package com.ticketmesh.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicPricingEngineTest {

    private DynamicPricingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DynamicPricingEngine();
    }

    @Test
    void ampleCapacityWithNoDemandStaysAtBaseline() {
        double rate = engine.surgeRate(0, 100, 100, 0.0);

        assertEquals(1.0, rate, 1e-9);
    }

    @Test
    void scarcityAndDemandRaiseTheSurgeMultiplier() {
        double scarce = engine.surgeRate(80, 5, 100, 0.3);
        double ample = engine.surgeRate(80, 80, 100, 0.3);

        assertTrue(scarce > ample, "low remaining capacity must surge harder");
        assertTrue(ample > 1.0);
    }

    @Test
    void surgeNeverExceedsMaxMultiplierGuardrail() {
        double rate = engine.surgeRate(100, 0, 100, 1.0);

        assertTrue(rate >= 1.0);
        assertTrue(rate <= DynamicPricingEngine.MAX_MULTIPLIER + 1e-9);
        assertEquals(DynamicPricingEngine.MAX_MULTIPLIER, rate, 1e-9);
    }

    @Test
    void soldOutHighDemandTriggersGuardrailAndCapsPrice() {
        DynamicPriceProjection projection =
                engine.projectedPrice(new BigDecimal("1000.00"), 100, 0, 100);

        assertTrue(projection.guardrailActive());
        assertEquals(1.6, projection.surgeRate(), 1e-9);
        assertEquals(new BigDecimal("1600.00"), projection.projectedPrice());
    }

    @Test
    void healthyCapacityKeepsGuardrailInactive() {
        DynamicPriceProjection projection =
                engine.projectedPrice(new BigDecimal("1000.00"), 40, 70, 100);

        assertFalse(projection.guardrailActive());
        assertEquals(new BigDecimal("1000.00"), projection.basePrice());
        assertTrue(projection.surgeRate() > 1.0);
        assertTrue(projection.projectedPrice().compareTo(projection.basePrice()) > 0);
    }

    @Test
    void projectedPriceScalesBaseBySurgeWithTwoDecimalScale() {
        DynamicPriceProjection projection =
                engine.projectedPrice(new BigDecimal("1000.00"), 40, 70, 100);

        BigDecimal expected = projection.basePrice()
                .multiply(BigDecimal.valueOf(projection.surgeRate()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(projection.projectedPrice(), expected);
        assertEquals(2, projection.projectedPrice().scale());
    }

    @Test
    void nullBasePriceResolvesToZeroAndStaysDeterministic() {
        DynamicPriceProjection first = engine.projectedPrice(null, 0, 100, 100);
        DynamicPriceProjection second = engine.projectedPrice(null, 0, 100, 100);

        assertEquals(new BigDecimal("0.00"), first.basePrice());
        assertEquals(new BigDecimal("0.00"), first.projectedPrice());
        assertEquals(first.surgeRate(), second.surgeRate(), 1e-9);
        assertFalse(first.guardrailActive());
    }

    @Test
    void demandScoreIsClampedIntoSupportedRange() {
        double negative = engine.surgeRate(-50, 50, 100, 0.0);
        double high = engine.surgeRate(500, 50, 100, 0.0);

        assertTrue(negative == engine.surgeRate(0, 50, 100, 0.0));
        assertTrue(high == engine.surgeRate(100, 50, 100, 0.0));
    }
}