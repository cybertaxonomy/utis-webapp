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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author a.kohlbecker
 * @date Jun 27, 2014
 *
 */

@Controller
@RequestMapping(produces={"application/json","application/xml"}) // produces is needed for swagger)
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
        for (ServiceProviderInfo info : ServiceProviderInfoUtils.generateChecklistInfoList()) {
            checklistInfoMap.put(info.getId(), info);
        }

        defaultProviders.add(checklistInfoMap.get(PESIClient.ID));
        defaultProviders.add(checklistInfoMap.get(BgbmEditClient.ID));
    }

    private BaseChecklistClient newClientFor(String id) {
        if (id.equals(PESIClient.ID)) {
            return new PESIClient();
        }

        if (id.equals(BgbmEditClient.ID)) {
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

    /**
     *
     * @param query The complete canonical scientific name to search for. For
     *          example: <code>Bellis perennis</code> or <code>Prionus</code>.
     *          This is a exact search so wildcard characters are not supported.
     *
     * @param providers
     *            A list of provider id strings concatenated by comma
     *            characters. The default : <code>pesi,edit</code> will be used
     *            if this parameter is not set. A list of all available provider
     *            ids can be obtained from the <code>/capabilities</code> service
     *            end point.
     * @param request
     * @param response
     * @return
     * @throws DRFChecklistException
     * @throws JsonGenerationException
     * @throws JsonMappingException
     * @throws IOException
     */
    @RequestMapping(method = { RequestMethod.GET }, value = "/search")
    public @ResponseBody
    TnrMsg search(
                @ApiParam(
                    value = "The scientific name to search for. "
                    +"For example: \"Bellis perennis\" or \"Prionus\". "
                    +"This is an exact search so wildcard characters are not supported."
                    ,required=true)
                @RequestParam(value = "query", required = false)
                String query,
                @ApiParam(value = "A list of provider id strings concatenated by comma "
                    +"characters. The default : \"pesi,bgbm-cdm-server[col]\" will be used "
                    + "if this parameter is not set. A list of all available provider "
                    +"ids can be obtained from the '/capabilities' service "
                    +"end point."
                    + "Providers can be nested, that is a parent provider can have "
                    + "sub providers. If the id of the parent provider is supplied all subproviders will "
                    + "be queried. The query can also be restriced to one or more subproviders by "
                    + "using the following syntax: parent-id[sub-id-1,sub-id2,...]",
                    defaultValue="pesi,bgbm-cdm-server[col]",
                    required=false)
                @RequestParam(value = "providers", required = false)
                String providers,
                HttpServletRequest request,
                HttpServletResponse response
            ) throws DRFChecklistException, JsonGenerationException, JsonMappingException,
            IOException {

        List<String> nameCompleteList;

        List<ServiceProviderInfo> providerList = defaultProviders;
        if (providers != null) {
            String[] providerIdTokens = providers.split(",");
            providerList = new ArrayList<ServiceProviderInfo>();
            for (String id : providerIdTokens) {

                List<String> subproviderIds = parsSubproviderIds(id);
                if(!subproviderIds.isEmpty()){
                    id = id.substring(0, id.indexOf("["));
                }

                if(checklistInfoMap.containsKey(id)){
                    ServiceProviderInfo provider = checklistInfoMap.get(id);
                    if(!subproviderIds.isEmpty()){
                        Collection<ServiceProviderInfo> removeCandidates = new ArrayList<ServiceProviderInfo>();
                        for(ServiceProviderInfo subProvider : provider.getSubChecklists()){
                            if(!subproviderIds.contains(subProvider.getId())){
                                removeCandidates.add(subProvider);
                            }
                        }
                        provider.getSubChecklists().removeAll(removeCandidates);
                    }
                    providerList.add(provider);
                }
            }
            if(providerList.isEmpty()){
                response.sendError(HttpStatus.BAD_REQUEST.value(), "invalid value for request parameter 'providers' given: " + defaultProviders.toString());
            }
        }

        if (query == null) {
            query = "Bellis perennis";
        }
        nameCompleteList = new ArrayList<String>();
        nameCompleteList.add(query);

        List<TnrMsg> accumulatedTnrMsgs = new ArrayList<TnrMsg>(providerList.size());

        List<TnrMsg> tnrMsgs = TnrMsgUtils.convertStringListToTnrMsgList(nameCompleteList);
        TnrMsg tnrMsg = TnrMsgUtils.mergeTnrMsgs(tnrMsgs);

        for (ServiceProviderInfo info : providerList) {
            BaseChecklistClient client = newClientFor(info.getId());
            client.queryChecklist(tnrMsg);
        }

        return tnrMsg;
    }

    private List<String> parsSubproviderIds(String id) {

        List<String> subIds = new ArrayList<String>();
        Pattern pattern = Pattern.compile("^.*\\[([\\w,]*)\\]$");

        Matcher m = pattern.matcher(id);
        if (m.matches()) {
            String subids = m.group(1);
            String[] subidTokens = subids.split(",");
            for (String subId : subidTokens) {
                subIds.add(subId);
            }
        }
        return subIds;
    }

    @RequestMapping(method = { RequestMethod.GET }, value = "/capabilities")
    public @ResponseBody List<ServiceProviderInfo> capabilities(HttpServletRequest request, HttpServletResponse response) {
        return defaultProviders;
    }

//    @RequestMapping(method = { RequestMethod.GET }, value = "/modelAndView")
//    public List<ServiceProviderInfo> modelAndView(HttpServletRequest request, HttpServletResponse response) {
//        return null;
//    }


}
