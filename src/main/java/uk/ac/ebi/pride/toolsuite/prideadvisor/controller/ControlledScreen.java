package uk.ac.ebi.pride.toolsuite.prideadvisor.controller;

import uk.ac.ebi.pride.toolsuite.prideadvisor.controller.ScreensController;

/**
 * Created by tobias on 08/08/2016.
 */
public interface ControlledScreen {
    //This method will allow the injection of the Parent ScreenPane
    public void setScreenParent(ScreensController screenPage);
}
