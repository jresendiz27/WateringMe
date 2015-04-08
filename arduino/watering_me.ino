#include <SPI.h>
#include <Servo.h> 
#include <Ethernet.h>
#include <EthernetClient.h>
// Ethernet Configuration
byte mac[] = { 
  0x54, 0x34, 0x41, 0x30, 0x30, 0x31 };                                      
EthernetClient client;
char server[] = "8.40.1.71";
// Servo configuration
Servo domeServo;
// Arduino pin configuration
int waterPumpPin = 7;
int servoMotorPin = 8;
int sensorPin = A0;
// Constant values
int WATERING_TIME = 30000; // 7 seconds with water pump
int REQUEST_INTERVAL = 2000; // http request each minute
int BRIGHTNESS_CONSTANT = 80; // Using photo resistor!
int SERVO_INITIAL_POSITION = 0; // Initial position of the servo
int SERVO_FINAL_POSITION = 85; // Final position of the servo
int MAX_ATTEMPTS = 20; // If there's no internet 
int ATTEMPT_INTERVAL = 2000; // Request attempt every k seconds
int LOADING_DELAY = 10000;
// Auxiliar variables
// TODO change String response and replace it with Char array!
String response = 0;
int sensorValue = 0;
int domeIsOpen = 0;
int pos = 0;
int attemptCounter = 0;
// Setting up the arduino board.
void setup() {
  Ethernet.begin(mac);
  Serial.begin(9600);
  pinMode(waterPumpPin,OUTPUT);
  domeServo.attach(servoMotorPin);
  domeServo.write(0);
  delay(LOADING_DELAY);
  Serial.println(Ethernet.localIP());
  Serial.println("Let's start!");
}

int watering(){
  Serial.println("Watering");
  attemptCounter = 0;
  while(1){
    response="";
    if (client.connect(server, 80)) {
      Serial.println("-> Connected Watering");
      client.print("GET /start");
      client.println(" HTTP/1.1");
      client.print("Host: " );
      client.println(server);
      client.println("Connection: close" );
      client.println();
      client.println();
    }
    if (client.available()) {
      while(client.available()){
        char c = client.read();
        response.concat(c);
      }
      if(response.indexOf("riegame") != -1){
        digitalWrite(waterPumpPin,HIGH);
        delay(WATERING_TIME);
        digitalWrite(waterPumpPin,LOW);
        client.flush();
        client.stop();
        tweetInfo(1); // Watering the plant!
        break;
      }
      if(response.indexOf("no_me_riegues") != -1){
        client.flush();
        client.stop();
        break;
      }
    }
    attemptCounter += 1;
    if(attemptCounter > MAX_ATTEMPTS){
      Serial.println("Limite peticiones alcanzado [Watering]");
      client.stop();
      break;
    }
    Serial.println(attemptCounter);
    delay(ATTEMPT_INTERVAL);
  }
  response = "";
  return 1;
}

int openDome(){
  for(pos = SERVO_INITIAL_POSITION; pos < SERVO_FINAL_POSITION; pos += 1){
    domeServo.write(pos);
    delay(40);
  }
  domeIsOpen = 1;
  tweetInfo(2); //Opening dome
  return 1;
}

int closeDome(){
  for(pos = SERVO_FINAL_POSITION; pos >= SERVO_INITIAL_POSITION; pos -= 1){
    domeServo.write(pos);
    delay(40);
  }
  domeIsOpen = 0;
  tweetInfo(3); //Closing dome
  return 1;
}

void domeAction(){
  sensorValue = analogRead(sensorPin);
  Serial.println("Valor Sensor:");
  Serial.println(sensorValue);
  if(sensorValue >= BRIGHTNESS_CONSTANT && !domeIsOpen){
    openDome();
  } 
  if(sensorValue < BRIGHTNESS_CONSTANT && domeIsOpen) {
    closeDome();   
  }
}
/*
1: Watering plant
 2: Open Dome
 3: Close Dome
 */
void tweetInfo(int status){
  Serial.println("Tweet status");
  Serial.println(status);
  attemptCounter = 0;
  while(1){
    if (client.connect(server, 80)) {
      Serial.println("-> Connected Twitter");
      client.print("GET /tweet/");
      client.print(status);
      client.println(" HTTP/1.1");
      client.print("Host: " );
      client.println(server);
      client.println("Connection: close" );
      client.println();
      client.println();
    }
    attemptCounter += 1;
    if(client.available()){
      client.flush();
      client.stop();
      break;
    }
    if(attemptCounter > MAX_ATTEMPTS){
      Serial.println("Limite peticiones alcanzado [Tweeting]");
      client.stop();
      break;
    }
    Serial.println(attemptCounter);
    delay(ATTEMPT_INTERVAL);    
  }
  response = "";
}

void loop() {
  watering();
  domeAction();
  delay(REQUEST_INTERVAL);
}
