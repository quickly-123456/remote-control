#include "channel.h"

#include "usersocket.h"
#include "rdtmessage.h"

Channel::Channel(const QString & administrator, QObject *parent)
    : QObject{parent}
{
    _admin = administrator;
}

UserSocket * Channel::createUserSocket(const User & user)
{
    UserSocket *userSocket = new UserSocket(user);
    _userSockets[user.phone()] = userSocket;
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

void Channel::toMessage(RDTMessage & msg)
{
    msg << (int)_userSockets.size();
    for (QMap<QString, UserSocket *>::iterator it = _userSockets.begin(); it != _userSockets.end(); ++it) {
        QString key = it.key();
        UserSocket* userSocket = it.value();
        msg << userSocket->user().phone();
    }
}

