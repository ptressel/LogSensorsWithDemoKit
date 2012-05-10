#include <avrpins.h>
#include <max3421e.h>
#include <usbhost.h>
#include <usb_ch9.h>
#include <Usb.h>
#include <usbhub.h>
#include <avr/pgmspace.h>
#include <address.h>
#include <adk.h>
#include <string.h>

// Set true to have debugmsg send messages via Serial.
#define DEBUG 1
// Set true to echo back any incoming messages. Use this to test if your
// messages got to the board.
#define ECHO 1
// Max incoming messages to read before sending a mock sensor message.
// Want to keep the incoming queue from filling, but also want to be sure
// we send mock sensor messages eventually.
#define MAX_IN_PER_LOOP 10
// Pause for this many milliseconds between checks for a live usb connection.
#define CONNECT_WAIT 1000
// Normal return codes from RcvData -- anything else is an error:
#define SUCCESS 0x00
#define NAK 0x04  // no data available
// Longest expected message on input, and longest mock message we'll send.
// If a longer message is received, it will get split up before it's sent
// back. Outgoing messages will just get truncated. If longer mock messages
// are needed, increase this. @ToDo: Find out max USB packet length.
#define BUF_LEN 100
uint8_t buffer[BUF_LEN + 1] = {0};

// Each fake sensor message is paired with a pause to follow the message.
// The pauses allow for approximate testing of timestamping on the receiving end.
// The pause time is specified in milliseconds.
// The messages are char* for convenience -- allows using literal strings.
// But SndData will want uint8_t*.
// Do not try to typedef this.
// http://softsolder.com/2009/10/06/arduino-be-careful-with-the-preprocessor/
struct message_and_pause {
  char* message;
  int pause;
};

// Collection of messages to send, in rotation. Quoted literals will be
// null-terminated by the compiler.
struct message_and_pause mock_messages[] = {
  {"XYZZY", 30},
  {"PLUGH", 60},
  {"KILLDRAGON", 90}
};
int num_mock_messages = sizeof(mock_messages) / sizeof(message_and_pause);
int which_message = 0;

USB Usb;
USBHub hub0(&Usb);
USBHub hub1(&Usb);
ADK adk(&Usb,
        "Jigsaw Renaissance Robo-Magellan Team",
        "LogSensors",
        "Test mock for sensors connected to the Arduino Mega ADK board.",
        "0.1",
        "http://www.jigsawrenaissance.org",
        "0000000027182818");

void debugmsg(char* msg, boolean ln)
{
  if (DEBUG) {
    if (ln) {
      Serial.println(msg);
    } else {
      Serial.print(msg);
    }
  }
}

void debugmsg(char* msg) {
  debugmsg(msg, true);
}

// So far, numbers are at the end of debug messages.
void debugmsg(int num) {
  if (DEBUG) Serial.println(num);
}

void setup()
{
  Serial.begin(115200);
  debugmsg("\r\nStarting MockSensors.");
  if (Usb.Init() == -1) {
    Serial.println("OSCOKIRQ failed to assert");
    // Spin. @ToDo: Is there a better way to halt? This is a "halt" that
    // draws power.
    while(1);
  }
}

// On each pass, read incoming messages and optionally echo them
// back unchanged. Then send the next predefined message. Wait for
// the predefined interval paired with that message.
void loop()
{
  debugmsg("Before Usb.Task");
  Usb.Task();
  debugmsg("After Usb.Task");

  if (adk.isReady() == false) {
    debugmsg("adk.isReady is false");
    delay(CONNECT_WAIT);
    return;
  } else {
    debugmsg("adk.isReady is true");
    uint16_t len;
    uint8_t rcode;
    uint8_t* bufptr;
    
    for (int i = 0; i < MAX_IN_PER_LOOP; i++) {
      len = BUF_LEN;
      //bufptr = buffer;
      //rcode = adk.RcvData(&len, bufptr);
      debugmsg("Before adk.RcvData");
      rcode = adk.RcvData(&len, buffer);
      debugmsg("adk.RcvData returned ", false);
      debugmsg((int)rcode);
      if (rcode == NAK || len == 0) break;
      if (rcode) {
        // Got a real error.
        Serial.print("RcvData returned error ");
        Serial.println(rcode);
      }
      if (ECHO) {
        // Null terminate just in case it isn't already. Ignore
        // outgoing errors for now.
        buffer[len] = 0;
        rcode = adk.SndData(len, buffer);
      }
    }
    
    // Send the next mock sensor message.
    char* message = mock_messages[which_message].message;
    bufptr = (uint8_t*) message;
    len = strlen(message);
    debugmsg("About to send: ");
    debugmsg(message);
    debugmsg("Length is ", false);
    debugmsg((int)len);
    rcode = adk.SndData(len, bufptr);
    // @ToDo: Check return code and retransmit if needed.
    
    // Pause for the specified time.
    delay(mock_messages[which_message].pause);
    
    // Next!
    which_message = (which_message + 1) % num_mock_messages;
  }
}

