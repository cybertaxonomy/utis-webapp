/**
 *
 */
package org.bgbm.utis.jackson;

import org.cybertaxonomy.utis.tnr.msg.Classification;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This mixin class can only be used for serialization.
 *
 *  Only method (and field) name and signature are used for matching
 *  annotations: access definitions (private etc) and method
 *  implementations are ignored. (hint: if you can, it often makes sense
 *  to define mix-in class as an [abstract?] sub-class of target class,
 *  and use @Override JDK annotation to ensure method name and signature
 *  match!)
 *
 *  see
 *  http://www.cowtowncoder.com/blog/archives/2009/08/entry_305.html
 *
 *  @author a.kohlbecker
 *
 */
public abstract class ClassificationMixIn extends Classification {

    @Override
    @JsonProperty("class")
    abstract public String getClazz(); // rename property

}
