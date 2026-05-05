package com.mtg.service.meta;

public class MetaCard {
    private String name;
    private double inclusion;
    private String category;
    private Double avgCmc;

    public MetaCard() {}

    public MetaCard(String name, double inclusion, String category, Double avgCmc) {
        this.name = name;
        this.inclusion = inclusion;
        this.category = category;
        this.avgCmc = avgCmc;
    }

    public String getName() { return name; }
    public double getInclusion() { return inclusion; }
    public String getCategory() { return category; }
    public Double getAvgCmc() { return avgCmc; }
}
