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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bgbm.biovel.drf.checklist.BaseChecklistClient;
import org.bgbm.biovel.drf.checklist.BgbmEditClient;
import org.bgbm.biovel.drf.checklist.DRFChecklistException;
import org.bgbm.biovel.drf.checklist.PESIClient;
import org.bgbm.biovel.drf.checklist.SearchMode;
import org.bgbm.biovel.drf.checklist.WoRMSClient;
import org.bgbm.biovel.drf.rest.TaxoRESTClient;
import org.bgbm.biovel.drf.rest.TaxoRESTClient.ServiceProviderInfo;
import org.bgbm.biovel.drf.tnr.msg.Query;
import org.bgbm.biovel.drf.tnr.msg.Query.TnrClientStatus;
import org.bgbm.biovel.drf.tnr.msg.TnrMsg;
import org.bgbm.biovel.drf.tnr.msg.TnrResponse;
import org.bgbm.biovel.drf.utils.JSONUtils;
import org.bgbm.biovel.drf.utils.ServiceProviderInfoUtils;
import org.bgbm.biovel.drf.utils.TnrMsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sun.xml.internal.ws.server.provider.ProviderInvokerTube;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @author a.kohlbecker
 * @date Jun 27, 2014
 *
 */

@Controller
@RequestMapping(produces={"application/json","application/xml"}) // produces is needed for swagger)
public class UtisController {

    protected Logger logger = LoggerFactory.getLogger(UtisController.class);

    private Map<String, ChecklistInfo> checklistInfoMap;
    private Map<String, Class<? extends BaseChecklistClient>> clientClassMap;

    private final List<ChecklistInfo> defaultProviders = new ArrayList<ChecklistInfo>();

    public UtisController() throws ClassNotFoundException {
        initProviderMap();
    }


    public static <T extends TaxoRESTClient> Set<Class<T>> subclassesFor(Class<T> clazz) throws ClassNotFoundException{

        Set<Class<T>> subClasses = new HashSet<Class<T>>();
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
        provider.addIncludeFilter(new AssignableTypeFilter(clazz));

        // scan only in org.bgbm.biovel.drf
        Set<BeanDefinition> components = provider.findCandidateComponents("org/bgbm/biovel/drf");
        for (BeanDefinition component : components)
        {
            subClasses.add((Class<T>) Class.forName(component.getBeanClassName()));
        }
        return subClasses;
    }

    /**
     * @throws ClassNotFoundException
     *
     */
    private void initProviderMap() throws ClassNotFoundException {

        Set<Class<BaseChecklistClient>> checklistClients;
        checklistClients = subclassesFor(BaseChecklistClient.class);

        checklistInfoMap = new HashMap<String, ChecklistInfo>();
        clientClassMap = new HashMap<String, Class<? extends BaseChecklistClient>>();

        for(Class<BaseChecklistClient> clientClass: checklistClients){

            BaseChecklistClient client;
            try {
                client = clientClass.newInstance();
                ServiceProviderInfo info = client.buildServiceProviderInfo();

                clientClassMap.put(info.getId(), clientClass);
                checklistInfoMap.put(info.getId(), new ChecklistInfo(info, client.getSearchModes()));

            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        defaultProviders.add(checklistInfoMap.get(PESIClient.ID));
        defaultProviders.add(checklistInfoMap.get(BgbmEditClient.ID));
        defaultProviders.add(checklistInfoMap.get(WoRMSClient.ID));
    }

    private BaseChecklistClient newClientFor(String id) {

        BaseChecklistClient instance = null;

        if(!clientClassMap.containsKey(id)){
            logger.error("Unsupported Client ID: "+ id);

        } else {
            try {
                instance = clientClassMap.get(id).newInstance();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return instance;
    }

    /**
     *
     * @param query The complete canonical scientific name to search for. For
     *          example: <code>Bellis perennis</code>, <code>Prionus</code> or
     *          <code>Bolinus brandaris</code>.
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
                    +"For example: \"Bellis perennis\", \"Prionus\" or \"Bolinus brandaris\". "
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
                @ApiParam(value = "Specifies the searchMode. "
                        + "Possible search modes are: scientificNameExact, scientificNameLike, vernacularName. "
                        + "If the a provider does not support the chosen searchMode it will be skiped and "
                        + "the response status will be set to 'unsupported search mode'")
                @RequestParam(value = "searchMode", required = false, defaultValue="scientificNameExact")
                SearchMode searchMode,
                @ApiParam(value = "The most millis milliseconds to wait for resones from any of the providers. "
                        + "If the timeout is exceeded the service will jut return the resonses that have been "
                        + "received so far. The default timeout is 0 ms (wait for ever)")
                @RequestParam(value = "timeout", required = false, defaultValue="0")
                Long timeout,
                HttpServletRequest request,
                HttpServletResponse response
            ) throws DRFChecklistException, JsonGenerationException, JsonMappingException,
            IOException {

        List<String> nameCompleteList;

        List<ChecklistInfo> providerList = defaultProviders;
        if (providers != null) {
            String[] providerIdTokens = providers.split(",");
            providerList = new ArrayList<ChecklistInfo>();
            for (String id : providerIdTokens) {

                List<String> subproviderIds = parsSubproviderIds(id);
                if(!subproviderIds.isEmpty()){
                    id = id.substring(0, id.indexOf("["));
                }

                if(checklistInfoMap.containsKey(id)){
                     ChecklistInfo provider = checklistInfoMap.get(id);
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

        nameCompleteList = new ArrayList<String>();
        nameCompleteList.add(query);

        List<TnrMsg> accumulatedTnrMsgs = new ArrayList<TnrMsg>(providerList.size());

        List<TnrMsg> tnrMsgs = TnrMsgUtils.convertStringListToTnrMsgList(nameCompleteList);
        tnrMsgs.get(0).getQuery().get(0).getTnrRequest().setSearchMode(searchMode.toString());

        TnrMsg tnrMsg = TnrMsgUtils.mergeTnrMsgs(tnrMsgs);


        // query all providers
        List<ChecklistClientRunner> runners = new ArrayList<ChecklistClientRunner>(providerList.size());
        for (ChecklistInfo info : providerList) {
            BaseChecklistClient client = newClientFor(info.getId());
            if(client != null){
                logger.debug("sending query to " + info.getId());
                ChecklistClientRunner runner = new ChecklistClientRunner(client, tnrMsg, searchMode);
                runner.start();
                runners.add(runner);
            }
        }

        // wait for the responses
        logger.debug("All runners started, now waiting for them to complete ...");
        for(ChecklistClientRunner runner : runners){
            try {
                logger.debug("waiting for client runner '" + runner.getClient());
                runner.join(timeout);
            } catch (InterruptedException e) {
                logger.debug("client runner '" + runner.getClient() + "' was interrupted", e);
            }
        }
        logger.debug("end of waiting (all runners completed or timed out)");

        // collect, re-order the responses and set the status
        Query currentQuery = tnrMsg.getQuery().get(0);
        List<TnrResponse> tnrResponses = currentQuery.getTnrResponse(); // TODO HACK: we only are treating one query
        List<TnrResponse> tnrResponsesOrderd = new ArrayList<TnrResponse>(tnrResponses.size());

        for(ChecklistClientRunner runner : runners){
            ServiceProviderInfo info = runner.getClient().getServiceProviderInfo();
            TnrClientStatus tnrStatus = TnrMsgUtils.tnrClientStatusFor(info);
            TnrResponse tnrResponse = null;

            // --- handle all exception states and create one tnrResonse which will contain the status
            /* TODO replace by a separate list of tnrClientStatus objects in the query object and ceate no tnrResonse in these cases:
            // tnrClientStatus:
            //   - statusMessage
            //   - duration
            //
             */
            if(runner.isInterrupted()){
                logger.debug("client runner '" + runner.getClient() + "' was interrupted");
                tnrStatus.setStatusMessage("interrupted");
            }
            else
            if(runner.isAlive()){
                logger.debug("client runner '" + runner.getClient() + "' has timed out");
                tnrStatus.setStatusMessage("timeout");
            }
            else
            if(runner.isUnsupportedMode()){
                logger.debug("client runner '" + runner.getClient() + "' : unsupported search mode");
                tnrStatus.setStatusMessage("unsupported search mode");
            }
            else {

                tnrStatus.setStatusMessage("ok");
                // --- collect the ServiceProviderInfo objects by which the responses will be ordered
                List<ServiceProviderInfo> checklistInfos;
                if(info.getSubChecklists() != null && !info.getSubChecklists().isEmpty()){
                    // for subchecklists we will have to look for responses of each of the subchecklists
                    checklistInfos = info.getSubChecklists();
                } else {
                    // otherwise we only look for the responses of one checklist
                    checklistInfos = new ArrayList<ServiceProviderInfo>(1);
                    checklistInfos.add(info);
                }

                // --- order the tnrResponses
                for(ServiceProviderInfo subInfo : checklistInfos){
                    tnrResponse = null;
                    for(TnrResponse tnrr : tnrResponses){
                        // TODO compare by id, requires model change
                        if(subInfo.getLabel().equals(tnrr.getChecklist())){
                            tnrResponse = tnrr;
                            tnrStatus.setDuration(BigDecimal.valueOf(runner.getDuration()));
                            tnrResponsesOrderd.add(tnrResponse);
                        }
                    }
                    if(tnrResponse == null){
                        // no match found! will become obsolete with tnrClientStatus
                        tnrResponse = TnrMsgUtils.tnrResponseFor(info);
                        // in case of no match, status is ok but result set is empty
                        tnrResponsesOrderd.add(tnrResponse);
                    }
                }

            }
            currentQuery.getTnrClientStatus().add(tnrStatus);
        }
        currentQuery.getTnrResponse().clear();
        currentQuery.getTnrResponse().addAll(tnrResponsesOrderd);


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
    public @ResponseBody List<ChecklistInfo> capabilities(HttpServletRequest request, HttpServletResponse response) {
        return defaultProviders;
    }


}
