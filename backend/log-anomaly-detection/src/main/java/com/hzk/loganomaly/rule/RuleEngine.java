package com.hzk.loganomaly.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleEngine {
    private final List<Rule> rules;

    public List<RuleHit> evaluate(IpFeature ipFeature) {
        return rules.stream()
                    .filter(rule -> rule.match(ipFeature))
                    .map(rule -> new RuleHit(
                            rule.type(),
                            rule.describe(ipFeature)
                    ))
                    .toList();
    }
}
