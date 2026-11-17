package com.hzk.loganomaly.rule;

public interface Rule {
    String type();
    boolean match(IpFeature ipFeature);
    String describe(IpFeature ipFeature);
}
