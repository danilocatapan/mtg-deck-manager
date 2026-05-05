package com.mtg.service.meta;

public class MetaCard {
    private String name;
    private double inclusion;
    private String role;
    private Double avgCmc;

    public MetaCard() {}

    public MetaCard(String name, double inclusion, String role, Double avgCmc) {
        this.name = name;
        this.inclusion = inclusion;
        this.role = role;
        this.avgCmc = avgCmc;
    }

    public String getName() { return name; }
    public double getInclusion() { return inclusion; }
    public String getRole() { return role; }
    public Double getAvgCmc() { return avgCmc; }
}
