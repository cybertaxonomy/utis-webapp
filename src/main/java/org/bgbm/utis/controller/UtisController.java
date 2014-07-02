// $Id$
/**
 * Copyright (C) 2014 EDIT
 * European Distributed Institute of Taxonomy
 * http://www.e-taxonomy.eu
 *
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * See LICENSE.TXT at the top of this package for the full license terms.
 */
package org.bgbm.utis.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bgbm.biovel.drf.checklist.BaseChecklistClient;
import org.bgbm.biovel.drf.checklist.BgbmEditClient;
import org.bgbm.biovel.drf.checklist.DRFChecklistException;
import org.bgbm.biovel.drf.checklist.PESIClient;
import org.bgbm.biovel.drf.rest.TaxoRESTClient.ServiceProviderInfo;
import org.bgbm.biovel.drf.tnr.msg.TnrMsg;
import org.bgbm.biovel.drf.utils.JSONUtils;
import org.bgbm.biovel.drf.utils.ServiceProviderInfoUtils;
import org.bgbm.biovel.drf.utils.TnrMsgUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * @author a.kohlbecker
 * @date Jun 27, 2014
 *
 */

@Controller
public class UtisController {

    private Map<String, ServiceProviderInfo> checklistInfoMap;

    private final List<ServiceProviderInfo> defaultProviders = new ArrayList<ServiceProviderInfo>();

    public UtisController() {
        initProviderMap();
    }


    /**
     *
     */
    private void initProviderMap() {
        this.checklistInfoMap = new HashMap<String, ServiceProviderInfo>();
        for(ServiceProviderInfo info : ServiceProviderInfoUtils.generateChecklistInfoList()){
            checklistInfoMap.put(info.getId(), info);
        }

        defaultProviders.add(checklistInfoMap.get(PESIClient.ID));
        defaultProviders.add(checklistInfoMap.get(BgbmEditClient.ID));
    }

    private BaseChecklistClient newClientFor(String id){
        if(id.equals(PESIClient.ID)){
            return new PESIClient();
        }

        if(id.equals(BgbmEditClient.ID)){
            try {
                return new BgbmEditClient(JSONUtils.convertObjectToJson(checklistInfoMap.get(BgbmEditClient.ID)));
            } catch (DRFChecklistException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.err.println("Unsupported Client ID");
        return null;
    }


    @RequestMapping(method={RequestMethod.GET}, value="/search")
    public @ResponseBody TnrMsg search(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "providers", required = false) String providers,
            HttpServletRequest request,
            HttpServletResponse response) throws DRFChecklistException, JsonGenerationException, JsonMappingException, IOException {

        List<String> nameCompleteList;

        List<ServiceProviderInfo> providerList = defaultProviders;
        if(providers != null){
            String[] providerIdTokens = providers.split(",");
            providerList = new ArrayList<ServiceProviderInfo>();
            for(String t : providerIdTokens){
                providerList.add(checklistInfoMap.get(t));
            }
        }

        if(query == null) {
            query = "Bellis perennis";
        }
        nameCompleteList = new ArrayList<String>();
        nameCompleteList.add(query);

        List<TnrMsg> accumulatedTnrMsgs = new ArrayList<TnrMsg>(providerList.size());

        List<TnrMsg> tnrMsgs = TnrMsgUtils.convertStringListToTnrMsgList(nameCompleteList);
        TnrMsg tnrMsg = TnrMsgUtils.mergeTnrMsgs(tnrMsgs);

        for(ServiceProviderInfo info : providerList){
            BaseChecklistClient client = newClientFor(info.getId());
            client.queryChecklist(tnrMsg);
        }

        return tnrMsg;

//        ModelAndView mv = new ModelAndView();
//        mv.addObject("tnrMsgs", tnrMsgs);
//        return mv;

    }

    @RequestMapping(method={RequestMethod.GET}, value="/capabilities")
    public @ResponseBody List<ServiceProviderInfo> capabilities(HttpServletRequest request,
            HttpServletResponse response) throws DRFChecklistException {
        String message = "<h3>Service providers Capabilities</h3>";
//        List<ServiceProviderInfo> cil = ServiceProviderInfoUtils.generateChecklistInfoList();

        return defaultProviders;

//        ModelAndView mv = new ModelAndView();
//        mv.addObject("infoList", cil);
//        return mv;

    }

}
