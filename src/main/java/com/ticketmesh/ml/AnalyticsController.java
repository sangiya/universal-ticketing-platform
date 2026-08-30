package com.ticketmesh.ml;

import com.ticketmesh.model.Tenant;
import com.ticketmesh.model.User;
import com.ticketmesh.repository.TenantRepository;
import com.ticketmesh.repository.UserRepository;
import com.ticketmesh.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final MlSuiteService mlSuiteService;
    private final AiDataAnalystService analystService;
    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    public AnalyticsController(MlSuiteService mlSuiteService,
                               AiDataAnalystService analystService,
                               CurrentUser currentUser,
                               UserRepository userRepository,
                               TenantRepository tenantRepository) {
        this.mlSuiteService = mlSuiteService;
        this.analystService = analystService;
        this.currentUser = currentUser;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/forecast/{productId}")
    public DemandForecast forecast(@PathVariable("productId") Long productId,
                                   @RequestParam(name = "horizonDays", defaultValue = "7") int horizonDays) {
        return mlSuiteService.demandForecast(productId, horizonDays);
    }

    @GetMapping("/recommend")
    public List<Recommendation> recommend(@RequestParam(name = "limit", defaultValue = "5") int limit) {
        User user = resolveUser();
        if (user == null || user.getId() == null) {
            return List.of();
        }
        return mlSuiteService.recommend(user.getId(), resolveTenantId(null), limit);
    }

    @GetMapping("/price/{productId}")
    public PricePrediction price(@PathVariable("productId") Long productId,
                                 @RequestParam(name = "horizonDays", defaultValue = "7") int horizonDays) {
        return mlSuiteService.predictPrice(productId, horizonDays);
    }

    @GetMapping("/trend")
    public TrendReport trend(@RequestParam(name = "tenantId", required = false) Long tenantId) {
        return mlSuiteService.trendReport(resolveTenantId(tenantId));
    }

    @PostMapping("/ask")
    public AnalystAnswer ask(@RequestBody AnalystQuestionRequest request) {
        return analystService.answer(request.question(), resolveTenantId(null));
    }

    @GetMapping("/anomaly")
    public Map<String, Object> anomaly(@RequestParam(name = "amount") BigDecimal amount) {
        double score = mlSuiteService.anomalyScore(amount);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("anomalyScore", score);
        body.put("riskLevel", score >= 75 ? "HIGH" : score >= 50 ? "MEDIUM" : "LOW");
        return body;
    }

    private User resolveUser() {
        try {
            return userRepository.findByUsername(currentUser.username()).orElse(null);
        } catch (RuntimeException ex) {
            log.debug("No authenticated user for analytics request");
            return null;
        }
    }

    private Long resolveTenantId(Long param) {
        if (param != null) {
            return param;
        }
        User user = resolveUser();
        if (user != null && user.getTenantId() != null) {
            return user.getTenantId();
        }
        Tenant first = tenantRepository.findAll().stream()
                .filter(Tenant::isEnabled)
                .min(Comparator.comparing(Tenant::getId))
                .orElse(null);
        return first == null ? null : first.getId();
    }
}