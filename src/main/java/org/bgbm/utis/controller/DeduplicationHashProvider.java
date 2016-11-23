/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.controller;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;

import org.cybertaxonomy.utis.tnr.msg.Taxon;

/**
 * Creates hashes bases on the SHA-1 algorithm from a given taxon instance.
 *
 * @author a.kohlbecker
 * @date Oct 20, 2016
 *
 */
public enum DeduplicationHashProvider {
    id,
    name_id,
    name;

    private final static EnumSet<DeduplicationHashProvider> checkID;
    private final static EnumSet<DeduplicationHashProvider> checkScientificName;

    static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    static {
        checkID = EnumSet.of(id,  name_id);
        checkScientificName = EnumSet.of(name_id, name);
    }

    public String hash(Taxon t){

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if( t == null) {
            throw new NullPointerException();
        }

        StringBuilder hash = new StringBuilder();

        if(checkID.contains(this)) {
            digest.update(t.getIdentifier().getBytes());
            hash.append(toHexString(digest.digest()));
        }

        if(checkScientificName.contains(this)) {
            digest.update(t.getTaxonName().getScientificName().getBytes());
            hash.append(toHexString(digest.digest()));
        }

        return hash.toString();
    }

    private String toHexString(byte[] hash) {

        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            sb.append(HEX_CHARS[(b & 0xF0) >> 4]);
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        String hex = sb.toString();
        return hex;
    }


}
