#include "RDTMessage.h"

RDTMessage::RDTMessage() : _offset(0) {} // Initialize offset

RDTMessage::RDTMessage(const QByteArray & data) : _data(data), _offset(0) {}


RDTMessage& RDTMessage::operator<<(const QString &msg) {
    QByteArray byteArray = msg.toUtf8();
    int size = byteArray.size();
    _data.append(reinterpret_cast<const char*>(&size), sizeof(size));
    _data.append(byteArray);
    return *this;
}

RDTMessage& RDTMessage::operator<<(const QByteArray &msg) {
    int size = msg.size();
    _data.append(reinterpret_cast<const char*>(&size), sizeof(size));
    _data.append(msg);
    return *this;
}

RDTMessage& RDTMessage::operator<<(int value) {
    _data.append(reinterpret_cast<const char*>(&value), sizeof(value));
    return *this;
}

RDTMessage& RDTMessage::operator<<(float value) {
    _data.append(reinterpret_cast<const char*>(&value), sizeof(value));
    return *this;
}

RDTMessage& RDTMessage::fromRawData(const unsigned char *msg, int length) {
    if (length > 0) {
        _data.append(reinterpret_cast<const char*>(&length), sizeof(length));
        _data.append(reinterpret_cast<const char*>(msg), length);
    }
    return *this;
}

 RDTMessage& RDTMessage::operator>>(QString &msg) {
    if (_data.size() < (unsigned int)(_offset + sizeof(int))) return *this; // Ensure there's enough data
    int size = *reinterpret_cast<const int*>(_data.constData() + _offset); // Read the size as 4 bytes
    _offset += sizeof(int); // Update offset for string data
    if (size > 0 && size <= (_data.size() - _offset)) {
        msg = QString::fromUtf8(_data.constData() + _offset, size);
        _offset += size; // Update offset after reading the string
    }
    return *this;
}

RDTMessage& RDTMessage::operator>>(QByteArray &msg){
    if (_data.size() < (unsigned int)(_offset + sizeof(int))) return *this; // Ensure there's enough data
    int size = *reinterpret_cast<const int*>(_data.constData() + _offset);
    _offset += sizeof(int); // Update offset for byte array data
    if (size > 0 && size <= (_data.size() - _offset)) {
        msg = _data.mid(_offset, size);
        _offset += size; // Update offset after reading the byte array
    }
    return *this;
}

RDTMessage& RDTMessage::operator>>(int &value){
    if (_data.size() >= (unsigned int)(_offset + sizeof(value))) {
        value = *reinterpret_cast<const int*>(_data.constData() + _offset);
        _offset += sizeof(value); // Update offset after reading int
    }
    return *this;
}

RDTMessage& RDTMessage::operator>>(float &value){
    if (_data.size() >= (unsigned int)(_offset + sizeof(value))) {
        value = *reinterpret_cast<const float*>(_data.constData() + _offset);
        _offset += sizeof(value); // Update offset after reading float
    }
    return *this;
}

RDTMessage& RDTMessage::toRawData(unsigned char *msg, int *length) {
    if (_data.size() < (unsigned int)(_offset + sizeof(int))) return *this; // Ensure there's enough data
    int size = *reinterpret_cast<const int*>(_data.constData() + _offset);
    if (length)
        *length = size;
    _offset += sizeof(int); // Update offset for byte array data
    if (size > 0 && size <= (_data.size() - _offset)) {
        memcpy(msg, _data.constData() + _offset, size);
        _offset += size; // Update offset after reading the byte array
    }
    return *this;
}

void RDTMessage::clear() {
    _data.clear();
    _offset = 0; // Reset offset when clearing
}

const QByteArray& RDTMessage::data() const {
    return _data;
}

/*
#include <QDebug>
void RDTMessage::test() {
    RDTMessage message;

    // Test writing and reading
    message << QString("Hello, World!") << 123 << 45.67f;

    QString receivedStr;
    int intValue;
    float floatValue;

    message >> receivedStr >> intValue >> floatValue;

    qDebug() << "String:" << receivedStr; // Should output: "Hello, World!"
    qDebug() << "Int:" << intValue;       // Should output: 123
    qDebug() << "Float:" << floatValue;   // Should output: 45.67

    // Test raw data handling
    unsigned char rawData[] = { 0x00, 0x00, 0x00, 0x03, 'A', 'B', 'C' }; // 3 bytes of data
    message.fromRawData(rawData, sizeof(rawData));

    unsigned char szRawData[256] = {0};
    int len = 0;
    message.toRawData(szRawData, &len);

    qDebug() << len;
}
*/
