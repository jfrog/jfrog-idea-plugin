package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

public class Driver {

    @JsonProperty("name")
    private String name;

    @JsonProperty("rules")
    private List<Rule> rules;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @SuppressWarnings({"unused"})
    public  List<Rule> getRules() {
        return rules;
    }

    @SuppressWarnings({"unused"})
    public void setLocations( List<Rule> rules) {
        this.rules = rules;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, rules);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Driver)) {
            return false;
        }
        Driver rhs = ((Driver) other);
        return (Objects.equals(this.name, rhs.name) && (CollectionUtils.isEqualCollection(this.rules, rhs.rules)));
    }

    public Rule getRuleById(String ruleId) throws IndexOutOfBoundsException {
        return rules.stream()
                .filter(rule -> rule.getId().equals(ruleId))
                .findFirst()
                .orElseThrow(() -> new IndexOutOfBoundsException("Rule not found"));
    }


}
