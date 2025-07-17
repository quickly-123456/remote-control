#ifndef RDTSERVER_H
#define RDTSERVER_H

#include <QObject>
#include <QMap>

class QWebSocketServer;
class QWebSocket;

class Channel;
class UserSocket;
class User;

class RDTServer : public QObject
{
    Q_OBJECT
public:
    enum SocketOwnerInfo
    {
        SocketOwnerUnknown = -1,
        SocketOwnerUser,
        SocketOwnerVue,
        SocketOwnerAndroidAdmin
    };

    explicit RDTServer(QObject *parent = nullptr);

    static RDTServer *          instance();

    void                        checkChannelAndUser(const User & user);

signals:

private slots:
    void                        onNewConnection();

    void                        onBinaryMessage(const QByteArray &);
    void                        onDisconnected();

private:
    void                        loadChannel();

    Channel *                   createChannel(const QString & super_id);

    SocketOwnerInfo             getSocketOwner(QWebSocket *socket, User *user);
    void                        setSocketOwner(QWebSocket *socket, const User & user, SocketOwnerInfo owner = SocketOwnerUser);

    static RDTServer *          _instance;

    QWebSocketServer *          _server;

    QMap<QString, Channel *>    _channels;
};

#endif // RDTSERVER_H
