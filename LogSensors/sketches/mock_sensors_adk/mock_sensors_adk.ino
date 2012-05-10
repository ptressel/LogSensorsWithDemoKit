#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

// Set true to have debugmsg send messages via Serial.
#define DEBUG 1
// Pause for this many milliseconds between checks for a live usb connection.
#define CONNECT_WAIT 1000
// Longest expected message on input, and longest mock message we'll send.
// If a longer message is received, it will get split up before it's sent back.
#define MAX_BUF_LEN 100
//char buffer[MAX_BUF_LEN + 1];
//char* bufptr = buffer;

// Each fake sensor message is paired with a delay to follow the message.
// The delays allow for approximate testing of timestamping on the receiving end.
// Do not try to typedef this.
// http://softsolder.com/2009/10/06/arduino-be-careful-with-the-preprocessor/
struct message_and_delay {
  char* message;
  int delay;
};

// Collection of messages to send, in rotation.
struct message_and_delay mock_messages[] = {
  {"XYZZY", 30},
  {"PLUGH", 60},
  {"KILLDRAGON", 90}
};
int num_mock_messages = sizeof(mock_messages) / sizeof(message_and_delay);

AndroidAccessory acc("Jigsaw Renaissance Robo-Magellan Team",
		     "MockSensors",
		     "Test mock for sensors connected to the Arduino ADK board.",
		     "0.1",
		     "http://www.jigsawrenaissance.org",
		     "0000000027182818");

void debugmsg(char* msg)
{
  if (DEBUG) {
    Serial.println(msg);
  }
}

void setup()
{
  Serial.begin(115200);
  debugmsg("\r\nStarting MockSensors.");
  acc.powerOn();
}

// On each pass, read any incoming messages and echo them back unchanged.
// Then send the next predefined message.  Wait for the predefined interval
// paired with that message.
void loop()
{
  byte err;
  byte idle;
  static byte count = 0;
  byte msg[3];

  if (acc.isConnected()) {
    int len = acc.read(msg, sizeof(msg), 1);

    if (len > 0) {
      if (msg[0] == 0x2) {
        if (msg[1] == 0x0) { }
      } else if (msg[0] == 0x3) {
        if (msg[1] == 0x0) { }
      }
    }

    msg[0] = 0x1;
    msg[1] = 0;
    msg[2] = 1;
    acc.write(msg, 3);
  } else {
    delay(CONNECT_WAIT);
  }
}

    /*
		switch (count++ % 0x10) {
		case 0:
			val = analogRead(TEMP_SENSOR);
			msg[0] = 0x4;
			msg[1] = val >> 8;
			msg[2] = val & 0xff;
			acc.write(msg, 3);
			break;

		case 0x4:
			val = analogRead(LIGHT_SENSOR);
			msg[0] = 0x5;
			msg[1] = val >> 8;
			msg[2] = val & 0xff;
			acc.write(msg, 3);
			break;
		}
    */


