#ifndef USERSOCKET_H
#define USERSOCKET_H

#include <QObject>

#include "user.h"

class QWebSocket;
class RDTMessage;

class UserSocket : public QObject
{
    Q_OBJECT
public:
    explicit UserSocket(const User & user, QObject *parent = nullptr);

    QWebSocket *    socket() const;
    void            setSocket(QWebSocket * socket);

    const User &    user() const;

    bool            isConnected() const;

    void            send(RDTMessage & msg);

signals:

private:
    QWebSocket *    _socket;

    User            _user;
};

#endif // USERSOCKET_H
