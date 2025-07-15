#ifndef CHANEL_H
#define CHANEL_H

#include <QObject>
#include <QMap>

class UserSocket;
class User;
class RDTMessage;

class Channel : public QObject
{
    Q_OBJECT
public:
    explicit Channel(const QString & administrator, QObject *parent = nullptr);

    UserSocket *    createUserSocket(const User &);

    int             count() const;

    UserSocket *    findSocket(const QString & phone);

    void            toMessage(RDTMessage & message);

signals:

private:
    QString                     _admin;
    QMap<QString, UserSocket *> _userSockets;
};

#endif // CHANEL_H
