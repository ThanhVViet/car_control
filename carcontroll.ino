#define in1 5 // Chân điều khiển động cơ L298n
#define in2 6
#define in3 10
#define in4 11
#define LED 13
const int trig = 8;     // Chân trig của HC-SR04
const int echo = 7;     // Chân echo của HC-SR04
char command;           // Biến để lưu trạng thái lệnh từ ứng dụng
int Speed = 204;        // 0 - 255, tốc độ động cơ
int distance_const = 20; // Khoảng cách an toàn
unsigned long lastCommandTime = 0; // Thời gian của lệnh cuối cùng

int CalculateDistance() {
    unsigned long duration; // Biến đo thời gian
    int distance;           // Biến lưu khoảng cách

    // Phát xung từ chân trig
    digitalWrite(trig, LOW);
    delayMicroseconds(2);
    digitalWrite(trig, HIGH);
    delayMicroseconds(10);  // Độ dài xung là 10 micro giây
    digitalWrite(trig, LOW);
    
    // Đo độ rộng xung HIGH ở chân echo
    duration = pulseIn(echo, HIGH);
    // Tính khoảng cách
    distance = int(duration / 2 / 29.412);
    return distance;
}

void setup() {
    pinMode(in1, OUTPUT);
    pinMode(in2, OUTPUT);
    pinMode(in3, OUTPUT);
    pinMode(in4, OUTPUT);
    pinMode(trig, OUTPUT);   // Chân trig sẽ phát tín hiệu
    pinMode(echo, INPUT);    // Chân echo sẽ nhận tín hiệu
    pinMode(LED, OUTPUT);    // Thiết lập chân LED
    Serial.begin(9600);      // Thiết lập tốc độ truyền cho module Bluetooth
  
}

void forward() {
    digitalWrite(in2, LOW);  // Đảm bảo chân lùi ở mức LOW
    digitalWrite(in4, LOW);  // Đảm bảo chân lùi ở mức LOW
    analogWrite(in1, Speed);
    analogWrite(in3, Speed);
}

void back() {
    digitalWrite(in1, LOW);  // Đảm bảo chân tiến ở mức LOW
    digitalWrite(in3, LOW);  // Đảm bảo chân tiến ở mức LOW
    analogWrite(in2, Speed);
    analogWrite(in4, Speed);
}

void left() {
    digitalWrite(in2, LOW);  // Đảm bảo chân lùi ở mức LOW
    digitalWrite(in3, LOW);  // Đảm bảo chân tiến ở mức LOW
    analogWrite(in4, Speed);
    analogWrite(in1, Speed);
}

void right() {
    digitalWrite(in1, LOW);  // Đảm bảo chân tiến ở mức LOW
    digitalWrite(in4, LOW);  // Đảm bảo chân lùi ở mức LOW
    analogWrite(in3, Speed);
    analogWrite(in2, Speed);
}

void stop() {
    analogWrite(in1, 0);
    analogWrite(in2, 0);
    analogWrite(in3, 0);
    analogWrite(in4, 0);
}

void loop() {
    int distance = CalculateDistance();
    Serial.println(distance);

    if (distance <= distance_const) {
        stop();
    }

    if (Serial.available() > 0) {
        command = Serial.read();
        lastCommandTime = millis();
        stop(); // Khởi tạo với động cơ dừn
    
        switch (command) {
            case 'F':
                if (distance > distance_const) forward();
                break;
            case 'B':
                back();
                delay(50);
                break;
            case 'L':
                if (distance > distance_const) left();
                break;
            case 'R':
                if (distance > distance_const) right();
                break;
            case 'S':
                stop();
                break;
            default:
                break;
        }
    }

    // // Dừng xe nếu không nhận được lệnh trong vòng 2 giây
    // if (millis() - lastCommandTime > 1000 && (command=='R'|command =='L')) {
    //     stop();
    // }

    delay(100);  // Giảm delay để tăng khả năng phản hồi
}
