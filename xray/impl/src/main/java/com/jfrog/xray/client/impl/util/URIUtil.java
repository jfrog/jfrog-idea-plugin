/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//httpclient/src/java/org/apache/commons/httpclient/util/URIUtil.java,v 1.27 2004/05/05 20:34:01 olegk Exp $
 * $Revision: 507321 $
 * $Date: 2007-02-14 01:10:51 +0100 (Wed, 14 Feb 2007) $
 *
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
/*
 * Based on apache-httpclient 3.1 org.apache.commons.httpclient.util.URIUtil.java
 * Additional contributors:
 *    JFrog Ltd.
 */
package com.jfrog.xray.client.impl.util;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.http.HttpException;
import org.apache.http.util.EncodingUtils;

import java.util.BitSet;

/**
 * The URI escape and character encoding and decoding utility.
 * It's compatible with {org.apache.commons.httpclient.HttpURL} rather
 * than {org.apache.commons.httpclient.URI}.
 *
 * @author <a href="mailto:jericho@apache.org">Sung-Gu</a>
 * @version $Revision: 507321 $ $Date: 2002/03/14 15:14:01
 */
public class URIUtil {

    private static final String UTF8_CHARSET_NAME = "UTF-8";

    public static String concatUrl(String url1, String url2) {
        if (url1.endsWith("/") && url2.startsWith("/")) {
            return url1 + url2.substring(1);
        }
        if (url1.endsWith("/") || url2.startsWith("/")) {
            return url1 + url2;
        }
        return url1 + "/" + url2;
    }

    /**
     * Escape and encode a string regarded as the query component of an URI with
     * the default protocol charset.
     * When a query string is not misunderstood the reserved special characters
     * ("&amp;", "=", "+", ",", and "$") within a query component, this method
     * is recommended to use in encoding the whole query.
     *
     * @param unescaped an unescaped string
     * @return the escaped string
     * @throws HttpException if the default protocol charset is not supported
     * @see #encode
     */
    public static String encodeQuery(String unescaped) throws HttpException {
        return encodeQuery(unescaped, UTF8_CHARSET_NAME);
    }


    /**
     * Escape and encode a string regarded as the query component of an URI with
     * a given charset.
     * When a query string is not misunderstood the reserved special characters
     * ("&amp;", "=", "+", ",", and "$") within a query component, this method
     * is recommended to use in encoding the whole query.
     *
     * @param unescaped an unescaped string
     * @param charset   the charset
     * @return the escaped string
     * @throws HttpException if the charset is not supported
     * @see #encode
     */
    public static String encodeQuery(String unescaped, String charset)
            throws HttpException {

        return encode(unescaped, URI.allowed_query, charset);
    }


    /**
     * Escape and encode a given string with allowed characters not to be
     * escaped and the default protocol charset.
     *
     * @param unescaped a string
     * @param allowed   allowed characters not to be escaped
     * @return the escaped string
     * @throws HttpException if the default protocol charset is not supported
     */
    public static String encode(String unescaped, BitSet allowed)
            throws HttpException {

        return encode(unescaped, allowed, UTF8_CHARSET_NAME);
    }


    /**
     * Escape and encode a given string with allowed characters not to be
     * escaped and a given charset.
     *
     * @param unescaped a string
     * @param allowed   allowed characters not to be escaped
     * @param charset   the charset
     * @return the escaped string
     */
    public static String encode(String unescaped, BitSet allowed,
                                String charset) throws HttpException {
        byte[] rawdata = URLCodec.encodeUrl(allowed,
                EncodingUtils.getBytes(unescaped, charset));
        return EncodingUtils.getAsciiString(rawdata);
    }


    /**
     * Unescape and decode a given string regarded as an escaped string with the
     * default protocol charset.
     *
     * @param escaped a string
     * @return the unescaped string
     * @throws HttpException if the string cannot be decoded (invalid)
     */
    public static String decode(String escaped) throws HttpException {
        try {
            byte[] rawdata = URLCodec.decodeUrl(EncodingUtils.getAsciiBytes(escaped));
            return EncodingUtils.getString(rawdata, UTF8_CHARSET_NAME);
        } catch (DecoderException e) {
            throw new HttpException(e.getMessage());
        }
    }


    /**
     * Unescape and decode a given string regarded as an escaped string.
     *
     * @param escaped a string
     * @param charset the charset
     * @return the unescaped string
     * @throws HttpException if the charset is not supported
     */
    public static String decode(String escaped, String charset)
            throws HttpException {

        return URI.decode(escaped.toCharArray(), charset);
    }
}

