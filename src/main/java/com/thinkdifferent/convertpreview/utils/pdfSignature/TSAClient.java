/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thinkdifferent.convertpreview.utils.pdfSignature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.tsp.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Time Stamping Authority (TSA) Client [RFC 3161].
 * @author Vakhtang Koroghlishvili
 * @author John Hewson
 */
public class TSAClient
{
    private static final Log LOG = LogFactory.getLog(TSAClient.class);

    private final URL url;
    private final String username;
    private final String password;
    private final MessageDigest digest;

    // SecureRandom.getInstanceStrong() would be better, but sometimes blocks on Linux
    private static final Random RANDOM = new SecureRandom();

    /**
     *
     * @param url the URL of the TSA service
     * @param username user pdfSignName of TSA
     * @param password pdfPassword of TSA
     * @param digest the message digest to use
     */
    public TSAClient(URL url, String username, String password, MessageDigest digest)
    {
        this.url = url;
        this.username = username;
        this.password = password;
        this.digest = digest;
    }

    /**
     *
     * @param content
     * @return the time stamp token
     * @throws IOException if there was an error with the connection or data from the TSA server,
     *                     or if the time stamp response could not be validated
     */
    public TimeStampToken getTimeStampToken(byte[] content) throws IOException
    {
        digest.reset();
        byte[] hash = digest.digest(content);

        // 32-bit cryptographic nonce
        int nonce = RANDOM.nextInt();

        // generate TSA request
        TimeStampRequestGenerator tsaGenerator = new TimeStampRequestGenerator();
        tsaGenerator.setCertReq(true);
        ASN1ObjectIdentifier oid = getHashObjectIdentifier(digest.getAlgorithm());
        TimeStampRequest request = tsaGenerator.generate(oid, hash, BigInteger.valueOf(nonce));

        // get TSA response
        byte[] tsaResponse = getTSAResponse(request.getEncoded());

        TimeStampResponse response;
        try
        {
            response = new TimeStampResponse(tsaResponse);
            response.validate(request);
        }
        catch (TSPException e)
        {
            throw new IOException(e);
        }

        TimeStampToken timeStampToken = response.getTimeStampToken();
        if (timeStampToken == null)
        {
            // https://www.ietf.org/rfc/rfc3161.html#section-2.4.2
            throw new IOException("Response from " + url +
                    " does not have a time stamp token, status: " + response.getStatus() +
                    " (" + response.getStatusString() + ")");
        }

        return timeStampToken;
    }

    // gets response data for the given encoded TimeStampRequest data
    // throws IOException if a connection to the TSA cannot be established
    private byte[] getTSAResponse(byte[] request) throws IOException
    {
        LOG.debug("Opening connection to TSA server");

        // todo: support proxy servers
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "application/timestamp-query");

        LOG.debug("Established connection to TSA server");

        if (username != null && password != null && !username.isEmpty() && !password.isEmpty())
        {
            // See https://stackoverflow.com/questions/12732422/ (needs jdk8)
            // or see implementation in 3.0
            throw new UnsupportedOperationException("authentication not implemented yet");
        }

        // read response
        OutputStream output = null;
        try
        {
            output = connection.getOutputStream();
            output.write(request);
        }
        catch (IOException ex)
        {
            LOG.error("Exception when writing to " + this.url, ex);
            throw ex;
        }
        finally
        {
            IOUtils.closeQuietly(output);
        }

        LOG.debug("Waiting for response from TSA server");

        InputStream input = null;
        byte[] response;
        try
        {
            input = connection.getInputStream();
            response = IOUtils.toByteArray(input);
        }
        catch (IOException ex)
        {
            LOG.error("Exception when reading from " + this.url, ex);
            throw ex;
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }

        LOG.debug("Received response from TSA server");

        return response;
    }

    // returns the ASN.1 OID of the given hash algorithm
    private ASN1ObjectIdentifier getHashObjectIdentifier(String algorithm)
    {
        if (algorithm.equals("MD2"))
        {
            return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md2.getId());
        }
        else if (algorithm.equals("MD5"))
        {
            return new ASN1ObjectIdentifier(PKCSObjectIdentifiers.md5.getId());
        }
        else if (algorithm.equals("SHA-1"))
        {
            return new ASN1ObjectIdentifier(OIWObjectIdentifiers.idSHA1.getId());
        }
        else if (algorithm.equals("SHA-224"))
        {
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha224.getId());
        }
        else if (algorithm.equals("SHA-256"))
        {
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId());
        }
        else if (algorithm.equals("SHA-384"))
        {
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha384.getId());
        }
        else if (algorithm.equals("SHA-512"))
        {
            return new ASN1ObjectIdentifier(NISTObjectIdentifiers.id_sha512.getId());
        }
        else
        {
            return new ASN1ObjectIdentifier(algorithm);
        }
    }
}
