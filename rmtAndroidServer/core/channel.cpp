#include "channel.h"

#include "usersocket.h"
#include "rdtmessage.h"
#include "vuesocket.h"
#include "rdtDefine.h"
#include "logger.h"

#include <QDateTime>

Channel::Channel(const QString & administrator, QObject *parent)
    : QObject{parent}
{
    _admin = administrator;

    _vueSocketWeb = new VueSocket(_admin);
    _vueSocketMobile = new VueSocket(_admin);
}

UserSocket * Channel::createUserSocket(const User & user)
{
    UserSocket *userSocket = new UserSocket(user);
    _userSockets[user.phone()] = userSocket;

    Logger::info() << _admin << "channel added user :" << user.phone();

    return userSocket;
}

int Channel::count() const
{
    return _userSockets.size();
}

UserSocket * Channel::findSocket(const QString & phone)
{
    return _userSockets.value(phone);
}

VueSocket * Channel::vueSocketWeb() const
{
    return _vueSocketWeb;
}

VueSocket * Channel::vueSocketMobile() const
{
    return _vueSocketMobile;
}

void Channel::toMessage(RDTMessage & msg)
{
    msg << (int)_userSockets.size();
    for (QMap<QString, UserSocket *>::iterator it = _userSockets.begin(); it != _userSockets.end(); ++it) {
        QString key = it.key();
        UserSocket* userSocket = it.value();
        msg << key << (userSocket->socket() ? 1 : 0);
    }
}

void Channel::connected(QWebSocket *socket, const QString & phone)
{
    UserSocket *userSocket = _userSockets.value(phone);
    if (!userSocket)
        return;

    userSocket->setSocket(socket);

    RDTMessage userMessage;
    userMessage << SC_USER;
    userSocket->send(userMessage);

    RDTMessage vueMessage;
    vueMessage << SC_USER << phone;
    sendToVue(vueMessage);
}

void Channel::disconnected(const QString & phone)
{
    UserSocket *userSocket = _userSockets.value(phone);
    if (userSocket)
        userSocket->setSocket(NULL);

    RDTMessage vueMessage;
    vueMessage << SC_USER_DISCONNECT << phone;
    sendToVue(vueMessage);

    Logger::info() << _admin << "channel left user" << phone;
}

int Channel::receivedImageFrom(const QString & phone, RDTMessage & message)
{
    int timeStamp;
    QByteArray compScreenData;
    message >>  timeStamp >> compScreenData;

    int curtimemsec = QDateTime::currentMSecsSinceEpoch();
    int diff = curtimemsec - timeStamp;
    RDTMessage userMessage;
    userMessage << SC_SCREEN << diff;
    UserSocket *userSocket = _userSockets.value(phone);
    if (userSocket)
        userSocket->send(userMessage);

    RDTMessage vueMessage;
    vueMessage << SC_SCREEN << phone << compScreenData;
    sendToVue(vueMessage);

    return compScreenData.size();
}

void Channel::connectedWebVue(QWebSocket *socket)
{
    _vueSocketWeb->setSocket(socket);

    RDTMessage vueMessage;
    vueMessage << SC_VUE;
    toMessage(vueMessage);
    _vueSocketWeb->send(vueMessage);
}

void Channel::disconnectedWebVue()
{
    _vueSocketWeb->setSocket(NULL);
}

void Channel::sendToVue(RDTMessage & message)
{
    _vueSocketWeb->send(message);
    _vueSocketMobile->send(message);
}

