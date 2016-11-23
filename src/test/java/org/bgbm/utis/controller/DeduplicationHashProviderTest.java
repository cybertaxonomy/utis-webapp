/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.controller;

import org.cybertaxonomy.utis.tnr.msg.Taxon;
import org.cybertaxonomy.utis.tnr.msg.TaxonName;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author a.kohlbecker
 * @date Oct 20, 2016
 *
 */
public class DeduplicationHashProviderTest extends Assert {


    protected Taxon makeTaxon(String name, String id) {
        Taxon t1 = new Taxon();
        TaxonName tn1 = new TaxonName();
        tn1.setScientificName(name);
        t1.setTaxonName(tn1);
        t1.setIdentifier(id);
        return t1;
    }


    @Test
    public void idTest() {

        DeduplicationHashProvider hp = DeduplicationHashProvider.id;

        Taxon t1 = makeTaxon("Lapsana communis", "12345");
        Taxon t2 = makeTaxon("Lapsana L.", "9876");
        Taxon t3 = makeTaxon("Lapsana communis L.", "12345");

        String hash1 = hp.hash(t1);
        String hash2 = hp.hash(t2);
        String hash3 = hp.hash(t3);

        assertTrue(hash1.equals(hash3));
        assertTrue(!hash1.equals(hash2));
    }

    @Test
    public void nameTest() {

        DeduplicationHashProvider hp = DeduplicationHashProvider.name;

        Taxon t1 = makeTaxon("Lapsana communis", "12345");
        Taxon t2 = makeTaxon("Lapsana L.", "9876");
        Taxon t3 = makeTaxon("Lapsana communis", "456436");

        String hash1 = hp.hash(t1);
        String hash2 = hp.hash(t2);
        String hash3 = hp.hash(t3);

        assertTrue(hash1.equals(hash3));
        assertTrue(!hash1.equals(hash2));
    }

    @Test
    public void name_idTest() {

        DeduplicationHashProvider hp = DeduplicationHashProvider.name_id;

        Taxon t1 = makeTaxon("Lapsana communis", "12345");
        Taxon t2 = makeTaxon("Lapsana L.", "9876");
        Taxon t3 = makeTaxon("Lapsana communis L.", "12345");
        Taxon t4 = makeTaxon("Lapsana communis", "12345");

        String hash1 = hp.hash(t1);
        String hash2 = hp.hash(t2);
        String hash3 = hp.hash(t3);
        String hash4 = hp.hash(t4);

        assertTrue(hash1.equals(hash4));
        assertTrue(!hash1.equals(hash2));
        assertTrue(!hash1.equals(hash3));
    }


}
