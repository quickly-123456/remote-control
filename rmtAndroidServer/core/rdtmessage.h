#ifndef RDTMESSAGE_H
#define RDTMESSAGE_H

#include <QByteArray>
#include <QString>

class RDTMessage
{
public:
    RDTMessage();
    RDTMessage(const QByteArray & data);

    // Overloaded << operators for writing data
    RDTMessage& operator<<(const QString &msg);
    RDTMessage& operator<<(const QByteArray &msg);
    RDTMessage& operator<<(int value);
    RDTMessage& operator<<(float value);
    RDTMessage& fromRawData(const unsigned char *msg, int length);

    // Overloaded >> operators for reading data
    RDTMessage& operator>>(QString &msg);
    RDTMessage& operator>>(QByteArray &msg);
    RDTMessage& operator>>(int &value);
    RDTMessage& operator>>(float &value);
    RDTMessage& toRawData(unsigned char *msg, int * length = NULL);

    // Optional: Clear the data
    void clear();

    // Optional: Get raw data
    const QByteArray& data() const;

    //static  void    test();

private:
    QByteArray  _data;
    int         _offset;
};

#endif // RDTMESSAGE_H
