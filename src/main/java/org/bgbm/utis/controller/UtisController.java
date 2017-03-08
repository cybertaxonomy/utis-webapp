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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cybertaxonomy.utis.checklist.BaseChecklistClient;
import org.cybertaxonomy.utis.checklist.BgbmEditClient;
import org.cybertaxonomy.utis.checklist.ClassificationAction;
import org.cybertaxonomy.utis.checklist.DRFChecklistException;
import org.cybertaxonomy.utis.checklist.EUNIS_Client;
import org.cybertaxonomy.utis.checklist.GBIFBackboneClient;
import org.cybertaxonomy.utis.checklist.GBIFBetaBackboneClient;
import org.cybertaxonomy.utis.checklist.PESIClient;
import org.cybertaxonomy.utis.checklist.PlaziClient;
import org.cybertaxonomy.utis.checklist.SearchMode;
import org.cybertaxonomy.utis.checklist.WoRMSClient;
import org.cybertaxonomy.utis.client.AbstractClient;
import org.cybertaxonomy.utis.client.ClientFactory;
import org.cybertaxonomy.utis.client.ServiceProviderInfo;
import org.cybertaxonomy.utis.tnr.msg.Query;
import org.cybertaxonomy.utis.tnr.msg.Query.ClientStatus;
import org.cybertaxonomy.utis.tnr.msg.Response;
import org.cybertaxonomy.utis.tnr.msg.TnrMsg;
import org.cybertaxonomy.utis.utils.TnrMsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.swagger.annotations.ApiParam;

/**
 * @author a.kohlbecker
 * @date Jun 27, 2014
 *
 */

@Controller
@RequestMapping(produces={"application/json"}) // produces is needed for swagger) // "application/xml" disabled due to problems with /capabilities
public class UtisController {

    private static final int MIN_QUERY_STRING_LEN = 3;

    protected Logger logger = LoggerFactory.getLogger(UtisController.class);

    private Map<String, ServiceProviderInfo> serviceProviderInfoMap;
    private Map<String, Class<? extends BaseChecklistClient>> clientClassMap;

    private final ClientFactory clientFactory = new ClientFactory();

    private final List<ServiceProviderInfo> defaultProviders = new ArrayList<ServiceProviderInfo>();

    private final static Map<String, String> disabledClients = new HashMap<>();

    static {
        disabledClients.put(GBIFBetaBackboneClient.class.getSimpleName(), "since this is broken");
        disabledClients.put(EUNIS_Client.class.getSimpleName(), "for testing");
        disabledClients.put(PlaziClient.class.getSimpleName(), "for testing");
    }

    public UtisController() throws ClassNotFoundException {
        initProviderMap();
    }


    public static <T extends AbstractClient> Set<Class<T>> subclassesFor(Class<T> clazz) throws ClassNotFoundException{

        Set<Class<T>> subClasses = new HashSet<Class<T>>();
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(true);
        provider.addIncludeFilter(new AssignableTypeFilter(clazz));

        // scan only in org.cybertaxonomy.utis
        Set<BeanDefinition> components = provider.findCandidateComponents("org/cybertaxonomy/utis");
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

        serviceProviderInfoMap = new HashMap<String, ServiceProviderInfo>();
        clientClassMap = new HashMap<String, Class<? extends BaseChecklistClient>>();

        for(Class<BaseChecklistClient> clientClass: checklistClients){

            String simpleName = clientClass.getSimpleName();
            if(disabledClients.containsKey(simpleName)) {
                logger.debug("Skipping " + simpleName + " " + disabledClients.get(simpleName));
                continue;
            }
            BaseChecklistClient client;
            try {
                client = clientClass.newInstance();
                ServiceProviderInfo info = client.buildServiceProviderInfo();

                clientClassMap.put(info.getId(), clientClass);
                info.getSupportedActions().addAll(client.getSearchModes());
                info.getSupportedActions().addAll(client.getClassificationActions());// TODO setSearchModes should be done in client impl
                serviceProviderInfoMap.put(info.getId(), info);

            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        if(!disabledClients.containsKey(PESIClient.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(PESIClient.ID));
        }
        if(!disabledClients.containsKey(EUNIS_Client.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(EUNIS_Client.ID));
        }
        if(!disabledClients.containsKey(BgbmEditClient.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(BgbmEditClient.ID));
        }
        if(!disabledClients.containsKey(WoRMSClient.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(WoRMSClient.ID));
        }
        if(!disabledClients.containsKey(PlaziClient.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(PlaziClient.ID));
        }
        if(!disabledClients.containsKey(GBIFBackboneClient.class.getName())){
            defaultProviders.add(serviceProviderInfoMap.get(GBIFBackboneClient.ID));
        }
    }

    /**
     * @param providers
     * @param response
     * @return
     * @throws IOException
     */
    private List<ServiceProviderInfo> createProviderList(String providers, HttpServletResponse response)
            throws IOException {
        List<ServiceProviderInfo> providerList = defaultProviders;
        if (providers != null) {
            String[] providerIdTokens = providers.split(",");
            providerList = new ArrayList<ServiceProviderInfo>();
            for (String id : providerIdTokens) {

                List<String> subproviderIds = parsSubproviderIds(id);
                if(!subproviderIds.isEmpty()){
                    id = id.substring(0, id.indexOf("["));
                }

                if(serviceProviderInfoMap.containsKey(id)){
                     ServiceProviderInfo providerInfo = serviceProviderInfoMap.get(id);
                    if(!subproviderIds.isEmpty()){
                        // clone it
                        providerInfo = new ServiceProviderInfo(providerInfo);
                        Collection<ServiceProviderInfo> removeCandidates = new ArrayList<ServiceProviderInfo>();
                        for(ServiceProviderInfo subProvider : providerInfo.getSubChecklists()){
                            if(!subproviderIds.contains(subProvider.getId())){
                                removeCandidates.add(subProvider);
                            }
                        }
                        providerInfo.getSubChecklists().removeAll(removeCandidates);
                    }
                    providerList.add(providerInfo);
                }
            }
            if(providerList.isEmpty()){
                response.sendError(HttpStatus.BAD_REQUEST.value(), "invalid value for request parameter 'providers' given: " + defaultProviders.toString());
                throw new IllegalArgumentException("invalid value for request parameter 'providers' given: " + defaultProviders.toString());
            }
        }
        return providerList;
    }


    private List<String> parsSubproviderIds(String id) {

        List<String> subIds = new ArrayList<String>();
        Pattern pattern = Pattern.compile("^.*\\[([\\w\\-,]*)\\]$");

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




    /**
     * @param timeout
     * @param providerList
     * @param tnrMsg
     * @param dedupHashProvider TODO
     */
    private void executeTnrRequest(Long timeout, List<ServiceProviderInfo> providerList, TnrMsg tnrMsg, DeduplicationHashProvider dedupHashProvider) {
        // query all providers
           List<ChecklistClientRunner> runners = new ArrayList<ChecklistClientRunner>(providerList.size());
           for (ServiceProviderInfo info : providerList) {
               BaseChecklistClient client = clientFactory.newClient(clientClassMap.get(info.getId()));
               if(client != null){
                   logger.debug("sending query to " + info.getId());
                   client.setChecklistInfo(info);
                   ChecklistClientRunner runner = new ChecklistClientRunner(client, tnrMsg);
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
           Query currentQuery = tnrMsg.getQuery().get(0); // TODO HACK: we only are treating one query
           List<Response> tnrResponses = currentQuery.getResponse();
           List<Response> tnrResponsesOrdered = new ArrayList<Response>(tnrResponses.size());

           for(ChecklistClientRunner runner : runners){
               ServiceProviderInfo info = runner.getClient().getServiceProviderInfo();
               ClientStatus tnrStatus = TnrMsgUtils.tnrClientStatusFor(info);
               Response tnrResponse = null;

               // --- handle all exception states and create one tnrResonse which will contain the status
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
               else
               if(runner.isUnsupportedIdentifier()){
                   logger.debug("client runner '" + runner.getClient() + "' : identifier type not supported");
                   tnrStatus.setStatusMessage("identifier type not supported");
               }
               else {

                   tnrStatus.setStatusMessage("ok");
                   // --- collect the ServiceProviderInfo objects by which the responses will be ordered
                   List<ServiceProviderInfo> ServiceProviderInfos;
                   if(info.getSubChecklists() != null && !info.getSubChecklists().isEmpty()){
                       // for subchecklists we will have to look for responses of each of the subchecklists
                       ServiceProviderInfos = info.getSubChecklists();
                   } else {
                       // otherwise we only look for the responses of one checklist
                       ServiceProviderInfos = new ArrayList<ServiceProviderInfo>(1);
                       ServiceProviderInfos.add(info);
                   }

                   // --- order the tnrResponses
                   for(ServiceProviderInfo subInfo : ServiceProviderInfos){
                       tnrResponse = null;
                       for(Response tnrr : tnrResponses){
                           // TODO compare by id, requires model change
                           if(subInfo.getLabel().equals(tnrr.getChecklist())){
                               tnrResponse = tnrr;
                               tnrStatus.setDuration(BigDecimal.valueOf(runner.getDuration()));
                               tnrResponsesOrdered.add(tnrResponse);
                           }
                       }
                   }



               }
               currentQuery.getClientStatus().add(tnrStatus);
           }


           // --- remove duplicates from the response
           if(dedupHashProvider != null) {
               List<Response> tnrResponsesTmp = tnrResponsesOrdered;
               HashSet<String> dedup = new HashSet<String>(tnrResponsesTmp.size());
               tnrResponsesOrdered = new ArrayList<Response>(tnrResponsesTmp.size());
               for (Response r : tnrResponsesTmp) {
                   String hash = dedupHashProvider.hash(r.getTaxon());
                   logger.debug(r.getTaxon().getTaxonName().getScientificName() + " - " + hash);
                   if(dedup.add(hash)) {
                       logger.debug(r.getTaxon().getTaxonName().getScientificName() + " - added");
                       tnrResponsesOrdered.add(r);
                   }
               }
           }

           currentQuery.getResponse().clear();
           currentQuery.getResponse().addAll(tnrResponsesOrdered);
    }


    @RequestMapping(method = { RequestMethod.GET }, value = "/capabilities")
    public @ResponseBody List<ServiceProviderInfo> capabilities(HttpServletRequest request, HttpServletResponse response) {
        return defaultProviders;
    }


    /**
     *
     * @param queryString The complete canonical scientific name to search for. For
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
                // @formatter:off
                    @ApiParam(
                        value = "The scientific name, vernacular name or identifier to search for. "
                        +"For example: \"Bellis perennis\", \"Prionus\" or \"Bolinus brandaris\". "
                        + "The search string must have a minimum length of 3 characters."
                        ,required=true)
                    @RequestParam(value = "query", required = false)
                String queryString,
                    @ApiParam(value = "A list of provider id strings concatenated by comma "
                        +"characters. The default : \"pesi,bgbm-cdm-server[col]\" will be used "
                        + "if this parameter is not set. A list of all available provider "
                        +"ids can be obtained from the '/capabilities' service "
                        +"end point. "
                        + "Providers can be nested, that is a parent provider can have "
                        + "sub providers. If the id of the parent provider is supplied all subproviders will "
                        + "be queried. The query can also be restriced to one or more subproviders by "
                        + "using the following syntax: parent-id[sub-id-1,sub-id2,...]",
                        defaultValue="pesi,eunis,bgbm-cdm-server[col]",
                        required=false)
                    @RequestParam(value = "providers", required = false)
                String providers,
                    @ApiParam(value = "Specifies the searchMode. "
                            + "Possible search modes are: scientificNameExact, scientificNameLike (begins with), vernacularNameExact, "
                            + "vernacularNameLike (contains), findByIdentifier"
                            + "If the a provider does not support the chosen searchMode it will be skipped and "
                            + "the status message in the tnrClientStatus will be set to 'unsupported search mode' in this case.")
                    @RequestParam(value = "searchMode", required = false, defaultValue="scientificNameExact")
                SearchMode searchMode,
                    @ApiParam(value = "Indicates whether the synonymy of the accepted taxon should be included into the response. "
                            + "Turning this option on may cause an increased response time.")
                    @RequestParam(value = "addSynonymy", required = false, defaultValue="false")
                Boolean addSynonymy,
                    @ApiParam(value = "Indicates whether the the parent taxon of the accepted taxon should be included into the response. "
                            + "Turning this option on may cause a slightly increased response time.")
                    @RequestParam(value = "addParentTaxon", required = false, defaultValue="false")
                Boolean addParentTaxon,
                @ApiParam(value = "Allows to deduplicate the resuls by making use of a deduplication strategy. "
                        + "The deduplication is done by comparing specific properties of the"
                        + " taxon:\n"
                        + "- id: compares 'taxon.identifier'\n"
                        + "- id_name: compares 'taxon.identifier' AND 'taxon.taxonName.scientificName'\n"
                        + "- name: compares 'taxon.taxonName.scientificName'\n"
                        + "Using the pure 'name' strategy is not recommended.")
                @RequestParam(value = "dedup", required = false)
                DeduplicationHashProvider dedupHashProvider,
                    @ApiParam(value = "The maximum of milliseconds to wait for responses from any of the providers. "
                            + "If the timeout is exceeded the service will jut return the resonses that have been "
                            + "received so far. The default timeout is 0 ms (wait for ever)")
                    @RequestParam(value = "timeout", required = false, defaultValue="0")
                Long timeout,
                HttpServletRequest request,
                HttpServletResponse response
                //@formatter:on
            ) throws DRFChecklistException, JsonGenerationException, JsonMappingException,
            IOException {

        if(queryString.length() < MIN_QUERY_STRING_LEN) {
            throw new MinQueryStringException();
        }

        List<ServiceProviderInfo> providerList = createProviderList(providers, response);

        TnrMsg tnrMsg = TnrMsgUtils.convertStringToTnrMsg(queryString, searchMode, addSynonymy, addParentTaxon);

        executeTnrRequest(timeout, providerList, tnrMsg, dedupHashProvider);
        return tnrMsg;
    }


    /**
    *
    * @param taxonId The complete canonical scientific name to search for. For
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
   @RequestMapping(method = { RequestMethod.GET }, value = "/classification/{taxonId}/parent")
   public @ResponseBody
   TnrMsg higherClassification(
               // @formatter:off
               @ApiParam(
                   value = "The identifier for the taxon. (LSID, DOI, URI, or any other identifier used by the checklist provider)"
                   ,required=true)
               @PathVariable(value = "taxonId")
               String taxonId,
               @ApiParam(value = "A list of provider id strings concatenated by comma "
                   +"characters. The default : \"pesi,bgbm-cdm-server[col]\" will be used "
                   + "if this parameter is not set. A list of all available provider "
                   +"ids can be obtained from the '/capabilities' service "
                   +"end point. "
                   + "Providers can be nested, that is a parent provider can have "
                   + "sub providers. If the id of the parent provider is supplied all subproviders will "
                   + "be queried. The query can also be restriced to one or more subproviders by "
                   + "using the following syntax: parent-id[sub-id-1,sub-id2,...]",
                   defaultValue="pesi,eunis,bgbm-cdm-server[col]",
                   required=false)
               @RequestParam(value = "providers", required = false)
               String providers,
               @ApiParam(value = "The maximum of milliseconds to wait for responses from any of the providers. "
                       + "If the timeout is exceeded the service will jut return the resonses that have been "
                       + "received so far. The default timeout is 0 ms (wait for ever)")
               @RequestParam(value = "timeout", required = false, defaultValue="0")
               Long timeout,
               HttpServletRequest request,
               HttpServletResponse response
               // @formatter:on
           ) throws DRFChecklistException, JsonGenerationException, JsonMappingException,
           IOException {


       List<ServiceProviderInfo> providerList = createProviderList(providers, response);

       TnrMsg tnrMsg = TnrMsgUtils.convertStringToTnrMsg(taxonId, ClassificationAction.higherClassification, false, false);

       executeTnrRequest(timeout, providerList, tnrMsg, null);

       return tnrMsg;
   }

   @RequestMapping(method = { RequestMethod.GET }, value = "/classification/{taxonId}/children")
   public @ResponseBody
   TnrMsg taxonomicChildren(
               // @formatter:off
                   @ApiParam(
                       value = "The identifier for the taxon. (LSID, DOI, URI, or any other identifier used by the checklist provider)"
                       ,required=true)
                   @PathVariable(value = "taxonId")
               String taxonId,
                   @ApiParam(value = "A list of provider id strings concatenated by comma "
                       +"characters. The default : \"pesi,bgbm-cdm-server[col]\" will be used "
                       + "if this parameter is not set. A list of all available provider "
                       +"ids can be obtained from the '/capabilities' service "
                       +"end point. "
                       + "Providers can be nested, that is a parent provider can have "
                       + "sub providers. If the id of the parent provider is supplied all subproviders will "
                       + "be queried. The query can also be restriced to one or more subproviders by "
                       + "using the following syntax: parent-id[sub-id-1,sub-id2,...]",
                       defaultValue="pesi,eunis,bgbm-cdm-server[col]",
                       required=false)
                   @RequestParam(value = "providers", required = false)
               String providers,
                   @ApiParam(value = "The maximum of milliseconds to wait for responses from any of the providers. "
                           + "If the timeout is exceeded the service will jut return the resonses that have been "
                           + "received so far. The default timeout is 0 ms (wait for ever)")
                   @RequestParam(value = "timeout", required = false, defaultValue="0")
               Long timeout,
               HttpServletRequest request,
               HttpServletResponse response
               // @formatter:on
           ) throws DRFChecklistException, JsonGenerationException, JsonMappingException,
           IOException {


       List<ServiceProviderInfo> providerList = createProviderList(providers, response);

       TnrMsg tnrMsg = TnrMsgUtils.convertStringToTnrMsg(taxonId, ClassificationAction.taxonomicChildren, false, false);

       executeTnrRequest(timeout, providerList, tnrMsg, null);

       return tnrMsg;
   }


}
