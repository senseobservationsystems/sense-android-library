function IVitalityPlugin() {

}

/**
 * @param successCallback
 *            The callback which will be called when Sense state was is successfully checked
 * @param failureCallback
 *            The callback which will be called when Sense state check encounters an error
 */
IVitalityPlugin.prototype.checkSenseStatus = function(successCallback, failureCallback) {
    // TODO
    console.log('cannot check Sense status');
};

/**
 * @param successCallback
 *            The callback which will be called when blood pressure measurement is successful
 * @param failureCallback
 *            The callback which will be called when blood pressure measurement encounters an error
 */
IVitalityPlugin.prototype.measurePressure = function(successCallback, failureCallback) {
    return PhoneGap.exec(successCallback, failureCallback, 'IVitalityPlugin', 'measure_pressure', []);
};

/**
 * @param argument
 *            Unused
 * @param successCallback
 *            The callback which will be called when reaction time measurement is successful
 * @param failureCallback
 *            The callback which will be called when reaction time measurement encounters an error
 */
IVitalityPlugin.prototype.measureReaction = function(successCallback, failureCallback) {
    return PhoneGap.exec(successCallback, failureCallback, 'IVitalityPlugin', 'measure_reaction', []);
};

/**
 * @param questionId 
 *            String with ID of the question, for administrative purposes
 * @param question
 *            String with question text
 * @param answers
 *            Array with strings for the answer texts
 * @param successCallback
 *            The callback which will be called when question display is successful
 * @param failureCallback
 *            The callback which will be called when question display encounters an error
 */
IVitalityPlugin.prototype.showMultipleChoice = function(questionId, question, answers, successCallback, failureCallback) {
    return PhoneGap.exec(successCallback, failureCallback, 'IVitalityPlugin', 'show_multiple_choice', [questionId, question, answers]);
};

/**
 * @param questionId 
 *            String with ID of the question, for administrative purposes
 * @param question
 *            String with question text
 * @param min
 *            Minimum value for the slider
 * @param max 
 *            Maximum value for the slider
 * @param step
 *            Step size for the slider 
 * @param successCallback
 *            The callback which will be called when question display is successful
 * @param failureCallback
 *            The callback which will be called when question display encounters an error
 */
IVitalityPlugin.prototype.showSliderQuestion = function(questionId, question, min, max, step, successCallback, failureCallback) {
    return PhoneGap.exec(successCallback, failureCallback, 'IVitalityPlugin', 'show_slider_question', [questionId, question, min, max, step]);
};

IVitalityPlugin.prototype.init = function() {
    // nothing to init
    return '';
};

PhoneGap.addConstructor(function() {
    PhoneGap.addPlugin("ivitality", new IVitalityPlugin());
});