/**
 * JavaScript implementation of plugin for Sense Platform interaction.
 *
 * @author steven
 */

/**
 * 
 * @return Object literal singleton instance of SensePlatform plugin.
 */
var SensePlatform = {
        
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
                function(e){console.log('bindToSense error: ' + e)}, 
                'SensePlatform', 
                'bind', 
                []);
        } else {
            // already bound
            return;
        }
    },

    /**
     * @param argument
     *            Array with username and password
     * @param successCallback
     *            The callback which will be called when Sense login is successful
     * @param failureCallback
     *            The callback which will be called when Sense login encounters an error
     */
    login : function(argument, successCallback, failureCallback) {
        this.bindToSense();
        PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'login', 
                 argument ); 
    },

    /**
     * @param argument
     *            Unused
     * @param successCallback
     *            The callback which will be called when Sense startup is successful
     * @param failureCallback
     *            The callback which will be called when Sense startup encounters an error
     */
    start : function(argument, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'start', 
                []); 
    },

    /**
     * @param argument
     *            Unused
     * @param successCallback
     *            The callback which will be called when Sense is successfully stopped
     * @param failureCallback
     *            The callback which will be called when Sense encounters an error while stopping
     */
    stop : function(argument, successCallback, failureCallback) {
        this.bindToSense();
        return PhoneGap.exec(successCallback,
                failureCallback, 
                'SensePlatform', 
                'stop_sense',
                []); 
    }
};