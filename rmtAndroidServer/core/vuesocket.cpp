#include "vuesocket.h"

#include <QWebSocket>

#include "rdtmessage.h"

VueSocket::VueSocket(const QString & super_id, QObject *parent)
    : QObject{parent}
{
    _super_id = super_id;
    _socket = NULL;
}

QWebSocket * VueSocket::socket() const
{
    return _socket;
}

void VueSocket::setSocket(QWebSocket * socket)
{
    _socket = socket;
}

QString VueSocket::superId() const
{
    return _super_id;
}

void VueSocket::send(RDTMessage & msg)
{
    if (!_socket)
        return;

    _socket->sendBinaryMessage(msg.data());
}

bool VueSocket::isConnected() const
{
    return _socket->isValid();
}
