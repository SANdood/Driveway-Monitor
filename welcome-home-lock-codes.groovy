/**
 *  Welcome Home Lock Codes
 *
 *  Copyright 2018 Barry A. Burke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
def getVersionNum() { return "1.0.0" }
private def getVersionLabel() { return "Welcome Home Lock Codes, version ${getVersionNum()}" }
 
definition(
    name: "Welcome Home Lock Codes",
    namespace: "sandood",
    author: "Barry A. Burke",
    description: "Welcome Home announcement for smart doors managed with @RBoy's lock codes",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page name:		"mainPage"
}

def mainPage() {
	dynamicPage(name: "mainPage", title: getVersionLabel(), install: true, uninstall: true) {
    	section(title: "Name for this Welcome Home Announcement") {
        	label title: "Name", required: true, defaultValue: "Driveway Monitor"
        }
        section("Locks") {
            input "theLocks", "capability.lock", title: "Locks to monitor", multiple: true, required: true
        }
        section("Echo devices") {
            input "theEchos", "device.echoSpeaksDevice", title: "Select Echo device(s)", required: true, multiple: true
            input "volume", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", submitOnChange: true
            // if (settings?.volume != null) input "normalVolume", "number", title: "Set normal volume to", description: "0-100%", required: false, range: "0..100"
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	subscribe( settings.theLocks, "lock.unlocked", unlockHandler )
    def msg = "${app.label} has been initialized."
    settings.theEchos.setVolumeSpeakAndRestore( settings.volume, msg )
    // if (settings.normalVolume && (settings.volume != settings.normalVolume)) settings.theEchos.setVolume(settings.normalVolume)
}

def unlockHandler(evt) {
	log.trace "${evt.device.displayName}: ${evt.descriptionText}"
	String who = ""
	//def desc = evt.descriptionText    // 'Unlocked by "name"'
    if (evt.descriptionText.contains("Unlocked by ")) {
    	who = (evt.descriptionText - "Unlocked by ")[1..-2]
    }
    if (who) {
	   	// runIn(2, sayHey, [data: [who: who]])
        settings.theEchos.setVolumeSpeakAndRestore( settings.volume, getRandomItem( headers() ) + " ${who}, " + getRandomItem( trailers() ) )
		//if (settings.normalVolume && (settings.volume != settings.normalVolume)) settings.theEchos.setVolume(settings.normalVolume)
    }
}

def sayHey(data) {
	def who = data.who
    settings.theEchos.setVolumeSpeakAndRestore( settings.volume, getRandomItem( headers() ) + " ${who}! " + getRandomItem( trailers() ) )
    // if (settings.normalVolume && (settings.volume != settings.normalVolume)) settings.theEchos.setVolume(settings.normalVolume)
}

List headers() {
	return [ "Welcome back ", "Hello again ", "Please come in ", "Come on in ", "I've been waiting for you ", "WOW! It's been so long ", 
    		 "Well well well, lookie who's here, It's ", "Well if you aren't a sight for sore eyes - so good to see you again ",
             "Hey hey hay! It's ", "Wow, long time no see " ]
}

List trailers() {
	return [ "The cats have really missed you!", "Wow, sure you look beat, Come on in and get some rest.", "Where the heck have you been?", 
    			"Did you miss me?, I REALLY missed you!", "I'm bored!, Wanna play a game? Please? Pretty please? With sprinkles on top? Huh? Huh?", 
                "It is SO good to see you again!", "I thought you musta shriveled up and blown away with the wind or something!" ]
}

def getRandomItem(List items) {   
    return items?.get(new Random().nextInt(items?.size()));
}
