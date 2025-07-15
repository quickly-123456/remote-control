#include "usersocket.h"

#include <QWebSocket>

#include "rdtmessage.h"

UserSocket::UserSocket(const User & user, QObject *parent)
    : QObject{parent}
{
    _user = user;
    _socket = NULL;
}

QWebSocket * UserSocket::socket() const
{
    return _socket;
}

void UserSocket::setSocket(QWebSocket * socket)
{
    _socket = socket;
}

const User & UserSocket::user() const
{
    return _user;
}

bool UserSocket::isConnected() const
{
    if (!_socket)
        return false;
    return _socket->isValid();
}

void UserSocket::send(RDTMessage & msg)
{
    if (!_socket)
        return;

    _socket->sendBinaryMessage(msg.data());
}
