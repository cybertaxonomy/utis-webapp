package org.bgbm.utis.controller;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import org.bgbm.biovel.drf.checklist.SearchMode;
import org.bgbm.biovel.drf.rest.TaxoRESTClient.ServiceProviderInfo;

import com.wordnik.swagger.annotations.ApiModelProperty;

public class ChecklistInfo extends ServiceProviderInfo {

    EnumSet<SearchMode> searchModes;

    public ChecklistInfo(ServiceProviderInfo info, EnumSet<SearchMode> matchModes) {
        super(info.getId(), info.getLabel(), info.getDocumentationUrl(), info.getCopyrightUrl(), info.getVersion());
        if(info.getSubChecklists() != null){
            for(ServiceProviderInfo subInfo : info.getSubChecklists()){

                this.addSubChecklist(new ChecklistInfo(subInfo, matchModes));
            }
        }
        this.searchModes = matchModes;
    }

    /**
     * @return the matchModes
     */
    @ApiModelProperty("Set of the different SearchModes supported by the service provider and client implementation."
            + "Possible search modes are: scientificNameExact, scientificNameLike, vernacularName")
    public EnumSet<SearchMode> getSearchModes() {
        return searchModes;
    }

    /**
     * @param matchModes the matchModes to set
     */
    public void setSearchModes(EnumSet<SearchMode> matchModes) {
        this.searchModes = matchModes;
    }

}
