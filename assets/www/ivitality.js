/**
 * JavaScript implementatation of iVitality plugin for Sense.
 *
 * @author steven
 */

/**
 * 
 * @return Object literal singleton instance of SenseIVit plugin for iVitality project.
 */
var IVitalityPlugin = {
        
    isBound : false,
    
    /** 
     * Executes "bind" action on the Plugin, if we are not bound yet.
     */ 
    bindToSense : function() {
        if (!this.isBound) {                
            PhoneGap.exec(
                    function(r){
                            console.log('bindToSense success: ' + r); 
                            isBound=true;
                        }, 
                    function(e){
                            console.log('bindToSense error: ' + e);
                            isBound=false;
                        }, 
                    'IVitalityPlugin', 
                    'bind', 
                    []);
        } else {
            // already bound
            return;
        }
    },

    /**
     * @param argument
     *            Unused
     * @param successCallback
     *            The callback which will be called when Sense state was is successfully checked
     * @param failureCallback
     *            The callback which will be called when Sense state check encounters an error
     */
    checkSenseStatus : function(argument, successCallback, failureCallback) {
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'IVitalityPlugin', 
                'check_sense_status', 
                []);
    },

    /**
     * @param argument
     *            Unused
     * @param successCallback
     *            The callback which will be called when blood pressure measurement is successful
     * @param failureCallback
     *            The callback which will be called when blood pressure measurement encounters an error
     */
    measurePressure : function(argument, successCallback, failureCallback) {
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'IVitalityPlugin', 
                'measure_pressure', 
                []); 
    },

    /**
     * @param argument
     *            Unused
     * @param successCallback
     *            The callback which will be called when reaction time measurement is successful
     * @param failureCallback
     *            The callback which will be called when reaction time measurement encounters an error
     */
    measureReaction : function(argument, successCallback, failureCallback) {
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'IVitalityPlugin', 
                'measure_reaction', 
                []); 
    },

    /**
     * @param argument
     *            Array containing question data
     * @param successCallback
     *            The callback which will be called when question display is successful
     * @param failureCallback
     *            The callback which will be called when question display encounters an error
     */
    showQuestion : function(argument, successCallback, failureCallback) {
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'IVitalityPlugin', 
                'show_question', 
                argument); 
    }
};