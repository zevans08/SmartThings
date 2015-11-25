/**
 * We're Done With That
 *
 * By Chip Rosenthal <chip@unicom.com>
 *  
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */
 
definition(
    name: "We're Done With That",
    namespace: "chip-rosenthal",
    author: "Chip Rosenthal",
    description: "Use this app if you have a mode like \"Watching TV\" and you want to change it automatically when everybody leaves the room",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page(name: "dynamicPrefs")
}

def dynamicPrefs() {
    dynamicPage(name: "dynamicPrefs", install: true, uninstall: true) {
    
    	// build sorted list of available actions
        def actions = location.helloHome?.getPhrases()*.label.sort()
        if (! actions) {
            log.error "dynamicPrefs: failed to retrieve available actions"
        }
        
        section("Settings") {            
            input "enableModes", "mode", \
                title: "When the house is in this mode:", required: true, multiple: true
            input "selectedSensor", "capability.motionSensor", \
                title: "And this motion sensor:", required: true
            input "idleMinutes", "number", \
                title: "Has been idle for this long (mins):", required: true
            input "runAction", "enum", \
                title: "Then run this action:", options: actions, required: true
        }
    }
}

def installed() {
	log.debug "installed: settings = ${settings}"    
	initialize()
}

def updated() {
	log.debug "updated: settings = ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(selectedSensor, "motion", motionHandler)
}

def motionHandler(evt) {
	switch (evt.value) {
    case "active":
    	log.debug "motionHandler: motion detector active - canceling any scheduled timer event"
    	unschedule("timeoutHandler")
    	break
    case "inactive":
    	def secs = 60 * idleMinutes
    	log.debug "motionHandler: motion detector inactive - scheduling timer event for ${secs} secs"
    	runIn(secs, timeoutHandler)
    	break
    default:
    	log.error "motionHandler: bad event value ${evt.value}"
    }
}

def timeoutHandler() {
    def currentMode = location.mode
    if (enableModes.contains(currentMode)) {    
    	log.debug "timeoutHandler: running action ${runAction}"
        location.helloHome?.execute(runAction)
        sendNotificationEvent("We're done with that. Everybody left the room, so I ran: ${runAction}")
    } else {
    	log.debug "timeoutHandler: ignoring event in mode ${currentMode}"
    }
}
