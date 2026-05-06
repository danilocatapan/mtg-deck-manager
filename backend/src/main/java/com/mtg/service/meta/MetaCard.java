package com.mtg.service.meta;

import java.util.List;

public class MetaCard {
    private String name;
    private double inclusion;
    private String role;
    private Double avgCmc;
    private double bracketWeight = 1.0;
    private double performanceWeight = 0.0;
    private List<String> synergyTags = List.of();
    private String source = "LOCAL";

    public MetaCard() {}

    public MetaCard(String name, double inclusion, String role, Double avgCmc) {
        this.name = name;
        this.inclusion = inclusion;
        this.role = role;
        this.avgCmc = avgCmc;
    }

    public MetaCard(String name, double inclusion, String role, Double avgCmc, double bracketWeight, double performanceWeight, List<String> synergyTags, String source) {
        this.name = name;
        this.inclusion = inclusion;
        this.role = role;
        this.avgCmc = avgCmc;
        this.bracketWeight = bracketWeight;
        this.performanceWeight = performanceWeight;
        this.synergyTags = synergyTags == null ? List.of() : List.copyOf(synergyTags);
        this.source = source == null || source.isBlank() ? "LOCAL" : source;
    }

    public String getName() { return name; }
    public double getInclusion() { return inclusion; }
    public String getRole() { return role; }
    public Double getAvgCmc() { return avgCmc; }
    public double getBracketWeight() { return bracketWeight; }
    public double getPerformanceWeight() { return performanceWeight; }
    public List<String> getSynergyTags() { return synergyTags == null ? List.of() : synergyTags; }
    public String getSource() { return source == null || source.isBlank() ? "LOCAL" : source; }

    public MetaCard withBracketWeight(double weight, double performanceWeight, String source) {
        return new MetaCard(name, inclusion, role, avgCmc, weight, performanceWeight, getSynergyTags(), source);
    }
}
