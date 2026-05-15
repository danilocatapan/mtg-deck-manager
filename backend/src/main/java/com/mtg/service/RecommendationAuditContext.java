package com.mtg.service;

import jakarta.enterprise.context.RequestScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestScoped
public class RecommendationAuditContext {
    private final List<Map<String, Object>> blockedPairs = new ArrayList<>();
    private final List<Map<String, Object>> protectedCuts = new ArrayList<>();

    public void reset() {
        blockedPairs.clear();
        protectedCuts.clear();
    }

    public void recordBlockedPair(String add, String addRole, String cut, String cutRole, String reason) {
        if (blockedPairs.size() >= 50) {
            return;
        }
        blockedPairs.add(Map.of(
                "add", safe(add),
                "addRole", safe(addRole),
                "cut", safe(cut),
                "cutRole", safe(cutRole),
                "reason", safe(reason)
        ));
    }

    public void recordProtectedCut(String card, String role, String reason, double strategicValue, double synergy) {
        if (protectedCuts.size() >= 50) {
            return;
        }
        protectedCuts.add(Map.of(
                "card", safe(card),
                "role", safe(role),
                "reason", safe(reason),
                "strategicValue", strategicValue,
                "synergy", synergy
        ));
    }

    public List<Map<String, Object>> blockedPairs() {
        return List.copyOf(blockedPairs);
    }

    public List<Map<String, Object>> protectedCuts() {
        return List.copyOf(protectedCuts);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
