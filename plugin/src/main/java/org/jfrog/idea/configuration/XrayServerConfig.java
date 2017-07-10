/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.jfrog.idea.configuration;

import com.google.common.base.Objects;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.PasswordUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import com.intellij.util.xmlb.annotations.Tag;
import org.apache.commons.lang.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class XrayServerConfig {
    @OptionTag
    private String url;
    @OptionTag
    private String username;
    @Tag
    private String password;

    private XrayServerConfig() {
        // necessary for XML deserialization
    }

    private XrayServerConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
    }

    public boolean isEmptry() {
        return StringUtils.isEmpty(this.url) && StringUtils.isEmpty(this.username) && StringUtils.isEmpty(this.password);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof XrayServerConfig)) {
            return false;
        }
        XrayServerConfig other = (XrayServerConfig) o;

        return Comparing.equal(getUrl(), other.getUrl()) &&
                Comparing.equal(getPassword(), other.getPassword()) &&
                Comparing.equal(getUsername(), other.getUsername());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUrl(), getPassword(), getUsername());
    }

    @CheckForNull
    public String getUsername() {
        return StringUtil.trim(username);
    }

    public String getUrl() {
        return StringUtil.trim(url);
    }

    @CheckForNull
    public String getPassword() {
        if (password == null) {
            return null;
        }
        try {
            return PasswordUtil.decodePassword(password);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return url;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private String username;
        private String password;

        private Builder() {
            // no args
        }

        public XrayServerConfig build() {
            return new XrayServerConfig(this);
        }

        public Builder setUsername(@Nullable String username) {
            this.username = username;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setPassword(@Nullable String password) {
            if (password != null) {
                this.password = PasswordUtil.encodePassword(password);
            } else {
                this.password = "";
            }
            return this;
        }
    }
}
