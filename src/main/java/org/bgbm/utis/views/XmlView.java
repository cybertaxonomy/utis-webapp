// $Id$
/**
* Copyright (C) 2014 EDIT
* European Distributed Institute of Taxonomy
* http://www.e-taxonomy.eu
*
* The contents of this file are subject to the Mozilla Public License Version 1.1
* See LICENSE.TXT at the top of this package for the full license terms.
*/
package org.bgbm.utis.views;

import java.io.Writer;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bgbm.biovel.drf.tnr.msg.TnrMsg;
import org.bgbm.biovel.drf.utils.TnrMsgUtils;
import org.springframework.web.servlet.View;

/**
 * @author a.kohlbecker
 * @date Jun 27, 2014
 *
 */
public class XmlView implements View {

    /* (non-Javadoc)
     * @see org.springframework.web.servlet.View#getContentType()
     */
    @Override
    public String getContentType() {
        return "application/xml";
    }

    /* (non-Javadoc)
     * @see org.springframework.web.servlet.View#render(java.util.Map, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType(getContentType());
        response.setCharacterEncoding("UTF-8");
        Writer out = response.getWriter();
        if(model.containsKey("tnrMsg")){
           out.append(TnrMsgUtils.convertTnrMsgToXML((TnrMsg) model.get("tnrMsg")));
        }
        out.flush();

    }

}
