#ifndef RDTSERVER_H
#define RDTSERVER_H

#include <QObject>
#include <QMap>

class QWebSocketServer;

class Channel;
class VueSocket;

class RDTServer : public QObject
{
    Q_OBJECT
public:
    explicit RDTServer(QObject *parent = nullptr);

signals:

private slots:
    void                        onNewConnection();

    void                        onBinaryMessage(const QByteArray &);
    void                        onDisconnected();

private:
    void                        loadChannel();

    Channel *                   createChannel(const QString & super_id);

    QWebSocketServer *          _server;

    QMap<QString, Channel *>    _channels;
    QMap<QString, VueSocket *>  _vues;
};

#endif // RDTSERVER_H
