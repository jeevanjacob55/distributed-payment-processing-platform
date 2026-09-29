package com.paymentplatform.fraud.api;

import com.paymentplatform.fraud.service.FraudEvaluationService;
import com.paymentplatform.fraud.service.FraudRuleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/fraud")
public class FraudController {
    private final FraudRuleService ruleService;
    private final FraudEvaluationService evaluationService;

    public FraudController(FraudRuleService ruleService, FraudEvaluationService evaluationService) {
        this.ruleService = ruleService;
        this.evaluationService = evaluationService;
    }

    @GetMapping("/rules")
    public List<FraudRuleResponse> listRules() {
        return ruleService.list();
    }

    @PutMapping("/rules/{code}")
    public FraudRuleResponse upsertRule(
            @PathVariable @Pattern(regexp = "[A-Z0-9_.-]{2,64}") String code,
            @Valid @RequestBody UpsertFraudRuleRequest request) {
        return ruleService.upsert(code, request);
    }

    @DeleteMapping("/rules/{code}")
    public void deleteRule(@PathVariable @Pattern(regexp = "[A-Z0-9_.-]{2,64}") String code) {
        ruleService.delete(code);
    }

    @PostMapping("/evaluate")
    public FraudEvaluationResponse evaluate(@Valid @RequestBody FraudEvaluationRequest request) {
        return evaluationService.evaluate(request);
    }
}
