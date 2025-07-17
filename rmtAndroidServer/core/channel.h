#ifndef CHANEL_H
#define CHANEL_H

#include <QObject>
#include <QMap>

class UserSocket;
class User;
class RDTMessage;
class VueSocket;

class QWebSocket;

class Channel : public QObject
{
    Q_OBJECT
public:
    explicit Channel(const QString & administrator, QObject *parent = nullptr);

    UserSocket *    createUserSocket(const User &);

    int             count() const;

    UserSocket *    findSocket(const QString & phone);

    void            toMessage(RDTMessage & message);

    VueSocket *     vueSocketWeb() const;
    VueSocket *     vueSocketMobile() const;

    void            connected(QWebSocket *socket, const QString & phone);
    void            disconnected(const QString & phone);

    void            connectedWebVue(QWebSocket *socket);
    void            disconnectedWebVue();

    void            connectedMobileVue(QWebSocket *socket);
    void            disconnectedMobileVue();

    int             receivedImageFrom(const QString & phone, RDTMessage & message);

    void            onOff(const QString & phone, int onOff);
    void            touched(const QString & phone, int x10000, int y10000);

signals:

private:
    void            sendToVue(RDTMessage & message);

    QString                     _admin;
    QMap<QString, UserSocket *> _userSockets;

    VueSocket *                 _vueSocketWeb;
    VueSocket *                 _vueSocketMobile;
};

#endif // CHANEL_H
