/**
 *  Activity Monitor
 *
 *  Copyright 2018 BARRY BURKE
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
 * Version History
 * 1.0.0	11/01/2018	Initial release
 * 1.0.1	11/02/2018	Rearranged settings sections
 * 1.0.2	11/06/2018	Added prioritized rooms for speakers, new icon
 * 1.0.3	11/07/2018	Cleaned up priority room search
 * 1.0.4	11/14/2018	Now supports Echo Speaks; more "who departed" deduction logic
 * 1.1.0	12/04/2018	Added Walkway and Mailbox monitors
 * 1.1.1	12/14/2018	Added separate Pushover sounds for driveway, walkway and mailbox
 */
def getVersionNum() { return "1.1.1" }
private def getVersionLabel() { return "${app.name} (${app.label}), v${getVersionNum()}" }
 
definition(
    name: 			"Activity Monitor",
    namespace: 		"sandood",
    author: 		"barry@thestorageanarchist.com",
    description: 	"Monitor various events and make intelligent Alexa announcements about them",
    category: 		"Safety & Security",
    iconUrl: 		"https://raw.githubusercontent.com/SANdood/Driveway-Monitor/master/images/driveway@1x.png",
    iconX2Url: 		"https://raw.githubusercontent.com/SANdood/Driveway-Monitor/master/images/driveway@2x.png",
    iconX3Url: 		"https://raw.githubusercontent.com/SANdood/Driveway-Monitor/master/images/driveway@3x.png")
      
preferences {
    page name:		"mainPage"
    page name:		"speakerPriority"
    page name:		"alexaPriority"
}

def mainPage() {
	dynamicPage(name: "mainPage", title: getVersionLabel(), install: true, uninstall: true) {
    	section(title: "Name for this ${app.name}") {
        	label title: "Name", required: true, defaultValue: "Activity Monitor"
        }
    	if (!settings?.tempDisable) {
            section("Driveway Sensors...") {
                paragraph "Entrance Sensor should be the first one to detect an arriving vehicle."
                input "entrySensor", "capability.contactSensor", title: "Entrance Sensor", multiple: false, required: true
                input "entryTrigger", "enum", title: "Entrance Sensor Trigger", required: true, defaultValue: 'closed', options: [ 'open', 'closed' ]
                input "entryToggle", "capability.momentary", title: "Arrival Momentary Switch", required: false, multiple: false

                paragraph "Exit Sensor should be the first one to detect a departing vehicle."
                input "exitSensor", "capability.contactSensor", title: "Exit Sensor", multiple: false, required: true
                input "exitTrigger", "enum", title: "Exit Sensor Trigger", required: true, defaultValue: 'closed', options: [ 'open', 'closed' ]
                input "exitToggle", "capability.momentary", title: "Departure Momementary Switch", required: false, multiple: false
			}
            section("Walkway Sensors...") {
            	paragraph "Entrance Sensor should be the first one to detect an arriving pedestrian."
                input "walkEntrySensor", "capability.contactSensor", title: "Walkway Entrance Sensor (optional)", multiple : false, required: false, submitOnChange: true
                if (settings?.walkEntrySensor != null) input "walkEntryTrigger", "enum", title: "Walkway Entrance Sensor Trigger", required: true, defaultValue: 'closed', options: [ 'open', 'closed' ]
                
                paragraph "Exit Sensors should be the first motion sensors to detect a departing pedestrian."
                input "walkExitSensors", "capability.motionSensor", title: "Walkway Exit Motion Sensors", multiple: true, required: false
                //if (settings?.walkExit) input "walkExitTrigger", "enum", title: "Walkway Exit Sensor Trigger", required: true, defaultValue: 'closed', options: [ 'open', 'closed' ]
                input "exitDoor", "capability.contactSensor", title: "Exit Door Open/Close Sensor", multiple: false, required: false
            }
            section("Mailbox Sensor...") {
            	paragraph "Mailbox Sensor must be configured as Normally Closed (alert on open). The You Have Mail switch should be manually turned off when you get your mail, and/or automatically turned off each night/morning."
                input "mailSensor", "capability.contactSensor", title: "Mailbox Sensor (optional)", multiple: false, required: false
                input "mailSwitch", "capability.switch", title: "You Have Mail switch", multiple: false, required: (settings?.mailSensor != null)
            }
            section("Driver Detection...") {
				paragraph "Use these devices to identify known drivers/passengers/autos."
                input "presenceSensors", "capability.presenceSensor", title: "Which Presence Sensors?", multiple: true, required: false
                input "garageDoors", "capability.contactSensor", title: "Named doors", multiple: true, required: false
            }
            section("Notification Selections...") {
                input "push", "bool", title: "Send SmartThings Push notifications?", defaultValue: false
                input "smsText", "bool", title: "Send SMS notifications?", defaultValue: false, submitOnChange: true
                if (!settings?.alexaText) input "speakText", "bool", title: "Announce over SONOS speakers?", defaultValue: false, submitOnChange: true
                if (!settings?.speakText) input "alexaText", "bool", title: "Announce over Echo Speaks?", defaultValue: false, submitOnChange: true
                input "pushOver", "bool", title: "Send PushOver notification?", defaultValue: false, submitOnChange: true
            }
            if (settings?.smsText) {
                section("SMS Settings") {
                    paragraph "You can enter multiple phone numbers seperated by a semi-colon (;)"
                    input "phone", "string", title: "Send SMS notifications to", description: "Phone Number(s)", required: true, submitOnChange: true 
                }
            }
            if (settings?.speakText) {
                section("Speaker Settings") {
                    input "volume", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100"
                    input "relativeVolume", "bool", title: "Use relative volume", defaultValue: false
                    input "sonosServerIP", "string", title: "Sonos HTTP API Server IP Address", description: "Enter your Sonos API Server's IP Address", required: true
                    input "sonosServerPort", "string", title: "Sonos HTTP API Server Port", description: "Enter your Sonos API Server's Port", defaultValue: 5005 , required: true
                    input "voice", "enum", title: "Text-to-Speech Voice", description: "Select your desired Voice", defaultValue: "Salli", 
                        options: ["Ivy", "Joanna", "Joey", "Justin", "Kendra", "Kimberly", "Matthew", "Salli"], required: true
                    input "speakModes", "mode", title: "Announce only when in these modes...", multiple: true, required: false
                }
                section(''){            
            		href(name: 'speakerPriority', title: 'Speaker Rooms and Priority', required: false, page: 'speakerPriority',
                		//image: 'http://www.plaidsystems.com/smartthings/st_zone16_225.png',
                		description: "${getSpeakerPriority()}"
            		)
                }
            } 
            if (settings?.alexaText) {
            	section("Echo Speaks Settings") {
                	input "volume", "number", title: "Set default announcement volume to", description: "0-100%", required: false, range: "0..100", submitOnChange: true
                    // if (settings?.volume) input "normalVolume", "number", title: "Set normal volume to", description: "0-100%", required: false, range: "0..100", submitOnChange: true
                	input "alexaModes", "mode", title: "Announce only when in these modes...", multiple: true, required: false
                }
                section(''){            
            		href(name: 'alexaPriority', title: 'Echo Speaks Rooms and Priority', required: false, page: 'alexaPriority',
                		//image: 'http://www.plaidsystems.com/smartthings/st_zone16_225.png',
                		description: "${getAlexaPriority()}"
            		)
                }
            }
            if (settings?.pushOver) {
                section("PushOver Settings") {
                    input "apiKey", "text", title: "API Key", required: true
                    input "userKey", "text", title: "User Key", required: true
                    input "deviceName", "text", title: "Device Name (blank for all)", required: false
                    input "priority", "enum", title: "Priority", required: true, options: [ '0':'Normal', '-1':'Low', '1':'High', '2':'Emergency' ]
                    input "sound", "enum", title: "Pushover alert sound for driveway events", required: false, options: [ 
                        'pushover', 'bike', 'bugle', 'cashregister', 'classical', 'cosmic',
                        'falling', 'gamelan', 'incoming', 'intermission', 'magic', 'mechanical', 'pianobar',
                        'siren', 'spacealarm', 'tugboat', 'alien', 'climb', 'persistent', 'echo', 'updown', 'none'
                    ]
                    input "walkSound", "enum", title: "Pushover alert sound for walkway events", required: false, options: [ 
                        'pushover', 'bike', 'bugle', 'cashregister', 'classical', 'cosmic',
                        'falling', 'gamelan', 'incoming', 'intermission', 'magic', 'mechanical', 'pianobar',
                        'siren', 'spacealarm', 'tugboat', 'alien', 'climb', 'persistent', 'echo', 'updown', 'none'
                    ]
                    input "mailSound", "enum", title: "Pushover alert sound for mailbox events", required: false, options: [ 
                        'pushover', 'bike', 'bugle', 'cashregister', 'classical', 'cosmic',
                        'falling', 'gamelan', 'incoming', 'intermission', 'magic', 'mechanical', 'pianobar',
                        'siren', 'spacealarm', 'tugboat', 'alien', 'climb', 'persistent', 'echo', 'updown', 'none'
                    ]
                }
            }
        } else {
        	section("Disabled") {
        		// We are currently disabled
            	paragraph "WARNING: Temporarily Disabled as requested. Turn back on below to re-activate."
            }
        }
        section(title: "Temporarily Disable?") {
           	input "tempDisable", "bool", title: "Temporarily disable this Monitor? ", defaultValue: false, required: false, description: "", submitOnChange: true            
        }
        section (getVersionLabel())
    }
}

def speakerPriority() {
	dynamicPage(name: "speakerPriority", title: "Setup Room/Speaker Priority", install: true, uninstall: false) {
    	section(title: "Priority 1") {
        	input "room1", "capability.beacon", title: "Select room", multiple: false, required: true
            input "speaker1", "capability.musicPlayer", title: "Select SONOS speaker", required: true, multiple: false
        }
		section(title: "Priority 2") {
            input "room2", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
            input "speaker2", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room2 != null), multiple: false
        }
        if (settings?.room2 || settings?.room3) {
            section(title: "Priority 3") {
                input "room3", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker3", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room3 != null), multiple: false
            }
        }
        if (settings?.room3 || settings?.room4) {
            section(title: "Priority 4") {
                input "room4", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker4", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room4 != null), multiple: false
            }
        }
        if (settings?.room4 || settings?.room5) {
            section(title: "Priority 5") {
                input "room5", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker5", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room5 != null), multiple: false
            }
        }
        if (settings?.room5 || settings?.room6) {
            section(title: "Priority 6") {
                input "room6", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker6", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room6 != null), multiple: false
            }
        }
        if (settings?.room6 || settings?.room7) {
            section(title: "Priority 7") {
                input "room7", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker7", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room7 != null), multiple: false
            }
        }
        if (settings?.room7 || settings?.room8) {
            section(title: "Priority 8") {
                input "room8", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker8", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room8 != null), multiple: false
            }
        }
        if (settings?.room8 || settings?.room9) {
            section(title: "Priority 9") {
                input "room9", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker9", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room9 != null), multiple: false
            }
        }
        if (settings?.room9 || settings?.room10) {
            section(title: "Priority 10") {
                input "room10", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "speaker10", "capability.musicPlayer", title: "Select SONOS speaker", required: (settings?.room10 != null), multiple: false
            }
        }
    }
}

def getSpeakerPriority() {
	if (!settings.room1) {
    	return "Prioritized Rooms & Speakers not yet defined...tap to configure"
    } else {
    	String rooms = "1: ${settings.room1}->${settings.speaker1}"
        if (settings.room2) {
        	rooms += "\n2: ${settings.room2}->${settings.speaker2}"
        }
        if (settings.room3) {
            rooms += "\n3: ${settings.room3}->${settings.speaker3}"
        }
        if (settings.room4) {
            rooms += "\n4: ${settings.room4}->${settings.speaker4}"
        }
        if (settings.room5) {
            rooms += "\n5: ${settings.room5}->${settings.speaker5}"
        }
        if (settings.room6) {
            rooms += "\n6: ${settings.room6}->${settings.speaker6}"
        }
        if (settings.room7) {
            rooms += "\n7: ${settings.room7}->${settings.speaker7}"
        }
        if (settings.room8) {
            rooms += "\n8: ${settings.room8}->${settings.speaker8}"
        }
        if (settings.room9) {
            rooms += "\n9: ${settings.room9}->${settings.speaker9}"
        }
        if (settings.room10) {
            rooms += "\n10: ${settings.room10}->${settings.speaker10}"
        }
        return rooms
    }
}

def alexaPriority() {
	dynamicPage(name: "alexaPriority", title: "Setup Room/Echo Priority", install: true, uninstall: false) {
    	section(title: "Priority 1") {
        	input "room1", "capability.beacon", title: "Select room", multiple: false, required: true
            input "echo1", "device.echoSpeaksDevice", title: "Select Echo device", required: true, multiple: true
            input "announceVol1", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
        }
		section(title: "Priority 2") {
            input "room2", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
            input "echo2", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room2 != null), multiple: true
            input "announceVol2", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
        }
        if (settings?.room2 || settings?.room3) {
            section(title: "Priority 3") {
                input "room3", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo3", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room3 != null), multiple: true
                input "announceVol3", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room3 || settings?.room4) {
            section(title: "Priority 4") {
                input "room4", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo4", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room4 != null), multiple: true
                input "announceVol4", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room4 || settings?.room5) {
            section(title: "Priority 5") {
                input "room5", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo5", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room5 != null), multiple: true
                input "announceVol5", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room5 || settings?.room6) {
            section(title: "Priority 6") {
                input "room6", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo6", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room6 != null), multiple: true
                input "announceVol6", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room6 || settings?.room7) {
            section(title: "Priority 7") {
                input "room7", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo7", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room7 != null), multiple: true
                input "announceVol7", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room7 || settings?.room8) {
            section(title: "Priority 8") {
                input "room8", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo8", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room8 != null), multiple: true
                input "announceVol8", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room8 || settings?.room9) {
            section(title: "Priority 9") {
                input "room9", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo9", "device.echoSpeaksDevice", title: "Select Echo device", required: tr(settings?.room9 != null), multiple: true
                input "announceVol9", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
        if (settings?.room9 || settings?.room10) {
            section(title: "Priority 10") {
                input "room10", "capability.beacon", title: "Select room", multiple: false, required: false, submitOnChange: true
                input "echo10", "device.echoSpeaksDevice", title: "Select Echo device", required: (settings?.room10 != null), multiple: true
                input "announceVol10", "number", title: "Set announcement volume to", description: "0-100%", required: false, range: "0..100", defaultValue: settings?.volume
            }
        }
    }
}

def getAlexaPriority() {
	if ((settings?.room1 == null) || (settings?.echo1 == null)) {
    	return "Prioritized Rooms & Echo devices not yet defined...tap to configure"
    } else {
    	String rooms = "1: ${settings.room1}->${settings.echo1.toString()[1..-2]}"
        if (settings?.room2 && settings?.echo2) {
        	rooms += "\n2: ${settings.room2}->${settings.echo2.toString()[1..-2]}"
        }
        if (settings?.room3 && settings?.echo3) {
            rooms += "\n3: ${settings.room3}->${settings.echo3.toString()[1..-2]}"
        }
        if (settings?.room4 && settings?.echo4) {
            rooms += "\n4: ${settings.room4}->${settings.echo4.toString()[1..-2]}"
        }
        if (settings?.room5 && settings?.echo5) {
            rooms += "\n5: ${settings.room5}->${settings.echo5.toString()[1..-2]}"
        }
        if (settings?.room6 && settings?.echo6) {
            rooms += "\n6: ${settings.room6}->${settings.echo6.toString()[1..-2]}"
        }
        if (settings?.room7 && settings?.echo7) {
            rooms += "\n7: ${settings.room7}->${settings.echo7.toString()[1..-2]}"
        }
        if (settings?.room8 && settings?.echo8) {
            rooms += "\n8: ${settings.room8}->${settings.echo8.toString()[1..-2]}"
        }
        if (settings?.room9 && settings?.echo9) {
            rooms += "\n9: ${settings.room9}->${settings.echo9.toString()[1..-2]}"
        }
        if (settings?.room10 && settings?.echo10) {
            rooms += "\n10: ${settings.room10}->${settings.echo10.toString()[1..-2]}"
        }
        return rooms
    }
}

def installed()
{
    log.debug "${app.label} installed with settings: ${settings}"
    if (!settings?.tempDisable) initialize( 'installed' )
}

def updated()
{
    log.debug "${app.label}' updated with settings: ${settings}"
    unsubscribe()
    unschedule()
    if (!settings?.tempDisable) {
    	initialize( 'updated' )
    } else {
    	if (settings?.speakText && (settings?.speakers || settings?.group) && settings.sonosServerIP && settings.sonosServerPort) {
        	sendMessage( "${app.label} at ${location.name} has been temperarily disabled." )
            // sendMessage("This is a test. This is only a test")
        } 
    }
}

def initialize( why )
{
	atomicState.handlingDeparture = false
    atomicState.handlingArrival = false
    
	subscribe( settings.entrySensor, "contact.${settings.entryTrigger}", exitHandler )
    subscribe( settings.exitSensor, "contact.${settings.exitTrigger}", entryHandler )
    if (settings.mailSensor) subscribe( settings.mailSensor, "contact.open", mailHandler )
    if (settings.walkEntrySensor) subscribe( settings.walkEntrySensor, "contact.${settings.walkEntryTrigger}", walkEntryHandler )
    if (settings.walkExitSensors) subscribe( settings.walkExitSensors, "motion.active", walkExitHandler )
    subscribe(app, appTouch)
	// sendMessage("The ${app.label} version ${getVersionNum()} at ${location.name} was successfully ${why}!")
}

def appTouch( evt ) {
	log.trace "appTouch()"
    pause()
}

def pause() {
	if (atomicState.pauseDetection) {
    	unschedule(unPause)
    	atomicState.pauseDetection = false
        sendMessage( "${location.name} ${app.label} activity monitoring has been resumed.")
    } else {
    	atomicState.pauseDetection = true
        runIn(1800, unPause, [overwrite: true]) // unpause in 30 minutes
        sendMessage( "${location.name} ${app.label} activity monitoring is paused for 30 minutes.")
    }
}

def unPause() {
	if (atomicState.pauseDetection) {
    	atomicState.pauseDetection = false
        sendMessage( "${location.name} ${app.label} activity monitoring is now resumed." )
    }
}

def mailHandler(evt) {
	log.trace "mailHandler(): ${evt.device.displayName} ${evt.value} (${settings?.mailSwitch?.currentSwitch})"
	if (settings?.mailSwitch != null) {
    	if (settings.mailSwitch.currentSwitch == 'off') {
        	settings.mailSwitch.on()
            log.info "You've got mail!"
    		sendMessage(getRandomItem(youGotMail()), settings.mailsound )
        } else {
        	// Let's see if someone just got home - they probably grabbed the mail...
            def whoArrived = ""
            if (settings.presenceSensors) {
                def lastFiveMinutes
                use(groovy.time.TimeCategory) { lastFiveMinutes = new Date() - 5.minute }
                settings.presenceSensors.each { person -> 
                    def events = person.eventsSince( lastFiveMinutes )
                    def search = true
                    events.each {
                        // log.debug "entryHandler(): ${person.displayName}, ${it.name}, ${it.value}"
                        if (search && (it.name == 'presence')) {    // we only want the last presence event for each person (first in the person's event list)
                            search = false
                            if (it.value == 'present') {
                                whoArrived = (whoArrived == "") ? person.displayName : whoArrived + ' and ' + person.displayName
                                // who = who == [] ? [person.displayName] : who << person.displayName
                            }
                        }
                    }
                }
                log.info "mailHandler(): Arrivals since ${lastFiveMinutes.format("yyyy-MM-dd HH:mm:ss", location.timeZone)}: ${whoArrived} (${who})"
            }
            if (whoArrived != "") {
            	log.info "${whoArrived} just came home and they got the mail..."
                settings.mailSwitch.off()
                sendMessage("${whoArrived} just arrived and ${whoArrived.contains(' and ')?'they are':'is'} picking up the mail.", mailSound)
            } else {
              	log.warn "Mailbox was opened again!"
                sendMessage("Somebody opened the mailbox again!", mailSound)
            }
        }
    } else {
    	sendMessage(getRandomItem(youGotMail()), settings.mailsound )
    }
}

def walkExitHandler(evt) {
	log.trace "walkExitHandler(): ${evt.device.displayName} ${evt.value}"
    if (atomicState.pauseDetection) { log.warn "Walkway detection has been paused"; return }
    
    if (!atomicState.handlingArrival) {
    	// First exit sensor to register motion, and we're not handling an arrival
    	if (atomicState.handlingDeparture) {
        	// Already handling a Departure
            return
        } else {
        	// First to see a departure
        	atomicState.handlingDeparture = true
        	atomicState.departureMessage = ""
        }
    } else {
    	// We're handling an Arrival right now, and they made it to the door - give a couple seconds for all the exit sensors to settle, then clear the arrival
    	runIn( 2, arrivalCompleted, [overwrite: true])
        return
	}

    // did anyone arrive in the last 2 minutes? Maybe they're leaving already...
    if (atomicState.lastWalkerArrivedAt?.isNumber() && ((now() - atomicState.lastWalkerArrivedAt) < 120000)) {
    	// If it's a delivery, they tend to come and go pretty quickly
        if (atomicState.lastWalker != "") {
        	atomicState.departureMessage = "It looks like ${atomicState.lastWalker} ${atomicState.lastWalker.contains(' and ')?'are':'is'} leaving ${location.name} already."
            atomicState.departureTime = now()
        } else {
        	atomicState.departureMessage( "It looks like whoever just arrived at ${location.name} is leaving already - it probably was just another delivery." )
            atomicState.departureTime = now()
        }
    } else if (settings.exitDoor) {
    	// Check if the door just opened/closed - somebody's leaving
        log.trace "walkExitHandler(): Checking ${settings.exitDoor.displayName}..."
        def isResident = false
        def lastOneMinute
    	use(groovy.time.TimeCategory) { lastOneMinute = new Date() - 1.minute }
        def events = settings.exitDoor.eventsSince( lastOneMinute )
        events?.each {
        	log.debug "walkExitHandler(): ${settings.exitDoor.displayName} event - ${it.name}, ${it.value}"
        	if (it.name == 'contact') isResident = true
        }
        atomicState.departureMessage = getRandomItem( isResident ? residentLeaving() : unknownLeaving() )	// "someone from inside"
        atomicState.departureTime = now()
    } else {
    	atomicState.departureMessage = getRandomItem( unknownLeaving() )
        atomicState.departureTime = now()
    }
	// atomicState.handlingDeparture = false
    // delay clearing the flag a bit, because they don't all register at the same time
    runIn( 5, departureCompleted, [overwrite: true] )
}

def departureCompleted() {
	atomicState.handlingDeparture = false
}

def arrivalCompleted() {
	atomicState.handlingArrival = false
}

def walkEntryHandler(evt) {
	log.trace "walkEntryHandler(): ${evt.device.displayName} ${evt.value}"
    if (atomicState.pauseDetection) { log.warn "Walkway detection has been paused"; return }
    
    // Handle the delayed departure message, if any
    if (atomicState.departureMessage != "" && (atomicState.departureTime?.isNumber && ((now() - atomicState.departureTime) < 300000))) {
    	sendMessage(atomicState.departureMessage, settings.walkSound)
        atomicState.departureMessage = ""
        atomicState.departureTime = null
        atomicState.lastWalker = ""
        atomicState.lastWalkerArrivedAt = null
        atomicState.handlingArrival = false
        departureCompleted()
        return
    }
    atomicState.handlingArrival = true
    
    // check if anyone just drove in, and make sure it wasn't in Barry's or Paula's car
    def whoArrived = ""
    if ((atomicState.lastArrivedList != []) && ((atomicState.lastArrivedAt != null) && ((now() - atomicState.lastArrivedAt) < 300000)))  {
    	def theList = atomicState.lastArrivedList
        theList.each {
        	// Skip "Barry's M37x" and "Paula's QX30" - note, also will skip "Joe's iPhone" and "Tom's Presence Sensor"
        	if (!it.contains("'s")) whoArrived = (whoArrived == "") ? whoArrived : (whoArrived + " and " + it)
        }
    }
    if (whoArrived != "") {
        // somebody known just drove in, let's just assume they're the walker
        atomicState.lastWalker = whoArrived
        atomicState.lastWalkerArrivedAt = now()
        sendMessage(getRandomItem(knownWalker()) + whoArrived + '.', settings.walkSound)
    } else {
        // let's see if we can figure out who it is...maybe a known person walked up
        def who = []
        def lastTwoMinutes
        use(groovy.time.TimeCategory) { lastTwoMinutes = new Date() - 2.minute }
        if (settings.presenceSensors) {	
            settings.presenceSensors.each { person -> 
                if (!person.displayName.contains("'s ")) {	// don't check Barry's Car
                    def events = person.eventsSince( lastTwoMinutes )
                    def search = true
                    events.each {
                        log.debug "walkEntryHandler(): ${person.displayName}, ${it.name}, ${it.value}"
                        if (search && (it.name == 'presence')) {    // we only want the last presence event for each person (first in the person's event list)
                            search = false
                            if (it.value == 'present') {
                                whoArrived = (whoArrived == "") ? person.displayName : whoArrived + ' and ' + person.displayName
                                who = who == [] ? [person.displayName] : who << person.displayName
                            }
                        }
                    }
                }
            }
        }
        if (whoArrived != "") {
        	// We have a good idea who just arrived
        	log.info "walkEntryHandler(): Arrivals since ${lastTwoMinutes.format("yyyy-MM-dd HH:mm:ss", location.timeZone)}: ${whoArrived} (${who})"
            atomicState.lastWalker = whoArrived
            atomicState.lastWalkerArrivedAt = now()
            sendMessage(getRandomItem(knownWalker()) + whoArrived, settings.walkSound)
        } else {
        	// We have no idea who just arrived
            atomicState.lastWalker = ""
            atomicState.lastWalkerArrivedAt = now()
            sendMessage(getRandomItem(unknownWalker()), settings.walkSound)
        }
    }
    runIn( 5, arrivalCompleted, [overwrite: true] ) // atomicState.handlingArrival = false
}

// exit handler called when the entry sensor is triggered (it's actually an exit if the exit sensor is currently triggered)
def exitHandler(evt) {
	log.trace "exitHandler(): ${evt.device.displayName} ${settings.entryTrigger}"
    if (atomicState.pauseDetection) { log.warn "exitHandler(): Driveway detection has been paused"; return }
  
	def whoDeparted = ""
    def who = []
    // Did they trip the exit sensor first?
	if (settings.exitSensor.currentContact == settings.exitTrigger) {
    	// Somebody just arrived, let's see if we can figure out who
        if (settings.presenceSensors) {
       		def lastFiveMinutes
    		use(groovy.time.TimeCategory) { lastFiveMinutes = new Date() - 5.minute }
    		presenceSensors.each { person -> 
            	log.debug "exitHandler(): Person: ${person.displayName}"
    			def events = person.eventsSince( lastFiveMinutes )
                def search = true
                events.each { pevent ->
               		if (search && (pevent.name == 'presence')) {    // we only want the last presence event for each person (first in the person's event list)
                    	log.debug "exitHandler(): ${person.displayName}, ${pevent.name}, ${pevent.value}"
                    	search = false
                        if (pevent.value == 'not present') {
                   			whoDeparted = (whoDeparted == "") ? person.displayName : whoDeparted + ' and/or ' + person.displayName
                            who = who == [] ? [person.displayName] : who << person.displayName
                        }
                    }
        		}
                if ((who == []) || !who?.contains(person.displayName)) search = true
                if (search && settings?.garageDoors) {
                	garageDoors.each { garage ->
                    	if (garage.displayName.contains(person.displayName)) {
                        	log.debug "Door: ${garage.displayName}"
                        	// Let's see if this person's garage door recently moved
                            def gevents = garage.eventsSince( lastFiveMinutes )
                            gevents.each { gevent ->
                            	// check for anything that indicates the door just moved (dependent upon which sensor types were selected)
                            	if (search && ((gevent.name == 'door') || (gevent.name == 'doorMoving') || (gevent.name == 'acceleration') || (gevent.name == 'contact'))) {
                                	log.debug "${garage.displayName} event ${gevent.name}"
                                	search = false
                                    whoDeparted = (whoDeparted == "") ? person.displayName : whoDeparted + ' and/or ' + person.displayName
                            		who = who == [] ? [person.displayName] : who << person.displayName
                                }
                            }
                        }
                    }
                }
            }
            if (whoDeparted != "") log.info "exitHandler(): Departures since ${lastFiveMinutes.format("yyyy-MM-dd HH:mm:ss", location.timeZone)}: ${whoDeparted} (${who})"
            if ((whoDeparted == "") && (who == [])) {
                if (atomicState.lastArrivedAt) {
                    if (atomicState.lastArrived == "") {
                        if ((now() - atomicState.lastArrivedAt) < 300000) {
                            // don't know who's leaving, but it is probably the unknown that just arrived within the last 5 minutes
                            whoDeparted = getRandomItem(["a delivery", "just a delivery", "something being delivered", "another Amazon delivery"])
                            who = [whoDeparted]
                            log.info "exitHandler(): Arrivals since ${lastFiveMinutes.format("yyyy-MM-dd HH:mm:ss", location.timeZone)}: ${whoDeparted} (${who})"
                        }
                    }
                }
            }
            if ((whoDeparted == "") && (who == [])) {
                // Let's see if there is only 1 person at home right now
                def i = 0
                def whoDat
                presenceSensors.each { p ->
                    if (p.currentPresence == "present") i += 1
                    if ( i == 1 ) whoDat = p
                }
                if (i == 1) {
                    // Only one known person at home now, it's probably that person leaving now.
                    whoDeparted = whoDat.displayName + " (well, maybe)"
                    who = [whoDeparted]
                    log.info "exitHandler(): Only ${whoDeparted} is home right now, that must (might) be who left."
                }     	
            }
    	}

        // If we have an unknown Departure, the announce the departure even if we're in night mode
        String msgText
        def nightOverride = false
    	if (whoDeparted != "") {
        	def phrase1 = getRandomItem( ["A vehicle", "Somebody"] )
            def phrase2 = getRandomItem( ["has departed from", "just left", "just departed"] )
        	msgText = "${phrase1} ${phrase2} ${location.name}. ${getRandomItem(whozitDeparted())} ${whoDeparted}."
        } else {
        	msgText = getRandomItem(iDunnoDeparted())
        	if (location.mode == 'night') nightOverride = true
        }   
    	sendMessage(msgText, settings.sound, nightOverride)
        if (settings.exitToggle) settings.exitToggle.push()
        return
    } // else { // somebody is driving in, because the exit sensor wasn't tripped yet }
}

// entry handler called when the exit sensor is triggered
def entryHandler(evt) {
	log.trace "entryHandler(): ${evt.device.displayName} ${settings.exitTrigger}"
    if (atomicState.pauseDetection) { log.warn "entryHandler(): Detection has been paused"; return }
	def whoArrived = ""
    def who = []
    // Did they trip the entry sensor first?
	if (settings.entrySensor.currentContact == settings.entryTrigger) {
    	// Somebody just arrived, let's see if we can figure out who
        if (settings.presenceSensors) {
       		def lastFiveMinutes
    		use(groovy.time.TimeCategory) { lastFiveMinutes = new Date() - 5.minute }
    		settings.presenceSensors.each { person -> 
    			def events = person.eventsSince( lastFiveMinutes )
                def search = true
                events.each {
                	log.debug "entryHandler(): ${person.displayName}, ${it.name}, ${it.value}"
               		if (search && (it.name == 'presence')) {    // we only want the last presence event for each person (first in the person's event list)
                    	search = false
                        if (it.value == 'present') {
                   			whoArrived = (whoArrived == "") ? person.displayName : whoArrived + ' and ' + person.displayName
                            who = who == [] ? [person.displayName] : who << person.displayName
                        }
                    }
        		}
            }
            log.info "entryHandler(): Arrivals since ${lastFiveMinutes.format("yyyy-MM-dd HH:mm:ss", location.timeZone)}: ${whoArrived} (${who})"
    	}    
        
        // If we have an unknown Arrival, the announce the arrival even if we're in night mode
        String msgText
        def nightOverride = false
    	if (whoArrived != "") {
        	def phrase1 = getRandomItem( ["A vehicle", "Somebody"] )
            def phrase2 = getRandomItem( ["has arrived at", "just pulled into", "is coming up the driveway at"] )
        	msgText = "${phrase1} ${phrase2} ${location.name}. ${getRandomItem(whozitArrived())} ${whoArrived}."
            atomicState.lastArrived = whoArrived
            atomicState.lastArrivedList = who
            atomicState.lastArrivedAt = now()
        } else {
        	msgText = getRandomItem(iDunnoArrived())
        	if (location.mode == 'night') nightOverride = true
            atomicState.lastArrived = ""
            atomicState.lastArrivedList = []
            atomicState.lastArrivedAt = now()
        }   
    	sendMessage(msgText, settings.sound, nightOverride)
        // atomicState.lastWalker = ""
        // atomicState.lastWalkerArrivedAt = null
        if (settings.entryToggle) settings.entryToggle.push()
        return
    }
}

private def sendMessage(notificationMessage, sound='none', nightOverride=false) {
	log.trace("Notification Message: ${notificationMessage} (${sound})")
    String msg = /* "${app.label} at ${location.name}: " + */ notificationMessage		// for those that have multiple locations, tell them where we are
    
    sendNotificationEvent( notificationMessage )								// Always send to hello home log
    
    // Send SMS to one or more users
    if (settings.smsText) { // check that the user did select a phone number
        if ( settings.phone.indexOf(";") > 0){
            def phones = settings.phone.split(";")
            for ( def i = 0; i < phones.size(); i++) {
                log.info("Sending SMS ${i+1} to ${phones[i]}")
                sendSmsMessage(phones[i], msg)
            }
        } else {
            log.info("Sending SMS to ${phone}")
            sendSmsMessage(phone, msg)
        }
    }
    
    // send a (global) Push message
    if (settings.push) {
        log.info("Sending Push to everyone")
        sendPushMessage(msg)
    }
    
    // Announce over speakers
    if (settings.speakText) {
    	if (settings.speakModes.contains(location.mode) || nightOverride) {
            String action
            String speaker = ""
            String room = ""
            if (settings?.room1?.currentValue('occupancy') == 'occupied') {
            	speaker = settings.speaker1.name
                room = settings.room1.displayName
            } else if (settings?.room2 && (settings.room2.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker2.name
                room = settings.room2.displayName
            } else if (settings?.room3 && (settings.room3.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker3.name
                room = settings.room3.displayName
            } else if (settings?.room4 && (settings.room4.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker4.name
                room = settings.room4.displayName
            } else if (settings?.room5 && (settings.room5.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker5.name
                room = settings.room5.displayName
            } else if (settings?.room6 && (settings.room6.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker6.name
                room = settings.room6.displayName
            } else if (settings?.room7 && (settings.room7.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker7.name
                room = settings.room7.displayName
            } else if (settings?.room8 && (settings.room8.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker8.name
                room = settings.room8.displayName
            } else if (settings?.room9 && (settings.room9.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker9.name
                room = settings.room9.displayName
            } else if (settings?.room10 && (settings.room10.currentValue('occupancy') == 'occupied')) {
            	speaker = settings.speaker10.name
                room = settings.room10.displayName
            }
            if (speaker != "") {
            	log.info "Highest priority room that is occupied is ${room}"
            } else {
                if (settings?.room1?.currentValue('occupancy') == 'engaged') {
                    speaker = settings.speaker1.name
                    room = settings.room1.displayName
                } else if (settings?.room2 && (settings.room2.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker2.name
                    room = settings.room2.displayName
                } else if (settings?.room3 && (settings.room3.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker3.name
                    room = settings.room3.displayName
                } else if (settings?.room4 && (settings.room4.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker4.name
                    room = settings.room4.displayName
                } else if (settings?.room5 && (settings.room5.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker5.name
                    room = settings.room5.displayName
                } else if (settings?.room6 && (settings.room6.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker6.name
                    room = settings.room6.displayName
                } else if (settings?.room7 && (settings.room7.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker7.name
                    room = settings.room7.displayName
                } else if (settings?.room8 && (settings.room8.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker8.name
                    room = settings.room8.displayName
                } else if (settings?.room9 && (settings.room9.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker9.name
                    room = settings.room9.displayName
                } else if (settings?.room10 && (settings.room10.currentValue('occupancy') == 'engaged')) {
                    speaker = settings.speaker10.name
                    room = settings.room10.displayName
                }
                if (speaker != "") {
                    log.info "Highest priority room that is engaged is ${room}"
                } else {
                    speaker = settings.speaker1.name
                    room = settings.room1.displayName
                    log.info "No rooms 'occupied' or 'engaged', defaulting to Priority 1: ${room}"
                }
            }
            speaker = speaker.contains(' (Sonos)') ? speaker - ' (Sonos)' : speaker
            action = 'say'
            log.info "Announcing over SONOS speaker \"${speaker}\""
            sendSonosCommand( speaker, action, msg )
        } else {
        	log.trace "Location mode is currently \"${location.mode},\" announcements are disabled"
        }
    }

	// Announce via Echo Speaks
    if (settings.alexaText) {
    	if (settings.alexaModes.contains(location.mode) || nightOverride) {
            String action
            def echo = null
            def announceVol = settings.volume
            String room = ""
            if (settings?.room1?.currentValue('occupancy') == 'occupied') {
            	echo = settings.echo1
                room = settings.room1.displayName
                if (settings.announceVol1) announceVol = settings.announceVol1
            } else if (settings?.room2 && (settings.room2.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo2
                room = settings.room2.displayName
                if (settings.announceVol2) announceVol = settings.announceVol2
            } else if (settings?.room3 && (settings.room3.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo3
                room = settings.room3.displayName
                if (settings.announceVol3) announceVol = settings.announceVol3
            } else if (settings?.room4 && (settings.room4.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo4
                room = settings.room4.displayName
                if (settings.announceVol4) announceVol = settings.announceVol4
            } else if (settings?.room5 && (settings.room5.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo5
                room = settings.room5.displayName
                if (settings.announceVol5) announceVol = settings.announceVol5
            } else if (settings?.room6 && (settings.room6.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo6
                room = settings.room6.displayName
                if (settings.announceVol6) announceVol = settings.announceVol6
            } else if (settings?.room7 && (settings.room7.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo7
                room = settings.room7.displayName
                if (settings.announceVol7) announceVol = settings.announceVol7
            } else if (settings?.room8 && (settings.room8.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo8
                room = settings.room8.displayName
                if (settings.announceVol8) announceVol = settings.announceVol8
            } else if (settings?.room9 && (settings.room9.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo9
                room = settings.room9.displayName
                if (settings.announceVol9) announceVol = settings.announceVol9
            } else if (settings?.room10 && (settings.room10.currentValue('occupancy') == 'occupied')) {
            	echo = settings.echo10
                room = settings.room10.displayName
                if (settings.announceVol10) announceVol = settings.announceVol10
            }
            if (echo != null) {
            	log.info "Highest priority room that is occupied is ${room}"
            } else {
                if (settings?.room1?.currentValue('occupancy') == 'engaged') {
                    echo = settings.echo1
                    room = settings.room1.displayName
                    if (settings.announceVol1) announceVol = settings.announceVol1
                } else if (settings?.room2 && (settings.room2.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo2
                    room = settings.room2.displayName
                    if (settings.announceVol2) announceVol = settings.announceVol2
                } else if (settings?.room3 && (settings.room3.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo3
                    room = settings.room3.displayName
                    if (settings.announceVol3) announceVol = settings.announceVol3
                } else if (settings?.room4 && (settings.room4.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo4
                    room = settings.room4.displayName
                    if (settings.announceVol4) announceVol = settings.announceVol4
                } else if (settings?.room5 && (settings.room5.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo5
                    room = settings.room5.displayName
                    if (settings.announceVol5) announceVol = settings.announceVol5
                } else if (settings?.room6 && (settings.room6.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo6
                    room = settings.room6.displayName
                    if (settings.announceVol6) announceVol = settings.announceVol6
                } else if (settings?.room7 && (settings.room7.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo7
                    room = settings.room7.displayName
                    if (settings.announceVol7) announceVol = settings.announceVol7
                } else if (settings?.room8 && (settings.room8.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo8
                    room = settings.room8.displayName
                    if (settings.announceVol8) announceVol = settings.announceVol8
                } else if (settings?.room9 && (settings.room9.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo9
                    room = settings.room9.displayName
                    if (settings.announceVol9) announceVol = settings.announceVol9
                } else if (settings?.room10 && (settings.room10.currentValue('occupancy') == 'engaged')) {
                    echo = settings.echo10
                    room = settings.room10.displayName
                    if (settings.announceVol10) announceVol = settings.announceVol10
                }
                if (echo != null) {
                    log.info "Highest priority room that is engaged is ${room}"
                } else {
                    echo = settings.echo1
                    room = settings.room1.displayName
                    if (settings.announce1) announceVol = settings.announceVol1
                    log.info "No rooms 'occupied' or 'engaged', defaulting to Priority 1: ${room}"
                }
            }
            echo.setVolumeSpeakAndRestore( announceVol, msg )
            // if (normalVol && (settings.volume != normalVol)) echo.setVolume( normalVolume )
        } else {
        	log.trace "Location mode is currently \"${location.mode},\" announcements are disabled"
        }
    }
    
	// PushOver
    if (settings.pushOver && apiKey && userKey) {
      	log.trace "Sending Pushover with API Key [$apiKey] and User Key [$userKey] with sound=${sound}"
      
      	def postBody = []
      	def pushPriority = settings.priority.toInteger()
		// log.trace "priority: ${pushPriority}"
      	// log.trace "sound: ${settings.sound}"
      
      	if(settings.deviceName) {
        	log.trace "Sending Pushover to Device: $deviceName"
        	if(pushPriority == 2) {
          		postBody = [token: "$apiKey", user: "$userKey", device: "$deviceName", message: "${msg}", priority: "$pushPriority", retry: "60", expire: "3600", sound: sound]
        	} else {
          		postBody = [token: "$apiKey", user: "$userKey", device: "$deviceName", message: "${msg}", priority: "$pushPriority", sound: sound]
        	}
      	} else {
        	log.trace "Sending Pushover to All Devices"
            if(pushPriority == 2) {
          		postBody = [token: "$apiKey", user: "$userKey", message: "${msg}", priority: "$pushPriority", retry: "60", expire: "3600", sound: sound]
        	} else {
          		postBody = [token: "$apiKey", user: "$userKey", message: "${msg}", priority: "$pushPriority", sound: sound]
        	}
      	}
        
        log.trace "postBody: ${postBody}"
      	def params = [
      		uri: 'https://api.pushover.net/1/messages.json',
            body: postBody
        ]
      
      	httpPost(params){ response ->
        	log.info "Response Received: Status [$response.status]"
          
          	if(response.status != 200) {
            	log.error "Received HTTP Error Response (${response.status}). Check Install Parameters."
          	}
      	}
    }
}

// Execute the hubAction request
def sendSonosCommand(String speaker, String action, String parameters) {
	String command = '/'
	if (['sayall', 'pauseall', 'resumeall'].contains(action)) {
    	command += "${action}"
    } else {
    	command += "${speaker}/${action}"
    }
    if (parameters) command += "/" + parameters
    if (['say', 'sayall'].contains(action)) {
    	if (settings.voice) command += "/${settings.voice}"
    }
    if (['say', 'play', 'sayall', 'playall'].contains(action)) {
    	if (settings.volume) {
            if (relativeVolume) {
        		command += "/+${settings.volume}"
            } else {
            	command += "/${settings.volume}"
            }
        }
    }
    command = command.replaceAll(' ', '%20')
    log.info "Sending Sonos Command: \"${command}\""
    // Create the hubAction request based on updated preferences
    def hubAction = new physicalgraph.device.HubAction(
            method: "GET",
            path: command,
            headers: [ HOST: "${sonosServerIP}:${sonosServerPort}", contenttype: 'application.json; charset="utf-8"' ],
            // query: null, // ['template': "{\"timestamp\":${now()}," + state.meteoTemplate, 'contenttype': 'application/json;charset=utf-8' ],
            null,
            [callback: sonosCommandCallback]
        )
    try {
        sendHubCommand(hubAction)
    } catch (Exception e) {
    	log.error "sendHubCommand Exception $e on $hubAction"
        state.sonosCommand = null
    }
}

// Handle the hubAction response
def sonosCommandCallback(physicalgraph.device.HubResponse hubResponse) {
	log.info "sonosCommandCallback() status: " + hubResponse.status
    if (hubResponse.status == 200) {
    	if (hubResponse.json) state.sonosCommand = hubResponse.json
        return true
    } else {
    	state.sonosCommand = null
        return false
    }
}

def getRandomItem(List items) {    
    return items?.get(new Random().nextInt(items?.size()));
}

List whozitArrived() {
	return ["I think it is", "It could be", "it is probably", "I am pretty sure that it is", "My sensors say it is", "All evidence indicates that it is", "It's", 
    		"No worries it's just", "Oh, it's only", "As far as I can tell it is" ]
}
List whozitDeparted() {
	return ["I think it was", "It was probably", "Oh I know! It was", "It was apparently", "I am pretty sure it was", "My educated guess is that it was",
    		"All indications are that it was", "Oh it was"]
}
List iDunnoArrived() {
	return ["An unknown vehicle just arrived at ${location.name}.", "An unknown vehicle is coming up the driveway at ${location.name}.", 
    		"Somebody just drove into the driveway at ${location.name}, but I don't know who it is.", "A vehicle just arrived at ${location.name}, but I can't tell who is driving.",
    		"I'm baffled, someone just drove up the driveway at ${location.name}, but I don't know who.", "Someone is in the driveway at ${location.name}.", 
            "WARNING! , unknown vehicle has been detected in the driveway at ${location.name}.", "We have unknown visitors in the driveway at ${location.name}."]
}
List iDunnoDeparted() {
	return ["An unknown vehicle just left ${location.name}.", "An unknown vehicle just departed from ${location.name}.", "Somebody just left ${location.name}.", 
    		"Sombody just departed ${location.name}.", "I don't know who, but somebody just left ${location.name}.", "I don't know who just departed from ${location.name}.", 
            "It's a mystery to me. I can't figure out who just left ${location.name}.", "Somebody just left ${location.name}, I hope they enjoyed their stay!"]
}
List youGotMail() {
	return [ "You have mail!", "You got mail!", "I think some mail has just been delivered to ${location.name}.", "Somebody just opened the mailbox at ${location.name}", 
    		 "Looks like somebody at ${location.name} is getting some mail today!", "Guess what? I think you just got mail!" ]
}
List knownWalker() {
	return [ "There is someone coming up ${location.name}'s front walkway, I think it might be ", "Somebody is on the walkway at ${location.name}, it could be ",
             "My sensors detect that somebody is on the front walkway at ${location.name}, it is probably " ]
}
List unknownWalker() {
	return [ "There's somebody on the front walkway, but I don't know who it is.", "Somebody is on the walkway at ${location.name}.", 
    		 "I think someone is coming up the front walkway at ${location.name}.", "An unknown visitor is on the front walkway at ${location.name}." ]
}
List residentLeaving() {
	return [ "Somebody is leaving ${location.name} via the walkway.", "Someone just left ${location.name} down the front walkway",
    		 "Whoever just exited ${location.name} is walking down the walkway."] 
}
List unknownLeaving() {
	return [ "Somebody just left ${location.name}", "Someone is leaving ${location.name}"]
}
