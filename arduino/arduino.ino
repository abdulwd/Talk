#include <Adafruit_SSD1306.h>
#include <SoftwareSerial.h>
#include <SPI.h>
#include <Wire.h>
#include <Adafruit_GFX.h>

// If using software SPI (the default case):
#define OLED_MOSI  11   //D1
#define OLED_CLK   12   //D0
#define OLED_DC    9
#define OLED_CS    8
#define OLED_RESET 10

Adafruit_SSD1306 display(OLED_MOSI, OLED_CLK, OLED_DC, OLED_RESET, OLED_CS);
boolean readFlag = false;
String inputString = "                                                                                    ";
int index = 0;
SoftwareSerial S1(5, 6); //RX,TX

void setup() { // run once, when the sketch starts
  Serial.begin(9600);
  S1.begin(9600);
  Serial.println("Bluetooth On please press 1 or 0 blink LED ..");
  display.begin(SSD1306_SWITCHCAPVCC);
  display.display();
  delay(1000);
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(WHITE);
  display.ssd1306_command( ((0xc8) >> 8) | 0xc0 ); // Used to rotate characters
}

void loop() {
  display.setCursor(0, 0);
  while (S1.available() > 0) {
    char ch = S1.read(); //read the input
    inputString[index++] = ch;
    Serial.print(inputString);
    readFlag = true;
  }

  if (readFlag) {
    display.clearDisplay();
    display.println(inputString);
    display.display();
    Serial.println(inputString);
    readFlag = false;
    index = 0;
    inputString = "                                                                                    ";
  }
}
