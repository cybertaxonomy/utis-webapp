// $Id$
/**
* Copyright (C) 2016 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author a.kohlbecker
 * @date Mar 15, 2016
 *
 */
@ResponseStatus(value=HttpStatus.BAD_REQUEST, reason="Query string too short.")  // 400
public class MinQueryStringException extends RuntimeException {

}
