package com.jfrog.ide.idea.scan.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

@JsonPropertyOrder({"driver"})
public class Tool {

    @JsonProperty("driver")
    private Driver driver;

    @SuppressWarnings("UnusedReturnValue")
    public Driver getDriver() {
        return driver;
    }

    @SuppressWarnings("UnusedReturnValue")
    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Tool)) {
            return false;
        }
        Tool rhs = ((Tool) other);
        return (Objects.equals(this.driver, rhs.driver));
    }

}
