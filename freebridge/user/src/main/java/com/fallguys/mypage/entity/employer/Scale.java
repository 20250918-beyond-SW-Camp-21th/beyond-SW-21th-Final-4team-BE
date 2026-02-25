package com.fallguys.mypage.entity.employer;

public enum Scale {
    S1_4("1-4명"),
    S5_9("5-9명"),
    S10_29("10-29명"),
    S30_99("30-99명"),
    S100_299("100-299명"),
    S300_999("300-999명"),
    S1000_PLUS("1000명+");

    private final String label;

    Scale(String label) { this.label = label; }
    public String label() { return label; }
}