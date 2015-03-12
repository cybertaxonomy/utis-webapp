package org.bgbm.utis.controller;

import org.bgbm.biovel.drf.checklist.BaseChecklistClient;
import org.bgbm.biovel.drf.checklist.DRFChecklistException;
import org.bgbm.biovel.drf.checklist.SearchMode;
import org.bgbm.biovel.drf.checklist.UnsupportedIdentifierException;
import org.bgbm.biovel.drf.tnr.msg.TnrMsg;
import org.bgbm.biovel.drf.tnr.msg.Response;
import org.bgbm.biovel.drf.utils.TnrMsgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecklistClientRunner extends Thread{

    protected Logger logger = LoggerFactory.getLogger(ChecklistClientRunner.class);

    private BaseChecklistClient client;

    private TnrMsg tnrMsg;

    private long duration;

    private boolean unsupportedMode = false;

    private boolean unsupportedIdentifier = false;

    public BaseChecklistClient getClient() {
        return client;
    }

    public ChecklistClientRunner(BaseChecklistClient client, TnrMsg tnrMsg){
        this.client = client;
        this.tnrMsg = tnrMsg;
        TnrMsgUtils.assertSearchModeSet(tnrMsg, true);
        unsupportedMode = !client.getSearchModes().contains(TnrMsgUtils.getSearchMode(tnrMsg));
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
            client.queryChecklist(tnrMsg);
            if(logger.isDebugEnabled()){
                logger.debug("query to " + client.getServiceProviderInfo().getId() + " completed");
            }
        } catch (DRFChecklistException e) {
            if(e instanceof UnsupportedIdentifierException){
                setUnsupportedIdentifier(true);
                // skip
                return;
            }
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

    /**
     * @return the unsupportedIdentifier
     */
    public boolean isUnsupportedIdentifier() {
        return unsupportedIdentifier;
    }

    /**
     * @param unsupportedIdentifier the unsupportedIdentifier to set
     */
    public void setUnsupportedIdentifier(boolean unsupportedIdentifier) {
        this.unsupportedIdentifier = unsupportedIdentifier;
    }

}
