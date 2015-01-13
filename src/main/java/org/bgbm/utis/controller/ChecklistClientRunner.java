package org.bgbm.utis.controller;

import org.bgbm.biovel.drf.checklist.BaseChecklistClient;
import org.bgbm.biovel.drf.checklist.DRFChecklistException;
import org.bgbm.biovel.drf.checklist.SearchMode;
import org.bgbm.biovel.drf.tnr.msg.TnrMsg;
import org.bgbm.biovel.drf.tnr.msg.TnrResponse;
import org.bgbm.biovel.drf.utils.TnrMsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecklistClientRunner extends Thread{

    protected Logger logger = LoggerFactory.getLogger(ChecklistClientRunner.class);

    private BaseChecklistClient client;

    private TnrMsg tnrMsg;

    private long duration;

    private boolean unsupportedMode = false;

    private SearchMode searchMode;

    public BaseChecklistClient getClient() {
        return client;
    }

    public ChecklistClientRunner(BaseChecklistClient client, TnrMsg tnrMsg, SearchMode searchMode){
        this.client = client;
        this.tnrMsg = tnrMsg;
        this.searchMode = searchMode;
        unsupportedMode = !client.getSearchModes().contains(searchMode);
    }

    @Override
    public void run() {

        if(tnrMsg == null){
            logger.error("TnrMsg object must not be NULL");
        }

        if(isUnsupportedMode()){
            // skip
            return;
        }

        long start = System.currentTimeMillis();
        try {
            client.queryChecklist(tnrMsg, searchMode);
            if(logger.isDebugEnabled()){
                logger.debug("query to " + client.getServiceProviderInfo().getId() + " completed");
            }
        } catch (DRFChecklistException e) {
            logger.error("Error during request to " + client.getServiceProviderInfo().getId(), e);
        }
        duration = System.currentTimeMillis() - start;
    }

    /**
     * @return the duration of the last run
     */
    public long getDuration() {
        return duration;
    }

    /**
     * @return the unsupportedMode
     */
    public boolean isUnsupportedMode() {
        return unsupportedMode;
    }

    /**
     * @param unsupportedMode the unsupportedMode to set
     */
    public void setUnsupportedMode(boolean unsupportedMode) {
        this.unsupportedMode = unsupportedMode;
    }

}
