package com.jfrog.ide.idea.configuration;

/**
 * @author yahavi
 **/
@Deprecated
public class XrayServerConfigImpl extends ServerConfigImpl {

    public XrayServerConfigImpl() {
    }

    public XrayServerConfigImpl(Builder builder) {
        super(builder);
    }

    public static class Builder extends ServerConfigImpl.Builder {
        public XrayServerConfigImpl build() {
            return new XrayServerConfigImpl(this);
        }
    }

}
